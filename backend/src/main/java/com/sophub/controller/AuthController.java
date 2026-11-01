package com.sophub.controller;

import com.sophub.model.LoginRequest;
import com.sophub.model.RegisterRequest;
import com.sophub.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sop/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Login mit E-Mail-Adresse und Passwort
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request.getEmail(), request.getPasswort()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // Registrierung nur mit E-Mail-Adresse und Passwort (Namen werden aus der E-Mail abgeleitet)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request.getEmail(), request.getPasswort()));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Link aus der Bestaetigungs-Mail: GET /sop/api/auth/bestaetigen?token=...
    // Ohne Login erreichbar (siehe SecurityConfig), denn der Nutzer ist ja noch nicht angemeldet.
    @GetMapping("/bestaetigen")
    public ResponseEntity<?> bestaetigen(@RequestParam(required = false) String token) {
        try {
            return ResponseEntity.ok(authService.bestaetigen(token));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT ist stateless — das Token wird clientseitig gelöscht
        return ResponseEntity.ok("Erfolgreich abgemeldet.");
    }
}