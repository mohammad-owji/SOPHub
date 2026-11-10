package com.sophub.service.werkzeug;

import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * An einen bestimmten Nutzer gebundener Satz verfügbarer Werkzeuge.
 * Die KI-Schnittstelle liest die {@link #werkzeuge()} als Funktionsangebot und
 * ruft Treffer über {@link #ausfuehren(String, JsonNode)} auf; das Ergebnis ist
 * bereits als JSON-String serialisiert (zum Zurückgeben an die KI).
 */
public interface Werkzeugkasten {

    List<Werkzeug> werkzeuge();

    /** Führt das Werkzeug mit dem Namen aus; liefert das Ergebnis als JSON-String. */
    String ausfuehren(String name, JsonNode argumente);

    /**
     * Strukturierte Blöcke (idea_card/project_ref), die von Ausgabe-Werkzeugen
     * während der Aufrufe gesammelt wurden. Werden vom Chat an der KI-Nachricht
     * gespeichert und vom Frontend als Karten gerendert.
     */
    default List<JsonNode> gesammelteBloecke() {
        return List.of();
    }

    /** Spezifikation eines Werkzeugs für die KI-Schnittstelle. */
    record Werkzeug(String name, String beschreibung, JsonNode parameterSchema) {}

    /** Rückgabetyp von Ausgabe-Werkzeugen: zu rendernde Blöcke statt Datenabfrage. */
    record AusgabeBloecke(List<JsonNode> bloecke) {}
}
