package com.sophub.service.werkzeug;

import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.repository.ProjektRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Werkzeug {@code get_tag_landscape}: Häufigkeit der Technologien/Themen (Tags)
 * über alle Projekte - Grundlage für Fragen wie "was wurde selten/nie gemacht".
 */
@Component
public class TagLandschaftTool implements ChatTool {

    private static final int MAX = 30;

    private final ProjektRepository projektRepository;
    private final ObjectMapper objectMapper;

    public TagLandschaftTool(ProjektRepository projektRepository, ObjectMapper objectMapper) {
        this.projektRepository = projektRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "get_tag_landscape";
    }

    @Override
    public String beschreibung() {
        return "Liefert die häufigsten Technologien/Themen (Tags) über alle Projekte mit ihrer Anzahl.";
    }

    @Override
    public JsonNode parameterSchema() {
        // keine Parameter
        return objectMapper.createObjectNode().put("type", "object");
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        Map<String, Long> haeufigkeit = projektRepository.findAll().stream()
                .flatMap(p -> p.getTags().stream())
                .collect(Collectors.groupingBy(Tag::getName, Collectors.counting()));

        List<TagAnzahl> top = haeufigkeit.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(MAX)
                .map(e -> new TagAnzahl(e.getKey(), e.getValue()))
                .toList();

        return new Ergebnis(top.size(), top);
    }

    record TagAnzahl(String tag, long anzahl) {}

    record Ergebnis(int anzahl, List<TagAnzahl> tags) {}
}
