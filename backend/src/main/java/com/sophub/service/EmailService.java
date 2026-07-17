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
    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.from}")
    private String absenderAdresse;

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
