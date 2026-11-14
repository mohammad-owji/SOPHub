package com.sophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Einladung einer weiteren Person zur Betreuung eines Projekts.
 * <ul>
 *     <li>art CO_BETREUUNG: Co-Betreuung (z.B. wissenschaftliche Mitarbeitende), nach Annahme
 *     mit denselben Verwaltungsrechten wie die Betreuung.</li>
 *     <li>art UEBERGABE: Die Betreuer:in uebergibt das Projekt (z.B. wegen Krankheit) an eine
 *     andere Person. Nach der Annahme ist diese Person die neue Betreuer:in.</li>
 * </ul>
 * Eingeladen wird ueber die Hochschul-E-Mail. Hat die Person noch kein Konto, wird die Einladung
 * nach der Registrierung mit derselben Adresse gefunden. Eintraege werden nicht geloescht,
 * damit der Verlauf der Betreuung nachvollziehbar bleibt.
 */
@Entity
@Table(name = "betreuungs_einladungen")
public class BetreuungsEinladung {

    public static final String ART_CO_BETREUUNG = "CO_BETREUUNG";
    public static final String ART_UEBERGABE = "UEBERGABE";

    public static final String OFFEN = "OFFEN";
    public static final String ANGENOMMEN = "ANGENOMMEN";
    public static final String ABGELEHNT = "ABGELEHNT";
    public static final String ZURUECKGEZOGEN = "ZURUECKGEZOGEN";
    public static final String BEENDET = "BEENDET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Wird das Projekt geloescht, verschwinden auch seine Einladungen
    @ManyToOne(optional = false)
    @JoinColumn(name = "projekt_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Projekt projekt;

    @Column(nullable = false, length = 30)
    private String art;

    // Hochschul-E-Mail der eingeladenen Person (klein geschrieben)
    @Column(nullable = false)
    private String email;

    // Konto der eingeladenen Person (null, solange sie noch nicht registriert ist)
    @ManyToOne
    @JoinColumn(name = "eingeladener_id")
    private User eingeladener;

    @ManyToOne(optional = false)
    @JoinColumn(name = "eingeladen_von_id", nullable = false)
    private User eingeladenVon;

    // OFFEN, ANGENOMMEN, ABGELEHNT, ZURUECKGEZOGEN (vor der Antwort), BEENDET (nach der Annahme)
    @Column(nullable = false, length = 20)
    private String status;

    // Optionale Nachricht an die eingeladene Person (bei einer Uebergabe: der Grund)
    @Column(columnDefinition = "TEXT")
    private String nachricht;

    // Nur bei einer Uebergabe: Bleibt die bisherige Betreuer:in als Co-Betreuung im Projekt?
    @Column(name = "als_co_bleiben")
    private Boolean alsCoBleiben;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @Column(name = "beantwortet_am")
    private LocalDateTime beantwortetAm;

    @PrePersist
    protected void onCreate() {
        erstelltAm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Projekt getProjekt() { return projekt; }
    public String getArt() { return art; }
    public String getEmail() { return email; }
    public User getEingeladener() { return eingeladener; }
    public User getEingeladenVon() { return eingeladenVon; }
    public String getStatus() { return status; }
    public String getNachricht() { return nachricht; }
    public Boolean getAlsCoBleiben() { return alsCoBleiben; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
    public LocalDateTime getBeantwortetAm() { return beantwortetAm; }

    public void setId(Long id) { this.id = id; }
    public void setProjekt(Projekt projekt) { this.projekt = projekt; }
    public void setArt(String art) { this.art = art; }
    public void setEmail(String email) { this.email = email; }
    public void setEingeladener(User eingeladener) { this.eingeladener = eingeladener; }
    public void setEingeladenVon(User eingeladenVon) { this.eingeladenVon = eingeladenVon; }
    public void setStatus(String status) { this.status = status; }
    public void setNachricht(String nachricht) { this.nachricht = nachricht; }
    public void setAlsCoBleiben(Boolean alsCoBleiben) { this.alsCoBleiben = alsCoBleiben; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
    public void setBeantwortetAm(LocalDateTime beantwortetAm) { this.beantwortetAm = beantwortetAm; }
}