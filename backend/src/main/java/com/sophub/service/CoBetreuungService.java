package com.sophub.service;

import com.sophub.model.BetreuungsEinladung;
import com.sophub.model.Mitteilung;
import com.sophub.model.Projekt;
import com.sophub.model.ProjektMitglied;
import com.sophub.model.User;
import com.sophub.repository.BetreuungsEinladungRepository;
import com.sophub.repository.MitteilungRepository;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Co-Betreuung und Uebergabe der Betreuung eines Projekts.
 * <ul>
 *     <li>Die Betreuung (Betreuer:in oder angenommene Co-Betreuung) laedt weitere Personen
 *     ueber ihre Hochschul-E-Mail (@hs-bochum.de) zur Co-Betreuung ein.</li>
 *     <li>Nach der Annahme hat die Co-Betreuung dieselben Verwaltungsrechte wie die Betreuung
 *     (Mitteilungen, Abgaben anfordern und pruefen, Dokumente).</li>
 *     <li>Die Betreuer:in kann das Projekt (z.B. wegen Krankheit) an eine Co-Betreuung oder eine andere
 *     Person uebergeben. Die Person muss die Uebergabe annehmen. Mitteilungen, Abgaben und Dokumente
 *     bleiben erhalten, die Uebergabe wird als Mitteilung im Projekt vermerkt.</li>
 *     <li>Alle Einladungen bleiben gespeichert (Verlauf der Betreuung).</li>
 * </ul>
 */
@Service
public class CoBetreuungService {

    private static final String HOCHSCHUL_DOMAIN = "@hs-bochum.de";
    private static final int MAX_NACHRICHT = 1000;

    private final BetreuungsEinladungRepository einladungRepository;
    private final ProjektRepository projektRepository;
    private final UserRepository userRepository;
    private final ProjektMitgliedRepository projektMitgliedRepository;
    private final EmailService emailService;
    private final MitteilungRepository mitteilungRepository;

    public CoBetreuungService(BetreuungsEinladungRepository einladungRepository,
                              ProjektRepository projektRepository,
                              UserRepository userRepository,
                              ProjektMitgliedRepository projektMitgliedRepository,
                              EmailService emailService,
                              MitteilungRepository mitteilungRepository) {
        this.einladungRepository = einladungRepository;
        this.projektRepository = projektRepository;
        this.userRepository = userRepository;
        this.projektMitgliedRepository = projektMitgliedRepository;
        this.emailService = emailService;
        this.mitteilungRepository = mitteilungRepository;
    }

    // ---------------------------------------------------------------- Bereich "Betreuung" eines Projekts

    /** Betreuung und Co-Betreuung eines Projekts (fuer Team und Betreuung). */
    @Transactional(readOnly = true)
    public BetreuungAnsicht uebersicht(Long projektId, String benutzername) {
        Projekt projekt = ladeProjekt(projektId);
        User user = ladeUser(benutzername);

        boolean istTeam = istTeam(projekt, user);
        boolean istHauptbetreuer = istHauptbetreuer(projekt, user);
        boolean istCoBetreuer = betreuungAktiv(projekt) && einladungRepository.istCoBetreuer(projektId, user.getId());

        if (!istTeam && !istHauptbetreuer && !istCoBetreuer) {
            throw new AccessDeniedException("Nur das Team und die Betreuung sehen die Betreuung des Projekts.");
        }

        List<EinladungAnsicht> coBetreuung = einladungRepository
                .findByProjektIdAndArtOrderByErstelltAmAsc(projektId, BetreuungsEinladung.ART_CO_BETREUUNG)
                .stream()
                .map(this::ansicht)
                .toList();

        List<EinladungAnsicht> uebergaben = einladungRepository
                .findByProjektIdAndArtOrderByErstelltAmAsc(projektId, BetreuungsEinladung.ART_UEBERGABE)
                .stream()
                .map(this::ansicht)
                .toList();

        boolean laeuft = "ANGENOMMEN".equalsIgnoreCase(projekt.getStatus());
        boolean darfEinladen = (istHauptbetreuer || istCoBetreuer) && laeuft;
        boolean offeneUebergabe = uebergaben.stream().anyMatch(u -> BetreuungsEinladung.OFFEN.equals(u.status()));
        boolean darfUebergeben = istHauptbetreuer && laeuft && !offeneUebergabe;

        return new BetreuungAnsicht(
                projekt.getBetreuer() == null ? null : PersonAnsicht.von(projekt.getBetreuer()),
                projekt.getStatus(), coBetreuung, uebergaben, istTeam, istHauptbetreuer, istCoBetreuer,
                darfEinladen, darfUebergeben);
    }

