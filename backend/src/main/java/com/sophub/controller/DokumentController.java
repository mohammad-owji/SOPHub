package com.sophub.controller;

import com.sophub.model.Dokument;
import com.sophub.service.DokumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/sop/api/dokumente")
public class DokumentController {

    private final DokumentService dokumentService;

    public DokumentController(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> hochladen(
            @RequestParam("datei") MultipartFile datei,
            @RequestParam("benutzerId") Long benutzerId,
            @RequestParam(value = "projektId", required = false) Long projektId,
            @RequestParam("typ") String typ) {
        try {
            Dokument gespeichert = dokumentService.hochladen(datei, benutzerId, projektId, typ);
            return ResponseEntity.ok(gespeichert);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("/benutzer/{benutzerId}")
    public ResponseEntity<List<Dokument>> nachBenutzer(@PathVariable Long benutzerId) {
        return ResponseEntity.ok(dokumentService.nachBenutzer(benutzerId));
    }

    @GetMapping("/projekt/{projektId}")
    public ResponseEntity<List<Dokument>> nachProjekt(@PathVariable Long projektId) {
        return ResponseEntity.ok(dokumentService.nachProjekt(projektId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> herunterladen(@PathVariable Long id) {
        try {
            Resource resource = dokumentService.herunterladen(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> loeschen(@PathVariable Long id) {
        try {
            dokumentService.loeschen(id);
            return ResponseEntity.ok("Dokument gelöscht.");
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/zusammenfassung")
    public ResponseEntity<?> zusammenfassungErzeugen(@PathVariable Long id) {
        try {
            Dokument gespeichert = dokumentService.zusammenfassungErzeugen(id);
            return ResponseEntity.ok(gespeichert);
        } catch (Exception e) {
            return ResponseEntity.status(503).body(e.getMessage());
        }
    }
}
