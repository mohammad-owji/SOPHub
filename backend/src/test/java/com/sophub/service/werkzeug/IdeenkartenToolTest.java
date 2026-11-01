package com.sophub.service.werkzeug;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

class IdeenkartenToolTest {

    private final JsonMapper om = JsonMapper.builder().build();
    private final IdeenkartenTool tool = new IdeenkartenTool(om);

    @Test
    void normalisiertKartenUndUeberspringtOhneTitel() {
        ObjectNode args = om.createObjectNode();
        ArrayNode karten = args.putArray("karten");
        ObjectNode k1 = karten.addObject();
        k1.put("titel", "Lern-App");
        k1.put("aufwand", "riesig");       // ungültig -> Default mittel
        k1.put("machbarkeit", "gruen");
        k1.putArray("mvp").add("Login").add("Kurse");
        karten.addObject().put("problem", "Karte ohne Titel wird verworfen");

        Werkzeugkasten.AusgabeBloecke ergebnis =
                (Werkzeugkasten.AusgabeBloecke) tool.ausfuehren(args, "stud");

        assertThat(ergebnis.bloecke()).hasSize(1);
        JsonNode b = ergebnis.bloecke().get(0);
        assertThat(b.get("typ").asText()).isEqualTo("idea_card");
        assertThat(b.get("titel").asText()).isEqualTo("Lern-App");
        assertThat(b.get("aufwand").asText()).isEqualTo("mittel");
        assertThat(b.get("machbarkeit").asText()).isEqualTo("gruen");
        assertThat(b.get("mvp").size()).isEqualTo(2);
    }
}
