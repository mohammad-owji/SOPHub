package com.sophub.controller;

import com.sophub.model.Projekt;
import com.sophub.service.ProjektService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sop/api/projekte")
public class ProjektController {

    private final ProjektService projektService;

    public ProjektController(ProjektService projektService) {
        this.projektService = projektService;
    }

    @GetMapping
    public ResponseEntity<List<Projekt>> alleProjeKte(Authentication authentication) {
        String benutzername = authentication.getName();
        String rolle = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority() != null ? a.getAuthority().replace("ROLE_", "") : "STUDENT")
                .orElse("STUDENT");
        return ResponseEntity.ok(projektService.nachRolle(benutzername, rolle));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> projektById(@PathVariable Long id) {
        return projektService.projektById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Projekt>> projektByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(projektService.projektByStudent(studentId));
    }

    @PostMapping("/student/{studentId}")
    public ResponseEntity<?> erstellen(@PathVariable Long studentId, @RequestBody Projekt projekt) {
        try {
            return ResponseEntity.ok(projektService.erstellen(projekt, studentId));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> aktualisieren(@PathVariable Long id, @RequestBody Projekt projekt) {
        try {
            return ResponseEntity.ok(projektService.aktualisieren(id, projekt));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> loeschen(@PathVariable Long id) {
        try {
            projektService.loeschen(id);
            return ResponseEntity.ok("Projekt gelöscht.");
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
