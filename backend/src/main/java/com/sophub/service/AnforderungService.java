package com.sophub.service;

import com.sophub.model.AnforderungEintrag;
import com.sophub.model.Dokument;
import com.sophub.model.DokumentAnforderung;
import com.sophub.model.Projekt;
import com.sophub.model.ProjektMitglied;
import com.sophub.model.User;
import com.sophub.repository.AnforderungEintragRepository;
import com.sophub.repository.DokumentAnforderungRepository;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dokument-Anforderungen ("Abgaben") eines Projekts.
 * <ul>
 *     <li>Die Betreuung (nach Annahme des Projekts) fordert Dokumente an und prueft sie (OK / Ueberarbeiten).</li>
 *     <li>Das Team (Ersteller:in + Teammitglieder) laedt angeforderte Dokumente hoch.</li>
 *     <li>Jeder Schritt wird im Verlauf gespeichert und per E-Mail gemeldet.</li>
 * </ul>
 */
@Service
public class AnforderungService {

    // Waehlbare Dokumentarten (SONSTIGES = eigener Name)
    private static final Map<String, String> DOKUMENTARTEN = Map.of(
            "LASTENHEFT", "Lastenheft",
            "PFLICHTENHEFT", "Pflichtenheft",
            "DATENBANKMODELL", "Datenbankmodell",
            "DOKUMENTATION", "Dokumentation",
            "TESTKONZEPT", "Testkonzept",
            "PRAESENTATION", "Präsentation",
            "SONSTIGES", "Sonstiges"
    );

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int MAX_TEXT = 2000;

    private final DokumentAnforderungRepository anforderungRepository;
    private final AnforderungEintragRepository eintragRepository;
    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;
    private final ProjektMitgliedRepository projektMitgliedRepository;
    private final DokumentService dokumentService;
    private final AnforderungEmailService anforderungEmailService;

    public AnforderungService(DokumentAnforderungRepository anforderungRepository,
                              AnforderungEintragRepository eintragRepository,
                              ProjektRepository projektRepository,
                              UserRepository userRepository,
                              ProjektMitgliedRepository projektMitgliedRepository,
                              DokumentService dokumentService,
                              AnforderungEmailService anforderungEmailService) {
        this.anforderungRepository = anforderungRepository;
        this.eintragRepository = eintragRepository;
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
        this.projektMitgliedRepository = projektMitgliedRepository;
        this.dokumentService = dokumentService;
        this.anforderungEmailService = anforderungEmailService;
    }

    /** Alle Anforderungen des Projekts samt Verlauf, dazu die Rolle des angemeldeten Benutzers. */
    @Transactional(readOnly = true)
    public Uebersicht liste(Long projektId, String benutzername) {
        Zugang zugang = pruefeZugang(projektId, benutzername);

        List<AnforderungAnsicht> ergebnis = new ArrayList<>();
        for (DokumentAnforderung a : anforderungRepository.findByProjektIdOrderByErstelltAmDesc(projektId)) {
            List<EintragAnsicht> verlauf = eintragRepository.findByAnforderungIdOrderByErstelltAmAsc(a.getId())
                    .stream()
                    .map(e -> new EintragAnsicht(e.getId(), e.getArt(), e.getKommentar(), e.getErstelltAm(),
                            person(e.getPerson(), zugang.projekt()),
                            e.getDokument() == null ? null : e.getDokument().getId(),
                            e.getDokument() == null ? null : e.getDokument().getDateiName()))
                    .toList();
            ergebnis.add(new AnforderungAnsicht(a.getId(), a.getTyp(), a.getTitel(), a.getBeschreibung(),
                    a.getFrist(), a.getStatus(), a.getErstelltAm(), person(a.getAngefordertVon(), zugang.projekt()),
                    verlauf));
        }
        return new Uebersicht(zugang.istTeam(), zugang.istBetreuer(), ergebnis);
    }

