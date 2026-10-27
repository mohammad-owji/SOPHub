package com.sophub.service.werkzeug;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Kleine Helfer zum robusten Lesen der von der KI übergebenen Argumente. */
final class Args {

    private Args() {}

    static String text(JsonNode node, String feld) {
        JsonNode wert = node == null ? null : node.get(feld);
        if (wert == null || wert.isNull()) {
            return null;
        }
        String s = wert.asText();
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    static int ganzzahl(JsonNode node, String feld, int standard) {
        JsonNode wert = node == null ? null : node.get(feld);
        return (wert != null && wert.isNumber()) ? wert.asInt() : standard;
    }

    static List<String> textListe(JsonNode node, String feld) {
        List<String> liste = new ArrayList<>();
        JsonNode wert = node == null ? null : node.get(feld);
        if (wert != null && wert.isArray()) {
            for (JsonNode e : wert) {
                if (e != null && !e.isNull() && !e.asText().isBlank()) {
                    liste.add(e.asText().trim());
                }
            }
        }
        return liste;
    }
}
