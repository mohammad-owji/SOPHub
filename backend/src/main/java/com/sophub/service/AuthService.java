package com.sophub.service;

import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.RolleRepository;
import com.sophub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private LdapService ldapService;

    public AuthService(UserRepository userRepository, RolleRepository rolleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(String benutzername, String password, String email) {
        if (benutzername == null || benutzername.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Benutzername und Passwort dürfen nicht leer sein.");
        }
        if (userRepository.findByBenutzername(benutzername).isPresent()) {
            throw new RuntimeException("Diese Benutzername ist bereits vergeben.");
        }

        Rolle rolle = findOrCreateRolle("STUDENT");

        User user = new User();
        user.setBenutzername(benutzername); // Username sollte einige Bedingungen erfüllen.
        user.setPassword(passwordEncoder.encode(password)); // Password sollte einige Bedingungen erfüllen.
        user.setEmail(email != null && !email.isBlank() ? email : benutzername + "@stud.hs-bochum.de"); // Email sollte einige Bedingungen erfüllen.
        user.setRolle(rolle);
        userRepository.save(user);

        return "Registrierung erfolgreich! Du kannst dich jetzt anmelden.";
    }

    public String login(String benutzername, String password) {
        if (benutzername == null || benutzername.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Benutzername und Passwort dürfen nicht leer sein."); // Username / Password sollte einige Bedingungen erfüllen.
        }

        // LDAP-Authentifizierung (wenn konfiguriert)
        if (ldapService != null && ldapService.authenticate(benutzername, password)) {
            syncLdapUser(benutzername);
            return "Willkommen zurück, " + benutzername + "!";
        }

        // Fallback: lokale Datenbank
        User user = userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new RuntimeException("Benutzername oder Passwort falsch."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Benutzername oder Passwort falsch.");
        }

        return "Willkommen zurück, " + user.getBenutzername() + "!";
    }

    // Legt LDAP-User automatisch in der lokalen DB an, falls noch nicht vorhanden
    private void syncLdapUser(String benutzername) {
        if (userRepository.findByBenutzername(benutzername).isPresent()) {
            return;
        }
        Rolle rolle = findOrCreateRolle("STUDENT");
        User user = new User();
        user.setBenutzername(benutzername);
        user.setPassword(""); // kein lokales Passwort fuer LDAP-User
        user.setEmail(benutzername + "@stud.hs-bochum.de");
        user.setRolle(rolle);
        userRepository.save(user);
    }

    private Rolle findOrCreateRolle(String name) {
        return rolleRepository.findByName(name).orElseGet(() -> {
            Rolle r = new Rolle();
            r.setName(name);
            return rolleRepository.save(r);
        });
    }
}
