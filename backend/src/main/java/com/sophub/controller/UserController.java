package com.sophub.controller;

import com.sophub.model.User;
import com.sophub.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sop/api/benutzer")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profil")
    public ResponseEntity<?> profil(Authentication authentication) {
        String benutzername = authentication.getName();
        return userRepository.findByBenutzername(benutzername)
                .map(user -> ResponseEntity.ok(new ProfilResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    record ProfilResponse(
        Long id,
        String benutzername,
        String vorname,
        String name,
        String email,
        String rolle,
        String erstelltAm,
        String lastLogin
    ) {
        ProfilResponse(User user) {
            this(
                user.getId(),
                user.getBenutzername(),
                user.getVorname(),
                user.getName(),
                user.getEmail(),
                user.getRolle().getName(),
                user.getErstelltAm() != null ? user.getErstelltAm().toString() : null,
                user.getLastLogin() != null ? user.getLastLogin().toString() : null
            );
        }
    }
}
