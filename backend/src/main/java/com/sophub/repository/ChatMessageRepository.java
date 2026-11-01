package com.sophub.repository;

import com.sophub.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Alle Nachrichten eines Chats in zeitlicher Reihenfolge (zum Öffnen des Chats). */
    List<ChatMessage> findByConversationIdOrderByErstelltAmAsc(UUID conversationId);

    /**
     * Die letzten 10 Nachrichten eines Chats (neueste zuerst) als KI-Kontext.
     * Der Aufrufer dreht die Liste für die chronologische Reihenfolge um.
     */
    List<ChatMessage> findTop10ByConversationIdOrderByErstelltAmDesc(UUID conversationId);

    long countByConversationId(UUID conversationId);
}
