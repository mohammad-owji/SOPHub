package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.repository.ProjektRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * KI-Ideenfinder: schlaegt Studierenden ohne eigenes Thema passende Projektideen vor.
 * Nutzt das Prompt-Skript {@code prompts/generate_project_ideas.md} und KI:connect (wie die
 * Projekt-Zusammenfassung). Die Titel vorhandener Projekte werden mitgegeben, damit die KI
 * keine Dubletten vorschlaegt. Es werden keine personenbezogenen Daten an die KI geschickt.
 */
@Service
public class ProjektIdeenService {

    private static final Logger log = LoggerFactory.getLogger(ProjektIdeenService.class);

    private static final String SKRIPT = "generate_project_ideas";
    private static final int ANZAHL_IDEEN = 3;

    // Grenzen, damit niemand riesige Texte an die KI schickt
    private static final int MAX_EINTRAEGE = 30;
    private static final int MAX_LAENGE_EINTRAG = 80;
    private static final int MAX_LAENGE_WUNSCH = 500;
    private static final int MAX_BESTEHENDE_PROJEKTE = 80;

    private static final Set<String> SCHWIERIGKEITEN = Set.of("Einsteiger", "Mittel", "Anspruchsvoll");
    private static final Set<String> VARIANTEN = Set.of("einfacher", "anspruchsvoller", "andere");

    private final AIService aiService;
    private final PromptLoader promptLoader;
    private final ProjektRepository projektRepository;
    private final ObjectMapper objectMapper;

    public ProjektIdeenService(AIService aiService, PromptLoader promptLoader,
                               ProjektRepository projektRepository, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.promptLoader = promptLoader;
        this.projektRepository = projektRepository;
        this.objectMapper = objectMapper;
    }

    /** Angaben aus dem Ideenfinder (alle Felder optional). */
    public record IdeenAnfrage(String fachbereich, String studiengang, String projektart,
                               Integer gruppengroesse, String schwierigkeit,
                               List<String> themenfelder, String zielgruppe,
                               List<String> technologienBekannt, List<String> technologienLernen,
                               List<String> tools, String wuensche, String variante,
                               List<String> bisherigeVorschlaege) {}

    /**
     * Fragt die KI nach Projektideen und gibt das JSON {@code { "ideen": [ ... ] }} zurueck.
     */
    public JsonNode ideenVorschlagen(IdeenAnfrage anfrage) {
        if (anfrage == null) {
            throw new IllegalArgumentException("Es wurden keine Angaben gesendet.");
        }

        String schwierigkeit = SCHWIERIGKEITEN.contains(anfrage.schwierigkeit()) ? anfrage.schwierigkeit() : "Mittel";
        String variante = VARIANTEN.contains(anfrage.variante()) ? anfrage.variante() : "";
        int gruppengroesse = anfrage.gruppengroesse() == null ? 3 : Math.max(1, Math.min(10, anfrage.gruppengroesse()));

        Map<String, String> werte = new HashMap<>();
        werte.put("FACHBEREICH", text(anfrage.fachbereich()));
        werte.put("STUDIENGANG", text(anfrage.studiengang()));
        werte.put("PROJEKTART", text(anfrage.projektart()));
        werte.put("GRUPPENGROESSE", gruppengroesse + (gruppengroesse == 1 ? " Person" : " Personen"));
        werte.put("SCHWIERIGKEIT", schwierigkeit);
        werte.put("THEMENFELDER", liste(anfrage.themenfelder()));
        werte.put("ZIELGRUPPE", text(anfrage.zielgruppe()));
        werte.put("TECHNOLOGIEN_BEKANNT", liste(anfrage.technologienBekannt()));
        werte.put("TECHNOLOGIEN_LERNEN", liste(anfrage.technologienLernen()));
        werte.put("TOOLS", liste(anfrage.tools()));
        werte.put("WUENSCHE", kuerze(anfrage.wuensche(), MAX_LAENGE_WUNSCH));
        werte.put("VARIANTE", variante.isEmpty() ? "keine" : variante);
        werte.put("ANZAHL", String.valueOf(ANZAHL_IDEEN));
        werte.put("BESTEHENDE_PROJEKTE", bestehendeProjekte());
        werte.put("BISHERIGE_VORSCHLAEGE", liste(anfrage.bisherigeVorschlaege()));

        PromptLoader.RenderedPrompt prompt = promptLoader.lade(SKRIPT, werte);

        long start = System.currentTimeMillis();
        String antwort = bereinigeJson(aiService.generiereAntwort(prompt.kombiniert(), true));
        log.info("KI-Ideenfinder: Antwort nach {} ms", System.currentTimeMillis() - start);

        JsonNode json;
        try {
            json = objectMapper.readTree(antwort);
        } catch (Exception e) {
            log.warn("KI-Ideenfinder: Antwort ist kein gueltiges JSON: {}", e.getMessage());
            throw new RuntimeException("Die KI hat keine verwertbaren Ideen geliefert. Bitte versuchen Sie es erneut.");
        }
        if (json == null || !json.has("ideen") || !json.get("ideen").isArray() || json.get("ideen").isEmpty()) {
            throw new RuntimeException("Die KI hat keine verwertbaren Ideen geliefert. Bitte versuchen Sie es erneut.");
        }
        return json;
    }

    // Titel aller vorhandenen Projekte (ohne Personen), damit die KI keine Dubletten vorschlaegt
    private String bestehendeProjekte() {
        String titel = projektRepository.findAll().stream()
                .map(Projekt::getTitel)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .limit(MAX_BESTEHENDE_PROJEKTE)
                .map(t -> "- " + kuerze(t, 120))
                .collect(Collectors.joining("\n"));
        return titel.isEmpty() ? "keine" : titel;
    }

    private String liste(List<String> eintraege) {
        if (eintraege == null || eintraege.isEmpty()) {
            return "keine Angabe";
        }
        String ergebnis = eintraege.stream()
                .filter(Objects::nonNull)
                .map(e -> kuerze(e, MAX_LAENGE_EINTRAG))
                .filter(e -> !e.isEmpty())
                .distinct()
                .limit(MAX_EINTRAEGE)
                .collect(Collectors.joining(", "));
        return ergebnis.isEmpty() ? "keine Angabe" : ergebnis;
    }

    private String text(String wert) {
        String gekuerzt = kuerze(wert, MAX_LAENGE_EINTRAG * 2);
        return gekuerzt.isEmpty() ? "keine Angabe" : gekuerzt;
    }

    // Zeilenumbrueche und Markierungen entfernen, Laenge begrenzen
    private String kuerze(String wert, int maxLaenge) {
        if (wert == null) {
            return "";
        }
        String bereinigt = wert.replace("<<<", "").replace(">>>", "").replaceAll("\\s+", " ").trim();
        return bereinigt.length() > maxLaenge ? bereinigt.substring(0, maxLaenge) : bereinigt;
    }

    /** Entfernt einen evtl. vorhandenen Markdown-Codefence um das JSON. */
    private String bereinigeJson(String antwort) {
        if (antwort == null) {
            return "";
        }
        String bereinigt = antwort.trim();
        if (bereinigt.startsWith("```")) {
            int start = bereinigt.indexOf('\n');
            int ende = bereinigt.lastIndexOf("```");
            if (start >= 0 && ende > start) {
                bereinigt = bereinigt.substring(start + 1, ende).trim();
            }
        }
        return bereinigt;
    }
}