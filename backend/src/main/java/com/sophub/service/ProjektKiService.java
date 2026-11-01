package com.sophub.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.sophub.model.Dokument;
import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Erzeugt die KI-Übersicht eines Projekts aus allen zugehörigen Dokumenten
 * (Map-Reduce nach den externen Prompt-Skripten):
 * <ol>
 *     <li>Map: {@code summarize_document} pro Dokument → JSON-Einzelzusammenfassung</li>
 *     <li>Reduce: {@code summarize_project} → Projektzusammenfassung (JSON)</li>
 *     <li>Reduce: {@code extract_keywords} → kategorisierte Stichwörter (JSON)</li>
 * </ol>
 * Die Ergebnisse werden am Projekt bzw. am Dokument gecacht, sodass beim Öffnen
 * des Projekts nur gelesen und nicht erneut die KI aufgerufen wird.
 * Schritt 2 und 3 laufen gleichzeitig, weil sie unabhängig voneinander sind. Der
 * Dokumenttext wird vor jedem KI-Aufruf über den {@link AnonymizerService}
 * anonymisiert.
 */
@Service
public class ProjektKiService {

    private static final Logger log = LoggerFactory.getLogger(ProjektKiService.class);

    // Datenschutz-sichere Fassung: keine internen Details (F27).
    private static final String SICHTBARKEIT = "oeffentlich";

    private final ProjektRepository projektRepository;
    private final DokumentRepository dokumentRepository;
    private final TagRepository tagRepository;
    private final AIService aiService;
    private final PromptLoader promptLoader;
    private final AnonymizerService anonymizerService;
    private final ObjectMapper objectMapper;

    public ProjektKiService(ProjektRepository projektRepository,
                            DokumentRepository dokumentRepository,
                            TagRepository tagRepository,
                            AIService aiService,
                            PromptLoader promptLoader,
                            AnonymizerService anonymizerService,
                            ObjectMapper objectMapper) {
        this.projektRepository = projektRepository;
        this.dokumentRepository = dokumentRepository;
        this.tagRepository = tagRepository;
        this.aiService = aiService;
        this.promptLoader = promptLoader;
        this.anonymizerService = anonymizerService;
        this.objectMapper = objectMapper;
    }

    /**
     * Liefert die KI-Übersicht des Projekts. Ist sie bereits vorhanden und wird
     * keine Neuerzeugung erzwungen, wird die gecachte Fassung zurückgegeben;
     * andernfalls wird sie neu erzeugt und gespeichert.
     *
     * @param projektId Projekt-PK
     * @param neu       {@code true} erzwingt eine Neuerzeugung der Projektzusammenfassung und
     *                  der Stichwörter. Die Einzelzusammenfassungen der Dokumente werden
     *                  wiederverwendet, weil sich ein bereits hochgeladenes Dokument nicht ändert.
     */
    public KiUebersicht uebersicht(Long projektId, boolean neu) {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

        List<Dokument> dokumente = dokumentRepository.findByProjektId(projektId).stream()
                .filter(d -> d.getExtrahierterText() != null && !d.getExtrahierterText().isBlank())
                .toList();
        String signatur = signatur(dokumente);

        // Cache gilt nur, wenn nicht erzwungen, vorhanden UND der Dokumentsatz
        // unverändert ist (keine Datei hinzugekommen oder gelöscht).
        if (!neu && istVorhanden(projekt) && signatur.equals(projekt.getKiDokumentIds())) {
            return new KiUebersicht(
                    projekt.getId(),
                    leseJson(projekt.getKiZusammenfassungJson()),
                    leseJson(projekt.getKiStichwoerterJson()),
                    projekt.getKiUebersichtAm());
        }

        if (dokumente.isEmpty()) {
            throw new RuntimeException("Für dieses Projekt liegen keine auswertbaren Dokumente vor.");
        }

        // 1. Map: Einzelzusammenfassung je Dokument. Bereits vorhandene werden immer
        // wiederverwendet - neu erzeugt wird nur für neue Dokumente.
        ArrayNode material = objectMapper.createArrayNode();
        for (Dokument dokument : dokumente) {
            JsonNode analyse = leseJson(einzelZusammenfassung(projekt, dokument));
            ObjectNode eintrag = objectMapper.createObjectNode();
            eintrag.put("datei", dokument.getDateiName());
            eintrag.put("typ", dokument.getTyp());
            eintrag.set("analyse", analyse);
            material.add(eintrag);
        }
        String dokumentZusammenfassungen = schreibeJson(material);
        String dokumentListe = dokumente.stream()
                .map(d -> "- " + d.getDateiName() + " (Typ: " + d.getTyp() + ")")
                .collect(Collectors.joining("\n"));

        // 2. Reduce: Projektzusammenfassung.
        Map<String, String> projektWerte = new HashMap<>();
        projektWerte.put("PROJEKT_TITEL", wertOderLeer(projekt.getTitel()));
        projektWerte.put("PROJEKT_STATUS", wertOderLeer(projekt.getStatus()));
        projektWerte.put("PROJEKT_ZEITRAUM", wertOderLeer(projekt.getSemester()));
        projektWerte.put("PROJEKT_KURZBESCHREIBUNG", wertOderLeer(projekt.getBeschreibung()));
        projektWerte.put("SICHTBARKEIT", SICHTBARKEIT);
        projektWerte.put("DOKUMENT_ZUSAMMENFASSUNGEN", dokumentZusammenfassungen);
        String zusammenfassungsPrompt = bereiteSkriptVor("summarize_project", projektWerte, projektId);

        // 3. Reduce: kategorisierte Stichwörter.
        Map<String, String> stichwortWerte = new HashMap<>();
        stichwortWerte.put("PROJEKT_TITEL", wertOderLeer(projekt.getTitel()));
        stichwortWerte.put("PROJEKT_STATUS", wertOderLeer(projekt.getStatus()));
        stichwortWerte.put("DOKUMENT_LISTE", dokumentListe);
        stichwortWerte.put("DOKUMENT_ZUSAMMENFASSUNGEN", dokumentZusammenfassungen);
        stichwortWerte.put("BEKANNTE_TAGS", bekannteTags());
        String stichwortPrompt = bereiteSkriptVor("extract_keywords", stichwortWerte, projektId);

        // Beide KI-Aufrufe gleichzeitig starten und auf beide Ergebnisse warten.
        CompletableFuture<String> zusammenfassungAufruf =
                CompletableFuture.supplyAsync(() -> frageKi(zusammenfassungsPrompt));
        CompletableFuture<String> stichwortAufruf =
                CompletableFuture.supplyAsync(() -> frageKi(stichwortPrompt));
        String zusammenfassungJson = warteAuf(zusammenfassungAufruf);
        String stichwoerterJson = warteAuf(stichwortAufruf);

        projekt.setKiZusammenfassungJson(zusammenfassungJson);
        projekt.setKiStichwoerterJson(stichwoerterJson);
        projekt.setKiDokumentIds(signatur);
        projekt.setKiUebersichtAm(LocalDateTime.now());
        projektRepository.save(projekt);

        return new KiUebersicht(projekt.getId(), leseJson(zusammenfassungJson),
                leseJson(stichwoerterJson), projekt.getKiUebersichtAm());
    }

