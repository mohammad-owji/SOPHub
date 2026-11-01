package com.sophub.service;

import com.sophub.model.Mitteilung;
import com.sophub.model.Projekt;
import com.sophub.model.ProjektMitglied;
import com.sophub.model.User;
import com.sophub.repository.MitteilungRepository;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mitteilungsbrett eines Projekts: wichtige Mitteilungen zwischen Team und Betreuer:in.
 * <ul>
 *     <li>Team = Ersteller:in + Teammitglieder. Das Team sieht alle Mitteilungen.</li>
 *     <li>Betreuer:in (nach Annahme des Projekts) sieht alle Mitteilungen ausser "Nur Team".</li>
 *     <li>Alle anderen haben keinen Zugriff.</li>
 * </ul>
 */
@Service
public class MitteilungService {

    // Erlaubte Kategorien - bewusst nur "wichtige" Arten, damit kein Small Talk entsteht
    private static final Set<String> KATEGORIEN = Set.of("WICHTIG", "FRAGE", "TERMIN", "ABGABE", "INFO");
    private static final int MAX_BETREFF = 150;
    private static final int MAX_TEXT = 2000;

    private final MitteilungRepository mitteilungRepository;
    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;
    private final ProjektMitgliedRepository projektMitgliedRepository;
    private final MitteilungEmailService mitteilungEmailService;

    public MitteilungService(MitteilungRepository mitteilungRepository,
                             ProjektRepository projektRepository,
                             UserRepository userRepository,
                             ProjektMitgliedRepository projektMitgliedRepository,
                             MitteilungEmailService mitteilungEmailService) {
        this.mitteilungRepository = mitteilungRepository;
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
        this.projektMitgliedRepository = projektMitgliedRepository;
        this.mitteilungEmailService = mitteilungEmailService;
    }

    /** Alle Mitteilungen des Projekts, die der angemeldete Benutzer sehen darf (neueste zuerst). */
    @Transactional(readOnly = true)
    public List<MitteilungAnsicht> liste(Long projektId, String benutzername) {
        Zugang zugang = pruefeZugang(projektId, benutzername);

        List<MitteilungAnsicht> ergebnis = new ArrayList<>();
        for (Mitteilung m : mitteilungRepository.findByProjektIdAndAntwortAufIsNullOrderByErstelltAmDesc(projektId)) {
            if (!darfSehen(zugang, m)) {
                continue;
            }
            List<AntwortAnsicht> antworten = mitteilungRepository.findByAntwortAufIdOrderByErstelltAmAsc(m.getId())
                    .stream()
                    .map(a -> new AntwortAnsicht(a.getId(), a.getText(), a.getErstelltAm(),
                            person(a.getAutor(), zugang.projekt()), a.getAutor().getId().equals(zugang.user().getId())))
                    .toList();
            ergebnis.add(new MitteilungAnsicht(m.getId(), m.getKategorie(), m.getBetreff(), m.getText(),
                    m.isNurTeam(), m.getErstelltAm(), person(m.getAutor(), zugang.projekt()),
                    m.getAutor().getId().equals(zugang.user().getId()), antworten));
        }
        return ergebnis;
    }

    /** Neue Hauptmitteilung schreiben. */
    @Transactional
    public void erstellen(Long projektId, String benutzername, String kategorie, String betreff,
                          String text, boolean nurTeam) {
        Zugang zugang = pruefeZugang(projektId, benutzername);

        String kat = kategorie == null ? "" : kategorie.trim().toUpperCase();
        if (!KATEGORIEN.contains(kat)) {
            throw new IllegalArgumentException("Bitte wählen Sie eine gültige Kategorie.");
        }
        String betreffBereinigt = betreff == null ? "" : betreff.trim();
        if (betreffBereinigt.isEmpty()) {
            throw new IllegalArgumentException("Bitte geben Sie einen Betreff ein.");
        }
        if (betreffBereinigt.length() > MAX_BETREFF) {
            throw new IllegalArgumentException("Der Betreff darf höchstens " + MAX_BETREFF + " Zeichen lang sein.");
        }
        String textBereinigt = pruefeText(text);

        // Die Betreuerin/der Betreuer schreibt immer an das ganze Projekt
        boolean nurFuerTeam = nurTeam && zugang.istTeam();

        Mitteilung mitteilung = new Mitteilung();
        mitteilung.setProjekt(zugang.projekt());
        mitteilung.setAutor(zugang.user());
        mitteilung.setKategorie(kat);
        mitteilung.setBetreff(betreffBereinigt);
        mitteilung.setText(textBereinigt);
        mitteilung.setNurTeam(nurFuerTeam);
        mitteilungRepository.save(mitteilung);

        // Alle anderen Beteiligten per E-Mail informieren
        List<MitteilungEmailService.Empfaenger> empfaenger = new ArrayList<>();
        for (User u : teamMitglieder(zugang.projekt())) {
            fuegeHinzu(empfaenger, u, zugang.user());
        }
        if (!nurFuerTeam && betreuerAktiv(zugang.projekt())) {
            fuegeHinzu(empfaenger, zugang.projekt().getBetreuer(), zugang.user());
        }
        mitteilungEmailService.benachrichtige(empfaenger, projektId, zugang.projekt().getTitel(),
                vollerName(zugang.user()), false, kat, betreffBereinigt, textBereinigt);
    }

