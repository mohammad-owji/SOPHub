package com.sophub.service;

import com.sophub.config.JwtService;
import com.sophub.model.LoginResponse;
import com.sophub.model.User;
import com.sophub.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Änderungen am eigenen Konto über die Profilseite: Benutzername und Passwort.
 * Beide Änderungen erfordern zur Sicherheit das aktuelle Passwort.
 * Die E-Mail-Adresse wird bewusst nicht geändert (Hochschuladresse, bestimmt die Rolle).
 */
@Service
public class ProfilService {

    // Systemkonto der Beispielprojekte (siehe BeispielprojekteInitializer) - dieser Name ist reserviert
    private static final String SYSTEM_BENUTZERNAME = "sophub.beispielprojekte";

    // 3 bis 30 Zeichen: Buchstaben, Ziffern, Punkt, Bindestrich, Unterstrich
    private static final Pattern BENUTZERNAME_MUSTER = Pattern.compile("[A-Za-z0-9._-]{3,30}");

    private static final int PASSWORT_MINDESTLAENGE = 8;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ProfilService(UserRepository userRepository,
                         BCryptPasswordEncoder passwordEncoder,
                         JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Ändert den Benutzernamen des angemeldeten Kontos.
     * Weil der Login-Token den Benutzernamen enthält, wird ein neuer Token zurückgegeben.
     */
    @Transactional
    public LoginResponse benutzernameAendern(String angemeldeterBenutzer, String neuerBenutzername, String passwort) {
        User user = ladeUndPruefePasswort(angemeldeterBenutzer, passwort);

        String neuerName = neuerBenutzername == null ? "" : neuerBenutzername.trim();
        if (!BENUTZERNAME_MUSTER.matcher(neuerName).matches()) {
            throw new IllegalArgumentException(
                    "Der Benutzername muss 3 bis 30 Zeichen lang sein und darf nur Buchstaben, "
                            + "Ziffern, Punkt, Bindestrich und Unterstrich enthalten.");
        }
        if (neuerName.equals(user.getBenutzername())) {
            throw new IllegalArgumentException("Das ist bereits Ihr aktueller Benutzername.");
        }
        if (SYSTEM_BENUTZERNAME.equalsIgnoreCase(neuerName)) {
            throw new IllegalArgumentException("Dieser Benutzername ist reserviert.");
        }
        // Gross-/Kleinschreibung zaehlt nicht: "Tom.Test" gilt als vergeben, wenn es "tom.test" schon gibt.
        // Das eigene Konto ist ausgenommen (z.B. "lena.test" -> "Lena.Test" ist erlaubt).
        boolean vergeben = userRepository.findAll().stream()
                .anyMatch(anderer -> !anderer.getId().equals(user.getId())
                        && anderer.getBenutzername() != null
                        && anderer.getBenutzername().equalsIgnoreCase(neuerName));
        if (vergeben) {
            throw new IllegalArgumentException("Dieser Benutzername ist bereits vergeben.");
        }

        user.setBenutzername(neuerName);
        userRepository.save(user);

        String rolle = user.getRolle().getName();
        String token = jwtService.generateToken(user.getBenutzername(), rolle);
        return new LoginResponse(token, user.getId(), user.getBenutzername(),
                user.getVorname(), user.getName(), rolle);
    }

    /** Ändert das Passwort des angemeldeten Kontos. */
    @Transactional
    public void passwortAendern(String angemeldeterBenutzer, String aktuellesPasswort, String neuesPasswort) {
        User user = ladeUndPruefePasswort(angemeldeterBenutzer, aktuellesPasswort);

        if (neuesPasswort == null || neuesPasswort.length() < PASSWORT_MINDESTLAENGE) {
            throw new IllegalArgumentException(
                    "Das neue Passwort muss mindestens " + PASSWORT_MINDESTLAENGE + " Zeichen lang sein.");
        }
        if (passwordEncoder.matches(neuesPasswort, user.getPasswort())) {
            throw new IllegalArgumentException("Das neue Passwort muss sich vom aktuellen unterscheiden.");
        }

        user.setPasswort(passwordEncoder.encode(neuesPasswort));
        userRepository.save(user);
    }

    private User ladeUndPruefePasswort(String benutzername, String passwort) {
        User user = userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden."));
        if (passwort == null || passwort.isBlank() || !passwordEncoder.matches(passwort, user.getPasswort())) {
            throw new IllegalArgumentException("Das aktuelle Passwort ist nicht korrekt.");
        }
        return user;
    }
}