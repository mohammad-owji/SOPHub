package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.User;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjektService {

    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ProjektService(ProjektRepository projektRepository, UserRepository userRepository,
                          EmailService emailService) {
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
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

        if (betreuer != null) {
            emailService.sendeBetreuerZuweisungsEmail(betreuer, student, gespeichert);
        }

        return gespeichert;
    }

    public Projekt aktualisieren(Long id, Projekt aktuell) {
        Projekt vorhandenes = projektRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

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

        return projektRepository.save(vorhandenes);
    }

    public void loeschen(Long id) {
        if (!projektRepository.existsById(id)) {
            throw new RuntimeException("Projekt nicht gefunden.");
        }
        projektRepository.deleteById(id);
    }
}
