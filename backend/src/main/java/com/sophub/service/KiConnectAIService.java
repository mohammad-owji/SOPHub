package com.sophub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.sophub.service.werkzeug.Werkzeugkasten;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KI-Engine über KI:connect.nrw (OpenAI-kompatible Chat-Completions-API der Hochschule).
 * Einzige Implementierung von {@link AIService} - Konfiguration unter {@code kiconnect.*}
 * in den application.properties.
 * <p>
 * Die Antwort wird bewusst als Text gelesen und selbst als JSON ausgewertet.
 * So funktioniert es auch, wenn der Server einen falschen Content-Type
 * (z.B. application/octet-stream) mitschickt.
 */
@Service
public class KiConnectAIService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(KiConnectAIService.class);

    /** Maximale Anzahl Werkzeug-Aufrufe pro Anfrage (Token-/Schleifenschutz). */
    private static final int MAX_WERKZEUG_AUFRUFE = 5;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final ObjectMapper objectMapper;

    public KiConnectAIService(
            @Value("${kiconnect.api.url}") String apiUrl,
            @Value("${kiconnect.api.key:}") String apiKey,
            @Value("${kiconnect.model}") String model,
            @Value("${kiconnect.max-tokens:1000}") int maxTokens,
            ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10s bis Verbindung steht
        factory.setReadTimeout(180_000);     // bis 3 Min. auf die KI-Antwort warten
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generiereAntwort(String prompt) {
        return generiereAntwort(prompt, false);
    }

    @Override
    public String generiereAntwort(String prompt, boolean alsJson) {
        if (prompt == null || prompt.isBlank()) {
            throw new RuntimeException("Kein Text zur Verarbeitung übergeben.");
        }
        List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", prompt));
        return fuehreChatAus(messages, alsJson);
    }

    @Override
    public String chat(String systemPrompt, List<Nachricht> verlauf) {
        if (verlauf == null || verlauf.isEmpty()) {
            throw new RuntimeException("Kein Nachrichtenverlauf übergeben.");
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (Nachricht n : verlauf) {
            messages.add(Map.of("role", n.rolle(), "content", n.inhalt() == null ? "" : n.inhalt()));
        }
        return fuehreChatAus(messages, false);
    }

    @Override
    public String chatMitWerkzeugen(String systemPrompt, List<Nachricht> verlauf, Werkzeugkasten werkzeuge) {
        if (verlauf == null || verlauf.isEmpty()) {
            throw new RuntimeException("Kein Nachrichtenverlauf übergeben.");
        }
        if (werkzeuge == null || werkzeuge.werkzeuge().isEmpty()) {
            return chat(systemPrompt, verlauf); // keine Werkzeuge -> normaler Chat
        }

        List<Object> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (Nachricht n : verlauf) {
            messages.add(Map.of("role", n.rolle(), "content", n.inhalt() == null ? "" : n.inhalt()));
        }

        List<Map<String, Object>> toolSpezifikationen = baueToolSpezifikationen(werkzeuge.werkzeuge());
        int aufrufe = 0;

        // Mehrere Runden: KI darf Werkzeuge anfordern, bis sie eine Textantwort gibt.
        for (int runde = 0; runde <= MAX_WERKZEUG_AUFRUFE + 1; runde++) {
            boolean werkzeugeAnbieten = aufrufe < MAX_WERKZEUG_AUFRUFE;

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            if (maxTokens > 0) {
                body.put("max_tokens", maxTokens);
            }
            if (werkzeugeAnbieten) {
                body.put("tools", toolSpezifikationen);
                body.put("tool_choice", "auto");
            }

            JsonNode message = leseMessage(postChatCompletions(body));
            JsonNode toolCalls = werkzeugeAnbieten ? message.get("tool_calls") : null;

            if (toolCalls == null || !toolCalls.isArray() || toolCalls.isEmpty()) {
                JsonNode content = message.get("content");
                return (content == null || content.isNull()) ? "" : content.asText();
            }

            // Assistant-Nachricht (mit Tool-Aufrufen) in den Verlauf aufnehmen.
            Map<String, Object> assistant = new HashMap<>();
            assistant.put("role", "assistant");
            JsonNode content = message.get("content");
            assistant.put("content", (content == null || content.isNull()) ? null : content.asText());
            assistant.put("tool_calls", toolCalls);
            messages.add(assistant);

            for (JsonNode tc : toolCalls) {
                if (aufrufe >= MAX_WERKZEUG_AUFRUFE) {
                    break;
                }
                aufrufe++;
                JsonNode funktion = tc.get("function");
                String fname = funktion == null ? null : textWert(funktion.get("name"));
                JsonNode argumente = parseArgumente(funktion == null ? null : textWert(funktion.get("arguments")));
                String ergebnis = werkzeuge.ausfuehren(fname, argumente);

                Map<String, Object> toolMsg = new HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", textWert(tc.get("id")));
                toolMsg.put("content", ergebnis);
                messages.add(toolMsg);
            }
        }

        return "Ich konnte die Anfrage nicht abschließen. Bitte formuliere sie etwas einfacher.";
    }

    /** Gemeinsame Chat-Completions-Anfrage; liefert Text der ersten Antwort. */
    private String fuehreChatAus(List<Map<String, Object>> messages, boolean alsJson) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        // Die Obergrenze gilt fuer Chat-Antworten. Strukturierte JSON-Antworten
        // (z.B. KI-Ideenfinder, KI-Uebersicht) sind laenger und waeren sonst
        // abgeschnitten und damit kein gueltiges JSON mehr.
        if (maxTokens > 0 && !alsJson) {
            body.put("max_tokens", maxTokens);
        }
        if (alsJson) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return extractText(postChatCompletions(body));
    }

    /** Sendet den Request an die Chat-Completions-API und liefert die Rohantwort (JSON-Text). */
    private String postChatCompletions(Map<String, Object> body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("KI-Dienst ist nicht konfiguriert (kein KI:connect API-Key hinterlegt).");
        }
        long start = System.currentTimeMillis();
        try {
            String rohAntwort = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("KI-Antwort von Modell {} nach {} s erhalten.",
                    model, (System.currentTimeMillis() - start) / 1000);
            return rohAntwort;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RuntimeException("Die KI ist gerade überlastet (Rate Limit erreicht). Bitte versuchen Sie es in Kürze erneut.");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            String detail = serverFehlerText(e.getResponseBodyAsString());
            log.warn("KI-Zugriff abgelehnt (Status {}): {}", e.getStatusCode(), detail);
            String basis = "Der KI:connect-Zugang wurde abgelehnt. Bitte prüfen Sie die Konfiguration";
            throw new RuntimeException(detail == null ? basis + "." : basis + ": " + detail);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("KI-Anfrage fehlgeschlagen (Status {}): {}", e.getStatusCode(), kuerze(e.getResponseBodyAsString()));
            throw new RuntimeException("Die KI-Anfrage ist fehlgeschlagen (Status " + e.getStatusCode().value() + ").");
        } catch (ResourceAccessException e) {
            log.warn("KI nicht erreichbar nach {} s: {}", (System.currentTimeMillis() - start) / 1000, e.getMessage());
            throw new RuntimeException("Die KI hat nicht rechtzeitig geantwortet oder ist nicht erreichbar. Bitte versuchen Sie es erneut.");
        } catch (RestClientException e) {
            log.warn("KI-Antwort konnte nicht gelesen werden: {}", e.getMessage());
            throw new RuntimeException("Die Antwort der KI konnte nicht gelesen werden.");
        }
    }

    /** Baut die OpenAI-kompatible {@code tools}-Liste aus den Werkzeug-Spezifikationen. */
    private List<Map<String, Object>> baueToolSpezifikationen(List<Werkzeugkasten.Werkzeug> werkzeuge) {
        List<Map<String, Object>> liste = new ArrayList<>();
        for (Werkzeugkasten.Werkzeug w : werkzeuge) {
            Map<String, Object> funktion = new HashMap<>();
            funktion.put("name", w.name());
            funktion.put("description", w.beschreibung());
            funktion.put("parameters", w.parameterSchema());
            liste.add(Map.of("type", "function", "function", funktion));
        }
        return liste;
    }

    /** Liest choices[0].message als Baum (für Tool-Calls). */
    private JsonNode leseMessage(String rohAntwort) {
        if (rohAntwort == null || rohAntwort.isBlank()) {
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        JsonNode wurzel;
        try {
            wurzel = objectMapper.readTree(rohAntwort);
        } catch (Exception e) {
            log.warn("KI-Antwort ist kein gültiges JSON (Modell {}): {}", model, kuerze(rohAntwort));
            throw new RuntimeException("Die Antwort der KI konnte nicht gelesen werden.");
        }
        JsonNode choices = wurzel.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        JsonNode message = choices.get(0).get("message");
        if (message == null || message.isNull()) {
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        return message;
    }

    private JsonNode parseArgumente(String argumenteJson) {
        if (argumenteJson == null || argumenteJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(argumenteJson);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private String textWert(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText();
    }

    /** Liest choices[0].message.content aus der (als Text gelesenen) JSON-Antwort. */
    private String extractText(String rohAntwort) {
        if (rohAntwort == null || rohAntwort.isBlank()) {
            log.warn("KI-Antwort war leer (Modell {}).", model);
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }

        ChatResponse response;
        try {
            response = objectMapper.readValue(rohAntwort, ChatResponse.class);
        } catch (Exception e) {
            log.warn("KI-Antwort ist kein gültiges JSON (Modell {}): {}", model, kuerze(rohAntwort));
            throw new RuntimeException("Die Antwort der KI konnte nicht gelesen werden.");
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            log.warn("KI-Antwort ohne Auswahl (Modell {}): {}", model, kuerze(rohAntwort));
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        Message message = response.choices().get(0).message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            log.warn("KI-Antwort ohne Inhalt (Modell {}): {}", model, kuerze(rohAntwort));
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        return message.content();
    }

    /**
     * Liest die vom KI:connect-Server gelieferte Fehlermeldung aus dem Antwort-Body
     * ({@code {"error":{"message":"..."}}}) aus. So wird z.B. "API key temporarily
     * disabled" an den Nutzer durchgereicht, statt alles als "ungültig/abgelaufen"
     * zu verallgemeinern. {@code null}, wenn keine verwertbare Meldung vorliegt.
     */
    private String serverFehlerText(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode fehler = objectMapper.readTree(body).get("error");
            JsonNode nachricht = fehler == null ? null : fehler.get("message");
            if (nachricht != null && !nachricht.isNull()) {
                String text = nachricht.asText().trim();
                return text.isEmpty() ? null : text;
            }
        } catch (Exception ignored) {
            // Kein JSON / unerwartetes Format -> keine Detailmeldung.
        }
        return null;
    }

    /** Kürzt lange Texte für das Log. */
    private String kuerze(String text) {
        if (text == null) {
            return "(leer)";
        }
        return text.length() > 500 ? text.substring(0, 500) + " ..." : text;
    }

    private record ChatResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String role, String content) {}
}