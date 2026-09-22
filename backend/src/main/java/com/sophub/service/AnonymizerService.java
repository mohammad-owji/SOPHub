package com.sophub.service;

import com.sophub.model.ProjektMitglied;
import com.sophub.model.User;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ersetzt Namen von Projekt-Teammitgliedern (Ersteller, Betreuer, weitere Mitglieder)
 * durch den Platzhalter [STUDENT], bevor ein Text an die KI geschickt wird.
 */
@Service
public class AnonymizerService {

    private static final String PLATZHALTER = "[STUDENT]";

    private final ProjektRepository projektRepository;
    private final ProjektMitgliedRepository projektMitgliedRepository;

    public AnonymizerService(ProjektRepository projektRepository,
                             ProjektMitgliedRepository projektMitgliedRepository) {
        this.projektRepository = projektRepository;
        this.projektMitgliedRepository = projektMitgliedRepository;
    }

    public String anonymisiere(String text, Long projektId) {
        if (text == null || text.isBlank() || projektId == null) {
            return text;
        }

        String ergebnis = text;
        for (String name : namenDesTeams(projektId)) {
            ergebnis = ersetzeGanzesWort(ergebnis, name);
        }
        return ergebnis;
    }

    private Set<String> namenDesTeams(Long projektId) {
        Set<String> namen = new LinkedHashSet<>();

        projektRepository.findById(projektId).ifPresent(projekt -> {
            namen.addAll(namenVonUser(projekt.getStudent()));
            namen.addAll(namenVonUser(projekt.getBetreuer()));
        });

        for (ProjektMitglied mitglied : projektMitgliedRepository.findByProjektId(projektId)) {
            namen.addAll(namenVonUser(mitglied.getStudent()));
        }

        return namen;
    }

    private Set<String> namenVonUser(User user) {
        Set<String> teile = new LinkedHashSet<>();
        if (user == null) {
            return teile;
        }

        String vorname = user.getVorname();
        String name = user.getName();

        if (vorname != null && !vorname.isBlank()) teile.add(vorname);
        if (name != null && !name.isBlank()) teile.add(name);
        if (vorname != null && !vorname.isBlank() && name != null && !name.isBlank()) {
            teile.add(vorname + " " + name);
        }
        return teile;
    }

    private String ersetzeGanzesWort(String text, String wort) {
        String regex = "(?iU)\\b" + Pattern.quote(wort) + "\\b";
        return Pattern.compile(regex).matcher(text).replaceAll(Matcher.quoteReplacement(PLATZHALTER));
    }
}
