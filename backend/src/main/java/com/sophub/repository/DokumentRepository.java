package com.sophub.repository;

import com.sophub.model.Dokument;
import com.sophub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DokumentRepository extends JpaRepository<Dokument, Long> {
    List<Dokument> findByHochgeladenVon(User user);
    List<Dokument> findByProjektId(Long projektId);
}
