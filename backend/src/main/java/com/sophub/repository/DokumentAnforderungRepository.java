package com.sophub.repository;

import com.sophub.model.DokumentAnforderung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DokumentAnforderungRepository extends JpaRepository<DokumentAnforderung, Long> {

    // Neueste Anforderungen zuerst
    List<DokumentAnforderung> findByProjektIdOrderByErstelltAmDesc(Long projektId);
}