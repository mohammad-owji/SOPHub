package com.sophub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String kennung;

    @Column(nullable = false)
    private String password;

    private String email;

    @ManyToOne
    @JoinColumn(name = "rolle_id", nullable = false)
    private Rolle rolle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKennung() { return kennung; }
    public void setKennung(String kennung) { this.kennung = kennung; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Rolle getRolle() { return rolle; }
    public void setRolle(Rolle rolle) { this.rolle = rolle; }
}
