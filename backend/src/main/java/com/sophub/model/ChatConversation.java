package com.sophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ein gespeicherter Chat des SOP-Assistenten (Verlauf im Stil von Claude).
 * Gehört genau einem Nutzer; beim Löschen des Nutzers werden seine Chats per
 * Datenbank-CASCADE mitgelöscht ({@link OnDelete}).
 * <p>
 * {@code aktualisiertAm} wird bei jeder neuen Nachricht aktualisiert und
 * bestimmt zusammen mit {@code angepinnt} die Sortierung in der Seitenleiste.
 */
@Entity
@Table(name = "chat_conversations", indexes = {
        @Index(name = "idx_chat_conv_user_aktualisiert",
                columnList = "user_id, aktualisiert_am DESC")
})
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(length = 120)
    private String titel;

    @Column(nullable = false)
    private boolean angepinnt = false;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @Column(name = "aktualisiert_am", nullable = false)
    private LocalDateTime aktualisiertAm;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("erstelltAm ASC")
    private List<ChatMessage> nachrichten = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime jetzt = LocalDateTime.now();
        erstelltAm = jetzt;
        if (aktualisiertAm == null) {
            aktualisiertAm = jetzt;
        }
    }

    // getter
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getTitel() { return titel; }
    public boolean isAngepinnt() { return angepinnt; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
    public LocalDateTime getAktualisiertAm() { return aktualisiertAm; }
    public List<ChatMessage> getNachrichten() { return nachrichten; }

    // setter
    public void setId(UUID id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setTitel(String titel) { this.titel = titel; }
    public void setAngepinnt(boolean angepinnt) { this.angepinnt = angepinnt; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
    public void setAktualisiertAm(LocalDateTime aktualisiertAm) { this.aktualisiertAm = aktualisiertAm; }
    public void setNachrichten(List<ChatMessage> nachrichten) { this.nachrichten = nachrichten; }
}
