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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DokumentService {

    private static final String UPLOAD_DIR = "uploads/dokumente/";

    private final DokumentRepository dokumentRepository;
    private final UserRepository userRepository;
    private final ProjektRepository projektRepository;

    public DokumentService(DokumentRepository dokumentRepository,
                           UserRepository userRepository,
                           ProjektRepository projektRepository) {
        this.dokumentRepository = dokumentRepository;
        this.userRepository = userRepository;
        this.projektRepository = projektRepository;
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

        if (originName.toLowerCase().endsWith(".pdf")) {
            dokument.setExtrahierterText(textAusPdfExtrahieren(datei));
        }

        return dokumentRepository.save(dokument);
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
}
