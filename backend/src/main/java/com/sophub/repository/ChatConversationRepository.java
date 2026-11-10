package com.sophub.repository;

import com.sophub.model.ChatConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    /**
     * Chats eines Nutzers, angepinnte zuerst, danach nach letzter Aktualisierung
     * absteigend. Die Sortierung steckt bewusst im Methodennamen, damit sie nicht
     * von einem evtl. mitgegebenen {@link Pageable}-Sort überschrieben wird.
     */
    List<ChatConversation> findByUserIdOrderByAngepinntDescAktualisiertAmDesc(Long userId, Pageable pageable);

    /** Wie oben, zusätzlich gefiltert nach Titel (Teilstring, Groß-/Kleinschreibung egal). */
    List<ChatConversation> findByUserIdAndTitelContainingIgnoreCaseOrderByAngepinntDescAktualisiertAmDesc(
            Long userId, String titel, Pageable pageable);

    /** Besitzerprüfung: liefert den Chat nur, wenn er dem Nutzer gehört (sonst leer -> 404). */
    Optional<ChatConversation> findByIdAndUserId(UUID id, Long userId);

    long countByUserId(Long userId);

    /** Ältester, nicht angepinnter Chat eines Nutzers - zum Durchsetzen des Chat-Limits. */
    Optional<ChatConversation> findFirstByUserIdAndAngepinntFalseOrderByAktualisiertAmAsc(Long userId);
}
