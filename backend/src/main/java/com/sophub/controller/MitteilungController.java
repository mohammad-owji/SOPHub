package com.sophub.controller;

import com.sophub.service.MitteilungService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Mitteilungsbrett eines Projekts (wichtige Mitteilungen zwischen Team und Betreuer:in).
 * Zugriff haben nur Ersteller:in, Teammitglieder und die Betreuung (siehe MitteilungService).
 */
@RestController
@RequestMapping("/sop/api/projekte/{projektId}/mitteilungen")
public class MitteilungController {

    private final MitteilungService mitteilungService;

    public MitteilungController(MitteilungService mitteilungService) {
        this.mitteilungService = mitteilungService;
    }

    @GetMapping
    public ResponseEntity<?> liste(@PathVariable Long projektId, Authentication authentication) {
        try {
            return ResponseEntity.ok(mitteilungService.liste(projektId, authentication.getName()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> erstellen(@PathVariable Long projektId, @RequestBody NeueMitteilung anfrage,
                                       Authentication authentication) {
        try {
            mitteilungService.erstellen(projektId, authentication.getName(), anfrage.kategorie(),
                    anfrage.betreff(), anfrage.text(), Boolean.TRUE.equals(anfrage.nurTeam()));
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{mitteilungId}/antworten")
    public ResponseEntity<?> antworten(@PathVariable Long projektId, @PathVariable Long mitteilungId,
                                       @RequestBody NeueAntwort anfrage, Authentication authentication) {
        try {
            mitteilungService.antworten(projektId, mitteilungId, authentication.getName(), anfrage.text());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{mitteilungId}")
    public ResponseEntity<?> loeschen(@PathVariable Long projektId, @PathVariable Long mitteilungId,
                                      Authentication authentication) {
        try {
            mitteilungService.loeschen(projektId, mitteilungId, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    record NeueMitteilung(String kategorie, String betreff, String text, Boolean nurTeam) {}

    record NeueAntwort(String text) {}
}