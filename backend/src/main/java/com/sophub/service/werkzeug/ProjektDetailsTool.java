package com.sophub.service.werkzeug;

import com.sophub.model.Dokument;
import com.sophub.model.Projekt;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.service.MitgliedschaftService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Werkzeug {@code get_project_details}: Details eines Projekts inklusive
 * Dokumentliste. Dokument-Typ und Metadaten sind für alle sichtbar; der
 * Originaltext (extrahierter Inhalt) NUR für Berechtigte (Teammitglied,
 * zugewiesener Betreuer, Admin). Alle anderen erhalten ausschließlich die
 * KI-Zusammenfassung (F27) - niemals Rohinhalte interner Dokumente.
 */
@Component
public class ProjektDetailsTool implements ChatTool {

    private final ProjektRepository projektRepository;
    private final DokumentRepository dokumentRepository;
    private final MitgliedschaftService mitgliedschaftService;
    private final ObjectMapper objectMapper;

    public ProjektDetailsTool(ProjektRepository projektRepository,
                              DokumentRepository dokumentRepository,
                              MitgliedschaftService mitgliedschaftService,
                              ObjectMapper objectMapper) {
        this.projektRepository = projektRepository;
        this.dokumentRepository = dokumentRepository;
        this.mitgliedschaftService = mitgliedschaftService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "get_project_details";
    }

    @Override
    public String beschreibung() {
        return "Liefert Details zu einem Projekt inkl. Dokumentliste (nur Typ/Metadaten; "
                + "Originalinhalte nur, wenn der Nutzer berechtigt ist).";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("project_id").put("type", "integer").put("description", "Projekt-Id");
        schema.putArray("required").add("project_id");
        return schema;
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        JsonNode idNode = argumente == null ? null : argumente.get("project_id");
        if (idNode == null || !idNode.canConvertToLong()) {
            return new Fehler("Kein gültiges project_id übergeben.");
        }
        Long projektId = idNode.asLong();
        Projekt projekt = projektRepository.findById(projektId).orElse(null);
        if (projekt == null) {
            return new Fehler("Projekt nicht gefunden.");
        }

        boolean berechtigt = mitgliedschaftService.darfInterneDetails(projektId, benutzername);

        List<DokumentInfo> dokumente = dokumentRepository.findByProjektId(projektId).stream()
                .map(d -> zuInfo(d, berechtigt))
                .toList();

        return new Details(
                projekt.getId(), projekt.getTitel(), projekt.getBeschreibung(),
                projekt.getStatus(), projekt.getSemester(), projekt.getFachbereich(),
                projekt.getProjektart(), projekt.getTags().stream().map(t -> t.getName()).toList(),
                berechtigt, dokumente);
    }

    private DokumentInfo zuInfo(Dokument d, boolean berechtigt) {
        // Immer: Typ + Name + KI-Zusammenfassung. Originaltext nur für Berechtigte.
        String inhalt = berechtigt ? d.getExtrahierterText() : null;
        return new DokumentInfo(d.getId(), d.getDateiName(), d.getTyp(),
                d.getKiZusammenfassung(), inhalt);
    }

    record DokumentInfo(Long id, String dateiName, String typ, String kiZusammenfassung,
                        String originalInhalt) {}

    record Details(Long id, String titel, String beschreibung, String status, String semester,
                   String fachbereich, String projektart, List<String> tags,
                   boolean darfInterneInhalte, List<DokumentInfo> dokumente) {}

    record Fehler(String fehler) {}
}
