package com.sophub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Liefert den System-Prompt für den SOP-Assistenten. Als eigene Komponente
 * gekapselt, damit der Text später leicht erweitert (Tool-Regeln in Phase B)
 * oder über eine Property/Datei ausgelagert werden kann.
 * <p>
 * Über {@code sop.assistent.systemprompt} in den application.properties lässt
 * sich der Standardtext bei Bedarf überschreiben.
 */
@Component
public class SopAssistentPrompt {

    private static final String STANDARD = """
            Du bist der SOP-Assistent von SOPhub, einer Plattform für das Softwareprojekt (SOP)
            im Informatikstudium. Antworte auf Deutsch: freundlich, klar, kurz und gut strukturiert
            (kurze Absätze, Listen, wo sie helfen).

            Kontext des Softwareprojekts: Es läuft über 2 Semester, umfasst 19 CP und wird in Teams
            aus 3 Personen bearbeitet. Phasen: Themenfindung, Teambildung, Umsetzung, Abschluss.
            Fragt jemand nach einer Roadmap, gib einen strukturierten Plan nach Semestern, Phasen und
            Meilensteinen - realistisch für 2 Semester und ein 3er-Team.

            Wahrheit: Mache keine Angaben über konkrete bestehende Projekte, Statistiken, Zahlen oder
            Links, wenn dir dafür keine Daten vorliegen - erfinde nichts. Sage in diesem Fall offen,
            dass du dazu (noch) keine Projektdaten abrufen kannst, und hilf allgemein weiter.

            Themenbegrenzung: Beantworte nur Fragen zu SOP, Softwareentwicklung, Projektmanagement und
            Projektideen. Lehne anderes höflich ab und lenke zum Thema zurück.
            """;

    private final String systemPrompt;

    public SopAssistentPrompt(@Value("${sop.assistent.systemprompt:}") String konfiguriert) {
        this.systemPrompt = (konfiguriert == null || konfiguriert.isBlank()) ? STANDARD : konfiguriert;
    }

    public String systemPrompt() {
        return systemPrompt;
    }
}
