package com.sophub.repository;

import com.sophub.model.Dokument;
import com.sophub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DokumentRepository extends JpaRepository<Dokument, Long> {
    List<Dokument> findByHochgeladenVon(User user);
    List<Dokument> findByProjektId(Long projektId);

    // Beim Loeschen eines Projekts: Dokumente bleiben erhalten, gehoeren aber zu keinem Projekt mehr
    @Modifying
    @Query("UPDATE Dokument d SET d.projekt = null WHERE d.projekt.id = :projektId")
    void trenneVonProjekt(@Param("projektId") Long projektId);
}