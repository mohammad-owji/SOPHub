package com.sophub.service.werkzeug;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registry: dispatcht an das passende Werkzeug, reicht den Benutzernamen durch
 * und meldet unbekannte Werkzeuge als Fehler-JSON (statt zu werfen).
 */
class WerkzeugRegistryTest {

    private final JsonMapper om = JsonMapper.builder().build();

    /** Minimales Testwerkzeug, das den übergebenen Benutzernamen zurückgibt. */
    private final class EchoTool implements ChatTool {
        @Override public String name() { return "echo"; }
        @Override public String beschreibung() { return "Gibt den Benutzernamen zurueck."; }
        @Override public JsonNode parameterSchema() { return om.createObjectNode().put("type", "object"); }
        @Override public Object ausfuehren(JsonNode argumente, String benutzername) { return new Echo(benutzername); }
    }

    record Echo(String benutzer) {}

    @Test
    void fuehrtWerkzeugImNutzerkontextAus() {
        WerkzeugRegistry registry = new WerkzeugRegistry(List.of(new EchoTool()), om);
        Werkzeugkasten kasten = registry.fuer("stud");

        assertThat(kasten.werkzeuge()).hasSize(1);
        assertThat(kasten.werkzeuge().get(0).name()).isEqualTo("echo");

        String ergebnis = kasten.ausfuehren("echo", om.createObjectNode());
        assertThat(ergebnis).contains("stud");
    }

    @Test
    void unbekanntesWerkzeugLiefertFehlerJson() {
        WerkzeugRegistry registry = new WerkzeugRegistry(List.of(new EchoTool()), om);
        Werkzeugkasten kasten = registry.fuer("stud");

        String ergebnis = kasten.ausfuehren("gibtsnicht", null);
        assertThat(ergebnis).contains("fehler");
    }
}
