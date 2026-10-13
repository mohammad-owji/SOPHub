package com.sophub.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.sophub.config.JwtService;
import com.sophub.model.LoginResponse;
import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.RolleRepository;
import com.sophub.repository.UserRepository;

@Service
public class AuthService {

    private static final String DOMAIN_PROFESSOR = "@hs-bochum.de";

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            RolleRepository rolleRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    // Signatur bleibt passend zum bisherigen AuthController.
    public String register(
            String benutzername,
            String passwort,
            String email,
            String name,
            String vorname) {

        if (passwort == null || passwort.isBlank()) {
            throw new RuntimeException("Passwort darf nicht leer sein.");
        }

        String bereinigteEmail = bereinigeEmail(email);

        Optional<User> vorhandenesKonto = userRepository.findByEmail(bereinigteEmail);

        if (vorhandenesKonto.isPresent()) {
            User konto = vorhandenesKonto.get();

            if (konto.istKontoAktiviert()) {
                throw new RuntimeException(
                        "Diese E-Mail-Adresse ist bereits registriert.");
            }

            // Vorhandene Namen und den internen Benutzernamen behalten.
            konto.setPasswort(passwordEncoder.encode(passwort));
            konto.setKontoAktiviert(true);
            userRepository.save(konto);

            return "Konto aktiviert! Sie können sich jetzt anmelden.";
        }

        Rolle rolle = findOrCreateRolle(
                bestimmeRolle(bereinigteEmail));

        User user = new User();
        user.setBenutzername("user_" + UUID.randomUUID());
        user.setPasswort(passwordEncoder.encode(passwort));
        user.setEmail(bereinigteEmail);
        user.setName("");
        user.setVorname("");
        user.setRolle(rolle);
        user.setKontoAktiviert(true);

        userRepository.save(user);
        emailService.sendeRegistrierungsEmail(user);

        return "Registrierung erfolgreich! Du kannst dich jetzt anmelden.";
    }

    public LoginResponse login(String email, String passwort) {
        if (passwort == null || passwort.isBlank()) {
            throw new RuntimeException(
                    "E-Mail und Passwort dürfen nicht leer sein.");
        }

        String bereinigteEmail = bereinigeEmail(email);

        User user = userRepository.findByEmail(bereinigteEmail)
                .orElseThrow(() -> new RuntimeException(
                        "E-Mail oder Passwort falsch."));

        if (!user.istKontoAktiviert()) {
            throw new RuntimeException(
                    "Ihr Konto ist noch nicht aktiviert. "
                            + "Bitte registrieren Sie sich einmalig "
                            + "mit Ihrer Hochschul-E-Mail.");
        }

        if (!passwordEncoder.matches(passwort, user.getPasswort())) {
            throw new RuntimeException("E-Mail oder Passwort falsch.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getBenutzername(),
                user.getRolle().getName());

        return new LoginResponse(
                token,
                user.getId(),
                user.getBenutzername(),
                user.getVorname(),
                user.getName(),
                user.getRolle().getName());
    }

    private String bereinigeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("E-Mail darf nicht leer sein.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String bestimmeRolle(String email) {
        if (email.endsWith(DOMAIN_PROFESSOR)) {
            return "PROFESSOR";
        }

        return "STUDENT";
    }

    private Rolle findOrCreateRolle(String name) {
        return rolleRepository.findByName(name).orElseGet(() -> {
            Rolle rolle = new Rolle();
            rolle.setName(name);
            return rolleRepository.save(rolle);
        });
    }
}