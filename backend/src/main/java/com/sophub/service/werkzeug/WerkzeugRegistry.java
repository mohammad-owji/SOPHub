package com.sophub.service.werkzeug;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Sammelt alle {@link ChatTool}-Beans und gibt sie als nutzergebundenen
 * {@link Werkzeugkasten} aus. Jeder Aufruf läuft im Kontext des eingeloggten
 * Nutzers; das Ergebnis wird als kompakter JSON-String zurückgegeben.
 */
@Service
public class WerkzeugRegistry {

    private final List<ChatTool> tools;
    private final ObjectMapper objectMapper;

    public WerkzeugRegistry(List<ChatTool> tools, ObjectMapper objectMapper) {
        this.tools = tools;
        this.objectMapper = objectMapper;
    }

    public Werkzeugkasten fuer(String benutzername) {
        return new GebundenerKasten(benutzername);
    }

    private ChatTool finde(String name) {
        return tools.stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }

    private String fehlerJson(String nachricht) {
        try {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("fehler", nachricht);
            return objectMapper.writeValueAsString(n);
        } catch (Exception e) {
            return "{\"fehler\":\"interner Fehler\"}";
        }
    }

    private final class GebundenerKasten implements Werkzeugkasten {
        private final String benutzername;
        private final List<JsonNode> bloecke = new java.util.ArrayList<>();

        private GebundenerKasten(String benutzername) {
            this.benutzername = benutzername;
        }

        @Override
        public List<Werkzeug> werkzeuge() {
            return tools.stream()
                    .map(t -> new Werkzeug(t.name(), t.beschreibung(), t.parameterSchema()))
                    .toList();
        }

        @Override
        public String ausfuehren(String name, JsonNode argumente) {
            ChatTool tool = finde(name);
            if (tool == null) {
                return fehlerJson("Unbekanntes Werkzeug: " + name);
            }
            try {
                JsonNode args = (argumente == null || argumente.isNull())
                        ? objectMapper.createObjectNode() : argumente;
                Object ergebnis = tool.ausfuehren(args, benutzername);
                // Ausgabe-Werkzeuge liefern Blöcke zum Rendern: einsammeln + kurzen Beleg zurückgeben.
                if (ergebnis instanceof Werkzeugkasten.AusgabeBloecke ausgabe) {
                    bloecke.addAll(ausgabe.bloecke());
                    ObjectNode beleg = objectMapper.createObjectNode();
                    beleg.put("ok", true);
                    beleg.put("gespeicherte_bloecke", ausgabe.bloecke().size());
                    return objectMapper.writeValueAsString(beleg);
                }
                return objectMapper.writeValueAsString(ergebnis);
            } catch (Exception e) {
                return fehlerJson("Werkzeug '" + name + "' fehlgeschlagen: " + e.getMessage());
            }
        }

        @Override
        public List<JsonNode> gesammelteBloecke() {
            return bloecke;
        }
    }
}