    /** Betreuung fordert ein Dokument an. */
    @Transactional
    public void anfordern(Long projektId, String benutzername, String typ, String titel,
                          String beschreibung, String frist) {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        if (!zugang.istBetreuer()) {
            throw new AccessDeniedException("Nur die Betreuung kann Dokumente anfordern.");
        }

        String art = typ == null ? "" : typ.trim().toUpperCase();
        if (!DOKUMENTARTEN.containsKey(art)) {
            throw new IllegalArgumentException("Bitte wählen Sie eine gültige Dokumentart.");
        }
        // Bei "Sonstiges" ist ein eigener Name Pflicht, sonst wird der Name der Dokumentart verwendet
        String name = titel == null ? "" : titel.trim();
        if ("SONSTIGES".equals(art) && name.isEmpty()) {
            throw new IllegalArgumentException("Bitte geben Sie einen Namen für das Dokument ein.");
        }
        if (name.isEmpty()) {
            name = DOKUMENTARTEN.get(art);
        }
        if (name.length() > 150) {
            throw new IllegalArgumentException("Der Name darf höchstens 150 Zeichen lang sein.");
        }

        DokumentAnforderung anforderung = new DokumentAnforderung();
        anforderung.setProjekt(zugang.projekt());
        anforderung.setAngefordertVon(zugang.user());
        anforderung.setTyp(art);
        anforderung.setTitel(name);
        anforderung.setBeschreibung(pruefeOptionalenText(beschreibung));
        anforderung.setFrist(leseFrist(frist));
        anforderung.setStatus(DokumentAnforderung.ANGEFORDERT);
        anforderungRepository.save(anforderung);

        // Ganzes Team informieren
        StringBuilder inhalt = new StringBuilder();
        inhalt.append(vollerName(zugang.user())).append(" hat im Projekt \"").append(zugang.projekt().getTitel())
                .append("\" das Dokument \"").append(name).append("\" angefordert.");
        if (anforderung.getBeschreibung() != null) {
            inhalt.append("\n\nHinweise: ").append(anforderung.getBeschreibung());
        }
        if (anforderung.getFrist() != null) {
            inhalt.append("\n\nAbgabe bis: ").append(anforderung.getFrist().format(DATUM));
        }
        inhalt.append("\n\nBitte laden Sie das Dokument in SOPhub im Reiter \"Abgaben\" hoch.");
        anforderungEmailService.benachrichtige(empfaengerTeam(zugang), projektId,
                "Dokument angefordert: " + name, inhalt.toString());
    }

    /** Team reicht eine Datei zu einer Anforderung ein (auch erneut nach "Ueberarbeiten"). */
    @Transactional
    public void einreichen(Long projektId, Long anforderungId, String benutzername,
                           MultipartFile datei, String kommentar) throws IOException {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        if (!zugang.istTeam()) {
            throw new AccessDeniedException("Nur das Projektteam kann Dokumente einreichen.");
        }
        DokumentAnforderung anforderung = ladeAnforderung(projektId, anforderungId);
        if (DokumentAnforderung.AKZEPTIERT.equals(anforderung.getStatus())) {
            throw new IllegalArgumentException("Dieses Dokument wurde bereits akzeptiert.");
        }
        if (datei == null || datei.isEmpty()) {
            throw new IllegalArgumentException("Bitte wählen Sie eine Datei aus.");
        }

        // Datei wie gewohnt speichern (erscheint auch in der Dokumentenliste und in der KI-Zusammenfassung)
        String dokumentTyp = "SONSTIGES".equals(anforderung.getTyp()) ? "SONSTIGES" : anforderung.getTyp();
        Dokument dokument = dokumentService.hochladen(datei, zugang.user().getId(), projektId,
                dokumentTyp, benutzername);

        AnforderungEintrag eintrag = new AnforderungEintrag();
        eintrag.setAnforderung(anforderung);
        eintrag.setArt(DokumentAnforderung.EINGEREICHT);
        eintrag.setPerson(zugang.user());
        eintrag.setKommentar(pruefeOptionalenText(kommentar));
        eintrag.setDokument(dokument);
        eintragRepository.save(eintrag);

        anforderung.setStatus(DokumentAnforderung.EINGEREICHT);
        anforderungRepository.save(anforderung);

        // Betreuung informieren
        Projekt projekt = zugang.projekt();
        if (projekt.getBetreuer() != null) {
            String inhalt = vollerName(zugang.user()) + " hat im Projekt \"" + projekt.getTitel()
                    + "\" das angeforderte Dokument \"" + anforderung.getTitel() + "\" eingereicht (Datei: "
                    + dokument.getDateiName() + ")."
                    + (eintrag.getKommentar() != null ? "\n\nKommentar: " + eintrag.getKommentar() : "")
                    + "\n\nBitte prüfen Sie das Dokument in SOPhub im Reiter \"Abgaben\".";
            anforderungEmailService.benachrichtige(List.of(empfaenger(projekt.getBetreuer())), projektId,
                    "Dokument eingereicht: " + anforderung.getTitel(), inhalt);
        }
    }

