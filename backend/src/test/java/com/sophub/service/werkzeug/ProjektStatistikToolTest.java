package com.sophub.service.werkzeug;

import com.sophub.model.Dokument;
import com.sophub.model.Projekt;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * get_project_stats: zählt Projekte je Dokumenttyp; alte Freitext-Werte (z.B.
 * DOKUMENTATION) werden auf SONSTIGES normalisiert.
 */
@ExtendWith(MockitoExtension.class)
class ProjektStatistikToolTest {

    @Mock ProjektRepository projektRepository;
    @Mock DokumentRepository dokumentRepository;

    private final JsonMapper om = JsonMapper.builder().build();
    private ProjektStatistikTool tool;

    @BeforeEach
    void setup() {
        tool = new ProjektStatistikTool(projektRepository, dokumentRepository, om);
    }

    private Projekt projekt(long id) {
        Projekt p = new Projekt();
        p.setId(id);
        return p;
    }

    private Dokument dok(String typ) {
        Dokument d = new Dokument();
        d.setTyp(typ);
        return d;
    }

    @Test
    void zaehltProjekteJeDokumenttyp() {
        when(projektRepository.findAll()).thenReturn(List.of(projekt(1L), projekt(2L)));
        when(dokumentRepository.findByProjektId(1L)).thenReturn(List.of(dok("LASTENHEFT"), dok("LASTENHEFT")));
        when(dokumentRepository.findByProjektId(2L)).thenReturn(List.of(dok("PFLICHTENHEFT"), dok("DOKUMENTATION")));

        ProjektStatistikTool.Ergebnis e = (ProjektStatistikTool.Ergebnis)
                tool.ausfuehren(om.createObjectNode().put("metric", "dokumenttyp"), "stud");

        assertThat(e.projekteGesamt()).isEqualTo(2);
        assertThat(e.anzahlProWert().get("LASTENHEFT")).isEqualTo(1L);      // nur Projekt 1 (distinct)
        assertThat(e.anzahlProWert().get("PFLICHTENHEFT")).isEqualTo(1L);   // Projekt 2
        assertThat(e.anzahlProWert().get("SONSTIGES")).isEqualTo(1L);       // DOKUMENTATION -> SONSTIGES
        assertThat(e.anzahlProWert().get("PRAESENTATION")).isEqualTo(0L);
    }
}
