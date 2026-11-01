package com.sophub.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lädt die externen KI-Prompt-Dateien aus {@code resources/prompts/} und
 * bereitet sie für einen KI-Aufruf auf.
 * <p>
 * Jede Prompt-Datei ist eine Markdown-Datei mit YAML-Frontmatter (zwischen
 * {@code ---}) sowie einem SYSTEM- und einem USER-Block, die durch
 * HTML-Kommentar-Markierungen abgegrenzt sind. Der Loader trennt beide Blöcke,
 * ersetzt die {@code {{PLATZHALTER}}} durch die übergebenen Werte und gibt das
 * Ergebnis als {@link RenderedPrompt} zurück. Der Inhalt der Dateien wird dabei
 * nicht verändert.
 * <p>
 * Die eingelesenen Rohtexte werden gecacht, damit die Dateien nicht bei jedem
 * KI-Aufruf erneut von der Platte gelesen werden.
 */
@Component
public class PromptLoader {

    private static final String VERZEICHNIS = "prompts/";

    private static final Pattern FRONTMATTER = Pattern.compile("^\\s*---.*?---\\s*", Pattern.DOTALL);
    private static final Pattern SYSTEM_BLOCK = Pattern.compile(
            "<!--\\s*=*\\s*SYSTEM\\s*=*\\s*-->(.*?)<!--\\s*=*\\s*/SYSTEM\\s*=*\\s*-->", Pattern.DOTALL);
    private static final Pattern USER_BLOCK = Pattern.compile(
            "<!--\\s*=*\\s*USER\\s*=*\\s*-->(.*?)<!--\\s*=*\\s*/USER\\s*=*\\s*-->", Pattern.DOTALL);
    private static final Pattern PLATZHALTER = Pattern.compile("\\{\\{\\s*([A-Z0-9_]+)\\s*}}");

    private final Map<String, RohPrompt> cache = new ConcurrentHashMap<>();

    /**
     * Lädt den Prompt mit der angegebenen id (Dateiname ohne {@code .md}) und
     * ersetzt die Platzhalter durch die Werte aus {@code werte}.
     *
     * @param id    z.B. {@code "summarize_document"}
     * @param werte Zuordnung Platzhaltername -> einzusetzender Wert
     */
    public RenderedPrompt lade(String id, Map<String, String> werte) {
        RohPrompt roh = cache.computeIfAbsent(id, this::leseUndZerlege);
        String system = ersetze(roh.system(), werte);
        String user = ersetze(roh.user(), werte);
        return new RenderedPrompt(system, user);
    }

    private RohPrompt leseUndZerlege(String id) {
        String inhalt = leseDatei(VERZEICHNIS + id + ".md");
        String ohneFrontmatter = FRONTMATTER.matcher(inhalt).replaceFirst("");

        String system = extrahiere(SYSTEM_BLOCK, ohneFrontmatter, id, "SYSTEM");
        String user = extrahiere(USER_BLOCK, ohneFrontmatter, id, "USER");
        return new RohPrompt(system.trim(), user.trim());
    }

    private String extrahiere(Pattern block, String inhalt, String id, String name) {
        Matcher matcher = block.matcher(inhalt);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Prompt-Datei \"" + id + ".md\" enthält keinen " + name + "-Block.");
        }
        return matcher.group(1);
    }

    private String ersetze(String vorlage, Map<String, String> werte) {
        Matcher matcher = PLATZHALTER.matcher(vorlage);
        StringBuilder ergebnis = new StringBuilder();
        while (matcher.find()) {
            String schluessel = matcher.group(1);
            String wert = werte.get(schluessel);
            matcher.appendReplacement(ergebnis, Matcher.quoteReplacement(wert == null ? "" : wert));
        }
        matcher.appendTail(ergebnis);
        return ergebnis.toString();
    }

    private String leseDatei(String pfad) {
        ClassPathResource resource = new ClassPathResource(pfad);
        try (InputStream in = resource.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt-Datei nicht lesbar: " + pfad, e);
        }
    }

    /** Roh eingelesene, noch nicht mit Werten befüllte Prompt-Blöcke. */
    private record RohPrompt(String system, String user) {}

    /**
     * Fertig befüllter Prompt: getrennter System- und User-Anteil sowie eine
     * kombinierte Fassung für KI-Schnittstellen, die keinen separaten
     * System-Prompt unterstützen.
     */
    public record RenderedPrompt(String system, String user) {
        public String kombiniert() {
            return system + "\n\n" + user;
        }
    }
}
