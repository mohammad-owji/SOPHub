package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.repository.ProjektRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlSimilarityServiceTest {

    @Mock ProjektRepository projektRepository;
    @InjectMocks SqlSimilarityService service;

    @Test
    void findetUeberschneidendeProjekteUndIgnoriertUnpassende() {
        Projekt p1 = new Projekt();
        p1.setId(1L);
        p1.setTitel("Lernplattform für Studierende");
        p1.setBeschreibung("Eine Plattform für Lerngruppen und Austausch");
        p1.setTags(new LinkedHashSet<>(List.of(new Tag("Datenbank"))));

        Projekt p2 = new Projekt();
        p2.setId(2L);
        p2.setTitel("Rennspiel");
        p2.setBeschreibung("Ein Autospiel in 3D");

        when(projektRepository.findAll()).thenReturn(List.of(p1, p2));

        List<SimilarityService.AehnlichkeitTreffer> treffer =
                service.findeAehnliche("Ich moechte eine Lernplattform fuer Lerngruppen bauen", 5);

        assertThat(treffer).hasSize(1);
        assertThat(treffer.get(0).projektId()).isEqualTo(1L);
        assertThat(treffer.get(0).gemeinsameBegriffe()).contains("lernplattform");
        assertThat(treffer.get(0).punktzahl()).isGreaterThan(0);
    }

    @Test
    void leererIdeentextLiefertNichts() {
        assertThat(service.findeAehnliche("   ", 5)).isEmpty();
    }
}