    /** Weitere Person zur Co-Betreuung einladen (nur die aktive Betreuung). */
    @Transactional
    public EinladungAnsicht einladen(Long projektId, String benutzername, String email, String nachricht) {
        Projekt projekt = ladeProjekt(projektId);
        User einladender = ladeUser(benutzername);

        boolean darf = istHauptbetreuer(projekt, einladender)
                || (betreuungAktiv(projekt) && einladungRepository.istCoBetreuer(projektId, einladender.getId()));
        if (!darf) {
            throw new AccessDeniedException("Nur die Betreuung des Projekts kann zur Co-Betreuung einladen.");
        }
        if (!"ANGENOMMEN".equalsIgnoreCase(projekt.getStatus())) {
            throw new IllegalArgumentException("Eine Co-Betreuung ist nur bei laufenden (angenommenen) Projekten möglich.");
        }

        String adresse = pruefeHochschulAdresse(email);
        if (projekt.getBetreuer() != null && adresse.equalsIgnoreCase(projekt.getBetreuer().getEmail())) {
            throw new IllegalArgumentException("Diese Person betreut das Projekt bereits.");
        }
        boolean schonDa = einladungRepository.existsByProjektIdAndArtAndEmailIgnoreCaseAndStatusIn(
                projektId, BetreuungsEinladung.ART_CO_BETREUUNG, adresse,
                List.of(BetreuungsEinladung.OFFEN, BetreuungsEinladung.ANGENOMMEN));
        if (schonDa) {
            throw new IllegalArgumentException("Diese Person ist bereits eingeladen oder Co-Betreuung des Projekts.");
        }

        Optional<User> konto = pruefeBetreuungsKonto(adresse);
        String text = pruefeNachricht(nachricht);

        BetreuungsEinladung einladung = new BetreuungsEinladung();
        einladung.setProjekt(projekt);
        einladung.setArt(BetreuungsEinladung.ART_CO_BETREUUNG);
        einladung.setEmail(adresse);
        einladung.setEingeladener(konto.orElse(null));
        einladung.setEingeladenVon(einladender);
        einladung.setStatus(BetreuungsEinladung.OFFEN);
        einladung.setNachricht(text);
        BetreuungsEinladung gespeichert = einladungRepository.save(einladung);

        emailService.sendeCoBetreuungEinladungEmail(adresse, konto.orElse(null), einladender, projekt,
                gespeichert.getNachricht());

        return ansicht(gespeichert);
    }

    /**
     * Einladung zurueckziehen bzw. Co-Betreuung beenden.
     * Das darf nur die Betreuer:in, nicht die Co-Betreuung selbst.
     */
    @Transactional
    public void entfernen(Long projektId, Long einladungId, String benutzername) {
        Projekt projekt = ladeProjekt(projektId);
        User user = ladeUser(benutzername);
        BetreuungsEinladung einladung = einladungRepository.findById(einladungId)
                .filter(e -> e.getProjekt().getId().equals(projektId)
                        && BetreuungsEinladung.ART_CO_BETREUUNG.equals(e.getArt()))
                .orElseThrow(() -> new IllegalArgumentException("Einladung nicht gefunden."));

        if (!istHauptbetreuer(projekt, user)) {
            throw new AccessDeniedException("Nur die Betreuer:in kann Co-Betreuungen beenden.");
        }

        if (BetreuungsEinladung.OFFEN.equals(einladung.getStatus())) {
            einladung.setStatus(BetreuungsEinladung.ZURUECKGEZOGEN);
        } else if (BetreuungsEinladung.ANGENOMMEN.equals(einladung.getStatus())) {
            einladung.setStatus(BetreuungsEinladung.BEENDET);
        } else {
            throw new IllegalArgumentException("Diese Einladung ist nicht mehr aktiv.");
        }
        einladung.setBeantwortetAm(LocalDateTime.now());
        einladungRepository.save(einladung);
    }

