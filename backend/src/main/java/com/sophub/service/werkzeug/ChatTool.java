package com.sophub.service.werkzeug;

import tools.jackson.databind.JsonNode;

/**
 * Ein vom SOP-Assistenten per Tool-Calling aufrufbares Werkzeug.
 * <p>
 * Jedes Werkzeug läuft im Kontext des eingeloggten Nutzers ({@code benutzername})
 * und muss selbst dieselben Zugriffsregeln wie der Rest der App einhalten:
 * Aussagen über bestehende Projekte stammen ausschließlich aus echten
 * Datenbankabfragen, interne Dokumentinhalte nur für Berechtigte.
 */
public interface ChatTool {

    /** Technischer Name (wird der KI als Funktionsname angeboten). */
    String name();

    /** Kurzbeschreibung für die KI (wann das Werkzeug zu nutzen ist). */
    String beschreibung();

    /** JSON-Schema der Parameter (OpenAI-kompatibel). */
    JsonNode parameterSchema();

    /**
     * Führt das Werkzeug aus und liefert ein serialisierbares, kompaktes Ergebnis.
     *
     * @param argumente    die von der KI übergebenen Argumente (nie {@code null})
     * @param benutzername der eingeloggte Nutzer (für Berechtigungsprüfungen)
     */
    Object ausfuehren(JsonNode argumente, String benutzername);
}
