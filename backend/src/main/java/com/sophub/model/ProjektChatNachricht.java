package com.sophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Eine Nachricht im internen Projekt-Chat.
 * <p>
 * Der Chat ist bewusst an das PROJEKT gebunden (nicht an einzelne Teilnehmer):
 * Wer Zugriff auf das Projekt hat (Team + Betreuung), sieht den gesamten Verlauf.
 * Dadurch sieht auch ein später hinzugefügtes Teammitglied automatisch alle
 * bisherigen Nachrichten.
 */
@Entity
@Table(name = "projekt_chat_nachrichten")
public class ProjektChatNachricht {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Wird das Projekt geloescht, verschwindet auch sein Chat
    @ManyToOne(optional = false)
    @JoinColumn(name = "projekt_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Projekt projekt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private User autor;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    // Soft-Delete: Die Nachricht bleibt im Verlauf stehen, der Inhalt wird aber entfernt
    // und im Frontend als "Nachricht gelöscht" angezeigt (wie bei WhatsApp).
    @Column(name = "geloescht", nullable = false, columnDefinition = "boolean default false")
    private boolean geloescht = false;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @PrePersist
    protected void onCreate() {
        erstelltAm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Projekt getProjekt() { return projekt; }
    public User getAutor() { return autor; }
    public String getText() { return text; }
    public boolean isGeloescht() { return geloescht; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }

    public void setId(Long id) { this.id = id; }
    public void setProjekt(Projekt projekt) { this.projekt = projekt; }
    public void setAutor(User autor) { this.autor = autor; }
    public void setText(String text) { this.text = text; }
    public void setGeloescht(boolean geloescht) { this.geloescht = geloescht; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
}
