package com.sophub.service;

import com.sophub.model.Dokument;
import com.sophub.model.Projekt;
import com.sophub.model.User;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektMitgliedRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DokumentService {

    private static final Logger log = LoggerFactory.getLogger(DokumentService.class);
    private static final String UPLOAD_DIR = "uploads/dokumente/";

    // Systemkonto der Beispielprojekte (siehe BeispielprojekteInitializer).
    // Dokumente dieser Projekte sind Anschauungsmaterial und fuer alle angemeldeten Nutzer freigegeben.
    private static final String BEISPIEL_KONTO = "sophub.beispielprojekte";

    private final DokumentRepository dokumentRepository;
    private final UserRepository userRepository;
    private final ProjektRepository projektRepository;
    private final ProjektMitgliedRepository projektMitgliedRepository;
    private final AIService aiService;
    private final AnonymizerService anonymizerService;
    private final TagService tagService;

    public DokumentService(DokumentRepository dokumentRepository,
                           UserRepository userRepository,
                           ProjektRepository projektRepository,
                           ProjektMitgliedRepository projektMitgliedRepository,
                           AIService aiService,
                           AnonymizerService anonymizerService,
                           TagService tagService) {
        this.dokumentRepository = dokumentRepository;
        this.userRepository = userRepository;
        this.projektRepository = projektRepository;
        this.projektMitgliedRepository = projektMitgliedRepository;
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

        if (dokument.getExtrahierterText() != null && !dokument.getExtrahierterText().isBlank()) {
            zusammenfassungBeiUploadErzeugen(dokument);
        }

        Dokument gespeichert = dokumentRepository.save(dokument);

        if (gespeichert.getProjekt() != null
                && gespeichert.getExtrahierterText() != null
                && !gespeichert.getExtrahierterText().isBlank()) {
            autoTaggingAusloesen(gespeichert);
        }

        return gespeichert;
    }

    /**
     * Prüft, ob der angegebene Benutzer Zugriff auf die Originaldatei eines Dokuments hat:
     * Teammitglied des Projekts (Ersteller, hinzugefügtes Mitglied), zugewiesener Betreuer,
     * oder Admin. Ohne Projektbezug hat nur der Uploader selbst Zugriff.
     * Ausnahme: Dokumente der Beispielprojekte darf jeder angemeldete Nutzer herunterladen.
     */
    public boolean hatZugriffAufOriginal(Long dokumentId, String benutzername) {
        Dokument dokument = dokumentRepository.findById(dokumentId)
                .orElseThrow(() -> new RuntimeException("Dokument nicht gefunden."));
        return hatZugriffAufOriginal(dokument, benutzername);
    }

    private boolean hatZugriffAufOriginal(Dokument dokument, String benutzername) {
        User benutzer = userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden."));

        if ("ADMIN".equalsIgnoreCase(benutzer.getRolle().getName())) {
            return true;
        }

        Projekt projekt = dokument.getProjekt();
        if (projekt == null) {
            return dokument.getHochgeladenVon().getId().equals(benutzer.getId());
        }

        if (projekt.getStudent() != null && BEISPIEL_KONTO.equals(projekt.getStudent().getBenutzername())) {
            return true;
        }

        if (projekt.getBetreuer() != null && projekt.getBetreuer().getId().equals(benutzer.getId())) {
            return true;
        }
        if (projekt.getStudent() != null && projekt.getStudent().getId().equals(benutzer.getId())) {
            return true;
        }
        return projektMitgliedRepository.existsByProjektIdAndStudentId(projekt.getId(), benutzer.getId());
    }

    /**
     * Erzeugt eine KI-Zusammenfassung (max. halbe Seite) für eine hochgeladene PDF,
     * ohne die Datei oder das Ergebnis zu speichern.
     */
    public String pdfZusammenfassen(MultipartFile datei) throws IOException {
        if (datei == null || datei.isEmpty()) {
            throw new IllegalArgumentException("Keine Datei hochgeladen.");
        }
        String name = datei.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Nur PDF-Dateien sind erlaubt.");
        }

        String text = textAusPdfExtrahieren(datei);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Aus der PDF konnte kein Text extrahiert werden.");
        }

        return aiService.generiereAntwort(PromptTemplates.pdfZusammenfassung(text));
    }

    /**
     * Liest ein hochgeladenes Dokument (PDF, DOCX, TXT) und lässt die KI die wichtigsten
     * Stichwörter extrahieren. Es wird nichts gespeichert.
     */
    public List<String> stichwoerterExtrahieren(MultipartFile datei) throws IOException {
        if (datei == null || datei.isEmpty()) {
            throw new IllegalArgumentException("Keine Datei hochgeladen.");
        }
        String name = datei.getOriginalFilename() == null ? "" : datei.getOriginalFilename().toLowerCase();

        String text;
        if (name.endsWith(".pdf")) {
            text = textAusPdfExtrahieren(datei);
        } else if (name.endsWith(".docx")) {
            text = textAusDocxExtrahieren(datei);
        } else if (name.endsWith(".txt")) {
            text = textAusTxtExtrahieren(datei);
        } else {
            throw new IllegalArgumentException("Nur PDF-, DOCX- und TXT-Dateien sind erlaubt.");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Aus dem Dokument konnte kein Text extrahiert werden.");
        }

        String antwort = aiService.generiereAntwort(PromptTemplates.stichwoerter(text));
        if (antwort == null) {
            return List.of();
        }

        Set<String> stichwoerter = new LinkedHashSet<>();
        for (String teil : antwort.replace("`", "").split("[,\\n]")) {
            String wort = teil.replaceAll("^[\\s\\-*•\"']+|[\\s\"'.]+$", "");
            if (!wort.isEmpty()) {
                stichwoerter.add(wort);
            }
        }
        return new ArrayList<>(stichwoerter);
    }

    public Dokument einzelnesDokument(Long dokumentId) {
        return dokumentRepository.findById(dokumentId)
                .orElseThrow(() -> new RuntimeException("Dokument nicht gefunden."));
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

    /**
     * Erzeugt einmalig beim Upload die KI-Zusammenfassung des Dokumenttexts (F27).
     * Schlägt der KI-Aufruf fehl, bleibt kiZusammenfassung einfach leer statt den Upload abzubrechen.
     */
    private void zusammenfassungBeiUploadErzeugen(Dokument dokument) {
        try {
            Long projektId = dokument.getProjekt() != null ? dokument.getProjekt().getId() : null;
            String prompt = PromptTemplates.dokumentZusammenfassung(dokument.getExtrahierterText());
            String anonymisiert = anonymizerService.anonymisiere(prompt, projektId);
            String zusammenfassung = aiService.generiereAntwort(anonymisiert);
            dokument.setKiZusammenfassung(zusammenfassung);
        } catch (Exception e) {
            log.warn("KI-Zusammenfassung konnte für Dokument \"{}\" beim Upload nicht erzeugt werden: {}",
                    dokument.getDateiName(), e.getMessage());
        }
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