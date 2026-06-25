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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request.getBenutzername(), request.getPasswort()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(
                request.getBenutzername(),
                request.getPasswort(),
                request.getEmail(),
                request.getName(),
                request.getVorname()
            ));
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
