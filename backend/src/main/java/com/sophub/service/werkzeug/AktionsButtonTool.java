package com.sophub.service.werkzeug;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ausgabe-Werkzeug {@code zeige_aktion}: Zeigt dem Nutzer einen Hinweis mit
 * Schaltfläche an, die ihn direkt zu einer passenden Funktion der Plattform
 * führt (z. B. "Projekt erstellen" oder den KI-Ideenfinder).
 * <p>
 * Die Ziele (Links) sind bewusst SERVERSEITIG festgelegt: Die KI wählt nur den
 * Aktions-Schlüssel, nie die URL oder den Text. So kann das Modell keine
 * beliebigen oder fremden Links in den Chat schreiben.
 */
@Component
public class AktionsButtonTool implements ChatTool {

    /** Eine anklickbare Aktion: Überschrift, Erklärungstext, Button-Beschriftung und internes Ziel. */
    private record Aktion(String titel, String text, String label, String ziel) {}

    // Feste, erlaubte Aktionen. Schlüssel = das, was die KI übergibt.
    private static final Map<String, Aktion> AKTIONEN = new LinkedHashMap<>();
    static {
        AKTIONEN.put("projekt_erstellen", new Aktion(
                "Projekt erstellen",
                "Dafür gibt es in SOPhub ein eigenes Werkzeug. Darüber legst du dein Projekt mit "
                        + "Titel, Beschreibung, Betreuung und Team an.",
                "Projekt erstellen",
                "/create-project"));
        AKTIONEN.put("ideenfinder", new Aktion(
                "KI-Ideenfinder öffnen",
                "Mit dem KI-Ideenfinder entwickelst du in wenigen Schritten passende Projektideen "
                        + "und übernimmst eine davon direkt in ein neues Projekt.",
                "Zum KI-Ideenfinder",
                "/ideas"));
    }

    private final ObjectMapper objectMapper;

    public AktionsButtonTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "zeige_aktion";
    }

    @Override
    public String beschreibung() {
        return "Zeigt dem Nutzer eine Schaltfläche, die ihn zu einer Funktion der Plattform führt. "
                + "Nutze dies, wenn der Nutzer etwas tun möchte, wofür es ein Werkzeug gibt - z. B. "
                + "ein Projekt anlegen (aktion=projekt_erstellen) oder mit KI eine Projektidee finden "
                + "(aktion=ideenfinder). Erkläre kurz im Text und rufe dann dieses Werkzeug auf.";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode aktion = props.putObject("aktion");
        aktion.put("type", "string");
        aktion.put("description", "Welche Aktion angeboten wird: " + String.join(" | ", AKTIONEN.keySet()));
        aktion.putArray("enum").add("projekt_erstellen").add("ideenfinder");
        schema.putArray("required").add("aktion");
        return schema;
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        List<JsonNode> bloecke = new ArrayList<>();
        String schluessel = Args.text(argumente, "aktion");
        Aktion aktion = schluessel == null ? null : AKTIONEN.get(schluessel.toLowerCase());
        if (aktion != null) {
            ObjectNode block = objectMapper.createObjectNode();
            block.put("typ", "action");
            block.put("aktion", schluessel.toLowerCase());
            block.put("titel", aktion.titel());
            block.put("text", aktion.text());
            block.put("label", aktion.label());
            block.put("ziel", aktion.ziel());
            bloecke.add(block);
        }
        return new Werkzeugkasten.AusgabeBloecke(bloecke);
    }
}
