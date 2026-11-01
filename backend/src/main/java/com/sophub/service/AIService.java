package com.sophub.service;

import com.sophub.service.werkzeug.Werkzeugkasten;

import java.util.List;

public interface AIService {
    String generiereAntwort(String prompt);

    /**
     * Wie {@link #generiereAntwort(String)}, weist die KI aber an, ausschließlich
     * gültiges JSON zurückzugeben (für die skriptbasierten Prompts, die ein
     * JSON-Objekt als Ausgabe verlangen).
     */
    String generiereAntwort(String prompt, boolean alsJson);

    /**
     * Mehrstufiger Chat mit Rollen: ein optionaler System-Prompt und ein
     * Nachrichtenverlauf (Rollen {@code user}/{@code assistant}). Liefert den
     * reinen Antworttext der KI. Grundlage für den SOP-Assistenten.
     *
     * @param systemPrompt System-Anweisung (darf {@code null}/leer sein)
     * @param verlauf      chronologischer Verlauf, mindestens eine Nachricht
     */
    String chat(String systemPrompt, List<Nachricht> verlauf);

    /**
     * Wie {@link #chat(String, List)}, bietet der KI aber Werkzeuge an (Function-/Tool-Calling).
     * Fordert die KI ein Werkzeug an, wird es ausgeführt und das Ergebnis zurückgegeben,
     * bis die KI eine finale Textantwort liefert (begrenzte Anzahl Durchläufe). Unterstützt
     * das Modell keine Werkzeuge, verhält es sich wie ein normaler Chat.
     */
    String chatMitWerkzeugen(String systemPrompt, List<Nachricht> verlauf, Werkzeugkasten werkzeuge);

    /** Eine Chat-Nachricht für die KI-Schnittstelle. {@code rolle} ist {@code user} oder {@code assistant}. */
    record Nachricht(String rolle, String inhalt) {}
}
