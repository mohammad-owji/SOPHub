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
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DOMAIN_PROFESSOR = "@hs-bochum.de";

    // So lange ist ein Bestaetigungslink gueltig
    private static final int TOKEN_GUELTIG_STUNDEN = 24;

    private static final String HINWEIS_BESTAETIGUNG =
            "Fast geschafft! Wir haben Ihnen eine E-Mail mit einem Bestätigungslink geschickt. "
            + "Bitte klicken Sie auf den Link, danach können Sie sich anmelden.";

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

    public String register(String benutzername, String passwort, String email, String name, String vorname) {
        if (benutzername == null || benutzername.isBlank()) {
            throw new RuntimeException("Benutzername darf nicht leer sein.");
        }
        if (passwort == null || passwort.isBlank()) {
            throw new RuntimeException("Passwort darf nicht leer sein.");
        }
        if (email == null || email.isBlank()) {
            throw new RuntimeException("E-Mail darf nicht leer sein.");
        }
        if (name == null || name.isBlank() || vorname == null || vorname.isBlank()) {
            throw new RuntimeException("Name und Vorname dürfen nicht leer sein.");
        }

        // E-Mail einheitlich speichern (ohne Leerzeichen, klein geschrieben)
        String bereinigteEmail = email.trim().toLowerCase();

        Optional<User> vorhandenesKonto = userRepository.findByEmail(bereinigteEmail);

        if (vorhandenesKonto.isPresent()) {
            User konto = vorhandenesKonto.get();

            // Fertiges Konto (aktiviert UND bestaetigt) darf nicht erneut registriert (= uebernommen) werden
            if (konto.istKontoAktiviert() && konto.istEmailBestaetigt()) {
                throw new RuntimeException("Diese E-Mail-Adresse ist bereits registriert.");
            }

            // Vorbereitetes Konto (z.B. Betreuer) ODER Registrierung, die noch nicht bestaetigt wurde:
            // Daten uebernehmen und einen NEUEN Bestaetigungslink schicken.
            // Freigeschaltet wird das Konto erst, wenn der Link angeklickt wurde.
            pruefeBenutzernameFrei(benutzername, konto.getId());

            konto.setBenutzername(benutzername);
            konto.setPasswort(passwordEncoder.encode(passwort));
            konto.setName(name);
            konto.setVorname(vorname);
            String token = neuerBestaetigungsToken(konto);
            userRepository.save(konto);

            emailService.sendeBestaetigungsEmail(konto, token);
            return HINWEIS_BESTAETIGUNG;
        }

        // Ganz neues Konto
        pruefeBenutzernameFrei(benutzername, null);

        String rollenName = bestimmeRolle(bereinigteEmail);
        Rolle rolle = findOrCreateRolle(rollenName);

        User user = new User();
        user.setBenutzername(benutzername);
        user.setPasswort(passwordEncoder.encode(passwort));
        user.setEmail(bereinigteEmail);
        user.setName(name);
        user.setVorname(vorname);
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

    // Erzeugt einen zufaelligen, nicht erratbaren Token und setzt das Konto auf "nicht bestaetigt"
    private String neuerBestaetigungsToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setEmailBestaetigt(false);
        user.setBestaetigungsToken(token);
        user.setTokenGueltigBis(LocalDateTime.now().plusHours(TOKEN_GUELTIG_STUNDEN));
        return token;
    }

    // Benutzername darf nicht von einem ANDEREN Konto belegt sein
    private void pruefeBenutzernameFrei(String benutzername, Long eigeneId) {
        userRepository.findByBenutzername(benutzername).ifPresent(anderer -> {
            if (eigeneId == null || !anderer.getId().equals(eigeneId)) {
                throw new RuntimeException("Dieser Benutzername ist bereits vergeben.");
            }
        });
    }

    public LoginResponse login(String benutzername, String passwort) {
        if (benutzername == null || benutzername.isBlank() || passwort == null || passwort.isBlank()) {
            throw new RuntimeException("Benutzername und Passwort dürfen nicht leer sein.");
        }

        User user = userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new RuntimeException("Benutzername oder Passwort falsch."));

        // Vorbereitetes Konto (z.B. Betreuer), das noch nicht aktiviert wurde
        if (!user.istKontoAktiviert()) {
            throw new RuntimeException("Ihr Konto ist noch nicht aktiviert. "
                    + "Bitte registrieren Sie sich einmalig mit Ihrer Hochschul-E-Mail.");
        }

        if (!passwordEncoder.matches(passwort, user.getPasswort())) {
            throw new RuntimeException("Benutzername oder Passwort falsch.");
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