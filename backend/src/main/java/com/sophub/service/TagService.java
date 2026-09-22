package com.sophub.service;

import com.sophub.config.ErlaubteTags;
import com.sophub.model.Projekt;
import com.sophub.model.Tag;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ProjektRepository projektRepository;

    public TagService(TagRepository tagRepository, ProjektRepository projektRepository) {
        this.tagRepository = tagRepository;
        this.projektRepository = projektRepository;
    }

    /**
     * Gleicht die KI-Antwort (kommagetrennte Tag-Liste) gegen die erlaubte Tag-Liste ab,
     * verwirft Unbekanntes und ordnet die gültigen Tags dem Projekt zu (ohne Duplikate).
     */
    public void tagsAusKiAntwortUebernehmen(Long projektId, String kiAntwort) {
        if (projektId == null || kiAntwort == null || kiAntwort.isBlank()) {
            return;
        }

        Projekt projekt = projektRepository.findById(projektId)
                .orElseThrow(() -> new RuntimeException("Projekt nicht gefunden."));

        Set<Tag> gueltigeTags = new LinkedHashSet<>(projekt.getTags());

        for (String kandidat : kiAntwort.split(",")) {
            String bereinigt = kandidat.trim();
            if (bereinigt.isEmpty()) continue;

            ErlaubteTags.ALLE.stream()
                    .filter(erlaubt -> erlaubt.equalsIgnoreCase(bereinigt))
                    .findFirst()
                    .ifPresent(erlaubterName -> gueltigeTags.add(findOderErstellen(erlaubterName)));
        }

        projekt.setTags(gueltigeTags);
        projektRepository.save(projekt);
    }

    private Tag findOderErstellen(String name) {
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> tagRepository.save(new Tag(name)));
    }
}
