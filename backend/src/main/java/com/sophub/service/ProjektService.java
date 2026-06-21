package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.User;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class ProjektService {

    private static final String UPLOAD_DIR = "uploads/projekte/";

    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;

    public ProjektService(ProjektRepository projektRepository, UserRepository userRepository) {
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
    }

    public List<Projekt> alleProjeKte() {
        return projektRepository.findAll();
    }

    public Optional<Projekt> projektById(Long id) {
        return projektRepository.findById(id);
    }

    public List<Projekt> projektByStudent(Long studentId) {
        return projektRepository.findByStudentId(studentId);
    }

    public Projekt erstellen(Projekt projekt, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student nicht gefunden."));
        projekt.setStudent(student);
        if (projekt.getStatus() == null) {
            projekt.setStatus("ENTWURF");
        }
        return projektRepository.save(projekt);
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

    public Projekt pdfHochladen(Long projektId, MultipartFile datei, String dokumentTyp) throws IOException {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

        Path uploadPfad = Paths.get(UPLOAD_DIR + projektId + "/");
        Files.createDirectories(uploadPfad);

        String dateiName = dokumentTyp + "_" + datei.getOriginalFilename();
        Path zielPfad = uploadPfad.resolve(dateiName);
        Files.copy(datei.getInputStream(), zielPfad);

        String extrahierterText = textAusPdfExtrahieren(datei);

        switch (dokumentTyp) {
            case "lastenheft" -> {
                projekt.setLastenheftPdfPfad(zielPfad.toString());
                projekt.setLastenheftText(extrahierterText);
            }
            case "pflichtenheft" -> {
                projekt.setPflichtenheftPdfPfad(zielPfad.toString());
                projekt.setPflichtenheftText(extrahierterText);
            }
            case "dokumentation" -> {
                projekt.setDokumentationPdfPfad(zielPfad.toString());
                projekt.setDokumentationText(extrahierterText);
            }
            default -> throw new RuntimeException("Unbekannter Dokumenttyp: " + dokumentTyp);
        }

        return projektRepository.save(projekt);
    }

    private String textAusPdfExtrahieren(MultipartFile datei) throws IOException {
        try (PDDocument document = Loader.loadPDF(datei.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}