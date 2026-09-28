package com.sophub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophub.model.EmailTemplate;
import com.sophub.model.Projekt;
import com.sophub.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String REGISTRIERUNGS_TEMPLATE = "email-templates/registrierung.json";
    private static final String BETREUER_ZUWEISUNGS_TEMPLATE = "email-templates/betreuer-zuweisung.json";
    private static final String TEAM_HINZUGEFUEGT_TEMPLATE = "email-templates/team-hinzugefuegt.json";
    private static final String PROJEKT_ANFRAGE_TEMPLATE = "email-templates/projekt-anfrage.json";
    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.from}")
    private String absenderAdresse;

    // Adresse des React-Frontends. Daraus wird der Link in der Projektanfrage-Mail gebaut.
    // Steht nichts in application.properties, wird http://localhost:5173 verwendet.
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendeRegistrierungsEmail(User user) {
        Map<String, String> werte = Map.of(
                "benutzername", user.getBenutzername(),
                "email", user.getEmail(),
                "name", user.getName(),
                "vorname", user.getVorname()
        );
        sende(REGISTRIERUNGS_TEMPLATE, user.getEmail(), werte);
    }

    public void sendeBetreuerZuweisungsEmail(User betreuer, User student, Projekt projekt) {
        Map<String, String> werte = Map.of(
                "betreuerVorname", betreuer.getVorname(),
                "betreuerName", betreuer.getName(),
                "studentVorname", student.getVorname(),
                "studentName", student.getName(),
                "projektTitel", projekt.getTitel(),
                "datum", projekt.getErstelltAm() != null
                        ? projekt.getErstelltAm().format(DATUM_FORMAT)
                        : java.time.LocalDate.now().format(DATUM_FORMAT)
        );
        sende(BETREUER_ZUWEISUNGS_TEMPLATE, betreuer.getEmail(), werte);
    }

    public void sendeTeamMitgliedEmail(User mitglied, User ersteller, Projekt projekt) {
        Map<String, String> werte = Map.of(
                "mitgliedVorname", mitglied.getVorname(),
                "mitgliedName", mitglied.getName(),
                "erstellerVorname", ersteller.getVorname(),
                "erstellerName", ersteller.getName(),
                "projektTitel", projekt.getTitel(),
                "datum", java.time.LocalDate.now().format(DATUM_FORMAT)
        );
        sende(TEAM_HINZUGEFUEGT_TEMPLATE, mitglied.getEmail(), werte);
    }

    /**
     * Schickt dem eingeladenen Betreuer eine Projektanfrage mit Link zur Einladungsseite.
     * Der Link fuehrt direkt auf die React-Route /project-invitation/{projektId}.
     */
    public void sendeProjektAnfrageEmail(User betreuer, User student, Projekt projekt) {
        String link = frontendUrl + "/project-invitation/" + projekt.getId();

        Map<String, String> werte = Map.of(
                "betreuerVorname", textOderStrich(betreuer.getVorname()),
                "betreuerName", textOderStrich(betreuer.getName()),
                "studentVorname", textOderStrich(student.getVorname()),
                "studentName", textOderStrich(student.getName()),
                "projektTitel", textOderStrich(projekt.getTitel()),
                "fachbereich", textOderStrich(projekt.getFachbereich()),
                "projektart", textOderStrich(projekt.getProjektart()),
                "semester", textOderStrich(projekt.getSemester()),
                "link", link
        );
        sende(PROJEKT_ANFRAGE_TEMPLATE, betreuer.getEmail(), werte);
    }

    // Map.of(...) erlaubt keine null-Werte. Leere Felder werden deshalb als "-" angezeigt.
    private String textOderStrich(String text) {
        return (text == null || text.isBlank()) ? "-" : text;
    }

    private void sende(String templatePfad, String empfaenger, Map<String, String> werte) {
        try {
            EmailTemplate template = ladeTemplate(templatePfad);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(absenderAdresse);
            message.setTo(empfaenger);
            message.setSubject(fuelleTemplate(template.getSubject(), werte));
            message.setText(fuelleTemplate(template.getBody(), werte));

            mailSender.send(message);
        } catch (Exception e) {
            log.warn("E-Mail ({}) konnte nicht an {} gesendet werden: {}", templatePfad, empfaenger, e.getMessage());
        }
    }

    private EmailTemplate ladeTemplate(String klassenpfad) throws IOException {
        try (InputStream in = new ClassPathResource(klassenpfad).getInputStream()) {
            return objectMapper.readValue(in, EmailTemplate.class);
        }
    }

    private String fuelleTemplate(String vorlage, Map<String, String> werte) {
        String ergebnis = vorlage;
        for (Map.Entry<String, String> eintrag : werte.entrySet()) {
            ergebnis = ergebnis.replace("${" + eintrag.getKey() + "}", eintrag.getValue());
        }
        return ergebnis;
    }
}