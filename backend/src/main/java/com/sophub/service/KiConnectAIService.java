package com.sophub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KI-Engine über KI:connect.nrw (OpenAI-kompatible Chat-Completions-API der Hochschule).
 * Einzige Implementierung von {@link AIService} - Konfiguration unter {@code kiconnect.*}
 * in den application.properties.
 */
@Service
public class KiConnectAIService implements AIService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public KiConnectAIService(
            @Value("${kiconnect.api.url}") String apiUrl,
            @Value("${kiconnect.api.key:}") String apiKey,
            @Value("${kiconnect.model}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10s bis Verbindung steht
        factory.setReadTimeout(180_000);     // bis 3 Min. auf die KI-Antwort warten
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.model = model;
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

        try {
            ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);

            return extractText(response);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RuntimeException("Die KI ist gerade überlastet (Rate Limit erreicht). Bitte versuche es in Kürze erneut.");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new RuntimeException("Der KI:connect-API-Key ist ungültig oder abgelaufen. Bitte prüfe die Konfiguration.");
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Die KI konnte nicht erreicht werden. Bitte prüfe die Netzwerkverbindung.");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Die KI-Anfrage ist fehlgeschlagen.");
        }
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        Message message = response.choices().get(0).message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        return message.content();
    }

    private record ChatResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String role, String content) {}
}