    /** Betreuung prueft die eingereichte Fassung: OK (akzeptiert) oder Ueberarbeiten (mit Begruendung). */
    @Transactional
    public void pruefen(Long projektId, Long anforderungId, String benutzername, String ergebnis, String kommentar) {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        if (!zugang.istBetreuer()) {
            throw new AccessDeniedException("Nur die Betreuung kann Dokumente prüfen.");
        }
        DokumentAnforderung anforderung = ladeAnforderung(projektId, anforderungId);
        if (!DokumentAnforderung.EINGEREICHT.equals(anforderung.getStatus())) {
            throw new IllegalArgumentException("Es liegt keine neue Einreichung zum Prüfen vor.");
        }

        boolean ok = "OK".equalsIgnoreCase(ergebnis) || DokumentAnforderung.AKZEPTIERT.equalsIgnoreCase(ergebnis);
        boolean ueberarbeiten = DokumentAnforderung.UEBERARBEITEN.equalsIgnoreCase(ergebnis);
        if (!ok && !ueberarbeiten) {
            throw new IllegalArgumentException("Ungültiges Prüfergebnis.");
        }
        String text = pruefeOptionalenText(kommentar);
        if (ueberarbeiten && text == null) {
            throw new IllegalArgumentException("Bitte beschreiben Sie, was überarbeitet werden soll.");
        }

        String neuerStatus = ok ? DokumentAnforderung.AKZEPTIERT : DokumentAnforderung.UEBERARBEITEN;

        AnforderungEintrag eintrag = new AnforderungEintrag();
        eintrag.setAnforderung(anforderung);
        eintrag.setArt(neuerStatus);
        eintrag.setPerson(zugang.user());
        eintrag.setKommentar(text);
        eintragRepository.save(eintrag);

        anforderung.setStatus(neuerStatus);
        anforderungRepository.save(anforderung);

        // Ganzes Team informieren
        String projektTitel = zugang.projekt().getTitel();
        String inhalt = ok
                ? vollerName(zugang.user()) + " hat im Projekt \"" + projektTitel + "\" das Dokument \""
                    + anforderung.getTitel() + "\" akzeptiert."
                    + (text != null ? "\n\nAnmerkung: " + text : "")
                : vollerName(zugang.user()) + " bittet im Projekt \"" + projektTitel + "\" um eine Überarbeitung des Dokuments \""
                    + anforderung.getTitel() + "\".\n\nWas geändert werden soll:\n" + text
                    + "\n\nBitte laden Sie danach eine neue Fassung im Reiter \"Abgaben\" hoch.";
        String betreff = ok ? "Dokument akzeptiert: " + anforderung.getTitel()
                : "Bitte überarbeiten: " + anforderung.getTitel();
        anforderungEmailService.benachrichtige(empfaengerTeam(zugang), projektId, betreff, inhalt);
    }

    /** Betreuung zieht eine Anforderung zurueck (die hochgeladenen Dateien bleiben im Projekt). */
    @Transactional
    public void loeschen(Long projektId, Long anforderungId, String benutzername) {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        if (!zugang.istBetreuer()) {
            throw new AccessDeniedException("Nur die Betreuung kann Anforderungen löschen.");
        }
        DokumentAnforderung anforderung = ladeAnforderung(projektId, anforderungId);
        eintragRepository.deleteAll(eintragRepository.findByAnforderungIdOrderByErstelltAmAsc(anforderungId));
        anforderungRepository.delete(anforderung);
    }

    // ---------------------------------------------------------------- Hilfsmethoden

