package com.sophub.service.werkzeug;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

class AktionsButtonToolTest {

    private final JsonMapper om = JsonMapper.builder().build();
    private final AktionsButtonTool tool = new AktionsButtonTool(om);

    @Test
    void erzeugtAktionsblockMitServerseitigemZiel() {
        ObjectNode args = om.createObjectNode();
        args.put("aktion", "projekt_erstellen");

        Werkzeugkasten.AusgabeBloecke ergebnis =
                (Werkzeugkasten.AusgabeBloecke) tool.ausfuehren(args, "stud");

        assertThat(ergebnis.bloecke()).hasSize(1);
        JsonNode b = ergebnis.bloecke().get(0);
        assertThat(b.get("typ").asText()).isEqualTo("action");
        assertThat(b.get("aktion").asText()).isEqualTo("projekt_erstellen");
        assertThat(b.get("ziel").asText()).isEqualTo("/create-project");
        assertThat(b.get("label").asText()).isNotBlank();
        assertThat(b.get("text").asText()).isNotBlank();
    }

    @Test
    void ideenfinderZeigtAufIdeenseite() {
        ObjectNode args = om.createObjectNode();
        args.put("aktion", "IDEENFINDER"); // Groß-/Kleinschreibung egal

        Werkzeugkasten.AusgabeBloecke ergebnis =
                (Werkzeugkasten.AusgabeBloecke) tool.ausfuehren(args, "stud");

        assertThat(ergebnis.bloecke()).hasSize(1);
        assertThat(ergebnis.bloecke().get(0).get("ziel").asText()).isEqualTo("/ideas");
    }

    @Test
    void unbekannteAktionErzeugtKeinenBlock() {
        ObjectNode args = om.createObjectNode();
        args.put("aktion", "server_neustarten");

        Werkzeugkasten.AusgabeBloecke ergebnis =
                (Werkzeugkasten.AusgabeBloecke) tool.ausfuehren(args, "stud");

        assertThat(ergebnis.bloecke()).isEmpty();
    }
}
