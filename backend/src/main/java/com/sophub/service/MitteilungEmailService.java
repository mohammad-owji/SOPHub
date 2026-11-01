package com.sophub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophub.model.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Benachrichtigt Projektbeteiligte per E-Mail ueber neue Mitteilungen und Antworten.
 * Laeuft im Hintergrund (@Async), damit das Absenden einer Mitteilung nicht wartet.
 * Beachtet dieselbe Test-Umleitung wie der EmailService (app.mail.test-empfaenger).
 */
@Service
public class MitteilungEmailService {

    private static final Logger log = LoggerFactory.getLogger(MitteilungEmailService.class);
    private static final String TEMPLATE = "email-templates/neue-mitteilung.json";

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.from}")
    private String absenderAdresse;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.test-empfaenger:}")
    private String testEmpfaenger;

    public MitteilungEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Einfache Daten eines Empfaengers (keine Datenbank-Objekte, weil im Hintergrund-Thread). */
    public record Empfaenger(String email, String vorname, String name) {}

    @Async
    public void benachrichtige(List<Empfaenger> empfaengerListe, Long projektId, String projektTitel,
                               String autor, boolean istAntwort, String kategorie, String betreff, String text) {
        if (empfaengerListe == null || empfaengerListe.isEmpty()) {
            return;
        }
        EmailTemplate template;
        try (InputStream in = new ClassPathResource(TEMPLATE).getInputStream()) {
            template = objectMapper.readValue(in, EmailTemplate.class);
        } catch (Exception e) {
            log.warn("Mitteilungs-Vorlage konnte nicht geladen werden: {}", e.getMessage());
            return;
        }

        // Link oeffnet direkt den Reiter "Mitteilungen" auf der Projektseite
        String link = frontendUrl + "/projectdetails/" + projektId + "?reiter=mitteilungen";
        for (Empfaenger empfaenger : empfaengerListe) {
            Map<String, String> werte = Map.of(
                    "vorname", wert(empfaenger.vorname()),
                    "name", wert(empfaenger.name()),
                    "autor", wert(autor),
                    "projektTitel", wert(projektTitel),
                    "art", istAntwort ? "Neue Antwort" : "Neue Mitteilung",
                    "artText", istAntwort ? "auf Ihre Mitteilung geantwortet" : "eine neue Mitteilung geschrieben",
                    "kategorie", wert(kategorie),
                    "betreff", wert(betreff),
                    "text", wert(text),
                    "link", link
            );
            sende(empfaenger.email(), fuelle(template.getSubject(), werte), fuelle(template.getBody(), werte));
        }
    }

    private void sende(String empfaenger, String betreff, String text) {
        if (empfaenger == null || empfaenger.isBlank()) {
            return;
        }
        try {
            String ziel = empfaenger;
            if (testEmpfaenger != null && !testEmpfaenger.isBlank()) {
                ziel = testEmpfaenger;
                betreff = "[TEST] " + betreff;
                text = "[TEST] Eigentlich an: " + empfaenger + "\n\n" + text;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(absenderAdresse);
            message.setTo(ziel);
            message.setSubject(betreff);
            message.setText(text);
            mailSender.send(message);
            log.info("Mitteilungs-E-Mail gesendet an {} (eigentlicher Empfaenger: {})", ziel, empfaenger);
        } catch (Exception e) {
            log.warn("Mitteilungs-E-Mail an {} konnte nicht gesendet werden: {}", empfaenger, e.getMessage());
        }
    }

    private String fuelle(String vorlage, Map<String, String> werte) {
        String ergebnis = vorlage;
        for (Map.Entry<String, String> eintrag : werte.entrySet()) {
            ergebnis = ergebnis.replace("${" + eintrag.getKey() + "}", eintrag.getValue());
        }
        return ergebnis;
    }

    private String wert(String text) {
        return (text == null || text.isBlank()) ? "-" : text;
    }
}