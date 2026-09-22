package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.User;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjektService {

    private static final Logger log = LoggerFactory.getLogger(ProjektService.class);
    private static final String STATUS_ABGESCHLOSSEN = "abgeschlossen";

    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AIService aiService;
    private final AnonymizerService anonymizerService;

    public ProjektService(ProjektRepository projektRepository, UserRepository userRepository,
                          EmailService emailService, AIService aiService,
                          AnonymizerService anonymizerService) {
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.aiService = aiService;
        this.anonymizerService = anonymizerService;
    }

    public List<Projekt> alleProjeKte() {
        return projektRepository.findAll();
    }

    public List<Projekt> nachRolle(String benutzername, String rolle) {
        if ("STUDENT".equals(rolle)) {
            User student = userRepository.findByBenutzername(benutzername)
                    .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden."));
            return projektRepository.findByStudentId(student.getId());
        }
        return projektRepository.findAll();
    }

    public Optional<Projekt> projektById(Long id) {
        return projektRepository.findById(id);
    }

    public List<Projekt> projektByStudent(Long studentId) {
        return projektRepository.findByStudentId(studentId);
    }

    public Projekt erstellen(Projekt projekt, Long studentId, Long betreuerId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student nicht gefunden."));
        projekt.setStudent(student);

        User betreuer = null;
        if (betreuerId != null) {
            betreuer = userRepository.findById(betreuerId)
                    .orElseThrow(() -> new RuntimeException("Betreuer nicht gefunden."));
            projekt.setBetreuer(betreuer);
        }

        if (projekt.getStatus() == null) {
            projekt.setStatus("ENTWURF");
        }

        Projekt gespeichert = projektRepository.save(projekt);

        zusammenfassungGenerierenFallsNoetig(gespeichert);

        if (betreuer != null) {
            emailService.sendeBetreuerZuweisungsEmail(betreuer, student, gespeichert);
        }

        return gespeichert;
    }

    public Projekt aktualisieren(Long id, Projekt aktuell) {
        Projekt vorhandenes = projektRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

        String statusVorher = vorhandenes.getStatus();

        if (aktuell.getTitel() != null) vorhandenes.setTitel(aktuell.getTitel());
        if (aktuell.getBeschreibung() != null) vorhandenes.setBeschreibung(aktuell.getBeschreibung());
        if (aktuell.getSemester() != null) vorhandenes.setSemester(aktuell.getSemester());
        if (aktuell.getFachbereich() != null) vorhandenes.setFachbereich(aktuell.getFachbereich());
        if (aktuell.getProjektart() != null) vorhandenes.setProjektart(aktuell.getProjektart());
        if (aktuell.getSprache() != null) vorhandenes.setSprache(aktuell.getSprache());
        if (aktuell.getSchlagwoerter() != null) vorhandenes.setSchlagwoerter(aktuell.getSchlagwoerter());
        if (aktuell.getStatus() != null) vorhandenes.setStatus(aktuell.getStatus());
        if (aktuell.getZugriffsgrad() != null) vorhandenes.setZugriffsgrad(aktuell.getZugriffsgrad());
        if (aktuell.getGruppenanzahl() != null) vorhandenes.setGruppenanzahl(aktuell.getGruppenanzahl());
        if (aktuell.getRate() != null) vorhandenes.setRate(aktuell.getRate());
        if (aktuell.getKiGeneriert() != null) vorhandenes.setKiGeneriert(aktuell.getKiGeneriert());

        Projekt gespeichert = projektRepository.save(vorhandenes);

        boolean wurdeArchiviert = STATUS_ABGESCHLOSSEN.equalsIgnoreCase(gespeichert.getStatus())
                && !STATUS_ABGESCHLOSSEN.equalsIgnoreCase(statusVorher);
        if (wurdeArchiviert) {
            zusammenfassungGenerierenFallsNoetig(gespeichert);
        }

        return gespeichert;
    }

    // Projekt annehmen
    public Projekt annehmen(Long projektId, Long betreuerId) {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

        User betreuer = userRepository.findById(betreuerId)
                .orElseThrow(() -> new RuntimeException("Betreuer nicht gefunden."));

        if (projekt.getBetreuer() != null) {
            throw new RuntimeException("Das Projekt wurde bereits angenommen.");
        }

        projekt.setBetreuer(betreuer);
        projekt.setStatus("ANGENOMMEN");

        return projektRepository.save(projekt);
    }

    // Projekt ablehnen
    public Projekt ablehnen(Long projektId, Long betreuerId) {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

        userRepository.findById(betreuerId)
                .orElseThrow(() -> new RuntimeException("Betreuer nicht gefunden."));

        if (!"OFFEN".equals(projekt.getStatus())) {
            throw new RuntimeException("Das Projekt wurde bereits bearbeitet.");
        }

        projekt.setStatus("ABGELEHNT");

        return projektRepository.save(projekt);
    }

    public void loeschen(Long id) {
        if (!projektRepository.existsById(id)) {
            throw new RuntimeException("Projekt nicht gefunden.");
        }
        projektRepository.deleteById(id);
    }

    /**
     * Erzeugt (bei Bedarf) eine KI-Zusammenfassung für ein Projekt und gibt sie zurück.
     * Existiert bereits eine Zusammenfassung, wird sie unverändert zurückgegeben (kein erneuter KI-Aufruf).
     * Der Text wird vor dem KI-Aufruf immer über den AnonymizerService anonymisiert.
     */
    public String zusammenfassungErzeugen(Long projektId) {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

        if (projekt.getKiZusammenfassung() != null && !projekt.getKiZusammenfassung().isBlank()) {
            return projekt.getKiZusammenfassung();
        }

        String prompt = PromptTemplates.projektZusammenfassung(
                projekt.getTitel(), projekt.getBeschreibung(), projekt.getSchlagwoerter());
        String anonymisiert = anonymizerService.anonymisiere(prompt, projekt.getId());
        String zusammenfassung = aiService.generiereAntwort(anonymisiert);

        projekt.setKiZusammenfassung(zusammenfassung);
        projektRepository.save(projekt);

        return zusammenfassung;
    }

    private void zusammenfassungGenerierenFallsNoetig(Projekt projekt) {
        try {
            zusammenfassungErzeugen(projekt.getId());
        } catch (Exception e) {
            log.warn("KI-Zusammenfassung konnte für Projekt {} nicht automatisch erzeugt werden: {}",
                    projekt.getId(), e.getMessage());
        }
    }
}
