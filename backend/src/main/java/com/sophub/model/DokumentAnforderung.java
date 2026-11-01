package com.sophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ein Dokument, das die Betreuung beim Team anfordert (z.B. Lastenheft).
 * Ablauf: ANGEFORDERT -> EINGEREICHT -> (UEBERARBEITEN -> EINGEREICHT ...) -> AKZEPTIERT.
 * Der Verlauf (Einreichungen und Rueckmeldungen) steht in {@link AnforderungEintrag}.
 */
@Entity
@Table(name = "dokument_anforderungen")
public class DokumentAnforderung {

    public static final String ANGEFORDERT = "ANGEFORDERT";
    public static final String EINGEREICHT = "EINGEREICHT";
    public static final String UEBERARBEITEN = "UEBERARBEITEN";
    public static final String AKZEPTIERT = "AKZEPTIERT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Wird das Projekt geloescht, verschwinden auch seine Anforderungen
    @ManyToOne(optional = false)
    @JoinColumn(name = "projekt_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Projekt projekt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "angefordert_von_id", nullable = false)
    private User angefordertVon;

    // z.B. LASTENHEFT, PFLICHTENHEFT, ... oder SONSTIGES (dann zaehlt der Titel)
    @Column(nullable = false, length = 30)
    private String typ;

    // Anzeigename, z.B. "Lastenheft" oder ein eigener Name wie "Zwischenbericht"
    @Column(nullable = false, length = 150)
    private String titel;

    @Column(columnDefinition = "TEXT")
    private String beschreibung;

    // Optionales Abgabedatum
    private LocalDate frist;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @Column(name = "geaendert_am")
    private LocalDateTime geaendertAm;

    @PrePersist
    protected void onCreate() {
        erstelltAm = LocalDateTime.now();
        geaendertAm = erstelltAm;
    }

    @PreUpdate
    protected void onUpdate() {
        geaendertAm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Projekt getProjekt() { return projekt; }
    public User getAngefordertVon() { return angefordertVon; }
    public String getTyp() { return typ; }
    public String getTitel() { return titel; }
    public String getBeschreibung() { return beschreibung; }
    public LocalDate getFrist() { return frist; }
    public String getStatus() { return status; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
    public LocalDateTime getGeaendertAm() { return geaendertAm; }

    public void setId(Long id) { this.id = id; }
    public void setProjekt(Projekt projekt) { this.projekt = projekt; }
    public void setAngefordertVon(User angefordertVon) { this.angefordertVon = angefordertVon; }
    public void setTyp(String typ) { this.typ = typ; }
    public void setTitel(String titel) { this.titel = titel; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }
    public void setFrist(LocalDate frist) { this.frist = frist; }
    public void setStatus(String status) { this.status = status; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
    public void setGeaendertAm(LocalDateTime geaendertAm) { this.geaendertAm = geaendertAm; }
}