package com.sophub.service;

import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.RolleRepository;
import com.sophub.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RolleRepository rolleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(String kennung, String password, String email) {
        if (kennung == null || kennung.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Benutzername und Passwort dürfen nicht leer sein.");
        }
        if (userRepository.findByKennung(kennung).isPresent()) {
            throw new RuntimeException("Diese Benutzername ist bereits vergeben.");
        }

        Rolle rolle = rolleRepository.findByName("STUDENT").orElseGet(() -> {
            Rolle r = new Rolle();
            r.setName("STUDENT");
            return rolleRepository.save(r);
        });

        User user = new User();
        user.setKennung(kennung);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email != null && !email.isBlank() ? email : kennung + "@stud.hs-bochum.de");
        user.setRolle(rolle);
        userRepository.save(user);

        return "Registrierung erfolgreich! Du kannst dich jetzt anmelden.";
    }

    public String login(String kennung, String password) {
        if (kennung == null || kennung.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Benutzername und Passwort dürfen nicht leer sein.");
        }

        User user = userRepository.findByKennung(kennung)
                .orElseThrow(() -> new RuntimeException("Benutzername oder Passwort falsch."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Benutzername oder Passwort falsch.");
        }

        return "Willkommen zurück, " + user.getKennung() + "!";
    }
}
