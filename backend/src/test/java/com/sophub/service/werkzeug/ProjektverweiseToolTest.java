package com.sophub.service.werkzeug;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

class ProjektverweiseToolTest {

    private final JsonMapper om = JsonMapper.builder().build();
    private final ProjektverweiseTool tool = new ProjektverweiseTool(om);

    @Test
    void normalisiertVerweiseUndUeberspringtOhneId() {
        ObjectNode args = om.createObjectNode();
        ArrayNode verweise = args.putArray("verweise");
        ObjectNode v1 = verweise.addObject();
        v1.put("id", 5);
        v1.put("titel", "Lernplattform");
        v1.put("aehnlichkeit", "sehr hoch");   // ungültig -> Default mittel
        v1.put("begruendung", "Beide nutzen Lerngruppen");
        verweise.addObject().put("titel", "Verweis ohne id wird verworfen");

        Werkzeugkasten.AusgabeBloecke ergebnis =
                (Werkzeugkasten.AusgabeBloecke) tool.ausfuehren(args, "stud");

        assertThat(ergebnis.bloecke()).hasSize(1);
        JsonNode b = ergebnis.bloecke().get(0);
        assertThat(b.get("typ").asText()).isEqualTo("project_ref");
        assertThat(b.get("id").asLong()).isEqualTo(5L);
        assertThat(b.get("titel").asText()).isEqualTo("Lernplattform");
        assertThat(b.get("aehnlichkeit").asText()).isEqualTo("mittel");
    }
}
