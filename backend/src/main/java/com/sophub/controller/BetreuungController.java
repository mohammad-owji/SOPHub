package com.sophub.controller;

import com.sophub.service.CoBetreuungService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

/**
 * Betreuung eines Projekts: Co-Betreuung einladen, annehmen, ablehnen und beenden
 * sowie die Betreuung an eine andere Person uebergeben.
 * Die Rechte prueft der CoBetreuungService.
 */
@RestController
@RequestMapping("/sop/api")
public class BetreuungController {

    private final CoBetreuungService coBetreuungService;

    public BetreuungController(CoBetreuungService coBetreuungService) {
        this.coBetreuungService = coBetreuungService;
    }

    // Betreuung und Co-Betreuung eines Projekts
    @GetMapping("/projekte/{projektId}/betreuung")
    public ResponseEntity<?> uebersicht(@PathVariable Long projektId, Authentication authentication) {
        return antwort(() -> coBetreuungService.uebersicht(projektId, authentication.getName()));
    }

    // Co-Betreuung per Hochschul-E-Mail einladen
    @PostMapping("/projekte/{projektId}/betreuung/co")
    public ResponseEntity<?> einladen(@PathVariable Long projektId, @RequestBody CoEinladung anfrage,
                                      Authentication authentication) {
        return antwort(() -> coBetreuungService.einladen(projektId, authentication.getName(),
                anfrage == null ? null : anfrage.email(), anfrage == null ? null : anfrage.nachricht()));
    }

    // Einladung zurueckziehen bzw. Co-Betreuung beenden
    @DeleteMapping("/projekte/{projektId}/betreuung/co/{einladungId}")
    public ResponseEntity<?> entfernen(@PathVariable Long projektId, @PathVariable Long einladungId,
                                       Authentication authentication) {
        return antwort(() -> {
            coBetreuungService.entfernen(projektId, einladungId, authentication.getName());
            return "Co-Betreuung entfernt.";
        });
    }

    // Betreuung an eine andere Person uebergeben (die Person muss annehmen)
    @PostMapping("/projekte/{projektId}/betreuung/uebergabe")
    public ResponseEntity<?> uebergabeAnfragen(@PathVariable Long projektId, @RequestBody UebergabeAnfrage anfrage,
                                               Authentication authentication) {
        return antwort(() -> coBetreuungService.uebergabeAnfragen(projektId, authentication.getName(),
                anfrage == null ? null : anfrage.email(),
                anfrage == null ? null : anfrage.grund(),
                anfrage != null && Boolean.TRUE.equals(anfrage.alsCoBleiben())));
    }

    // Offene Uebergabe zurueckziehen
    @DeleteMapping("/projekte/{projektId}/betreuung/uebergabe/{einladungId}")
    public ResponseEntity<?> uebergabeZurueckziehen(@PathVariable Long projektId, @PathVariable Long einladungId,
                                                    Authentication authentication) {
        return antwort(() -> {
            coBetreuungService.uebergabeZurueckziehen(projektId, einladungId, authentication.getName());
            return "Übergabe zurückgezogen.";
        });
    }

    // Eigene Einladungen (Co-Betreuung und Uebergabe)
    @GetMapping("/betreuung/einladungen")
    public ResponseEntity<?> meineEinladungen(Authentication authentication) {
        return antwort(() -> coBetreuungService.meineEinladungen(authentication.getName()));
    }

    @PutMapping("/betreuung/einladungen/{einladungId}/annehmen")
    public ResponseEntity<?> annehmen(@PathVariable Long einladungId, Authentication authentication) {
        return antwort(() -> {
            coBetreuungService.annehmen(einladungId, authentication.getName());
            return "Einladung angenommen.";
        });
    }

    @PutMapping("/betreuung/einladungen/{einladungId}/ablehnen")
    public ResponseEntity<?> ablehnen(@PathVariable Long einladungId, Authentication authentication) {
        return antwort(() -> {
            coBetreuungService.ablehnen(einladungId, authentication.getName());
            return "Einladung abgelehnt.";
        });
    }

    // Einheitliche Fehlerbehandlung: 403 ohne Recht, 400 bei falschen Angaben
    private ResponseEntity<?> antwort(Supplier<Object> aktion) {
        try {
            return ResponseEntity.ok(aktion.get());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    record CoEinladung(String email, String nachricht) {}

    record UebergabeAnfrage(String email, String grund, Boolean alsCoBleiben) {}
}