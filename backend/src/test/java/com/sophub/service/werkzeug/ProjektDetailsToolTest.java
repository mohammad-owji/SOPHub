package com.sophub.service.werkzeug;

import com.sophub.model.Dokument;
import com.sophub.model.Projekt;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.service.MitgliedschaftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * get_project_details: Metadaten/Typ immer; Originalinhalt nur für Berechtigte.
 */
@ExtendWith(MockitoExtension.class)
class ProjektDetailsToolTest {

    @Mock ProjektRepository projektRepository;
    @Mock DokumentRepository dokumentRepository;
    @Mock MitgliedschaftService mitgliedschaftService;

    private final JsonMapper om = JsonMapper.builder().build();
    private ProjektDetailsTool tool;

    @BeforeEach
    void setup() {
        tool = new ProjektDetailsTool(projektRepository, dokumentRepository, mitgliedschaftService, om);
    }

    private JsonNode args(long id) {
        return om.createObjectNode().put("project_id", id);
    }

    private Projekt projekt() {
        Projekt p = new Projekt();
        p.setId(1L);
        p.setTitel("Testprojekt");
        return p;
    }

    private Dokument dokument() {
        Dokument d = new Dokument();
        d.setId(10L);
        d.setDateiName("lastenheft.pdf");
        d.setTyp("LASTENHEFT");
        d.setKiZusammenfassung("Kurzfassung");
        d.setExtrahierterText("GEHEIMER INHALT");
        return d;
    }

    @Test
    void berechtigterSiehtOriginalInhalt() {
        when(projektRepository.findById(1L)).thenReturn(Optional.of(projekt()));
        when(dokumentRepository.findByProjektId(1L)).thenReturn(List.of(dokument()));
        when(mitgliedschaftService.darfInterneDetails(1L, "stud")).thenReturn(true);

        ProjektDetailsTool.Details d = (ProjektDetailsTool.Details) tool.ausfuehren(args(1L), "stud");

        assertThat(d.darfInterneInhalte()).isTrue();
        assertThat(d.dokumente().get(0).originalInhalt()).isEqualTo("GEHEIMER INHALT");
        assertThat(d.dokumente().get(0).kiZusammenfassung()).isEqualTo("Kurzfassung");
    }

    @Test
    void fremderSiehtNurMetadatenUndZusammenfassung() {
        when(projektRepository.findById(1L)).thenReturn(Optional.of(projekt()));
        when(dokumentRepository.findByProjektId(1L)).thenReturn(List.of(dokument()));
        when(mitgliedschaftService.darfInterneDetails(1L, "fremd")).thenReturn(false);

        ProjektDetailsTool.Details d = (ProjektDetailsTool.Details) tool.ausfuehren(args(1L), "fremd");

        assertThat(d.darfInterneInhalte()).isFalse();
        assertThat(d.dokumente().get(0).originalInhalt()).isNull();      // kein Rohinhalt
        assertThat(d.dokumente().get(0).typ()).isEqualTo("LASTENHEFT");  // Metadaten ja
        assertThat(d.dokumente().get(0).kiZusammenfassung()).isEqualTo("Kurzfassung");
    }
}
