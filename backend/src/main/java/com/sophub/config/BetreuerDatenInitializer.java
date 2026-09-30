package com.sophub.config;

import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.RolleRepository;
import com.sophub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Legt beim Start des Backends die bekannten Betreuer:innen der HS Bochum an,
 * falls sie noch nicht in der Datenbank stehen.
 *
 * - Laeuft bei JEDEM Start, legt aber nur Fehlende an (keine Duplikate).
 * - So hat jedes Teammitglied dieselben Betreuer in seiner lokalen Datenbank.
 * - Die Konten bekommen ein zufaelliges Passwort, das niemand kennt.
 *   Einloggen koennen sich diese Betreuer also erst, wenn sie ihr Konto aktivieren
 *   (das bauen wir in einem spaeteren Schritt).
 */
@Component
public class BetreuerDatenInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BetreuerDatenInitializer.class);
    private static final String ROLLE_PROFESSOR = "PROFESSOR";

    // Vorname, Name, dienstliche E-Mail (laut Webseite der HS Bochum)
    private static final List<String[]> BETREUER = List.of(
            new String[]{"Henrik", "Blunck", "henrik.blunck@hs-bochum.de"},
            new String[]{"Katrin", "Brabender", "katrin.brabender@hs-bochum.de"},
            new String[]{"Edmund", "Coersmeier", "edmund.coersmeier@hs-bochum.de"},
            new String[]{"Carsten", "Köhn", "carsten.koehn@hs-bochum.de"},
            new String[]{"Haydar", "Mecit", "haydar.mecit@hs-bochum.de"},
            new String[]{"Stefan", "Müller-Schneiders", "stefan.mueller-schneiders@hs-bochum.de"},
            new String[]{"Wolf", "Ritschel", "wolf.ritschel@hs-bochum.de"},
            new String[]{"Christian", "Scheffer", "christian.scheffer@hs-bochum.de"},
            new String[]{"Anja", "Tenberge", "anja.tenberge@hs-bochum.de"}
    );

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public BetreuerDatenInitializer(UserRepository userRepository,
                                    RolleRepository rolleRepository,
                                    BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Rolle professorRolle = rolleRepository.findByName(ROLLE_PROFESSOR).orElseGet(() -> {
            Rolle neu = new Rolle();
            neu.setName(ROLLE_PROFESSOR);
            return rolleRepository.save(neu);
        });

        int neuAngelegt = 0;

        for (String[] eintrag : BETREUER) {
            String vorname = eintrag[0];
            String name = eintrag[1];
            String email = eintrag[2];

            // Gibt es die Person schon (z.B. Frau Tenberge, selbst registriert)? Dann nichts tun.
            if (userRepository.findByEmail(email).isPresent()) {
                continue;
            }

            User betreuer = new User();
            // Benutzername = Teil vor dem @, z.B. "henrik.blunck"
            betreuer.setBenutzername(email.substring(0, email.indexOf('@')));
            betreuer.setEmail(email);
            betreuer.setVorname(vorname);
            betreuer.setName(name);
            betreuer.setRolle(professorRolle);
            // Zufaelliges Passwort, das niemand kennt -> Login erst nach Aktivierung moeglich
            betreuer.setPasswort(passwordEncoder.encode(UUID.randomUUID().toString()));
            // Vorbereitetes Konto: wird erst aktiviert, wenn der Betreuer sich selbst registriert
            betreuer.setKontoAktiviert(false);

            userRepository.save(betreuer);
            neuAngelegt++;
        }

        log.info("Betreuer-Daten geprueft: {} neu angelegt, {} insgesamt in der Liste.",
                neuAngelegt, BETREUER.size());
    }
}