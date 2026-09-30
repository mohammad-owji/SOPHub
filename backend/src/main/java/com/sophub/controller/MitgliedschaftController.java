package com.sophub.controller;

import com.sophub.model.ProjektMitglied;
import com.sophub.service.MitgliedschaftService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<MitgliedInfo>> mitglieder(@PathVariable Long projektId) {
        List<MitgliedInfo> ergebnis = mitgliedschaftService.mitglieder(projektId).stream()
                .map(MitgliedInfo::new)
                .toList();
        return ResponseEntity.ok(ergebnis);
    }

    @PostMapping("/{studentId}")
    public ResponseEntity<?> hinzufuegen(@PathVariable Long projektId, @PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(new MitgliedInfo(mitgliedschaftService.hinzufuegen(projektId, studentId)));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Antwort "204 No Content" = erfolgreich geloescht, ohne Inhalt.
    // (Vorher kam reiner Text zurueck – das Frontend erwartet aber JSON oder eine leere Antwort.)
    @DeleteMapping("/{studentId}")
    public ResponseEntity<?> entfernen(@PathVariable Long projektId, @PathVariable Long studentId) {
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