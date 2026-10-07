package com.sophub.repository;

import com.sophub.model.Mitteilung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MitteilungRepository extends JpaRepository<Mitteilung, Long> {

    // Hauptmitteilungen eines Projekts, neueste zuerst
    List<Mitteilung> findByProjektIdAndAntwortAufIsNullOrderByErstelltAmDesc(Long projektId);

    // Antworten auf eine Mitteilung, aelteste zuerst (wie ein Gespraechsverlauf)
    List<Mitteilung> findByAntwortAufIdOrderByErstelltAmAsc(Long mitteilungId);
}