    // ---------------------------------------------------------------- Uebergabe an eine andere Betreuung

    /**
     * Die Betreuer:in bittet eine andere Person (z.B. die Co-Betreuung), das Projekt zu uebernehmen.
     * Es kann immer nur eine offene Uebergabe geben. Die Person muss die Uebergabe annehmen.
     */
    @Transactional
    public EinladungAnsicht uebergabeAnfragen(Long projektId, String benutzername, String email, String grund,
                                              boolean alsCoBleiben) {
        Projekt projekt = ladeProjekt(projektId);
        User betreuer = ladeUser(benutzername);

        if (!istHauptbetreuer(projekt, betreuer)) {
            throw new AccessDeniedException("Nur die Betreuer:in kann das Projekt übergeben.");
        }
        if (!"ANGENOMMEN".equalsIgnoreCase(projekt.getStatus())) {
            throw new IllegalArgumentException("Nur laufende (angenommene) Projekte können übergeben werden.");
        }
        if (einladungRepository.existsByProjektIdAndArtAndStatus(projektId, BetreuungsEinladung.ART_UEBERGABE,
                BetreuungsEinladung.OFFEN)) {
            throw new IllegalArgumentException("Es gibt bereits eine offene Übergabe. Bitte ziehen Sie diese zuerst zurück.");
        }

        String adresse = pruefeHochschulAdresse(email);
        if (adresse.equalsIgnoreCase(betreuer.getEmail())) {
            throw new IllegalArgumentException("Sie können das Projekt nicht an sich selbst übergeben.");
        }
        Optional<User> konto = pruefeBetreuungsKonto(adresse);

        String text = pruefeNachricht(grund);

        BetreuungsEinladung uebergabe = new BetreuungsEinladung();
        uebergabe.setProjekt(projekt);
        uebergabe.setArt(BetreuungsEinladung.ART_UEBERGABE);
        uebergabe.setEmail(adresse);
        uebergabe.setEingeladener(konto.orElse(null));
        uebergabe.setEingeladenVon(betreuer);
        uebergabe.setStatus(BetreuungsEinladung.OFFEN);
        uebergabe.setNachricht(text);
        uebergabe.setAlsCoBleiben(alsCoBleiben);
        BetreuungsEinladung gespeichert = einladungRepository.save(uebergabe);

        emailService.sendeUebergabeAnfrageEmail(adresse, konto.orElse(null), betreuer, projekt, text);

        return ansicht(gespeichert);
    }

    /** Die Betreuer:in zieht eine offene Uebergabe zurueck. */
    @Transactional
    public void uebergabeZurueckziehen(Long projektId, Long einladungId, String benutzername) {
        Projekt projekt = ladeProjekt(projektId);
        User user = ladeUser(benutzername);
        BetreuungsEinladung uebergabe = einladungRepository.findById(einladungId)
                .filter(e -> e.getProjekt().getId().equals(projektId)
                        && BetreuungsEinladung.ART_UEBERGABE.equals(e.getArt()))
                .orElseThrow(() -> new IllegalArgumentException("Übergabe nicht gefunden."));

        if (!istHauptbetreuer(projekt, user)) {
            throw new AccessDeniedException("Nur die Betreuer:in kann die Übergabe zurückziehen.");
        }
        if (!BetreuungsEinladung.OFFEN.equals(uebergabe.getStatus())) {
            throw new IllegalArgumentException("Diese Übergabe ist nicht mehr offen.");
        }
        uebergabe.setStatus(BetreuungsEinladung.ZURUECKGEZOGEN);
        uebergabe.setBeantwortetAm(LocalDateTime.now());
        einladungRepository.save(uebergabe);
    }

