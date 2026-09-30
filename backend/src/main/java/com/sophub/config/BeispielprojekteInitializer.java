package com.sophub.config;

import com.sophub.model.Dokument;
import com.sophub.model.Projekt;
import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.RolleRepository;
import com.sophub.repository.UserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Legt beim Start des Backends die fiktiven, abgeschlossenen Beispielprojekte an
 * (inkl. Lastenheft, Pflichtenheft und Datenbankmodell als PDF), falls sie noch fehlen.
 *
 * - Die PDFs liegen in src/main/resources/beispielprojekte/ (und damit auf GitHub).
 * - Beim Anlegen werden sie nach uploads/dokumente/beispielprojekte/ kopiert,
 *   damit der normale Download ueber SOPhub funktioniert.
 * - Ersteller ist das Systemkonto "SOPhub Beispielprojekte" (kein echter Mensch).
 * - Die Projekte haben bewusst KEINEN Betreuer, da sie fiktiv sind.
 */
@Component
public class BeispielprojekteInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BeispielprojekteInitializer.class);

    private static final String RESSOURCEN_ORDNER = "beispielprojekte/";
    private static final String UPLOAD_ORDNER = "uploads/dokumente/beispielprojekte/";
    private static final String STATUS_ABGESCHLOSSEN = "abgeschlossen";

    private static final String SYSTEM_BENUTZERNAME = "sophub.beispielprojekte";
    private static final String SYSTEM_EMAIL = "beispielprojekte@sophub.local";

    private static final String TECHNOLOGIEN = " Technologien: React, Spring Boot, PostgreSQL, REST API.";

    // Dokumenttyp in der Datenbank + Teil des Dateinamens
    private static final String[][] DOKUMENT_TYPEN = {
            {"LASTENHEFT", "Lastenheft"},
            {"PFLICHTENHEFT", "Pflichtenheft"},
            {"DATENBANKMODELL", "Datenbankmodell"}
    };

    // Ein Beispielprojekt: Titel, Praefix der PDF-Dateien, Beschreibung, Fachbereich, Projektart, Semester
    private record Beispiel(String titel, String dateiPraefix, String beschreibung,
                            String fachbereich, String projektart, String semester) { }

    private static final List<Beispiel> BEISPIELE = List.of(
            new Beispiel("SmartCampus Parking", "SmartCampusParking",
                    "Digitale Parkplatzverwaltung für den Hochschulcampus: Parkplatzsuche, Reservierung, "
                            + "Fahrzeug- und Benutzerverwaltung sowie Benachrichtigungen.",
                    "Informatik", "Softwareprojekt", "SoSe 2025"),
            new Beispiel("UniLibrary", "UniLibrary",
                    "Digitale Bibliotheksverwaltung: Büchersuche, Ausleihe, Reservierung, Rückgabe, "
                            + "Benutzerverwaltung und Benachrichtigungen.",
                    "Informatik", "Softwareprojekt", "WiSe 2025/26"),
            new Beispiel("CampusEvent Manager", "CampusEventManager",
                    "Verwaltung von Hochschulveranstaltungen: Veranstaltungen suchen und erstellen, "
                            + "Teilnehmer an- und abmelden, Räume und Kategorien verwalten, Benachrichtigungen.",
                    "Informatik", "Softwareprojekt", "SoSe 2026"),
            new Beispiel("CampusConnect", "CampusConnect",
                    "Webbasierte Plattform zur Organisation von Lerngruppen und Hochschulprojekten: "
                            + "Gruppen finden, Projekte anlegen, Einladungen verwalten, Dokumente austauschen "
                            + "und Termine koordinieren.",
                    "Informatik", "Softwareprojekt", "WiSe 2024/25")
    );

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final ProjektRepository projektRepository;
    private final DokumentRepository dokumentRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public BeispielprojekteInitializer(UserRepository userRepository,
                                       RolleRepository rolleRepository,
                                       ProjektRepository projektRepository,
                                       DokumentRepository dokumentRepository,
                                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.projektRepository = projektRepository;
        this.dokumentRepository = dokumentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User systemKonto = systemKontoHolenOderAnlegen();

        // Welche Beispielprojekte gibt es schon? (verhindert Duplikate bei jedem Start)
        Set<String> vorhandeneTitel = projektRepository.findByStudentId(systemKonto.getId()).stream()
                .map(Projekt::getTitel)
                .collect(Collectors.toSet());

        int neuAngelegt = 0;

        for (Beispiel beispiel : BEISPIELE) {
            if (vorhandeneTitel.contains(beispiel.titel())) {
                continue;
            }

            try {
                Projekt projekt = new Projekt();
                projekt.setTitel(beispiel.titel());
                projekt.setBeschreibung(beispiel.beschreibung() + TECHNOLOGIEN);
                projekt.setFachbereich(beispiel.fachbereich());
                projekt.setProjektart(beispiel.projektart());
                projekt.setSemester(beispiel.semester());
                projekt.setGruppenanzahl(3);
                projekt.setStatus(STATUS_ABGESCHLOSSEN);
                projekt.setStudent(systemKonto);
                Projekt gespeichert = projektRepository.save(projekt);

                for (String[] typ : DOKUMENT_TYPEN) {
                    String dateiName = beispiel.dateiPraefix() + "_" + typ[1] + "_ausfuehrlich.pdf";
                    dokumentAnlegen(gespeichert, systemKonto, dateiName, typ[0]);
                }

                neuAngelegt++;
            } catch (Exception e) {
                log.warn("Beispielprojekt '{}' konnte nicht angelegt werden: {}", beispiel.titel(), e.getMessage());
            }
        }

        log.info("Beispielprojekte geprueft: {} neu angelegt, {} insgesamt in der Liste.",
                neuAngelegt, BEISPIELE.size());
    }

    // Kopiert die PDF aus den Ressourcen in den Upload-Ordner und speichert den Datenbank-Eintrag
    private void dokumentAnlegen(Projekt projekt, User uploader, String dateiName, String typ) throws Exception {
        ClassPathResource quelle = new ClassPathResource(RESSOURCEN_ORDNER + dateiName);
        if (!quelle.exists()) {
            log.warn("Beispiel-PDF nicht gefunden: {}", RESSOURCEN_ORDNER + dateiName);
            return;
        }

        byte[] inhalt;
        try (InputStream in = quelle.getInputStream()) {
            inhalt = in.readAllBytes();
        }

        Path zielOrdner = Paths.get(UPLOAD_ORDNER);
        Files.createDirectories(zielOrdner);
        Path zielPfad = zielOrdner.resolve(dateiName);
        // Datei schreiben (eine evtl. vorhandene gleichnamige Datei wird ueberschrieben)
        Files.write(zielPfad, inhalt);

        Dokument dokument = new Dokument();
        dokument.setDateiName(dateiName);
        dokument.setDateiPfad(zielPfad.toString());
        dokument.setTyp(typ);
        dokument.setHochgeladenVon(uploader);
        dokument.setProjekt(projekt);
        dokument.setExtrahierterText(textAusPdf(inhalt));

        dokumentRepository.save(dokument);
    }

    // Text aus der PDF lesen (fuer Suche und KI). Null-Zeichen mag PostgreSQL nicht -> entfernen.
    private String textAusPdf(byte[] inhalt) {
        try (PDDocument pdf = Loader.loadPDF(inhalt)) {
            return new PDFTextStripper().getText(pdf).replace("\u0000", "");
        } catch (Exception e) {
            log.warn("Text konnte nicht aus PDF gelesen werden: {}", e.getMessage());
            return null;
        }
    }

    // Systemkonto als "Ersteller" der Beispielprojekte.
    // kontoAktiviert = true + zufaelliges Passwort: Niemand kann sich einloggen,
    // und das Konto kann auch nicht ueber "Registrieren" uebernommen werden.
    private User systemKontoHolenOderAnlegen() {
        return userRepository.findByBenutzername(SYSTEM_BENUTZERNAME).orElseGet(() -> {
            Rolle studentRolle = rolleRepository.findByName("STUDENT").orElseGet(() -> {
                Rolle neu = new Rolle();
                neu.setName("STUDENT");
                return rolleRepository.save(neu);
            });

            User konto = new User();
            konto.setBenutzername(SYSTEM_BENUTZERNAME);
            konto.setEmail(SYSTEM_EMAIL);
            konto.setVorname("SOPhub");
            konto.setName("Beispielprojekte");
            konto.setRolle(studentRolle);
            konto.setPasswort(passwordEncoder.encode(UUID.randomUUID().toString()));
            konto.setKontoAktiviert(true);
            return userRepository.save(konto);
        });
    }
}