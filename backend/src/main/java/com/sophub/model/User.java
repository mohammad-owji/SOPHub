package com.sophub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String benutzername;

    @Column(nullable = false)
    private String password;

    private String email;

    @ManyToOne
    @JoinColumn(name = "rolle_id", nullable = false)
    private Rolle rolle;

    // getter
    public Long getId() { return id; }
    public String getBenutzername() { return benutzername; }
    public String getPassword() { return password; }
    public String getEmail() { return email;}
    public Rolle getRolle() { return rolle; }

    // setter
    public void setId(Long id) { this.id = id; }
    public void setBenutzername(String benutzername) { this.benutzername = benutzername; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setRolle(Rolle rolle) { this.rolle = rolle; }
}
