package com.sophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Eine einzelne Nachricht innerhalb eines {@link ChatConversation}.
 * <p>
 * {@code bloecke} hält optionale strukturierte Ausgaben (idea_card, project_ref)
 * als jsonb. Bewusst NICHT gespeichert werden Tool-Rohergebnisse,
 * Dokumentinhalte oder der Systemprompt.
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_msg_conversation_erstellt",
                columnList = "conversation_id, erstellt_am")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChatConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatRolle rolle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String inhalt;

    // Strukturierte Blöcke (idea_card/project_ref) als JSON-Array, nullable.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String bloecke;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @PrePersist
    protected void onCreate() {
        if (erstelltAm == null) {
            erstelltAm = LocalDateTime.now();
        }
    }

    // getter
    public UUID getId() { return id; }
    public ChatConversation getConversation() { return conversation; }
    public ChatRolle getRolle() { return rolle; }
    public String getInhalt() { return inhalt; }
    public String getBloecke() { return bloecke; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }

    // setter
    public void setId(UUID id) { this.id = id; }
    public void setConversation(ChatConversation conversation) { this.conversation = conversation; }
    public void setRolle(ChatRolle rolle) { this.rolle = rolle; }
    public void setInhalt(String inhalt) { this.inhalt = inhalt; }
    public void setBloecke(String bloecke) { this.bloecke = bloecke; }
    public void setErstelltAm(LocalDateTime erstelltAm) { this.erstelltAm = erstelltAm; }
}
