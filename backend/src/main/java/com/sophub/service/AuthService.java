package com.sophub.service;

import com.sophub.config.JwtService;
import com.sophub.model.LoginResponse;
import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.RolleRepository;
import com.sophub.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Registrierung, E-Mail-Bestaetigung und Login.
 * <ul>
 *     <li>Registrierung nur mit E-Mail-Adresse und Passwort. Vorname, Nachname und der
 *         interne Benutzername werden aus der E-Mail-Adresse abgeleitet
 *         (z.B. lena.test@stud.hs-bochum.de -> "Lena" "Test", Benutzername "lena.test").</li>
 *     <li>Nach der Registrierung muss die E-Mail-Adresse ueber einen Link bestaetigt werden.</li>
 *     <li>Login mit E-Mail-Adresse und Passwort.</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final String DOMAIN_PROFESSOR = "@hs-bochum.de";

    // So lange ist ein Bestaetigungslink gueltig
    private static final int TOKEN_GUELTIG_STUNDEN = 24;

    private static final int PASSWORT_MINDESTLAENGE = 8;

    private static final String HINWEIS_BESTAETIGUNG =
            "Fast geschafft! Wir haben Ihnen eine E-Mail mit einem Bestätigungslink geschickt. "
            + "Bitte klicken Sie auf den Link, danach können Sie sich mit Ihrer E-Mail-Adresse anmelden.";

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, RolleRepository rolleRepository,
                       BCryptPasswordEncoder passwordEncoder, JwtService jwtService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public String register(String email, String passwort) {
        String bereinigteEmail = bereinigeEmail(email);
        if (passwort == null || passwort.length() < PASSWORT_MINDESTLAENGE) {
            throw new RuntimeException("Das Passwort muss mindestens " + PASSWORT_MINDESTLAENGE + " Zeichen lang sein.");
        }

        Optional<User> vorhandenesKonto = userRepository.findByEmail(bereinigteEmail);

        if (vorhandenesKonto.isPresent()) {
            User konto = vorhandenesKonto.get();

            // Fertiges Konto (aktiviert UND bestaetigt) darf nicht erneut registriert (= uebernommen) werden
            if (konto.istKontoAktiviert() && konto.istEmailBestaetigt()) {
                throw new RuntimeException("Diese E-Mail-Adresse ist bereits registriert.");
            }

            // Vorbereitetes Konto (z.B. Betreuer) ODER Registrierung, die noch nicht bestaetigt wurde:
            // Passwort setzen, vorhandene Namen behalten und einen NEUEN Bestaetigungslink schicken.
            // Freigeschaltet wird das Konto erst, wenn der Link angeklickt wurde.
            konto.setPasswort(passwordEncoder.encode(passwort));
            ergaenzeNamen(konto, bereinigteEmail);
            if (konto.getBenutzername() == null || konto.getBenutzername().isBlank()) {
                konto.setBenutzername(erzeugeBenutzername(bereinigteEmail));
            }
            String token = neuerBestaetigungsToken(konto);
            userRepository.save(konto);

            emailService.sendeBestaetigungsEmail(konto, token);
            return HINWEIS_BESTAETIGUNG;
        }

        // Ganz neues Konto
        Rolle rolle = findOrCreateRolle(bestimmeRolle(bereinigteEmail));

        User user = new User();
        user.setBenutzername(erzeugeBenutzername(bereinigteEmail));
        user.setPasswort(passwordEncoder.encode(passwort));
        user.setEmail(bereinigteEmail);
        ergaenzeNamen(user, bereinigteEmail);
        user.setRolle(rolle);
        user.setKontoAktiviert(true);
        String token = neuerBestaetigungsToken(user);
        userRepository.save(user);

        emailService.sendeBestaetigungsEmail(user, token);
        return HINWEIS_BESTAETIGUNG;
    }

    /**
     * Wird aufgerufen, wenn jemand auf den Link in der Bestaetigungs-Mail klickt.
     * Schaltet das Konto frei und loescht den Token (Link funktioniert nur einmal).
     */
    public String bestaetigen(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Der Bestätigungslink ist unvollständig.");
        }

        User user = userRepository.findByBestaetigungsToken(token)
                .orElseThrow(() -> new RuntimeException(
                        "Dieser Bestätigungslink ist ungültig oder wurde bereits verwendet."));

        if (user.getTokenGueltigBis() == null || user.getTokenGueltigBis().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Dieser Bestätigungslink ist abgelaufen. Bitte registrieren Sie sich erneut, "
                    + "dann erhalten Sie einen neuen Link.");
        }

        user.setEmailBestaetigt(true);
        user.setKontoAktiviert(true);
        user.setBestaetigungsToken(null);
        user.setTokenGueltigBis(null);
        userRepository.save(user);

        // Jetzt die Willkommens-Mail schicken
        emailService.sendeRegistrierungsEmail(user);

        return "Ihre E-Mail-Adresse wurde bestätigt. Sie können sich jetzt anmelden.";
    }

    public LoginResponse login(String email, String passwort) {
        if (email == null || email.isBlank() || passwort == null || passwort.isBlank()) {
            throw new RuntimeException("E-Mail-Adresse und Passwort dürfen nicht leer sein.");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("E-Mail-Adresse oder Passwort falsch."));

        // Vorbereitetes Konto (z.B. Betreuer), das noch nicht aktiviert wurde
        if (!user.istKontoAktiviert()) {
            throw new RuntimeException("Ihr Konto ist noch nicht aktiviert. "
                    + "Bitte registrieren Sie sich einmalig mit Ihrer Hochschul-E-Mail.");
        }

        if (!passwordEncoder.matches(passwort, user.getPasswort())) {
            throw new RuntimeException("E-Mail-Adresse oder Passwort falsch.");
        }

        // Erst nach dem Passwort pruefen, damit Fremde nicht herausfinden, welche Konten es gibt
        if (!user.istEmailBestaetigt()) {
            throw new RuntimeException("Bitte bestätigen Sie zuerst Ihre E-Mail-Adresse. "
                    + "Den Link finden Sie in der E-Mail, die wir Ihnen nach der Registrierung geschickt haben.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getBenutzername(), user.getRolle().getName());
        return new LoginResponse(token, user.getId(), user.getBenutzername(), user.getVorname(), user.getName(), user.getRolle().getName());
    }

    // ------------------------------------------------------------------ Hilfsmethoden

    // E-Mail einheitlich speichern (ohne Leerzeichen, klein geschrieben) und grob pruefen
    private String bereinigeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Bitte geben Sie Ihre E-Mail-Adresse ein.");
        }
        String bereinigt = email.trim().toLowerCase(Locale.ROOT);
        if (!bereinigt.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RuntimeException("Bitte geben Sie eine gültige E-Mail-Adresse ein.");
        }
        return bereinigt;
    }

    /**
     * Vorname/Nachname aus der E-Mail-Adresse ableiten, falls (noch) keine gesetzt sind.
     * "lena.test@stud.hs-bochum.de" -> Vorname "Lena", Nachname "Test".
     * Vorbereitete Konten (z.B. Betreuer) behalten ihre richtigen Namen.
     */
    private void ergaenzeNamen(User user, String email) {
        boolean ohneVorname = user.getVorname() == null || user.getVorname().isBlank();
        boolean ohneName = user.getName() == null || user.getName().isBlank();
        if (!ohneVorname && !ohneName) {
            return;
        }
        String lokal = email.substring(0, email.indexOf('@'));
        String[] teile = lokal.split("[._-]+");
        String vorname = teile.length > 0 ? grossAnfang(teile[0]) : lokal;
        String name = teile.length > 1 ? grossAnfang(teile[teile.length - 1]) : "";
        if (ohneVorname) user.setVorname(vorname);
        if (ohneName) user.setName(name);
    }

    private String grossAnfang(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    /**
     * Interner, eindeutiger Benutzername (wird fuer den Login-Token gebraucht, aber nicht mehr eingegeben).
     * Basis ist der Teil vor dem @, bei Bedarf mit Zahl: "lena.test", "lena.test2", ...
     */
    private String erzeugeBenutzername(String email) {
        String basis = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._-]", "");
        if (basis.length() < 3) {
            basis = "nutzer." + basis;
        }
        if (basis.length() > 25) {
            basis = basis.substring(0, 25);
        }
        String kandidat = basis;
        int zahl = 2;
        while (userRepository.findByBenutzername(kandidat).isPresent()) {
            kandidat = basis + zahl++;
        }
        return kandidat;
    }

    // Erzeugt einen zufaelligen, nicht erratbaren Token und setzt das Konto auf "nicht bestaetigt"
    private String neuerBestaetigungsToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setEmailBestaetigt(false);
        user.setBestaetigungsToken(token);
        user.setTokenGueltigBis(LocalDateTime.now().plusHours(TOKEN_GUELTIG_STUNDEN));
        return token;
    }

    private String bestimmeRolle(String email) {
        if (email.endsWith(DOMAIN_PROFESSOR)) {
            return "PROFESSOR";
        }
        return "STUDENT";
    }

    private Rolle findOrCreateRolle(String name) {
        return rolleRepository.findByName(name).orElseGet(() -> {
            Rolle r = new Rolle();
            r.setName(name);
            return rolleRepository.save(r);
        });
    }
}