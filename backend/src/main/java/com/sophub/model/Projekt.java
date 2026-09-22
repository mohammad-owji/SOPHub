package com.sophub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "projekte")
public class Projekt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titel;

    private String beschreibung;

    private String semester;

    private String fachbereich;

    private String projektart;

    private String sprache;

    private String schlagwoerter;

    private String status;

    private Integer gruppenanzahl;

    // 1=Admin, 2=Prof, 3=Studenten
    private Integer zugriffsgrad;

    // FK zur Bewertungstabelle (kommt später)
    @Column(name = "bewertung_id")
    private Long bewertungId;

    // Sterne-Bewertung durch Nutzer (1-5)
    private Integer rate;

    @Column(name = "benutzer_anzahl")
    private Integer benutzerAnzahl = 0;

    @Column(name = "ki_generiert")
    private Boolean kiGeneriert = false;

    @Column(name = "ki_zusammenfassung", columnDefinition = "TEXT")
    private String kiZusammenfassung;

    @ManyToMany
    @JoinTable(
            name = "projekt_tags",
            joinColumns = @JoinColumn(name = "projekt_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    @Column(name = "erstellt_am")
    private LocalDateTime erstelltAm;

    @Column(name = "geaendert_am")
    private LocalDateTime geaendertAm;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "betreuer_id")
    private User betreuer;

    @PrePersist
    protected void onCreate() {
        erstelltAm = LocalDateTime.now();
        geaendertAm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        geaendertAm = LocalDateTime.now();
    }

    // getter
    public Long getId() { return id; }
    public String getTitel() { return titel; }
    public String getBeschreibung() { return beschreibung; }
    public String getSemester() { return semester; }
    public String getFachbereich() { return fachbereich; }
    public String getProjektart() { return projektart; }
    public String getSprache() { return sprache; }
    public String getSchlagwoerter() { return schlagwoerter; }
    public String getStatus() { return status; }
    public Integer getGruppenanzahl() { return gruppenanzahl; }
    public Integer getZugriffsgrad() { return zugriffsgrad; }
    public Long getBewertungId() { return bewertungId; }
    public Integer getRate() { return rate; }
    public Integer getBenutzerAnzahl() { return benutzerAnzahl; }
    public Boolean getKiGeneriert() { return kiGeneriert; }
    public String getKiZusammenfassung() { return kiZusammenfassung; }
    public Set<Tag> getTags() { return tags; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
    public LocalDateTime getGeaendertAm() { return geaendertAm; }
    public User getStudent() { return student; }
    public User getBetreuer() { return betreuer; }

    // setter
    public void setId(Long id) { this.id = id; }
    public void setTitel(String titel) { this.titel = titel; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }
    public void setSemester(String semester) { this.semester = semester; }
    public void setFachbereich(String fachbereich) { this.fachbereich = fachbereich; }
    public void setProjektart(String projektart) { this.projektart = projektart; }
    public void setSprache(String sprache) { this.sprache = sprache; }
    public void setSchlagwoerter(String schlagwoerter) { this.schlagwoerter = schlagwoerter; }
    public void setStatus(String status) { this.status = status; }
    public void setGruppenanzahl(Integer gruppenanzahl) { this.gruppenanzahl = gruppenanzahl; }
    public void setZugriffsgrad(Integer zugriffsgrad) { this.zugriffsgrad = zugriffsgrad; }
    public void setBewertungId(Long bewertungId) { this.bewertungId = bewertungId; }
    public void setRate(Integer rate) { this.rate = rate; }
    public void setBenutzerAnzahl(Integer benutzerAnzahl) { this.benutzerAnzahl = benutzerAnzahl; }
    public void setKiGeneriert(Boolean kiGeneriert) { this.kiGeneriert = kiGeneriert; }
    public void setKiZusammenfassung(String kiZusammenfassung) { this.kiZusammenfassung = kiZusammenfassung; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
    public void setGeaendertAm(LocalDateTime geaendertAm) { this.geaendertAm = geaendertAm; }
    public void setStudent(User student) { this.student = student; }
    public void setBetreuer(User betreuer) { this.betreuer = betreuer; }
}