    // Wird beim Annehmen einer Uebergabe aufgerufen: neue Betreuer:in eintragen, Verlauf vermerken
    private void uebergabeDurchfuehren(BetreuungsEinladung uebergabe, User neu) {
        Projekt projekt = uebergabe.getProjekt();
        User bisher = uebergabe.getEingeladenVon();

        if (projekt.getBetreuer() == null || !projekt.getBetreuer().getId().equals(bisher.getId())) {
            throw new IllegalArgumentException("Die Betreuung des Projekts hat sich inzwischen geändert.");
        }

        projekt.setBetreuer(neu);
        projektRepository.save(projekt);

        // War die neue Betreuer:in vorher Co-Betreuung, endet diese Rolle (sie ist jetzt die Betreuung)
        for (BetreuungsEinladung co : einladungRepository.findByProjektIdAndArtOrderByErstelltAmAsc(
                projekt.getId(), BetreuungsEinladung.ART_CO_BETREUUNG)) {
            boolean gleichePerson = (co.getEingeladener() != null && co.getEingeladener().getId().equals(neu.getId()))
                    || co.getEmail().equalsIgnoreCase(neu.getEmail());
            if (gleichePerson && BetreuungsEinladung.ANGENOMMEN.equals(co.getStatus())) {
                co.setStatus(BetreuungsEinladung.BEENDET);
                co.setBeantwortetAm(LocalDateTime.now());
                einladungRepository.save(co);
            } else if (gleichePerson && BetreuungsEinladung.OFFEN.equals(co.getStatus())) {
                co.setStatus(BetreuungsEinladung.ZURUECKGEZOGEN);
                co.setBeantwortetAm(LocalDateTime.now());
                einladungRepository.save(co);
            }
        }

        // Auf Wunsch bleibt die bisherige Betreuer:in als Co-Betreuung im Projekt
        if (Boolean.TRUE.equals(uebergabe.getAlsCoBleiben())) {
            BetreuungsEinladung co = new BetreuungsEinladung();
            co.setProjekt(projekt);
            co.setArt(BetreuungsEinladung.ART_CO_BETREUUNG);
            co.setEmail(bisher.getEmail() == null ? "" : bisher.getEmail().toLowerCase());
            co.setEingeladener(bisher);
            co.setEingeladenVon(neu);
            co.setStatus(BetreuungsEinladung.ANGENOMMEN);
            co.setNachricht("Bleibt nach der Übergabe als Co-Betreuung im Projekt.");
            co.setBeantwortetAm(LocalDateTime.now());
            einladungRepository.save(co);
        }

        // Verlauf: Die Uebergabe steht als Mitteilung im Projekt (fuer Team und Betreuung sichtbar)
        Mitteilung hinweis = new Mitteilung();
        hinweis.setProjekt(projekt);
        hinweis.setAutor(neu);
        hinweis.setKategorie("INFO");
        hinweis.setBetreff("Betreuung übergeben");
        hinweis.setText(vollerName(bisher) + " hat die Betreuung des Projekts an " + vollerName(neu) + " übergeben."
                + (uebergabe.getNachricht() != null ? "\n\nGrund: " + uebergabe.getNachricht() : "")
                + (Boolean.TRUE.equals(uebergabe.getAlsCoBleiben())
                    ? "\n\n" + vollerName(bisher) + " bleibt als Co-Betreuung im Projekt."
                    : "")
                + "\n\nMitteilungen, Abgaben und Dokumente bleiben unverändert erhalten.");
        hinweis.setNurTeam(false);
        mitteilungRepository.save(hinweis);

        // Team informieren
        if (projekt.getStudent() != null) {
            emailService.sendeUebergabeTeamEmail(projekt.getStudent(), bisher, neu, projekt);
        }
        for (ProjektMitglied mitglied : projektMitgliedRepository.findByProjektId(projekt.getId())) {
            if (mitglied.getStudent() != null) {
                emailService.sendeUebergabeTeamEmail(mitglied.getStudent(), bisher, neu, projekt);
            }
        }
    }

    // ---------------------------------------------------------------- Seite "Einladungen" der eingeladenen Person

