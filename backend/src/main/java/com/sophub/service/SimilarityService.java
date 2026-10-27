package com.sophub.service;

import java.util.List;

/**
 * Findet zu einem freien Ideentext ähnliche bestehende Projekte.
 * <p>
 * Hinter diesem Interface steckt im MVP eine einfache Überschneidungs-Suche
 * (Tags/Technologien/Schlagwörter/Text). Es ist bewusst gekapselt, damit später
 * z.&nbsp;B. pgvector/Embeddings eingebaut werden können, ohne den Chat oder die
 * Tools zu ändern.
 */
public interface SimilarityService {

    List<AehnlichkeitTreffer> findeAehnliche(String ideenText, int limit);

    /**
     * Ein Treffer der Ähnlichkeitssuche.
     *
     * @param punktzahl         Überschneidungs-Score (höher = ähnlicher)
     * @param gemeinsameBegriffe die überschneidenden Begriffe (für die Begründung)
     */
    record AehnlichkeitTreffer(Long projektId, String titel, String kurzbeschreibung,
                               int punktzahl, List<String> gemeinsameBegriffe) {}
}
