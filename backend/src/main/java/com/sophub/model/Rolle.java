package com.sophub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rollen") // Erstellt die Tabelle "rollen" in PostgreSQL
public class Rolle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // Z.B. "STUDENT", "BETREUER", "ADMIN"

    // ==========================================
    // GETTER UND SETTER
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}