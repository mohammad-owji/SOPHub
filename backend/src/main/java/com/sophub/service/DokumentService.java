package com.sophub.service;

import com.sophub.model.Dokument;
import com.sophub.model.Projekt;
import com.sophub.model.User;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DokumentService {

    private static final Logger log = LoggerFactory.getLogger(DokumentService.class);
    private static final String UPLOAD_DIR = "uploads/dokumente/";

    private final DokumentRepository dokumentRepository;
    private final UserRepository userRepository;
    private final ProjektRepository projektRepository;
    private final AIService aiService;
    private final AnonymizerService anonymizerService;
    private final TagService tagService;

    public DokumentService(DokumentRepository dokumentRepository,
                           UserRepository userRepository,
                           ProjektRepository projektRepository,
                           AIService aiService,
                           AnonymizerService anonymizerService,
                           TagService tagService) {
        this.dokumentRepository = dokumentRepository;
        this.userRepository = userRepository;
        this.projektRepository = projektRepository;
        this.aiService = aiService;
        this.anonymizerService = anonymizerService;
        this.tagService = tagService;
    }

    public Dokument hochladen(MultipartFile datei, Long benutzerId, Long projektId, String typ) throws IOException {
        User benutzer = userRepository.findById(benutzerId)
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden."));

        Projekt projekt = null;
        if (projektId != null) {
            projekt = projektRepository.findById(projektId)
                    .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));
        }

        String originName = datei.getOriginalFilename();
        if (originName == null || originName.isBlank()) {
            throw new RuntimeException("Dateiname ungültig.");
        }

        Path zielOrdner = Paths.get(UPLOAD_DIR + benutzerId + "/");
        Files.createDirectories(zielOrdner);

        String dateiName = System.currentTimeMillis() + "_" + originName;
        Path zielPfad = zielOrdner.resolve(dateiName);
        Files.copy(datei.getInputStream(), zielPfad);

        Dokument dokument = new Dokument();
        dokument.setDateiName(originName);
        dokument.setDateiPfad(zielPfad.toString());
        dokument.setTyp(typ.toUpperCase());
        dokument.setHochgeladenVon(benutzer);
        dokument.setProjekt(projekt);

        String originNameLower = originName.toLowerCase();
        if (originNameLower.endsWith(".pdf")) {
            dokument.setExtrahierterText(textAusPdfExtrahieren(datei));
        } else if (originNameLower.endsWith(".docx")) {
            dokument.setExtrahierterText(textAusDocxExtrahieren(datei));
        } else if (originNameLower.endsWith(".txt")) {
            dokument.setExtrahierterText(textAusTxtExtrahieren(datei));
        }

        Dokument gespeichert = dokumentRepository.save(dokument);

        if (gespeichert.getProjekt() != null
                && gespeichert.getExtrahierterText() != null
                && !gespeichert.getExtrahierterText().isBlank()) {
            autoTaggingAusloesen(gespeichert);
        }

        return gespeichert;
    }

    public List<Dokument> nachBenutzer(Long benutzerId) {
        User benutzer = userRepository.findById(benutzerId)
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden."));
        return dokumentRepository.findByHochgeladenVon(benutzer);
    }

    public List<Dokument> nachProjekt(Long projektId) {
        return dokumentRepository.findByProjektId(projektId);
    }

    public Resource herunterladen(Long dokumentId) throws MalformedURLException {
        Dokument dokument = dokumentRepository.findById(dokumentId)
                .orElseThrow(() -> new RuntimeException("Dokument nicht gefunden."));

        Path pfad = Paths.get(dokument.getDateiPfad());
        Resource resource = new UrlResource(pfad.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Datei nicht lesbar.");
        }
        return resource;
    }

    public void loeschen(Long dokumentId) throws IOException {
        Dokument dokument = dokumentRepository.findById(dokumentId)
                .orElseThrow(() -> new RuntimeException("Dokument nicht gefunden."));

        Files.deleteIfExists(Paths.get(dokument.getDateiPfad()));
        dokumentRepository.deleteById(dokumentId);
    }

    private String textAusPdfExtrahieren(MultipartFile datei) throws IOException {
        try (PDDocument document = Loader.loadPDF(datei.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String textAusDocxExtrahieren(MultipartFile datei) throws IOException {
        try (XWPFDocument document = new XWPFDocument(datei.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String textAusTxtExtrahieren(MultipartFile datei) throws IOException {
        return new String(datei.getBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Erzeugt (bei Bedarf) eine KI-Kurzzusammenfassung für ein Dokument.
     * Der extrahierte Text wird vor dem KI-Aufruf immer über den AnonymizerService anonymisiert.
     */
    public Dokument zusammenfassungErzeugen(Long dokumentId) {
        Dokument dokument = dokumentRepository.findById(dokumentId)
                .orElseThrow(() -> new RuntimeException("Dokument nicht gefunden."));

        if (dokument.getKiZusammenfassung() != null && !dokument.getKiZusammenfassung().isBlank()) {
            return dokument;
        }
        if (dokument.getExtrahierterText() == null || dokument.getExtrahierterText().isBlank()) {
            throw new RuntimeException("Für dieses Dokument liegt kein extrahierter Text vor.");
        }

        Long projektId = dokument.getProjekt() != null ? dokument.getProjekt().getId() : null;

        String prompt = PromptTemplates.dokumentZusammenfassung(dokument.getExtrahierterText());
        String anonymisiert = anonymizerService.anonymisiere(prompt, projektId);
        String zusammenfassung = aiService.generiereAntwort(anonymisiert);

        dokument.setKiZusammenfassung(zusammenfassung);
        return dokumentRepository.save(dokument);
    }

    private void autoTaggingAusloesen(Dokument dokument) {
        try {
            Long projektId = dokument.getProjekt().getId();
            String prompt = PromptTemplates.autoTagging(dokument.getExtrahierterText());
            String anonymisiert = anonymizerService.anonymisiere(prompt, projektId);
            String kiAntwort = aiService.generiereAntwort(anonymisiert);
            tagService.tagsAusKiAntwortUebernehmen(projektId, kiAntwort);
        } catch (Exception e) {
            log.warn("Auto-Tagging für Dokument {} fehlgeschlagen: {}", dokument.getId(), e.getMessage());
        }
    }
}
