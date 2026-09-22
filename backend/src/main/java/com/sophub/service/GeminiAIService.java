package com.sophub.service;

import com.sophub.config.AIConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService implements AIService {

    private final RestClient geminiRestClient;
    private final AIConfig aiConfig;

    public GeminiAIService(RestClient geminiRestClient, AIConfig aiConfig) {
        this.geminiRestClient = geminiRestClient;
        this.aiConfig = aiConfig;
    }

    @Override
    public String generiereAntwort(String prompt) {
        if (aiConfig.getApiKey() == null || aiConfig.getApiKey().isBlank()) {
            throw new RuntimeException("KI-Dienst ist nicht konfiguriert (kein API-Key hinterlegt).");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new RuntimeException("Kein Text zur Verarbeitung übergeben.");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            GeminiResponse response = geminiRestClient.post()
                    .uri("/models/{model}:generateContent?key={key}", aiConfig.getModel(), aiConfig.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(GeminiResponse.class);

            return extractText(response);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RuntimeException("Die KI ist gerade überlastet (Rate Limit erreicht). Bitte versuche es in Kürze erneut.");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new RuntimeException("Der KI-API-Key ist ungültig oder fehlt. Bitte prüfe die Konfiguration.");
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Die KI konnte nicht erreicht werden. Bitte prüfe die Netzwerkverbindung.");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Die KI-Anfrage ist fehlgeschlagen.");
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new RuntimeException("Die KI hat keine Antwort geliefert.");
        }
        return content.parts().get(0).text();
    }

    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}
}
