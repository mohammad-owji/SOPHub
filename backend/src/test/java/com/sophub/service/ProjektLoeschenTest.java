package com.sophub.service;

import com.sophub.model.Projekt;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjektLoeschenTest {

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
    void teamKannProjektOhneBetreuerLoeschen() {
        Projekt projekt = projekt(10L, user("team", 1L), null);
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findByBenutzername("team")).thenReturn(Optional.of(user("team", 1L)));

        service.loeschen(10L, "team");

        verify(projektMitgliedRepository).deleteByProjektId(10L);
        verify(dokumentRepository).trenneVonProjekt(10L);
        verify(projektRepository).delete(projekt);
    }

    @Test
    void teammitgliedKannProjektOhneBetreuerLoeschen() {
        Projekt projekt = projekt(10L, user("owner", 1L), null);
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findByBenutzername("mitglied")).thenReturn(Optional.of(user("mitglied", 2L)));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 2L)).thenReturn(true);

        service.loeschen(10L, "mitglied");

        verify(projektRepository).delete(projekt);
    }

    @Test
    void projektErstellerKannBeiZugewiesenemBetreuerNichtLoeschen() {
        Projekt projekt = projekt(10L, user("team", 1L), user("betreuer", 2L));
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findByBenutzername("team")).thenReturn(Optional.of(user("team", 1L)));

        assertThrows(AccessDeniedException.class, () -> service.loeschen(10L, "team"));

        verifyNoInteractions(dokumentRepository);
    }

    @Test
    void zugewiesenerBetreuerKannProjektLoeschen() {
        Projekt projekt = projekt(10L, user("team", 1L), user("betreuer", 2L));
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findByBenutzername("betreuer")).thenReturn(Optional.of(user("betreuer", 2L)));

        service.loeschen(10L, "betreuer");

        verify(projektRepository).delete(projekt);
    }

    @Test
    void teamKannLoeschanfrageAnZugewiesenenBetreuerSenden() {
        User teammitglied = user("team", 1L);
        User betreuer = user("betreuer", 2L);
        Projekt projekt = projekt(10L, teammitglied, betreuer);
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findByBenutzername("team")).thenReturn(Optional.of(teammitglied));

        service.loeschanfrageSenden(10L, "team");

        verify(emailService).sendeProjektLoeschanfrageEmail(betreuer, teammitglied, projekt);
    }

    @Test
    void fremdeKoennenKeineLoeschanfrageSenden() {
        Projekt projekt = projekt(10L, user("owner", 1L), user("betreuer", 2L));
        when(projektRepository.findById(10L)).thenReturn(Optional.of(projekt));
        when(userRepository.findByBenutzername("fremd")).thenReturn(Optional.of(user("fremd", 3L)));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 3L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.loeschanfrageSenden(10L, "fremd"));

        verifyNoInteractions(emailService);
    }

    private Projekt projekt(Long id, User ersteller, User betreuer) {
        Projekt projekt = new Projekt();
        projekt.setId(id);
        projekt.setStudent(ersteller);
        projekt.setBetreuer(betreuer);
        return projekt;
    }

    private User user(String benutzername, Long id) {
        User user = new User();
        user.setBenutzername(benutzername);
        user.setId(id);
        user.setEmail(benutzername + "@example.org");
        user.setVorname(benutzername);
        user.setName("Test");
        return user;
    }
}