    /** Erzeugt (oder liest aus dem Cache) die Einzelzusammenfassung eines Dokuments. */
    private String einzelZusammenfassung(Projekt projekt, Dokument dokument) {
        if (dokument.getKiZusammenfassungJson() != null && !dokument.getKiZusammenfassungJson().isBlank()) {
            return dokument.getKiZusammenfassungJson();
        }
        Map<String, String> werte = new HashMap<>();
        werte.put("PROJEKT_TITEL", wertOderLeer(projekt.getTitel()));
        werte.put("DOKUMENT_DATEINAME", wertOderLeer(dokument.getDateiName()));
        werte.put("DOKUMENT_TYP_HINWEIS", wertOderLeer(dokument.getTyp()));
        werte.put("DOKUMENT_INHALT", wertOderLeer(dokument.getExtrahierterText()));

        String json = rufeSkriptAuf("summarize_document", werte, projekt.getId());
        dokument.setKiZusammenfassungJson(json);
        dokumentRepository.save(dokument);
        return json;
    }

    /** Bereitet das Prompt-Skript vor und ruft die KI direkt auf (für die Einzelzusammenfassungen). */
    private String rufeSkriptAuf(String skriptId, Map<String, String> werte, Long projektId) {
        return frageKi(bereiteSkriptVor(skriptId, werte, projektId));
    }

    /**
     * Lädt das Prompt-Skript, befüllt die Platzhalter und anonymisiert den
     * (potenziell personenbezogenen) USER-Teil. Läuft bewusst im normalen
     * Thread, weil hier die Datenbank gelesen wird.
     */
    private String bereiteSkriptVor(String skriptId, Map<String, String> werte, Long projektId) {
        PromptLoader.RenderedPrompt prompt = promptLoader.lade(skriptId, werte);
        String user = anonymizerService.anonymisiere(prompt.user(), projektId);
        return prompt.system() + "\n\n" + user;
    }

    /** Ruft die KI im JSON-Modus auf (ohne Datenbankzugriff, darf parallel laufen). */
    private String frageKi(String fertigerPrompt) {
        return bereinigeJson(aiService.generiereAntwort(fertigerPrompt, true));
    }

    /** Wartet auf einen parallelen KI-Aufruf und gibt dessen Fehlermeldung unverändert weiter. */
    private String warteAuf(CompletableFuture<String> aufruf) {
        try {
            return aufruf.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException fehler) {
                throw fehler;
            }
            throw new RuntimeException("Die KI-Anfrage ist fehlgeschlagen.", e.getCause());
        }
    }

    /** Stabile Kennung des aktuellen Dokumentsatzes (sortierte IDs), zur Änderungserkennung. */
    private String signatur(List<Dokument> dokumente) {
        return dokumente.stream()
                .map(Dokument::getId)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private boolean istVorhanden(Projekt projekt) {
        return projekt.getKiZusammenfassungJson() != null && !projekt.getKiZusammenfassungJson().isBlank()
                && projekt.getKiStichwoerterJson() != null && !projekt.getKiStichwoerterJson().isBlank();
    }

    private String bekannteTags() {
        return tagRepository.findAll().stream()
                .map(Tag::getName)
                .collect(Collectors.joining(", "));
    }

    /** Entfernt einen evtl. doch vorhandenen Markdown-Codefence um das JSON. */
    private String bereinigeJson(String antwort) {
        if (antwort == null) {
            return null;
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

    /** Wandelt einen gespeicherten JSON-String in einen JsonNode um (robust bei Fehlern). */
    private JsonNode leseJson(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("KI-Antwort ist kein gültiges JSON, gebe Rohtext zurück: {}", e.getMessage());
            return objectMapper.getNodeFactory().textNode(json);
        }
    }

    private String schreibeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException("JSON konnte nicht serialisiert werden.", e);
        }
    }

    private String wertOderLeer(String wert) {
        return (wert == null || wert.isBlank()) ? "-" : wert;
    }

    /** Fertige Projekt-KI-Übersicht für die API. */
    public record KiUebersicht(Long projektId, JsonNode projektZusammenfassung,
                               JsonNode stichwoerter, LocalDateTime generiertAm) {}
}