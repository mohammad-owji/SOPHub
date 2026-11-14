package com.sophub.repository;

import com.sophub.model.BetreuungsEinladung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BetreuungsEinladungRepository extends JpaRepository<BetreuungsEinladung, Long> {

    List<BetreuungsEinladung> findByProjektIdAndArtOrderByErstelltAmAsc(Long projektId, String art);

    List<BetreuungsEinladung> findByProjektIdAndArtAndStatus(Long projektId, String art, String status);

    // Einladungen einer Person: ueber ihr Konto oder (noch ohne Konto) ueber die E-Mail
    List<BetreuungsEinladung> findByEingeladenerIdOrEmailIgnoreCaseOrderByErstelltAmDesc(Long eingeladenerId, String email);

    boolean existsByProjektIdAndArtAndEmailIgnoreCaseAndStatusIn(Long projektId, String art, String email,
                                                               List<String> status);

    boolean existsByProjektIdAndArtAndStatus(Long projektId, String art, String status);

    boolean existsByProjektIdAndArtAndEingeladenerIdAndStatus(Long projektId, String art, Long eingeladenerId,
                                                              String status);

    /** Ist die Person angenommene Co-Betreuung dieses Projekts? */
    default boolean istCoBetreuer(Long projektId, Long userId) {
        if (projektId == null || userId == null) {
            return false;
        }
        return existsByProjektIdAndArtAndEingeladenerIdAndStatus(
                projektId, BetreuungsEinladung.ART_CO_BETREUUNG, userId, BetreuungsEinladung.ANGENOMMEN);
    }
}