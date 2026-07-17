package com.sophub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophub.model.EmailTemplate;
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

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String REGISTRIERUNGS_TEMPLATE = "email-templates/registrierung.json";

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.from}")
    private String absenderAdresse;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendeRegistrierungsEmail(User user) {
        try {
            EmailTemplate template = ladeTemplate(REGISTRIERUNGS_TEMPLATE);

            String betreff = fuelleTemplate(template.getSubject(), user);
            String text = fuelleTemplate(template.getBody(), user);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(absenderAdresse);
            message.setTo(user.getEmail());
            message.setSubject(betreff);
            message.setText(text);

            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Registrierungs-E-Mail konnte nicht an {} gesendet werden: {}", user.getEmail(), e.getMessage());
        }
    }

    private EmailTemplate ladeTemplate(String klassenpfad) throws IOException {
        try (InputStream in = new ClassPathResource(klassenpfad).getInputStream()) {
            return objectMapper.readValue(in, EmailTemplate.class);
        }
    }

    private String fuelleTemplate(String vorlage, User user) {
        return vorlage
                .replace("${benutzername}", user.getBenutzername())
                .replace("${email}", user.getEmail())
                .replace("${name}", user.getName())
                .replace("${vorname}", user.getVorname());
    }
}
