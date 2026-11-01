package com.sophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Mitteilung im Projekt (Mitteilungsbrett statt Chat).
 * Eine Hauptmitteilung hat Kategorie und Betreff. Antworten verweisen ueber "antwortAuf"
 * auf ihre Hauptmitteilung und haben nur einen Text.
 */
@Entity
@Table(name = "projekt_mitteilungen")
public class Mitteilung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Wird das Projekt geloescht, verschwinden auch seine Mitteilungen
    @ManyToOne(optional = false)
    @JoinColumn(name = "projekt_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Projekt projekt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private User autor;

    // null = Hauptmitteilung, sonst Antwort auf diese Mitteilung
    @ManyToOne
    @JoinColumn(name = "antwort_auf_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Mitteilung antwortAuf;

    // WICHTIG, FRAGE, TERMIN, ABGABE, INFO (nur bei Hauptmitteilungen)
    @Column(length = 20)
    private String kategorie;

    @Column(length = 150)
    private String betreff;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    // true = nur fuer das Team sichtbar (ohne Betreuer:in)
    @Column(name = "nur_team", nullable = false)
    private boolean nurTeam;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @PrePersist
    protected void onCreate() {
        erstelltAm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Projekt getProjekt() { return projekt; }
    public User getAutor() { return autor; }
    public Mitteilung getAntwortAuf() { return antwortAuf; }
    public String getKategorie() { return kategorie; }
    public String getBetreff() { return betreff; }
    public String getText() { return text; }
    public boolean isNurTeam() { return nurTeam; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }

    public void setId(Long id) { this.id = id; }
    public void setProjekt(Projekt projekt) { this.projekt = projekt; }
    public void setAutor(User autor) { this.autor = autor; }
    public void setAntwortAuf(Mitteilung antwortAuf) { this.antwortAuf = antwortAuf; }
    public void setKategorie(String kategorie) { this.kategorie = kategorie; }
    public void setBetreff(String betreff) { this.betreff = betreff; }
    public void setText(String text) { this.text = text; }
    public void setNurTeam(boolean nurTeam) { this.nurTeam = nurTeam; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
}