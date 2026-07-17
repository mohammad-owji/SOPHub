package com.sophub.repository;

import com.sophub.model.ProjektMitglied;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjektMitgliedRepository extends JpaRepository<ProjektMitglied, Long> {
    List<ProjektMitglied> findByProjektId(Long projektId);
    boolean existsByProjektIdAndStudentId(Long projektId, Long studentId);
    void deleteByProjektIdAndStudentId(Long projektId, Long studentId);
}
