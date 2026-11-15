package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.ProjektChatNachricht;
import com.sophub.model.User;
import com.sophub.repository.BetreuungsEinladungRepository;
import com.sophub.repository.ProjektChatRepository;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interner Projekt-Chat: lockere Unterhaltung zwischen Team und Betreuung.
 * <ul>
 *     <li>Team = Ersteller:in + Teammitglieder.</li>
 *     <li>Betreuung = Betreuer:in + angenommene Co-Betreuung (sobald das Projekt angenommen ist).</li>
 *     <li>Alle Beteiligten sehen den GESAMTEN Verlauf - auch später hinzugefügte Teammitglieder.</li>
 * </ul>
 * Im Gegensatz zum Mitteilungsbrett gibt es hier keine Kategorien und keine
 * E-Mail-Benachrichtigung pro Nachricht (sonst würde ein Chat zu viele Mails erzeugen).
 */
@Service
public class ProjektChatService {

    private static final int MAX_TEXT = 2000;

    private final ProjektChatRepository chatRepository;
    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;
    private final ProjektMitgliedRepository projektMitgliedRepository;
    private final BetreuungsEinladungRepository betreuungsEinladungRepository;

    public ProjektChatService(ProjektChatRepository chatRepository,
                              ProjektRepository projektRepository,
                              UserRepository userRepository,
                              ProjektMitgliedRepository projektMitgliedRepository,
                              BetreuungsEinladungRepository betreuungsEinladungRepository) {
        this.chatRepository = chatRepository;
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
        this.projektMitgliedRepository = projektMitgliedRepository;
        this.betreuungsEinladungRepository = betreuungsEinladungRepository;
    }

    /**
     * Nachrichten des Projekt-Chats (älteste zuerst). Ist {@code nachId} gesetzt,
     * werden nur neuere Nachrichten geliefert (fürs Polling).
     */
    @Transactional(readOnly = true)
    public List<NachrichtAnsicht> liste(Long projektId, String benutzername, Long nachId) {
        Zugang zugang = pruefeZugang(projektId, benutzername);

        List<ProjektChatNachricht> nachrichten = (nachId == null)
                ? chatRepository.findByProjektIdOrderByErstelltAmAsc(projektId)
                : chatRepository.findByProjektIdAndIdGreaterThanOrderByErstelltAmAsc(projektId, nachId);

        return nachrichten.stream().map(n -> abbilden(n, zugang)).toList();
    }

    /** Neue Chat-Nachricht schreiben; liefert die gespeicherte Nachricht zurück. */
    @Transactional
    public NachrichtAnsicht senden(Long projektId, String benutzername, String text) {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        String bereinigt = text == null ? "" : text.trim();
        if (bereinigt.isEmpty()) {
            throw new IllegalArgumentException("Bitte geben Sie einen Text ein.");
        }
        if (bereinigt.length() > MAX_TEXT) {
            throw new IllegalArgumentException("Die Nachricht darf höchstens " + MAX_TEXT + " Zeichen lang sein.");
        }

        ProjektChatNachricht nachricht = new ProjektChatNachricht();
        nachricht.setProjekt(zugang.projekt());
        nachricht.setAutor(zugang.user());
        nachricht.setText(bereinigt);
        chatRepository.save(nachricht);

        return abbilden(nachricht, zugang);
    }

    /**
     * Eigene Nachricht löschen (Soft-Delete, wie bei WhatsApp): Die Nachricht bleibt im
     * Verlauf, ihr Inhalt wird entfernt und als "gelöscht" markiert. Nur der Autor darf das.
     */
    @Transactional
    public void loeschen(Long projektId, Long nachrichtId, String benutzername) {
        Zugang zugang = pruefeZugang(projektId, benutzername);
        ProjektChatNachricht nachricht = chatRepository.findById(nachrichtId)
                .filter(n -> n.getProjekt().getId().equals(projektId))
                .orElseThrow(() -> new IllegalArgumentException("Nachricht nicht gefunden."));
        if (!nachricht.getAutor().getId().equals(zugang.user().getId())) {
            throw new AccessDeniedException("Sie können nur Ihre eigenen Nachrichten löschen.");
        }
        if (!nachricht.isGeloescht()) {
            nachricht.setGeloescht(true);
            nachricht.setText(""); // Inhalt nicht mehr vorhalten
            chatRepository.save(nachricht);
        }
    }

