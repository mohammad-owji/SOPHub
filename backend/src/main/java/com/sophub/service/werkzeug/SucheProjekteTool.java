package com.sophub.service.werkzeug;

import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.repository.ProjektRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Werkzeug {@code search_projects}: findet bestehende Projekte nach Freitext,
 * Technologien/Tags, Status und Semester. Liefert nur unkritische Metadaten
 * (keine internen Dokumentinhalte) und ist daher für alle Nutzer unbedenklich.
 */
@Component
public class SucheProjekteTool implements ChatTool {

    private static final int MAX_TREFFER = 15;

    private final ProjektRepository projektRepository;
    private final ObjectMapper objectMapper;

    public SucheProjekteTool(ProjektRepository projektRepository, ObjectMapper objectMapper) {
        this.projektRepository = projektRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "search_projects";
    }

    @Override
    public String beschreibung() {
        return "Sucht bestehende SOP-Projekte nach Stichwort, Technologien/Tags, Status oder Semester. "
                + "Liefert kompakte Metadaten (id, titel, kurzbeschreibung, technologien, status, semester).";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("query").put("type", "string").put("description", "Freitext-Suchbegriff");
        ObjectNode tech = props.putObject("technologies");
        tech.put("type", "array").put("description", "Technologien/Tags, die vorkommen sollen");
        tech.putObject("items").put("type", "string");
        ObjectNode tags = props.putObject("tags");
        tags.put("type", "array");
        tags.putObject("items").put("type", "string");
        props.putObject("status").put("type", "string");
        props.putObject("semester").put("type", "string");
        return schema;
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        String query = lower(Args.text(argumente, "query"));
        String status = lower(Args.text(argumente, "status"));
        String semester = lower(Args.text(argumente, "semester"));
        Set<String> gesuchteTags = Args.textListe(argumente, "technologies").stream()
                .map(this::lower).collect(Collectors.toSet());
        gesuchteTags.addAll(Args.textListe(argumente, "tags").stream().map(this::lower).collect(Collectors.toSet()));

        List<Treffer> treffer = projektRepository.findAll().stream()
                .filter(p -> query == null || enthaelt(p, query))
                .filter(p -> status == null || status.equalsIgnoreCase(sicher(p.getStatus())))
                .filter(p -> semester == null || semester.equalsIgnoreCase(sicher(p.getSemester())))
                .filter(p -> gesuchteTags.isEmpty() || tagNamen(p).stream().anyMatch(gesuchteTags::contains))
                .limit(MAX_TREFFER)
                .map(this::zuTreffer)
                .toList();

        return new Ergebnis(treffer.size(), treffer);
    }

    private boolean enthaelt(Projekt p, String q) {
        return sicher(p.getTitel()).toLowerCase().contains(q)
                || sicher(p.getBeschreibung()).toLowerCase().contains(q)
                || sicher(p.getSchlagwoerter()).toLowerCase().contains(q);
    }

    private Treffer zuTreffer(Projekt p) {
        return new Treffer(p.getId(), p.getTitel(), kurz(p.getBeschreibung()),
                p.getTags().stream().map(Tag::getName).toList(), p.getStatus(), p.getSemester());
    }

    private List<String> tagNamen(Projekt p) {
        return p.getTags().stream().map(t -> t.getName().toLowerCase()).toList();
    }

    private String sicher(String s) {
        return s == null ? "" : s;
    }

    private String lower(String s) {
        return s == null ? null : s.toLowerCase();
    }

    private String kurz(String beschreibung) {
        String b = sicher(beschreibung).trim();
        return b.length() > 200 ? b.substring(0, 200) + "..." : b;
    }

    record Treffer(Long id, String titel, String kurzbeschreibung, List<String> technologien,
                   String status, String semester) {}

    record Ergebnis(int anzahl, List<Treffer> projekte) {}
}
