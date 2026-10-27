package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.repository.ProjektRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MVP-Implementierung von {@link SimilarityService}: Vorauswahl über die
 * Überschneidung von Begriffen (Tags, Schlagwörter, Titel, Beschreibung).
 * Tags/Schlagwörter zählen stärker als Freitext. Deterministisch und ohne
 * externen Dienst - später durch pgvector/Embeddings ersetzbar.
 */
@Service
public class SqlSimilarityService implements SimilarityService {

    private static final int MIN_LAENGE = 4;
    private static final int TAG_GEWICHT = 3;

    private final ProjektRepository projektRepository;

    public SqlSimilarityService(ProjektRepository projektRepository) {
        this.projektRepository = projektRepository;
    }

    @Override
    public List<AehnlichkeitTreffer> findeAehnliche(String ideenText, int limit) {
        Set<String> ideenBegriffe = begriffe(ideenText);
        if (ideenBegriffe.isEmpty()) {
            return List.of();
        }
        int max = limit <= 0 ? 5 : limit;

        List<AehnlichkeitTreffer> treffer = new ArrayList<>();
        for (Projekt projekt : projektRepository.findAll()) {
            Set<String> tagBegriffe = projekt.getTags().stream()
                    .map(Tag::getName)
                    .flatMap(name -> begriffe(name).stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> textBegriffe = begriffe(
                    gefahrlos(projekt.getTitel()) + " " + gefahrlos(projekt.getSchlagwoerter())
                            + " " + gefahrlos(projekt.getBeschreibung()));

            Set<String> gemeinsameTags = schnittmenge(ideenBegriffe, tagBegriffe);
            Set<String> gemeinsamerText = schnittmenge(ideenBegriffe, textBegriffe);
            gemeinsamerText.removeAll(gemeinsameTags);

            int punktzahl = gemeinsameTags.size() * TAG_GEWICHT + gemeinsamerText.size();
            if (punktzahl > 0) {
                Set<String> alleGemeinsam = new LinkedHashSet<>(gemeinsameTags);
                alleGemeinsam.addAll(gemeinsamerText);
                treffer.add(new AehnlichkeitTreffer(projekt.getId(), projekt.getTitel(),
                        kurz(projekt.getBeschreibung()), punktzahl, new ArrayList<>(alleGemeinsam)));
            }
        }

        treffer.sort((a, b) -> Integer.compare(b.punktzahl(), a.punktzahl()));
        return treffer.size() > max ? treffer.subList(0, max) : treffer;
    }

    private Set<String> begriffe(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(text.toLowerCase().split("[^\\p{L}0-9+#]+"))
                .filter(w -> w.length() >= MIN_LAENGE)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> schnittmenge(Set<String> a, Set<String> b) {
        Set<String> s = new LinkedHashSet<>(a);
        s.retainAll(b);
        return s;
    }

    private String gefahrlos(String s) {
        return s == null ? "" : s;
    }

    private String kurz(String beschreibung) {
        if (beschreibung == null) {
            return "";
        }
        String b = beschreibung.trim();
        return b.length() > 200 ? b.substring(0, 200) + "..." : b;
    }
}
