package com.sophub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dokumente")
public class Dokument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dateiName;

    @Column(nullable = false)
    private String dateiPfad;

    // z.B. LASTENHEFT, PFLICHTENHEFT, DOKUMENTATION, SONSTIGES
    @Column(nullable = false)
    private String typ;

    @Column(name = "hochgeladen_am", nullable = false, updatable = false)
    private LocalDateTime hochgeladenAm;

    @Column(name = "extrahierter_text", columnDefinition = "TEXT")
    private String extrahierterText;

    @Column(name = "ki_zusammenfassung", columnDefinition = "TEXT")
    private String kiZusammenfassung;

    // Ergebnis von summarize_document (rohes JSON), Eingabe für die Projekt-Zusammenfassung
    @Column(name = "ki_zusammenfassung_json", columnDefinition = "TEXT")
    private String kiZusammenfassungJson;

    @ManyToOne
    @JoinColumn(name = "benutzer_id", nullable = false)
    private User hochgeladenVon;

    @ManyToOne
    @JoinColumn(name = "projekt_id")
    private Projekt projekt;

    @PrePersist
    protected void onCreate() {
        hochgeladenAm = LocalDateTime.now();
    }

    // getter
    public Long getId() { return id; }
    public String getDateiName() { return dateiName; }
    public String getDateiPfad() { return dateiPfad; }
    public String getTyp() { return typ; }
    public String getExtrahierterText() { return extrahierterText; }
    public String getKiZusammenfassung() { return kiZusammenfassung; }
    public String getKiZusammenfassungJson() { return kiZusammenfassungJson; }
    public LocalDateTime getHochgeladenAm() { return hochgeladenAm; }
    public User getHochgeladenVon() { return hochgeladenVon; }
    public Projekt getProjekt() { return projekt; }

    // setter
    public void setId(Long id) { this.id = id; }
    public void setDateiName(String dateiName) { this.dateiName = dateiName; }
    public void setDateiPfad(String dateiPfad) { this.dateiPfad = dateiPfad; }
    public void setTyp(String typ) { this.typ = typ; }
    public void setExtrahierterText(String extrahierterText) { this.extrahierterText = extrahierterText; }
    public void setKiZusammenfassung(String kiZusammenfassung) { this.kiZusammenfassung = kiZusammenfassung; }
    public void setKiZusammenfassungJson(String kiZusammenfassungJson) { this.kiZusammenfassungJson = kiZusammenfassungJson; }
    public void setHochgeladenAm(LocalDateTime hochgeladenAm) { this.hochgeladenAm = hochgeladenAm; }
    public void setHochgeladenVon(User hochgeladenVon) { this.hochgeladenVon = hochgeladenVon; }
    public void setProjekt(Projekt projekt) { this.projekt = projekt; }
}