    /** Alle Einladungen (Co-Betreuung und Uebergabe) der angemeldeten Person (neueste zuerst). */
    @Transactional
    public List<MeineEinladungAnsicht> meineEinladungen(String benutzername) {
        User user = ladeUser(benutzername);
        List<BetreuungsEinladung> einladungen = einladungRepository
                .findByEingeladenerIdOrEmailIgnoreCaseOrderByErstelltAmDesc(user.getId(), user.getEmail());

        // Einladungen, die vor der Registrierung verschickt wurden, mit dem Konto verbinden
        for (BetreuungsEinladung e : einladungen) {
            if (e.getEingeladener() == null) {
                e.setEingeladener(user);
                einladungRepository.save(e);
            }
        }

        return einladungen.stream()
                .filter(e -> !BetreuungsEinladung.ZURUECKGEZOGEN.equals(e.getStatus()))
                .map(e -> new MeineEinladungAnsicht(e.getId(), e.getArt(), e.getStatus(), e.getNachricht(),
                        e.getErstelltAm(), Boolean.TRUE.equals(e.getAlsCoBleiben()),
                        PersonAnsicht.von(e.getEingeladenVon()), ProjektKurz.von(e.getProjekt())))
                .toList();
    }

    @Transactional
    public void annehmen(Long einladungId, String benutzername) {
        entscheiden(einladungId, benutzername, true);
    }

    @Transactional
    public void ablehnen(Long einladungId, String benutzername) {
        entscheiden(einladungId, benutzername, false);
    }

    private void entscheiden(Long einladungId, String benutzername, boolean annehmen) {
        User user = ladeUser(benutzername);
        BetreuungsEinladung einladung = einladungRepository.findById(einladungId)
                .orElseThrow(() -> new IllegalArgumentException("Einladung nicht gefunden."));

        boolean fuerMich = (einladung.getEingeladener() != null && einladung.getEingeladener().getId().equals(user.getId()))
                || (user.getEmail() != null && user.getEmail().equalsIgnoreCase(einladung.getEmail()));
        if (!fuerMich) {
            throw new AccessDeniedException("Diese Einladung ist an eine andere Person gerichtet.");
        }
        if (!BetreuungsEinladung.OFFEN.equals(einladung.getStatus())) {
            throw new IllegalArgumentException("Diese Einladung wurde bereits beantwortet oder zurückgezogen.");
        }
        if (annehmen && !"ANGENOMMEN".equalsIgnoreCase(einladung.getProjekt().getStatus())) {
            throw new IllegalArgumentException("Das Projekt läuft nicht mehr. Die Einladung kann nicht angenommen werden.");
        }

        boolean istUebergabe = BetreuungsEinladung.ART_UEBERGABE.equals(einladung.getArt());
        if (annehmen && (user.getRolle() == null || !"PROFESSOR".equalsIgnoreCase(user.getRolle().getName()))) {
            throw new AccessDeniedException("Nur Betreuungs-Konten können diese Einladung annehmen.");
        }

        einladung.setEingeladener(user);
        einladung.setStatus(annehmen ? BetreuungsEinladung.ANGENOMMEN : BetreuungsEinladung.ABGELEHNT);
        einladung.setBeantwortetAm(LocalDateTime.now());
        einladungRepository.save(einladung);

        if (istUebergabe) {
            if (annehmen) {
                uebergabeDurchfuehren(einladung, user);
            }
            emailService.sendeUebergabeAntwortEmail(einladung.getEingeladenVon(), user, einladung.getProjekt(), annehmen);
        } else {
            // Die einladende Person informieren
            emailService.sendeCoBetreuungAntwortEmail(einladung.getEingeladenVon(), user, einladung.getProjekt(), annehmen);
        }
    }

    // ---------------------------------------------------------------- Hilfsmethoden

    private String pruefeHochschulAdresse(String email) {
        String adresse = email == null ? "" : email.trim().toLowerCase();
        if (adresse.isEmpty() || !adresse.contains("@")) {
            throw new IllegalArgumentException("Bitte geben Sie eine E-Mail-Adresse ein.");
        }
        if (!adresse.endsWith(HOCHSCHUL_DOMAIN)) {
            throw new IllegalArgumentException("Bitte geben Sie eine Hochschul-E-Mail-Adresse an (endet auf "
                    + HOCHSCHUL_DOMAIN + ").");
        }
        return adresse;
    }

    // Gibt es schon ein Konto zu der Adresse, muss es ein Betreuungs-Konto sein
    private Optional<User> pruefeBetreuungsKonto(String adresse) {
        Optional<User> konto = userRepository.findByEmail(adresse);
        if (konto.isPresent() && (konto.get().getRolle() == null
                || !"PROFESSOR".equalsIgnoreCase(konto.get().getRolle().getName()))) {
            throw new IllegalArgumentException("Diese E-Mail gehört keinem Betreuungs-Konto.");
        }
        return konto;
    }

