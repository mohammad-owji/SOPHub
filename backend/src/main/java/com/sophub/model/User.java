package com.sophub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String benutzername;

    @Column(nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String passwort;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String vorname;

    @ManyToOne
    @JoinColumn(name = "rolle_id", nullable = false)
    private Rolle rolle;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @Column(name = "bearbeitet_am")
    private LocalDateTime bearbeitetAm;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        erstelltAm = LocalDateTime.now();
        bearbeitetAm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        bearbeitetAm = LocalDateTime.now();
    }

    // getter
    public Long getId() { return id; }
    public String getBenutzername() { return benutzername; }
    public String getEmail() { return email; }
    public String getPasswort() { return passwort; }
    public String getName() { return name; }
    public String getVorname() { return vorname; }
    public Rolle getRolle() { return rolle; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
    public LocalDateTime getBearbeitetAm() { return bearbeitetAm; }
    public LocalDateTime getLastLogin() { return lastLogin; }

    // setter
    public void setId(Long id) { this.id = id; }
    public void setBenutzername(String benutzername) { this.benutzername = benutzername; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswort(String passwort) { this.passwort = passwort; }
    public void setName(String name) { this.name = name; }
    public void setVorname(String vorname) { this.vorname = vorname; }
    public void setRolle(Rolle rolle) { this.rolle = rolle; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
    public void setBearbeitetAm(LocalDateTime bearbeitetAm) { this.bearbeitetAm = bearbeitetAm; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}