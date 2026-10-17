package com.sophub.controller;

import com.sophub.service.AnforderungService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dokument-Anforderungen ("Abgaben") eines Projekts:
 * Betreuung fordert an und prueft, Team reicht ein. Zugriff nur fuer Team und Betreuung.
 */
@RestController
@RequestMapping("/sop/api/projekte/{projektId}/anforderungen")
public class AnforderungController {

    private final AnforderungService anforderungService;

    public AnforderungController(AnforderungService anforderungService) {
        this.anforderungService = anforderungService;
    }

    @GetMapping
    public ResponseEntity<?> liste(@PathVariable Long projektId, Authentication authentication) {
        try {
            return ResponseEntity.ok(anforderungService.liste(projektId, authentication.getName()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> anfordern(@PathVariable Long projektId, @RequestBody NeueAnforderung anfrage,
                                       Authentication authentication) {
        try {
            anforderungService.anfordern(projektId, authentication.getName(), anfrage.typ(), anfrage.titel(),
                    anfrage.beschreibung(), anfrage.frist());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/{anforderungId}/einreichen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> einreichen(@PathVariable Long projektId, @PathVariable Long anforderungId,
                                        @RequestParam("datei") MultipartFile datei,
                                        @RequestParam(value = "kommentar", required = false) String kommentar,
                                        Authentication authentication) {
        try {
            anforderungService.einreichen(projektId, anforderungId, authentication.getName(), datei, kommentar);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{anforderungId}/pruefen")
    public ResponseEntity<?> pruefen(@PathVariable Long projektId, @PathVariable Long anforderungId,
                                     @RequestBody Pruefung anfrage, Authentication authentication) {
        try {
            anforderungService.pruefen(projektId, anforderungId, authentication.getName(),
                    anfrage.ergebnis(), anfrage.kommentar());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{anforderungId}")
    public ResponseEntity<?> loeschen(@PathVariable Long projektId, @PathVariable Long anforderungId,
                                      Authentication authentication) {
        try {
            anforderungService.loeschen(projektId, anforderungId, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // frist als Text im Format JJJJ-MM-TT (z.B. "2026-11-15") oder leer
    record NeueAnforderung(String typ, String titel, String beschreibung, String frist) {}

    record Pruefung(String ergebnis, String kommentar) {}
}