package com.sophub.service.werkzeug;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ausgabe-Werkzeug {@code zeige_projektverweise}: Die KI verweist auf konkrete
 * bestehende Projekte (mit Ähnlichkeitsgrad und Begründung). Das Frontend rendert
 * sie als anklickbare Verweise auf die Projektdetailseite.
 */
@Component
public class ProjektverweiseTool implements ChatTool {

    private static final Set<String> AEHNLICHKEIT = Set.of("hoch", "mittel", "gering");

    private final ObjectMapper objectMapper;

    public ProjektverweiseTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "zeige_projektverweise";
    }

    @Override
    public String beschreibung() {
        return "Verweist den Nutzer auf konkrete bestehende Projekte (id + titel) mit Ähnlichkeitsgrad "
                + "(hoch/mittel/gering) und kurzer Begründung. Nutze nur ids aus Tool-Ergebnissen.";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode verweise = props.putObject("verweise");
        verweise.put("type", "array");
        ObjectNode item = verweise.putObject("items");
        item.put("type", "object");
        ObjectNode vp = item.putObject("properties");
        vp.putObject("id").put("type", "integer");
        vp.putObject("titel").put("type", "string");
        vp.putObject("aehnlichkeit").put("type", "string").put("description", "hoch|mittel|gering");
        vp.putObject("begruendung").put("type", "string");
        item.putArray("required").add("id").add("titel");
        schema.putArray("required").add("verweise");
        return schema;
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        List<JsonNode> bloecke = new ArrayList<>();
        JsonNode verweise = argumente == null ? null : argumente.get("verweise");
        if (verweise != null && verweise.isArray()) {
            for (JsonNode v : verweise) {
                JsonNode idNode = v.get("id");
                String titel = Args.text(v, "titel");
                if (idNode == null || !idNode.canConvertToLong() || titel == null) {
                    continue; // ohne gültige id + titel kein Verweis
                }
                ObjectNode block = objectMapper.createObjectNode();
                block.put("typ", "project_ref");
                block.put("id", idNode.asLong());
                block.put("titel", titel);
                String grad = Args.text(v, "aehnlichkeit");
                block.put("aehnlichkeit",
                        (grad != null && AEHNLICHKEIT.contains(grad.toLowerCase())) ? grad.toLowerCase() : "mittel");
                String begruendung = Args.text(v, "begruendung");
                if (begruendung != null) {
                    block.put("begruendung", begruendung);
                }
                bloecke.add(block);
            }
        }
        return new Werkzeugkasten.AusgabeBloecke(bloecke);
    }
}