    private String pruefeNachricht(String nachricht) {
        String text = nachricht == null ? "" : nachricht.trim();
        if (text.length() > MAX_NACHRICHT) {
            throw new IllegalArgumentException("Der Text darf höchstens " + MAX_NACHRICHT + " Zeichen lang sein.");
        }
        return text.isEmpty() ? null : text;
    }

    private String vollerName(User user) {
        return ((user.getVorname() == null ? "" : user.getVorname()) + " "
                + (user.getName() == null ? "" : user.getName())).trim();
    }

    private EinladungAnsicht ansicht(BetreuungsEinladung e) {
        User person = e.getEingeladener();
        return new EinladungAnsicht(e.getId(), e.getEmail(),
                person == null ? null : person.getId(),
                person == null ? null : person.getVorname(),
                person == null ? null : person.getName(),
                e.getStatus(), e.getNachricht(), Boolean.TRUE.equals(e.getAlsCoBleiben()),
                e.getErstelltAm(), e.getBeantwortetAm(),
                PersonAnsicht.von(e.getEingeladenVon()));
    }

    private boolean istTeam(Projekt projekt, User user) {
        return (projekt.getStudent() != null && projekt.getStudent().getId().equals(user.getId()))
                || projektMitgliedRepository.existsByProjektIdAndStudentId(projekt.getId(), user.getId());
    }

    private boolean istHauptbetreuer(Projekt projekt, User user) {
        return betreuungAktiv(projekt) && projekt.getBetreuer().getId().equals(user.getId());
    }

    // Die Betreuung ist erst nach der Annahme des Projekts aktiv
    private boolean betreuungAktiv(Projekt projekt) {
        String status = projekt.getStatus() == null ? "" : projekt.getStatus();
        return projekt.getBetreuer() != null
                && ("ANGENOMMEN".equalsIgnoreCase(status) || "ABGESCHLOSSEN".equalsIgnoreCase(status));
    }

    private Projekt ladeProjekt(Long projektId) {
        return projektRepository.findById(projektId)
                .orElseThrow(() -> new IllegalArgumentException("Projekt nicht gefunden."));
    }

    private User ladeUser(String benutzername) {
        return userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new AccessDeniedException("Benutzer nicht gefunden."));
    }

    // ---------------------------------------------------------------- Antwort-Objekte fuer das Frontend

    public record PersonAnsicht(Long id, String vorname, String name) {
        static PersonAnsicht von(User user) {
            return user == null ? null : new PersonAnsicht(user.getId(), user.getVorname(), user.getName());
        }
    }

    public record EinladungAnsicht(Long id, String email, Long personId, String vorname, String name, String status,
                                   String nachricht, boolean alsCoBleiben,
                                   LocalDateTime eingeladenAm, LocalDateTime beantwortetAm,
                                   PersonAnsicht eingeladenVon) {}

    public record BetreuungAnsicht(PersonAnsicht betreuer, String projektStatus, List<EinladungAnsicht> coBetreuung,
                                   List<EinladungAnsicht> uebergaben,
                                   boolean istTeam, boolean istBetreuer, boolean istCoBetreuer,
                                   boolean darfEinladen, boolean darfUebergeben) {}

    public record ProjektKurz(Long id, String titel, String beschreibung, String status, String semester,
                              String fachbereich, String studiengang, String projektart,
                              PersonAnsicht student, PersonAnsicht betreuer) {
        static ProjektKurz von(Projekt p) {
            return new ProjektKurz(p.getId(), p.getTitel(), p.getBeschreibung(), p.getStatus(), p.getSemester(),
                    p.getFachbereich(), p.getStudiengang(), p.getProjektart(),
                    PersonAnsicht.von(p.getStudent()), PersonAnsicht.von(p.getBetreuer()));
        }
    }

    public record MeineEinladungAnsicht(Long id, String art, String status, String nachricht,
                                        LocalDateTime eingeladenAm, boolean alsCoBleiben,
                                        PersonAnsicht eingeladenVon, ProjektKurz projekt) {}
}