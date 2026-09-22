package com.sophub.service;

import com.sophub.config.ErlaubteTags;

/**
 * Baut die Prompt-Texte für die verschiedenen KI-Funktionen.
 * Es fließen bewusst nur unkritische Projektfelder ein (Titel, Beschreibung, Technologien) -
 * niemals personenbezogene Daten aus der users-Tabelle.
 */
public class PromptTemplates {

    private PromptTemplates() {}

    public static String projektZusammenfassung(String titel, String beschreibung, String technologien) {
        return """
                Fasse das folgende Softwareprojekt in 2 bis 3 sachlichen Sätzen auf Deutsch zusammen.
                Nutze ausschließlich die gegebenen Informationen, erfinde nichts hinzu.

                Titel: %s
                Beschreibung: %s
                Technologien: %s
                """.formatted(wertOderPlatzhalter(titel), wertOderPlatzhalter(beschreibung), wertOderPlatzhalter(technologien));
    }

    public static String dokumentZusammenfassung(String dokumentText) {
        return """
                Fasse den folgenden Dokumenttext in 2 bis 3 sachlichen Sätzen auf Deutsch zusammen.
                Nutze ausschließlich die gegebenen Informationen, erfinde nichts hinzu.

                Text:
                %s
                """.formatted(wertOderPlatzhalter(dokumentText));
    }

    public static String autoTagging(String text) {
        String tagListe = String.join(", ", ErlaubteTags.ALLE);
        return """
                Analysiere den folgenden Text und wähle ausschließlich passende Tags aus dieser festen Liste aus:
                %s

                Gib NUR eine einfache, kommagetrennte Liste der passenden Tags zurück (z. B. "Backend, REST, PostgreSQL").
                Erfinde keine neuen Tags und verwende ausschließlich Begriffe aus der obigen Liste.
                Wenn kein Tag passt, antworte mit einem leeren String.

                Text:
                %s
                """.formatted(tagListe, wertOderPlatzhalter(text));
    }

    private static String wertOderPlatzhalter(String wert) {
        return (wert == null || wert.isBlank()) ? "-" : wert;
    }
}
