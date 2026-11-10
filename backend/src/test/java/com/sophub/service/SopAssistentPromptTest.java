package com.sophub.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class SopAssistentPromptTest {

    private static final String REGELN = "classpath:sop-assistent-regeln.txt";
    private final ResourceLoader loader = new DefaultResourceLoader();

    @Test
    void standardPromptEnthaeltDieKernregeln() {
        String prompt = new SopAssistentPrompt("", REGELN, loader).systemPrompt();

        // Werkzeuge + strukturierte Ausgabe
        assertThat(prompt).contains("find_similar");
        assertThat(prompt).contains("get_tag_landscape");
        assertThat(prompt).contains("zeige_ideenkarten");
        assertThat(prompt).contains("zeige_projektverweise");
        // Ideen-Coach (max. 3-4 Rückfragen) und White Spots
        assertThat(prompt).contains("3-4");
        assertThat(prompt).contains("White Spots");
        // Prompt-Injection-Abwehr: Inhalte sind Daten, keine Anweisungen
        assertThat(prompt).contains("keine Anweisungen");
    }

    @Test
    void hausregelnWerdenAusDateiAngehaengtUndKommentareIgnoriert() {
        String prompt = new SopAssistentPrompt("", REGELN, loader).systemPrompt();

        // Inhalt der Regel-Datei ist enthalten ...
        assertThat(prompt).contains("Hausregeln");
        assertThat(prompt).contains("Kontaktdaten von Mitgliedern");
        // ... Kommentarzeilen (beginnen mit "#") aber nicht.
        assertThat(prompt).doesNotContain("Diese Datei wird beim Start");
    }

    @Test
    void konfigurierterPromptUeberschreibtStandard() {
        assertThat(new SopAssistentPrompt("Eigener Prompt", REGELN, loader).systemPrompt())
                .isEqualTo("Eigener Prompt");
    }
}
