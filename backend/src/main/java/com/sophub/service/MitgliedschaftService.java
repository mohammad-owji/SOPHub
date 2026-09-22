package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.ProjektMitglied;
import com.sophub.model.User;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MitgliedschaftService {

    private final ProjektMitgliedRepository projektMitgliedRepository;
    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public MitgliedschaftService(ProjektMitgliedRepository projektMitgliedRepository,
                                 ProjektRepository projektRepository,
                                 UserRepository userRepository,
                                 EmailService emailService) {
        this.projektMitgliedRepository = projektMitgliedRepository;
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public List<ProjektMitglied> mitglieder(Long projektId) {
        return projektMitgliedRepository.findByProjektId(projektId);
    }

    public ProjektMitglied hinzufuegen(Long projektId, Long studentId) {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student nicht gefunden."));

        if (!"STUDENT".equals(student.getRolle().getName())) {
            throw new RuntimeException("Nur Studierende können als Teammitglied hinzugefügt werden.");
        }
        if (projekt.getStudent() != null && projekt.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Diese Person ist bereits der Projekt-Ersteller.");
        }
        if (projektMitgliedRepository.existsByProjektIdAndStudentId(projektId, studentId)) {
            throw new RuntimeException("Diese Person ist bereits Teammitglied.");
        }
        if (projekt.getGruppenanzahl() != null) {
            int aktuelleGroesse = 1 + projektMitgliedRepository.findByProjektId(projektId).size();
            if (aktuelleGroesse >= projekt.getGruppenanzahl()) {
                throw new RuntimeException("Maximale Gruppengröße (" + projekt.getGruppenanzahl() + ") bereits erreicht.");
            }
        }

        ProjektMitglied mitglied = new ProjektMitglied();
        mitglied.setProjekt(projekt);
        mitglied.setStudent(student);
        ProjektMitglied gespeichert = projektMitgliedRepository.save(mitglied);

        if (projekt.getStudent() != null) {
            emailService.sendeTeamMitgliedEmail(student, projekt.getStudent(), projekt);
        }

        return gespeichert;
    }

    public void entfernen(Long projektId, Long studentId) {
        projektMitgliedRepository.deleteByProjektIdAndStudentId(projektId, studentId);
    }
}
