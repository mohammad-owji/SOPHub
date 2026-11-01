package com.sophub.repository;

import com.sophub.model.AnforderungEintrag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnforderungEintragRepository extends JpaRepository<AnforderungEintrag, Long> {

    // Verlauf einer Anforderung, aeltester Eintrag zuerst
    List<AnforderungEintrag> findByAnforderungIdOrderByErstelltAmAsc(Long anforderungId);
}