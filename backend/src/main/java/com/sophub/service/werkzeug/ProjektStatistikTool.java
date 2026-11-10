package com.sophub.service.werkzeug;

import com.sophub.model.Dokument;
import com.sophub.model.Dokumenttyp;
import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Werkzeug {@code get_project_stats}: Aggregationen über alle Projekte - Anzahl
 * je Dokumenttyp, je Technologie/Tag, je Semester oder je Status. Es werden nur
 * Anzahlen über öffentlich sichtbare Metadaten gezählt, keine geschützten Inhalte.
 */
@Component
public class ProjektStatistikTool implements ChatTool {

    private final ProjektRepository projektRepository;
    private final DokumentRepository dokumentRepository;
    private final ObjectMapper objectMapper;

    public ProjektStatistikTool(ProjektRepository projektRepository,
                                DokumentRepository dokumentRepository,
                                ObjectMapper objectMapper) {
        this.projektRepository = projektRepository;
        this.dokumentRepository = dokumentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "get_project_stats";
    }

    @Override
    public String beschreibung() {
        return "Zählt Projekte gruppiert nach einer Kennzahl: 'dokumenttyp' (Projekte mit mind. "
                + "einem Dokument des Typs), 'technologie' (Tag), 'semester' oder 'status'.";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode metric = props.putObject("metric");
        metric.put("type", "string");
        metric.putArray("enum").add("dokumenttyp").add("technologie").add("semester").add("status");
        schema.putArray("required").add("metric");
        return schema;
    }

    @Override
    public Object ausfuehren(JsonNode argumente, String benutzername) {
        String metric = Args.text(argumente, "metric");
        if (metric == null) {
            return new Fehler("Parameter 'metric' fehlt.");
        }
        List<Projekt> projekte = projektRepository.findAll();

        Map<String, Long> werte = switch (metric.toLowerCase()) {
            case "dokumenttyp" -> jeDokumenttyp(projekte);
            case "technologie" -> jeTechnologie(projekte);
            case "semester" -> zaehle(projekte, p -> leerOder(p.getSemester()));
            case "status" -> zaehle(projekte, p -> leerOder(p.getStatus()));
            default -> null;
        };
        if (werte == null) {
            return new Fehler("Unbekannte metric: " + metric);
        }
        return new Ergebnis(metric.toLowerCase(), projekte.size(), werte);
    }

    private Map<String, Long> jeDokumenttyp(List<Projekt> projekte) {
        // Projekte mit mindestens einem Dokument des jeweiligen (normalisierten) Typs.
        Map<String, Long> ergebnis = new LinkedHashMap<>();
        for (Dokumenttyp typ : Dokumenttyp.values()) {
            ergebnis.put(typ.name(), 0L);
        }
        for (Projekt p : projekte) {
            List<Dokument> docs = dokumentRepository.findByProjektId(p.getId());
            docs.stream()
                    .map(d -> Dokumenttyp.vonEingabe(d.getTyp()))
                    .distinct()
                    .forEach(typ -> ergebnis.merge(typ.name(), 1L, Long::sum));
        }
        return ergebnis;
    }

    private Map<String, Long> jeTechnologie(List<Projekt> projekte) {
        Map<String, Long> ergebnis = new TreeMap<>();
        for (Projekt p : projekte) {
            for (Tag tag : p.getTags()) {
                ergebnis.merge(tag.getName(), 1L, Long::sum);
            }
        }
        return ergebnis;
    }

    private Map<String, Long> zaehle(List<Projekt> projekte, java.util.function.Function<Projekt, String> schluessel) {
        Map<String, Long> ergebnis = new TreeMap<>();
        for (Projekt p : projekte) {
            ergebnis.merge(schluessel.apply(p), 1L, Long::sum);
        }
        return ergebnis;
    }

    private String leerOder(String s) {
        return (s == null || s.isBlank()) ? "(ohne Angabe)" : s;
    }

    record Ergebnis(String metric, long projekteGesamt, Map<String, Long> anzahlProWert) {}

    record Fehler(String fehler) {}
}
