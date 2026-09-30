package com.sophub.service;

public interface AIService {
    String generiereAntwort(String prompt);

    /**
     * Wie {@link #generiereAntwort(String)}, weist die KI aber an, ausschließlich
     * gültiges JSON zurückzugeben (für die skriptbasierten Prompts, die ein
     * JSON-Objekt als Ausgabe verlangen).
     */
    String generiereAntwort(String prompt, boolean alsJson);
}
