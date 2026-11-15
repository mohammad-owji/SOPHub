package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.DokumentRepository;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjektBetreuerZuweisungTest {

    @Mock ProjektRepository projektRepository;
    @Mock ProjektMitgliedRepository projektMitgliedRepository;
    @Mock DokumentRepository dokumentRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @Mock AIService aiService;
    @Mock AnonymizerService anonymizerService;
    @Mock BetreuerService betreuerService;
    @InjectMocks ProjektService service;

    @Test
    void betreuerzuweisungSetztStatusUndSendetAnfrage() {
        User student = user(1L, "STUDENT");
        User betreuer = user(2L, "PROFESSOR");
        Projekt projekt = new Projekt();
        projekt.setId(10L);
        projekt.setStudent(student);
        projekt.setStatus("ENTWURF");
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(betreuer));
        when(projektRepository.save(projekt)).thenReturn(projekt);

        service.aktualisieren(10L, new Projekt(), 2L);

        assertEquals(betreuer, projekt.getBetreuer());
        assertEquals("OFFEN", projekt.getStatus());
        verify(emailService).sendeProjektAnfrageEmailSofort(betreuer, student, projekt);
    }

    @Test
    void betreuerKannNichtUeberschriebenWerden() {
        Projekt projekt = new Projekt();
        projekt.setId(10L);
        projekt.setBetreuer(user(2L, "PROFESSOR"));
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));

        assertThrows(IllegalArgumentException.class,
                () -> service.aktualisieren(10L, new Projekt(), 3L));
    }

    @Test
    void abgelehntesProjektKannEinemAnderenBetreuerZugewiesenWerden() {
        User student = user(1L, "STUDENT");
        User alterBetreuer = user(2L, "PROFESSOR");
        User neuerBetreuer = user(3L, "PROFESSOR");
        Projekt projekt = new Projekt();
        projekt.setId(10L);
        projekt.setStudent(student);
        projekt.setBetreuer(alterBetreuer);
        projekt.setStatus("ABGELEHNT");
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findById(3L)).thenReturn(Optional.of(neuerBetreuer));
        when(projektRepository.save(projekt)).thenReturn(projekt);

        service.aktualisieren(10L, new Projekt(), 3L);

        assertEquals(neuerBetreuer, projekt.getBetreuer());
        assertEquals("OFFEN", projekt.getStatus());
        verify(emailService).sendeProjektAnfrageEmailSofort(neuerBetreuer, student, projekt);
    }

    @Test
    void nachAblehnungMussEinAndererBetreuerAusgewaehltWerden() {
        User alterBetreuer = user(2L, "PROFESSOR");
        Projekt projekt = new Projekt();
        projekt.setId(10L);
        projekt.setBetreuer(alterBetreuer);
        projekt.setStatus("ABGELEHNT");
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(alterBetreuer));

        assertThrows(IllegalArgumentException.class,
                () -> service.aktualisieren(10L, new Projekt(), 2L));
    }

    @Test
    void nurProfessorenKoennenAlsBetreuerZugewiesenWerden() {
        Projekt projekt = new Projekt();
        projekt.setId(10L);
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, "STUDENT")));

        assertThrows(IllegalArgumentException.class,
                () -> service.aktualisieren(10L, new Projekt(), 3L));
    }

    private User user(Long id, String rollenName) {
        Rolle rolle = new Rolle();
        rolle.setName(rollenName);
        User user = new User();
        user.setId(id);
        user.setRolle(rolle);
        return user;
    }
}
