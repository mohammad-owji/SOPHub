package com.sophub.service.werkzeug;

import com.sophub.service.SimilarityService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Werkzeug {@code find_similar}: findet zu einer Ideenbeschreibung ähnliche
 * bestehende Projekte (über {@link SimilarityService}) und stuft die Ähnlichkeit
 * grob ein. Liefert nur öffentliche Metadaten.
 */
@Component
public class AehnlicheProjekteTool implements ChatTool {

    private static final int STANDARD_LIMIT = 5;

    private final SimilarityService similarityService;
    private final ObjectMapper objectMapper;

    public AehnlicheProjekteTool(SimilarityService similarityService, ObjectMapper objectMapper) {
        this.similarityService = similarityService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "find_similar";
    }

    @Override
    public String beschreibung() {
        return "Findet zu einer Projektidee (Freitext) ähnliche bestehende Projekte und schätzt die "
                + "Ähnlichkeit (hoch/mittel/gering) samt überschneidender Begriffe.";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("idea_text").put("type", "string").put("description", "Beschreibung der Idee");
        props.putObject("limit").put("type", "integer").put("description", "max. Anzahl Treffer");
        schema.putArray("required").add("idea_text");
        return schema;
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        String ideenText = Args.text(argumente, "idea_text");
        if (ideenText == null) {
            return new Fehler("Parameter 'idea_text' fehlt.");
        }
        int limit = Args.ganzzahl(argumente, "limit", STANDARD_LIMIT);

        List<Treffer> treffer = similarityService.findeAehnliche(ideenText, limit).stream()
                .map(t -> new Treffer(t.projektId(), t.titel(), t.kurzbeschreibung(),
                        stufe(t.punktzahl()), t.gemeinsameBegriffe()))
                .toList();

        return new Ergebnis(treffer.size(), treffer);
    }

    private String stufe(int punktzahl) {
        if (punktzahl >= 6) return "hoch";
        if (punktzahl >= 3) return "mittel";
        return "gering";
    }

    record Treffer(Long id, String titel, String kurzbeschreibung, String aehnlichkeit,
                   List<String> gemeinsameBegriffe) {}

    record Ergebnis(int anzahl, List<Treffer> treffer) {}

    record Fehler(String fehler) {}
}
