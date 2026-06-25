package com.sophub.repository;

import com.sophub.model.Projekt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjektRepository extends JpaRepository<Projekt, Long> {
    List<Projekt> findByStudentId(Long studentId);
    List<Projekt> findByStatus(String status);
    List<Projekt> findByZugriffsgrad(Integer zugriffsgrad);
}