    /** Antwort auf eine Hauptmitteilung schreiben. */
    @Transactional
    public void antworten(Long projektId, Long mitteilungId, String benutzername, String text) {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        Mitteilung haupt = ladeHauptmitteilung(projektId, mitteilungId);
        if (!darfSehen(zugang, haupt)) {
            throw new AccessDeniedException("Sie haben keinen Zugriff auf diese Mitteilung.");
        }
        String textBereinigt = pruefeText(text);

        Mitteilung antwort = new Mitteilung();
        antwort.setProjekt(zugang.projekt());
        antwort.setAutor(zugang.user());
        antwort.setAntwortAuf(haupt);
        antwort.setText(textBereinigt);
        antwort.setNurTeam(haupt.isNurTeam());
        mitteilungRepository.save(antwort);

        // Die Person, die die Mitteilung geschrieben hat, ueber die Antwort informieren
        List<MitteilungEmailService.Empfaenger> empfaenger = new ArrayList<>();
        fuegeHinzu(empfaenger, haupt.getAutor(), zugang.user());
        mitteilungEmailService.benachrichtige(empfaenger, projektId, zugang.projekt().getTitel(),
                vollerName(zugang.user()), true, haupt.getKategorie(), haupt.getBetreff(), textBereinigt);
    }

    /** Eigene Mitteilung oder Antwort loeschen (Antworten einer Mitteilung werden mitgeloescht). */
    @Transactional
    public void loeschen(Long projektId, Long mitteilungId, String benutzername) {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        Mitteilung mitteilung = mitteilungRepository.findById(mitteilungId)
                .filter(m -> m.getProjekt().getId().equals(projektId))
                .orElseThrow(() -> new IllegalArgumentException("Mitteilung nicht gefunden."));
        if (!mitteilung.getAutor().getId().equals(zugang.user().getId())) {
            throw new AccessDeniedException("Sie können nur Ihre eigenen Mitteilungen löschen.");
        }
        if (mitteilung.getAntwortAuf() == null) {
            mitteilungRepository.deleteAll(mitteilungRepository.findByAntwortAufIdOrderByErstelltAmAsc(mitteilungId));
        }
        mitteilungRepository.delete(mitteilung);
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
            throw new AccessDeniedException("Nur Projektmitglieder und die Betreuung sehen die Mitteilungen.");
        }
        return new Zugang(projekt, user, istTeam, istBetreuer);
    }

    // Betreuer:in nimmt erst teil, wenn das Projekt angenommen (oder schon abgeschlossen) ist
    private boolean betreuerAktiv(Projekt projekt) {
        String status = projekt.getStatus() == null ? "" : projekt.getStatus();
        return projekt.getBetreuer() != null
                && ("ANGENOMMEN".equalsIgnoreCase(status) || "ABGESCHLOSSEN".equalsIgnoreCase(status));
    }

    private boolean darfSehen(Zugang zugang, Mitteilung mitteilung) {
        return zugang.istTeam() || !mitteilung.isNurTeam();
    }

    private Mitteilung ladeHauptmitteilung(Long projektId, Long mitteilungId) {
        return mitteilungRepository.findById(mitteilungId)
                .filter(m -> m.getProjekt().getId().equals(projektId) && m.getAntwortAuf() == null)
                .orElseThrow(() -> new IllegalArgumentException("Mitteilung nicht gefunden."));
    }

    private String pruefeText(String text) {
        String bereinigt = text == null ? "" : text.trim();
        if (bereinigt.isEmpty()) {
            throw new IllegalArgumentException("Bitte geben Sie einen Text ein.");
        }
        if (bereinigt.length() > MAX_TEXT) {
            throw new IllegalArgumentException("Der Text darf höchstens " + MAX_TEXT + " Zeichen lang sein.");
        }
        return bereinigt;
    }

    private List<User> teamMitglieder(Projekt projekt) {
        List<User> team = new ArrayList<>();
        if (projekt.getStudent() != null) {
            team.add(projekt.getStudent());
        }
        for (ProjektMitglied pm : projektMitgliedRepository.findByProjektId(projekt.getId())) {
            team.add(pm.getStudent());
        }
        return team;
    }

    private void fuegeHinzu(List<MitteilungEmailService.Empfaenger> liste, User empfaenger, User autor) {
        if (empfaenger == null || empfaenger.getId().equals(autor.getId())) {
            return;
        }
        liste.add(new MitteilungEmailService.Empfaenger(empfaenger.getEmail(), empfaenger.getVorname(), empfaenger.getName()));
    }

    private String vollerName(User user) {
        return (user.getVorname() + " " + user.getName()).trim();
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

    public record AntwortAnsicht(Long id, String text, LocalDateTime erstelltAm,
                                 PersonAnsicht autor, boolean eigene) {}

    public record MitteilungAnsicht(Long id, String kategorie, String betreff, String text, boolean nurTeam,
                                    LocalDateTime erstelltAm, PersonAnsicht autor, boolean eigene,
                                    List<AntwortAnsicht> antworten) {}
}