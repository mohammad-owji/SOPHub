package com.sophub.service;

import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.RolleRepository;
import com.sophub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Findet einen Betreuer anhand seiner Hochschul-E-Mail oder legt ihn neu an.
 *
 * Wird benutzt, wenn ein Student beim Erstellen eines Projekts einen Betreuer
 * selbst eintippt, der noch nicht in der Liste steht.
 */
@Service
public class BetreuerService {

    private static final Logger log = LoggerFactory.getLogger(BetreuerService.class);
    private static final String ROLLE_PROFESSOR = "PROFESSOR";
    private static final String HOCHSCHUL_DOMAIN = "@hs-bochum.de";

    private final UserRepository userRepository;
    private final RolleRepository rolleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public BetreuerService(UserRepository userRepository,
                           RolleRepository rolleRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rolleRepository = rolleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findeOderLegeAn(String vorname, String name, String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Bitte die Hochschul-E-Mail des Betreuers angeben.");
        }

        String bereinigteEmail = email.trim().toLowerCase();

        if (!bereinigteEmail.endsWith(HOCHSCHUL_DOMAIN)) {
            throw new RuntimeException("Die E-Mail des Betreuers muss auf " + HOCHSCHUL_DOMAIN + " enden.");
        }

        // Gibt es die Person schon? Dann genau diese verwenden (keine Duplikate).
        Optional<User> vorhanden = userRepository.findByEmail(bereinigteEmail);
        if (vorhanden.isPresent()) {
            User user = vorhanden.get();
            if (user.getRolle() == null || !ROLLE_PROFESSOR.equals(user.getRolle().getName())) {
                throw new RuntimeException("Diese E-Mail gehört keinem Betreuer-Konto.");
            }
            return user;
        }

        if (vorname == null || vorname.isBlank() || name == null || name.isBlank()) {
            throw new RuntimeException("Bitte Vor- und Nachnamen des Betreuers angeben.");
        }

        Rolle professorRolle = rolleRepository.findByName(ROLLE_PROFESSOR).orElseGet(() -> {
            Rolle neu = new Rolle();
            neu.setName(ROLLE_PROFESSOR);
            return rolleRepository.save(neu);
        });

        User betreuer = new User();
        betreuer.setBenutzername(freierBenutzername(bereinigteEmail));
        betreuer.setEmail(bereinigteEmail);
        betreuer.setVorname(vorname.trim());
        betreuer.setName(name.trim());
        betreuer.setRolle(professorRolle);
        // Zufaelliges Passwort, das niemand kennt -> Login erst nach Aktivierung moeglich
        betreuer.setPasswort(passwordEncoder.encode(UUID.randomUUID().toString()));
        // Vorbereitetes Konto: wird erst aktiviert, wenn der Betreuer sich selbst registriert
        betreuer.setKontoAktiviert(false);

        User gespeichert = userRepository.save(betreuer);
        log.info("Neuer Betreuer angelegt: {} {} ({})", gespeichert.getVorname(), gespeichert.getName(), gespeichert.getEmail());
        return gespeichert;
    }

    // Benutzername = Teil vor dem @. Ist er schon vergeben, wird eine Zahl angehaengt.
    private String freierBenutzername(String email) {
        String basis = email.substring(0, email.indexOf('@'));
        String kandidat = basis;
        int zaehler = 2;
        while (userRepository.findByBenutzername(kandidat).isPresent()) {
            kandidat = basis + zaehler;
            zaehler++;
        }
        return kandidat;
    }
}