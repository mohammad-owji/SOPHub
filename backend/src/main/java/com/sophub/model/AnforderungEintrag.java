package com.sophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Ein Eintrag im Verlauf einer Dokument-Anforderung:
 * EINGEREICHT (Team laedt eine Datei hoch), UEBERARBEITEN oder AKZEPTIERT (Rueckmeldung der Betreuung).
 */
@Entity
@Table(name = "anforderung_verlauf")
public class AnforderungEintrag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "anforderung_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DokumentAnforderung anforderung;

    // EINGEREICHT, UEBERARBEITEN oder AKZEPTIERT
    @Column(nullable = false, length = 20)
    private String art;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private User person;

    @Column(columnDefinition = "TEXT")
    private String kommentar;

    // Hochgeladene Datei (nur bei EINGEREICHT). Wird die Datei geloescht, bleibt der Eintrag ohne Datei stehen.
    @ManyToOne
    @JoinColumn(name = "dokument_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Dokument dokument;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @PrePersist
    protected void onCreate() {
        erstelltAm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public DokumentAnforderung getAnforderung() { return anforderung; }
    public String getArt() { return art; }
    public User getPerson() { return person; }
    public String getKommentar() { return kommentar; }
    public Dokument getDokument() { return dokument; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }

    public void setId(Long id) { this.id = id; }
    public void setAnforderung(DokumentAnforderung anforderung) { this.anforderung = anforderung; }
    public void setArt(String art) { this.art = art; }
    public void setPerson(User person) { this.person = person; }
    public void setKommentar(String kommentar) { this.kommentar = kommentar; }
    public void setDokument(Dokument dokument) { this.dokument = dokument; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
}