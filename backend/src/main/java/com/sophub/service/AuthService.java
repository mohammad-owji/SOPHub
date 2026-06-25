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

@Service
public class AuthService {

    private static final String DOMAIN_STUDENT = "@stud.hs-bochum.de";
    private static final String DOMAIN_PROFESSOR = "@hs-bochum.de";

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RolleRepository rolleRepository,
                       BCryptPasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        if (userRepository.findByBenutzername(benutzername).isPresent()) {
            throw new RuntimeException("Dieser Benutzername ist bereits vergeben.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Diese E-Mail-Adresse ist bereits registriert.");
        }

        String rollenName = bestimmeRolle(email);
        Rolle rolle = findOrCreateRolle(rollenName);

        User user = new User();
        user.setBenutzername(benutzername);
        user.setPasswort(passwordEncoder.encode(passwort));
        user.setEmail(email);
        user.setName(name);
        user.setVorname(vorname);
        user.setRolle(rolle);
        userRepository.save(user);

        return "Registrierung erfolgreich! Du kannst dich jetzt anmelden.";
    }

    public LoginResponse login(String benutzername, String passwort) {
        if (benutzername == null || benutzername.isBlank() || passwort == null || passwort.isBlank()) {
            throw new RuntimeException("Benutzername und Passwort dürfen nicht leer sein.");
        }

        User user = userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new RuntimeException("Benutzername oder Passwort falsch."));

        if (!passwordEncoder.matches(passwort, user.getPasswort())) {
            throw new RuntimeException("Benutzername oder Passwort falsch.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getBenutzername(), user.getRolle().getName());
        return new LoginResponse(token, user.getBenutzername(), user.getVorname(), user.getName(), user.getRolle().getName());
    }

    private String bestimmeRolle(String email) {
        if (email.endsWith(DOMAIN_STUDENT)) {
            return "STUDENT";
        } else if (email.endsWith(DOMAIN_PROFESSOR)) {
            return "PROFESSOR";
        } else {
            throw new RuntimeException(
                "Ungültige E-Mail-Adresse. Nur Hochschul-Adressen erlaubt " +
                "(@stud.hs-bochum.de oder @hs-bochum.de)."
            );
        }
    }

    private Rolle findOrCreateRolle(String name) {
        return rolleRepository.findByName(name).orElseGet(() -> {
            Rolle r = new Rolle();
            r.setName(name);
            return rolleRepository.save(r);
        });
    }
}
