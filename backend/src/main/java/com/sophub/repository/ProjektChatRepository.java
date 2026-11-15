package com.sophub.repository;

import com.sophub.model.ProjektChatNachricht;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjektChatRepository extends JpaRepository<ProjektChatNachricht, Long> {

    /** Gesamter Verlauf eines Projekts, älteste zuerst. */
    List<ProjektChatNachricht> findByProjektIdOrderByErstelltAmAsc(Long projektId);

    /** Nur Nachrichten nach einer bekannten id (für das Nachladen/Polling), älteste zuerst. */
    List<ProjektChatNachricht> findByProjektIdAndIdGreaterThanOrderByErstelltAmAsc(Long projektId, Long id);
}
