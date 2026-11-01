package com.sophub.controller;

import com.sophub.service.ProjektIdeenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seite "Projektideen": KI-Ideenfinder.
 * Nur fuer angemeldete Personen (wie alle /sop/api-Adressen ausser Login/Registrierung).
 */
@RestController
@RequestMapping("/sop/api/ideen")
public class ProjektIdeenController {

    private final ProjektIdeenService projektIdeenService;

    public ProjektIdeenController(ProjektIdeenService projektIdeenService) {
        this.projektIdeenService = projektIdeenService;
    }

    @PostMapping("/vorschlagen")
    public ResponseEntity<?> vorschlagen(@RequestBody ProjektIdeenService.IdeenAnfrage anfrage) {
        try {
            return ResponseEntity.ok(projektIdeenService.ideenVorschlagen(anfrage));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // z.B. KI nicht erreichbar oder ueberlastet -> verstaendliche Meldung an das Frontend
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }
}