    // ---------------------------------------------------------------- Hilfsmethoden

    private NachrichtAnsicht abbilden(ProjektChatNachricht n, Zugang zugang) {
        return new NachrichtAnsicht(
                n.getId(),
                n.isGeloescht() ? "" : n.getText(),
                n.isGeloescht(),
                n.getErstelltAm(),
                person(n.getAutor(), zugang.projekt()),
                n.getAutor().getId().equals(zugang.user().getId()));
    }

    private Zugang pruefeZugang(Long projektId, String benutzername) {
        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new IllegalArgumentException("Projekt nicht gefunden."));
        User user = userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new AccessDeniedException("Benutzer nicht gefunden."));

        boolean istTeam = (projekt.getStudent() != null && projekt.getStudent().getId().equals(user.getId()))
                || projektMitgliedRepository.existsByProjektIdAndStudentId(projektId, user.getId());
        boolean istBetreuer = betreuerAktiv(projekt) && (projekt.getBetreuer().getId().equals(user.getId())
                || betreuungsEinladungRepository.istCoBetreuer(projektId, user.getId()));

        if (!istTeam && !istBetreuer) {
            throw new AccessDeniedException("Nur Projektmitglieder und die Betreuung sehen den Chat.");
        }
        return new Zugang(projekt, user, istTeam, istBetreuer);
    }

    // Betreuung nimmt erst teil, wenn das Projekt angenommen (oder schon abgeschlossen) ist
    private boolean betreuerAktiv(Projekt projekt) {
        String status = projekt.getStatus() == null ? "" : projekt.getStatus();
        return projekt.getBetreuer() != null
                && ("ANGENOMMEN".equalsIgnoreCase(status) || "ABGESCHLOSSEN".equalsIgnoreCase(status));
    }

    /**
     * Rolle und Chat-Seite des Autors. Team steht links, Betreuung rechts -
     * unabhängig davon, wer den Chat gerade ansieht.
     */
    private PersonAnsicht person(User user, Projekt projekt) {
        String rolle;
        String seite;
        if (projekt.getBetreuer() != null && projekt.getBetreuer().getId().equals(user.getId())) {
            rolle = "Betreuer:in";
            seite = "rechts";
        } else if (betreuungsEinladungRepository.istCoBetreuer(projekt.getId(), user.getId())) {
            rolle = "Co-Betreuer:in";
            seite = "rechts";
        } else if (projekt.getStudent() != null && projekt.getStudent().getId().equals(user.getId())) {
            rolle = "Ersteller:in";
            seite = "links";
        } else if (projektMitgliedRepository.existsByProjektIdAndStudentId(projekt.getId(), user.getId())) {
            rolle = "Teammitglied";
            seite = "links";
        } else {
            // z.B. eine ehemalige Betreuung nach einer Übergabe - der Verlauf bleibt nachvollziehbar
            rolle = "Ehemalige Betreuung";
            seite = "rechts";
        }
        return new PersonAnsicht(user.getId(), user.getVorname(), user.getName(), rolle, seite);
    }

    private record Zugang(Projekt projekt, User user, boolean istTeam, boolean istBetreuer) {}

    // ---------------------------------------------------------------- Antwort-Objekte fürs Frontend

    public record PersonAnsicht(Long id, String vorname, String name, String rolle, String seite) {}

    public record NachrichtAnsicht(Long id, String text, boolean geloescht, LocalDateTime erstelltAm,
                                   PersonAnsicht autor, boolean eigene) {}
}
