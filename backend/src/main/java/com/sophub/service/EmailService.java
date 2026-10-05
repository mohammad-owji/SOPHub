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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

// @Async: Alle oeffentlichen Methoden dieser Klasse laufen im Hintergrund.
// Die Webseite muss so nicht warten, bis der Mailserver (Gmail) geantwortet hat.
@Async
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String REGISTRIERUNGS_TEMPLATE = "email-templates/registrierung.json";
    private static final String BESTAETIGUNGS_TEMPLATE = "email-templates/email-bestaetigung.json";
    private static final String BETREUER_ZUWEISUNGS_TEMPLATE = "email-templates/betreuer-zuweisung.json";
    private static final String TEAM_HINZUGEFUEGT_TEMPLATE = "email-templates/team-hinzugefuegt.json";
    private static final String PROJEKT_ANFRAGE_TEMPLATE = "email-templates/projekt-anfrage.json";
    private static final String PROJEKT_ANGENOMMEN_TEMPLATE = "email-templates/projekt-angenommen.json";
    private static final String PROJEKT_ABGELEHNT_TEMPLATE = "email-templates/projekt-abgelehnt.json";
    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.from}")
    private String absenderAdresse;

    // Adresse des React-Frontends. Daraus wird der Link in der Projektanfrage-Mail gebaut.
    // Steht nichts in application.properties, wird http://localhost:5173 verwendet.
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // Test-Umleitung: Ist diese Adresse gesetzt, gehen ALLE Mails an sie statt an den echten Empfaenger.
    // So bekommen echte Personen (z.B. Betreuer) beim Entwickeln keine Testmails.
    // Leer lassen (oder Zeile weglassen), damit Mails an die echten Empfaenger gehen.
    @Value("${app.mail.test-empfaenger:}")
    private String testEmpfaenger;

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

    /**
     * Schickt nach der Registrierung den Link zur Bestaetigung der E-Mail-Adresse.
     * Der Link fuehrt auf die React-Route /email-bestaetigen?token=...
     * Der Token wird als Parameter uebergeben (nicht aus dem User gelesen),
     * weil diese Methode im Hintergrund (@Async) laeuft.
     */
    public void sendeBestaetigungsEmail(User user, String token) {
        String link = frontendUrl + "/email-bestaetigen?token=" + token;

        Map<String, String> werte = Map.of(
                "vorname", textOderStrich(user.getVorname()),
                "name", textOderStrich(user.getName()),
                "benutzername", textOderStrich(user.getBenutzername()),
                "link", link
        );
        sende(BESTAETIGUNGS_TEMPLATE, user.getEmail(), werte);
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

    /**
     * Informiert den Studenten, ob der eingeladene Betreuer das Projekt angenommen oder abgelehnt hat.
     * Der Link fuehrt auf die Projektdetails: /projectdetails/{projektId}.
     */
    public void sendeProjektEntscheidungEmail(User student, User betreuer, Projekt projekt, boolean angenommen) {
        String link = frontendUrl + "/projectdetails/" + projekt.getId();
        String vorlage = angenommen ? PROJEKT_ANGENOMMEN_TEMPLATE : PROJEKT_ABGELEHNT_TEMPLATE;

        Map<String, String> werte = Map.of(
                "studentVorname", textOderStrich(student.getVorname()),
                "studentName", textOderStrich(student.getName()),
                "betreuerVorname", textOderStrich(betreuer.getVorname()),
                "betreuerName", textOderStrich(betreuer.getName()),
                "projektTitel", textOderStrich(projekt.getTitel()),
                "fachbereich", textOderStrich(projekt.getFachbereich()),
                "projektart", textOderStrich(projekt.getProjektart()),
                "semester", textOderStrich(projekt.getSemester()),
                "link", link
        );
        sende(vorlage, student.getEmail(), werte);
    }

    // Map.of(...) erlaubt keine null-Werte. Leere Felder werden deshalb als "-" angezeigt.
    private String textOderStrich(String text) {
        return (text == null || text.isBlank()) ? "-" : text;
    }

    private void sende(String templatePfad, String empfaenger, Map<String, String> werte) {
        try {
            EmailTemplate template = ladeTemplate(templatePfad);

            String betreff = fuelleTemplate(template.getSubject(), werte);
            String text = fuelleTemplate(template.getBody(), werte);
            String tatsaechlicherEmpfaenger = empfaenger;

            // Test-Umleitung aktiv? Dann an die Testadresse schicken und den echten Empfaenger im Text vermerken.
            if (testEmpfaenger != null && !testEmpfaenger.isBlank()) {
                tatsaechlicherEmpfaenger = testEmpfaenger;
                betreff = "[TEST] " + betreff;
                text = "[TEST] Eigentlich an: " + empfaenger + "\n\n" + text;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(absenderAdresse);
            message.setTo(tatsaechlicherEmpfaenger);
            message.setSubject(betreff);
            message.setText(text);

            mailSender.send(message);
            log.info("E-Mail ({}) gesendet an {} (eigentlicher Empfaenger: {})",
                    templatePfad, tatsaechlicherEmpfaenger, empfaenger);
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