    private Zugang pruefeZugang(Long projektId, String benutzername) {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new IllegalArgumentException("Projekt nicht gefunden."));
        User user = userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new AccessDeniedException("Benutzer nicht gefunden."));

        boolean istTeam = (projekt.getStudent() != null && projekt.getStudent().getId().equals(user.getId()))
                || projektMitgliedRepository.existsByProjektIdAndStudentId(projektId, user.getId());
        boolean istBetreuer = betreuerAktiv(projekt) && projekt.getBetreuer().getId().equals(user.getId());

        if (!istTeam && !istBetreuer) {
            throw new AccessDeniedException("Nur Projektmitglieder und die Betreuung sehen die Abgaben.");
        }
        return new Zugang(projekt, user, istTeam, istBetreuer);
    }

    // Betreuer:in nimmt erst teil, wenn das Projekt angenommen (oder schon abgeschlossen) ist
    private boolean betreuerAktiv(Projekt projekt) {
        String status = projekt.getStatus() == null ? "" : projekt.getStatus();
        return projekt.getBetreuer() != null
                && ("ANGENOMMEN".equalsIgnoreCase(status) || "ABGESCHLOSSEN".equalsIgnoreCase(status));
    }

    private DokumentAnforderung ladeAnforderung(Long projektId, Long anforderungId) {
        return anforderungRepository.findById(anforderungId)
                .filter(a -> a.getProjekt().getId().equals(projektId))
                .orElseThrow(() -> new IllegalArgumentException("Anforderung nicht gefunden."));
    }

    private LocalDate leseFrist(String frist) {
        if (frist == null || frist.isBlank()) {
            return null;
        }
        try {
            LocalDate datum = LocalDate.parse(frist.trim());
            if (datum.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Die Frist darf nicht in der Vergangenheit liegen.");
            }
            return datum;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Die Frist ist kein gültiges Datum.");
        }
    }

    private String pruefeOptionalenText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String bereinigt = text.trim();
        if (bereinigt.length() > MAX_TEXT) {
            throw new IllegalArgumentException("Der Text darf höchstens " + MAX_TEXT + " Zeichen lang sein.");
        }
        return bereinigt;
    }

    // Ersteller:in + Teammitglieder (ohne die Person, die gerade handelt)
    private List<AnforderungEmailService.Empfaenger> empfaengerTeam(Zugang zugang) {
        List<AnforderungEmailService.Empfaenger> liste = new ArrayList<>();
        Projekt projekt = zugang.projekt();
        if (projekt.getStudent() != null && !projekt.getStudent().getId().equals(zugang.user().getId())) {
            liste.add(empfaenger(projekt.getStudent()));
        }
        for (ProjektMitglied pm : projektMitgliedRepository.findByProjektId(projekt.getId())) {
            if (!pm.getStudent().getId().equals(zugang.user().getId())) {
                liste.add(empfaenger(pm.getStudent()));
            }
        }
        return liste;
    }

    private AnforderungEmailService.Empfaenger empfaenger(User user) {
        return new AnforderungEmailService.Empfaenger(user.getEmail(), user.getVorname(), user.getName());
    }

    private String vollerName(User user) {
        return ((user.getVorname() == null ? "" : user.getVorname()) + " "
                + (user.getName() == null ? "" : user.getName())).trim();
    }

    private PersonAnsicht person(User user, Projekt projekt) {
        String rolle;
        if (projekt.getBetreuer() != null && projekt.getBetreuer().getId().equals(user.getId())) {
            rolle = "Betreuer:in";
        } else if (projekt.getStudent() != null && projekt.getStudent().getId().equals(user.getId())) {
            rolle = "Ersteller:in";
        } else {
            rolle = "Teammitglied";
        }
        return new PersonAnsicht(user.getId(), user.getVorname(), user.getName(), rolle);
    }

    private record Zugang(Projekt projekt, User user, boolean istTeam, boolean istBetreuer) {}

    // ---------------------------------------------------------------- Antwort-Objekte fuer das Frontend

    public record PersonAnsicht(Long id, String vorname, String name, String rolle) {}

    public record EintragAnsicht(Long id, String art, String kommentar, LocalDateTime erstelltAm,
                                 PersonAnsicht person, Long dokumentId, String dateiName) {}

    public record AnforderungAnsicht(Long id, String typ, String titel, String beschreibung, LocalDate frist,
                                     String status, LocalDateTime erstelltAm, PersonAnsicht angefordertVon,
                                     List<EintragAnsicht> verlauf) {}

    public record Uebersicht(boolean istTeam, boolean istBetreuer, List<AnforderungAnsicht> anforderungen) {}
}