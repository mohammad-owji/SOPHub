package com.sophub.service;

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

    /** Eine Chat-Nachricht für die KI-Schnittstelle. {@code rolle} ist {@code user} oder {@code assistant}. */
    record Nachricht(String rolle, String inhalt) {}
}
