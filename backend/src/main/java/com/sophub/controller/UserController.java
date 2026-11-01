package com.sophub.controller;

import com.sophub.model.User;
import com.sophub.repository.UserRepository;
import com.sophub.service.ProfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sop/api/benutzer")
public class UserController {

    // Systemkonto, das die Beispielprojekte "besitzt" (siehe BeispielprojekteInitializer).
    // Es ist kein echter Mensch und soll deshalb nicht als Teammitglied auswaehlbar sein.
    private static final String SYSTEM_BENUTZERNAME = "sophub.beispielprojekte";

    private final UserRepository userRepository;
    private final ProfilService profilService;

    public UserController(UserRepository userRepository, ProfilService profilService) {
        this.userRepository = userRepository;
        this.profilService = profilService;
    }

    @GetMapping("/profil")
    public ResponseEntity<?> profil(Authentication authentication) {
        String benutzername = authentication.getName();
        return userRepository.findByBenutzername(benutzername)
                .map(user -> ResponseEntity.ok(new ProfilResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Benutzername aendern - liefert einen neuen Login-Token zurueck (der alte enthaelt den alten Namen)
    @PutMapping("/profil/benutzername")
    public ResponseEntity<?> benutzernameAendern(@RequestBody BenutzernameAenderung anfrage,
                                                 Authentication authentication) {
        try {
            return ResponseEntity.ok(profilService.benutzernameAendern(
                    authentication.getName(), anfrage.neuerBenutzername(), anfrage.passwort()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/profil/passwort")
    public ResponseEntity<?> passwortAendern(@RequestBody PasswortAenderung anfrage,
                                             Authentication authentication) {
        try {
            profilService.passwortAendern(
                    authentication.getName(), anfrage.aktuellesPasswort(), anfrage.neuesPasswort());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/professoren")
    public ResponseEntity<List<BenutzerKurzInfo>> professoren() {
        List<BenutzerKurzInfo> ergebnis = userRepository.findByRolle_Name("PROFESSOR").stream()
                .map(BenutzerKurzInfo::new)
                .toList();
        return ResponseEntity.ok(ergebnis);
    }

    @GetMapping("/studenten")
    public ResponseEntity<List<BenutzerKurzInfo>> studenten() {
        List<BenutzerKurzInfo> ergebnis = userRepository.findByRolle_Name("STUDENT").stream()
                // Systemkonto der Beispielprojekte nicht in der Studierenden-Liste anzeigen
                .filter(user -> !SYSTEM_BENUTZERNAME.equals(user.getBenutzername()))
                .map(BenutzerKurzInfo::new)
                .toList();
        return ResponseEntity.ok(ergebnis);
    }

    record BenutzernameAenderung(String neuerBenutzername, String passwort) {}

    record PasswortAenderung(String aktuellesPasswort, String neuesPasswort) {}

    record BenutzerKurzInfo(Long id, String vorname, String name) {
        BenutzerKurzInfo(User user) {
            this(user.getId(), user.getVorname(), user.getName());
        }
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