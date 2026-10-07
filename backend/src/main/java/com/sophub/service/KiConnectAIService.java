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
import tools.jackson.databind.ObjectMapper;

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

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public KiConnectAIService(
            @Value("${kiconnect.api.url}") String apiUrl,
            @Value("${kiconnect.api.key:}") String apiKey,
            @Value("${kiconnect.model}") String model,
            ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10s bis Verbindung steht
        factory.setReadTimeout(180_000);     // bis 3 Min. auf die KI-Antwort warten
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generiereAntwort(String prompt) {
        return generiereAntwort(prompt, false);
    }

    @Override
    public String generiereAntwort(String prompt, boolean alsJson) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("KI-Dienst ist nicht konfiguriert (kein KI:connect API-Key hinterlegt).");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new RuntimeException("Kein Text zur Verarbeitung übergeben.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (alsJson) {
            body.put("response_format", Map.of("type", "json_object"));
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
            return extractText(rohAntwort);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RuntimeException("Die KI ist gerade überlastet (Rate Limit erreicht). Bitte versuchen Sie es in Kürze erneut.");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new RuntimeException("Der KI:connect-API-Key ist ungültig oder abgelaufen. Bitte prüfen Sie die Konfiguration.");
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