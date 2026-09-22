package com.sophub.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "projekt_mitglieder", uniqueConstraints = @UniqueConstraint(columnNames = {"projekt_id", "student_id"}))
public class ProjektMitglied {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "projekt_id", nullable = false)
    private Projekt projekt;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "hinzugefuegt_am", nullable = false, updatable = false)
    private LocalDateTime hinzugefuegtAm;

    @PrePersist
    protected void onCreate() {
        hinzugefuegtAm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Projekt getProjekt() { return projekt; }
    public User getStudent() { return student; }
    public LocalDateTime getHinzugefuegtAm() { return hinzugefuegtAm; }

    public void setId(Long id) { this.id = id; }
    public void setProjekt(Projekt projekt) { this.projekt = projekt; }
    public void setStudent(User student) { this.student = student; }
    public void setHinzugefuegtAm(LocalDateTime hinzugefuegtAm) { this.hinzugefuegtAm = hinzugefuegtAm; }
}
