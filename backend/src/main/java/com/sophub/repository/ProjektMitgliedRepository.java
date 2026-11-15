package com.sophub.repository;

import com.sophub.model.ProjektMitglied;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjektMitgliedRepository extends JpaRepository<ProjektMitglied, Long> {
    List<ProjektMitglied> findByProjektId(Long projektId);
    // Alle Mitgliedschaften einer Person (fuer "Meine Projekte", wenn man nur Teammitglied ist)
    List<ProjektMitglied> findByStudentId(Long studentId);
    boolean existsByProjektIdAndStudentId(Long projektId, Long studentId);
    // Beim Loeschen eines Projekts alle Teammitglieder entfernen
    void deleteByProjektId(Long projektId);
    void deleteByProjektIdAndStudentId(Long projektId, Long studentId);
}