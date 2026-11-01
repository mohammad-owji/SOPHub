package com.sophub.service.werkzeug;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ausgabe-Werkzeug {@code zeige_ideenkarten}: Die KI übergibt 1-3 Projektideen,
 * die das Frontend als Ideenkarten rendert. Es werden keine Daten gelesen -
 * die Karten werden nur validiert/normalisiert und als Blöcke gesammelt.
 */
@Component
public class IdeenkartenTool implements ChatTool {

    private static final Set<String> AUFWAND = Set.of("klein", "mittel", "gross");
    private static final Set<String> MACHBARKEIT = Set.of("gruen", "gelb", "rot");

    private final ObjectMapper objectMapper;

    public IdeenkartenTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "zeige_ideenkarten";
    }

    @Override
    public String beschreibung() {
        return "Zeigt dem Nutzer Projektideen als Ideenkarten an. Nutze dies, wenn du konkrete "
                + "Projektvorschläge machst (realistisch für 2 Semester, 3er-Team, 19 CP).";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode karten = props.putObject("karten");
        karten.put("type", "array");
        ObjectNode item = karten.putObject("items");
        item.put("type", "object");
        ObjectNode kp = item.putObject("properties");
        kp.putObject("titel").put("type", "string");
        kp.putObject("problem").put("type", "string");
        kp.putObject("zielgruppe").put("type", "string");
        feldArray(kp, "mvp");
        feldArray(kp, "technologien");
        feldArray(kp, "risiken");
        feldArray(kp, "teamrollen");
        kp.putObject("aufwand").put("type", "string").put("description", "klein|mittel|gross");
        kp.putObject("machbarkeit").put("type", "string").put("description", "gruen|gelb|rot");
        item.putArray("required").add("titel");
        schema.putArray("required").add("karten");
        return schema;
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        List<JsonNode> bloecke = new ArrayList<>();
        JsonNode karten = argumente == null ? null : argumente.get("karten");
        if (karten != null && karten.isArray()) {
            for (JsonNode karte : karten) {
                String titel = Args.text(karte, "titel");
                if (titel == null) {
                    continue; // ohne Titel keine Karte
                }
                ObjectNode block = objectMapper.createObjectNode();
                block.put("typ", "idea_card");
                block.put("titel", titel);
                setzeText(block, karte, "problem");
                setzeText(block, karte, "zielgruppe");
                block.set("mvp", liste(karte, "mvp"));
                block.set("technologien", liste(karte, "technologien"));
                block.set("risiken", liste(karte, "risiken"));
                block.set("teamrollen", liste(karte, "teamrollen"));
                block.put("aufwand", enumWert(karte, "aufwand", AUFWAND, "mittel"));
                block.put("machbarkeit", enumWert(karte, "machbarkeit", MACHBARKEIT, "gelb"));
                bloecke.add(block);
            }
        }
        return new Werkzeugkasten.AusgabeBloecke(bloecke);
    }

    private void feldArray(ObjectNode props, String name) {
        ObjectNode arr = props.putObject(name);
        arr.put("type", "array");
        arr.putObject("items").put("type", "string");
    }

    private void setzeText(ObjectNode block, JsonNode quelle, String feld) {
        String wert = Args.text(quelle, feld);
        if (wert != null) {
            block.put(feld, wert);
        }
    }

    private ArrayNode liste(JsonNode quelle, String feld) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (String s : Args.textListe(quelle, feld)) {
            arr.add(s);
        }
        return arr;
    }

    private String enumWert(JsonNode quelle, String feld, Set<String> erlaubt, String standard) {
        String wert = Args.text(quelle, feld);
        return (wert != null && erlaubt.contains(wert.toLowerCase())) ? wert.toLowerCase() : standard;
    }
}
