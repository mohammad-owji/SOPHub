package com.sophub.controller;

import com.sophub.model.ProjektMitglied;
import com.sophub.service.MitgliedschaftService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sop/api/projekte/{projektId}/mitglieder")
public class MitgliedschaftController {

    private final MitgliedschaftService mitgliedschaftService;

    public MitgliedschaftController(MitgliedschaftService mitgliedschaftService) {
        this.mitgliedschaftService = mitgliedschaftService;
    }

    @GetMapping
    public ResponseEntity<?> mitglieder(@PathVariable Long projektId, Authentication authentication) {
        if (!mitgliedschaftService.darfProjektVerwalten(projektId, authentication.getName())) {
            return ResponseEntity.status(403).body("Teamdaten sind nur für Projekt-Ersteller und Teammitglieder sichtbar.");
        }
        List<MitgliedInfo> ergebnis = mitgliedschaftService.mitglieder(projektId).stream()
                .map(MitgliedInfo::new)
                .toList();
        return ResponseEntity.ok(ergebnis);
    }

    @PostMapping("/{studentId}")
    public ResponseEntity<?> hinzufuegen(@PathVariable Long projektId, @PathVariable Long studentId,
                                          Authentication authentication) {
        if (!mitgliedschaftService.istErsteller(projektId, authentication.getName())) {
            return ResponseEntity.status(403).body("Nur der Projekt-Ersteller kann Teammitglieder hinzufügen.");
        }
        try {
            return ResponseEntity.ok(new MitgliedInfo(mitgliedschaftService.hinzufuegen(projektId, studentId)));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Antwort "204 No Content" = erfolgreich geloescht, ohne Inhalt.
    // (Vorher kam reiner Text zurueck – das Frontend erwartet aber JSON oder eine leere Antwort.)
    @DeleteMapping("/{studentId}")
    public ResponseEntity<?> entfernen(@PathVariable Long projektId, @PathVariable Long studentId,
                                        Authentication authentication) {
        if (!mitgliedschaftService.istErsteller(projektId, authentication.getName())) {
            return ResponseEntity.status(403).body("Nur der Projekt-Ersteller kann Teammitglieder entfernen.");
        }
        try {
            mitgliedschaftService.entfernen(projektId, studentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    record MitgliedInfo(Long studentId, String vorname, String name, String email) {
        MitgliedInfo(ProjektMitglied mitglied) {
            this(mitglied.getStudent().getId(), mitglied.getStudent().getVorname(),
                    mitglied.getStudent().getName(), mitglied.getStudent().getEmail());
        }
    }
}