package com.sophub.controller;

import com.sophub.model.AIResponse;
import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.model.User;
import com.sophub.service.ProjektKiService;
import com.sophub.service.ProjektService;
import com.sophub.service.MitgliedschaftService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sop/api/projekte")
public class ProjektController {

    private final ProjektService projektService;
    private final ProjektKiService projektKiService;
    private final MitgliedschaftService mitgliedschaftService;

    public ProjektController(ProjektService projektService, ProjektKiService projektKiService,
                             MitgliedschaftService mitgliedschaftService) {
        this.projektService = projektService;
        this.projektKiService = projektKiService;
        this.mitgliedschaftService = mitgliedschaftService;
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

    @GetMapping("/alle")
    public ResponseEntity<List<Projekt>> alle() {
        return ResponseEntity.ok(projektService.alleProjeKte());
    }

    @GetMapping("/{id}")
        public ResponseEntity<?> projektById(@PathVariable Long id, Authentication authentication) {
        Projekt projekt = projektService.projektById(id).orElse(null);
        if (projekt == null) return ResponseEntity.notFound().build();

        boolean owner = mitgliedschaftService.istErsteller(id, authentication.getName());
        boolean teammitglied = mitgliedschaftService.istTeammitglied(id, authentication.getName());
        boolean eingeladenerBetreuer = projekt.getBetreuer() != null
            && authentication.getName().equals(projekt.getBetreuer().getBenutzername());
        if (owner || teammitglied || eingeladenerBetreuer) return ResponseEntity.ok(projekt);
        return ResponseEntity.ok(oeffentlicheAnsicht(projekt));
        }

        @GetMapping("/{id}/details")
        public ResponseEntity<?> projektDetails(@PathVariable Long id, Authentication authentication) {
        Projekt projekt = projektService.projektById(id).orElse(null);
        if (projekt == null) return ResponseEntity.notFound().build();

        boolean owner = mitgliedschaftService.istErsteller(id, authentication.getName());
        boolean teammitglied = mitgliedschaftService.istTeammitglied(id, authentication.getName());
        if (!owner && !teammitglied) {
            return ResponseEntity.ok(oeffentlicheAnsicht(projekt));
        }

        return ResponseEntity.ok(new ProjektDetailansicht(
            projekt.getId(), projekt.getTitel(), projekt.getBeschreibung(), projekt.getSemester(),
            projekt.getFachbereich(), projekt.getProjektart(), projekt.getStatus(),
            projekt.getGruppenanzahl(), projekt.getSchlagwoerter(),
            projekt.getStudent() == null ? null : new PersonAnsicht(projekt.getStudent()),
            projekt.getBetreuer() == null ? null : new PersonAnsicht(projekt.getBetreuer()),
            projekt.getTags().stream().map(TagAnsicht::new).toList(), owner, teammitglied));
    }

    // Einladungen des eingeloggten Betreuers.
    // Wer eingeloggt ist, steht im JWT-Token -> niemand kann fremde Einladungen abrufen.
    @GetMapping("/einladungen")
    public ResponseEntity<?> meineEinladungen(Authentication authentication) {
        try {
            return ResponseEntity.ok(projektService.einladungenFuer(authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Projekt>> projektByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(projektService.projektByStudent(studentId));
    }

    // Betreuer entweder per betreuerId (aus der Liste gewaehlt)
    // oder per betreuerVorname + betreuerName + betreuerEmail (selbst eingetippt)
    @PostMapping("/student/{studentId}")
    public ResponseEntity<?> erstellen(@PathVariable Long studentId,
                                        @RequestParam(required = false) Long betreuerId,
                                        @RequestParam(required = false) String betreuerVorname,
                                        @RequestParam(required = false) String betreuerName,
                                        @RequestParam(required = false) String betreuerEmail,
                                        @RequestBody Projekt projekt) {
        try {
            return ResponseEntity.ok(projektService.erstellen(
                    projekt, studentId, betreuerId, betreuerVorname, betreuerName, betreuerEmail));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> aktualisieren(@PathVariable Long id, @RequestBody Projekt projekt,
                                           Authentication authentication) {
        if (!mitgliedschaftService.darfProjektVerwalten(id, authentication.getName())) {
            return ResponseEntity.status(403).body("Nur Projekt-Ersteller und Teammitglieder können das Projekt bearbeiten.");
        }
        try {
            Projekt aenderung = new Projekt();
            aenderung.setTitel(projekt.getTitel());
            aenderung.setBeschreibung(projekt.getBeschreibung());
            return ResponseEntity.ok(projektService.aktualisieren(id, aenderung));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Projekt annehmen
    @PutMapping("/{projektId}/annehmen/{betreuerId}")
    public ResponseEntity<?> annehmen(@PathVariable Long projektId, @PathVariable Long betreuerId) {
        try {
            return ResponseEntity.ok(projektService.annehmen(projektId, betreuerId));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Projekt ablehnen
    @PutMapping("/{projektId}/ablehnen/{betreuerId}")
    public ResponseEntity<?> ablehnen(@PathVariable Long projektId, @PathVariable Long betreuerId) {
        try {
            return ResponseEntity.ok(projektService.ablehnen(projektId, betreuerId));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> loeschen(@PathVariable Long id, Authentication authentication) {
        if (!mitgliedschaftService.istErsteller(id, authentication.getName())) {
            return ResponseEntity.status(403).body("Nur der Projekt-Ersteller kann das Projekt löschen.");
        }
        try {
            projektService.loeschen(id);
            return ResponseEntity.ok("Projekt gelöscht.");
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/zusammenfassung")
    public ResponseEntity<?> zusammenfassungErzeugen(@PathVariable Long id) {
        try {
            String zusammenfassung = projektService.zusammenfassungErzeugen(id);
            return ResponseEntity.ok(new AIResponse(zusammenfassung));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(e.getMessage());
        }
    }
    /**
     * KI-Übersicht des Projekts: skriptbasierte Projektzusammenfassung und
     * kategorisierte Stichwörter aus allen Dokumenten des Projekts.
     * Wird beim ersten Aufruf erzeugt und gecacht; {@code ?neu=true} erzwingt
     * eine Neuerzeugung.
     */
    @GetMapping("/{id}/ki-uebersicht")
    public ResponseEntity<?> kiUebersicht(@PathVariable Long id,
                                          @RequestParam(value = "neu", defaultValue = "false") boolean neu) {
        try {
            return ResponseEntity.ok(projektKiService.uebersicht(id, neu));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(e.getMessage());
        }
    }

    record OeffentlicheProjektansicht(Long id, String titel, String beschreibung,
                                      boolean istOwner, boolean istTeammitglied) {}

    record ProjektDetailansicht(Long id, String titel, String beschreibung, String semester,
                                String fachbereich, String projektart, String status, Integer gruppenanzahl,
                                String schlagwoerter, PersonAnsicht student, PersonAnsicht betreuer,
                                List<TagAnsicht> tags, boolean istOwner, boolean istTeammitglied) {}

    record PersonAnsicht(Long id, String vorname, String name, String benutzername) {
        PersonAnsicht(User user) {
            this(user.getId(), user.getVorname(), user.getName(), user.getBenutzername());
        }
    }

    record TagAnsicht(Long id, String name) {
        TagAnsicht(Tag tag) {
            this(tag.getId(), tag.getName());
        }
    }

    private OeffentlicheProjektansicht oeffentlicheAnsicht(Projekt projekt) {
        return new OeffentlicheProjektansicht(projekt.getId(), projekt.getTitel(), projekt.getBeschreibung(), false, false);
    }
}
