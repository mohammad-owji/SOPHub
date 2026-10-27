package com.sophub.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SopAssistentPromptTest {

    @Test
    void standardPromptEnthaeltDieKernregeln() {
        String prompt = new SopAssistentPrompt("").systemPrompt();

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
    void konfigurierterPromptUeberschreibtStandard() {
        assertThat(new SopAssistentPrompt("Eigener Prompt").systemPrompt()).isEqualTo("Eigener Prompt");
    }
}
