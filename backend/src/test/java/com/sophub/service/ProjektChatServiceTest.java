package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.ProjektChatNachricht;
import com.sophub.model.User;
import com.sophub.repository.BetreuungsEinladungRepository;
import com.sophub.repository.ProjektChatRepository;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Projekt-Chat: Zugriff nur für Team und Betreuung; Team-Nachrichten stehen links,
 * Betreuung rechts.
 */
@ExtendWith(MockitoExtension.class)
class ProjektChatServiceTest {

    @Mock ProjektChatRepository chatRepository;
    @Mock ProjektRepository projektRepository;
    @Mock UserRepository userRepository;
    @Mock ProjektMitgliedRepository projektMitgliedRepository;
    @Mock BetreuungsEinladungRepository betreuungsEinladungRepository;
    @InjectMocks ProjektChatService service;

    private User user(long id, String benutzername) {
        User u = new User();
        u.setId(id);
        u.setBenutzername(benutzername);
        u.setVorname("V" + id);
        u.setName("N" + id);
        return u;
    }

    private Projekt projekt(long id, User ersteller, User betreuer, String status) {
        Projekt p = new Projekt();
        p.setId(id);
        p.setStudent(ersteller);
        p.setBetreuer(betreuer);
        p.setStatus(status);
        return p;
    }

    @Test
    void aussenstehenderHatKeinenZugriff() {
        User ersteller = user(1, "chef");
        Projekt p = projekt(10, ersteller, null, "OFFEN");
        User fremder = user(99, "fremd");

        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        when(userRepository.findByBenutzername("fremd")).thenReturn(Optional.of(fremder));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.liste(10L, "fremd", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teammitgliedSendetUndStehtLinks() {
        User ersteller = user(1, "chef");
        Projekt p = projekt(10, ersteller, null, "OFFEN");
        User mitglied = user(5, "stud");

        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(mitglied));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 5L)).thenReturn(true);
        when(betreuungsEinladungRepository.istCoBetreuer(10L, 5L)).thenReturn(false);

        ProjektChatService.NachrichtAnsicht ansicht = service.senden(10L, "stud", "Hallo Team");

        assertThat(ansicht.text()).isEqualTo("Hallo Team");
        assertThat(ansicht.autor().rolle()).isEqualTo("Teammitglied");
        assertThat(ansicht.autor().seite()).isEqualTo("links");
        assertThat(ansicht.eigene()).isTrue();
    }

    @Test
    void betreuerSendetUndStehtRechts() {
        User ersteller = user(1, "chef");
        User betreuer = user(7, "prof");
        Projekt p = projekt(10, ersteller, betreuer, "ANGENOMMEN");

        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        when(userRepository.findByBenutzername("prof")).thenReturn(Optional.of(betreuer));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 7L)).thenReturn(false);

        ProjektChatService.NachrichtAnsicht ansicht = service.senden(10L, "prof", "Hallo zusammen");

        assertThat(ansicht.autor().rolle()).isEqualTo("Betreuer:in");
        assertThat(ansicht.autor().seite()).isEqualTo("rechts");
    }

    @Test
    void autorLoeschtEigeneNachrichtAlsSoftDelete() {
        User ersteller = user(1, "chef");
        Projekt p = projekt(10, ersteller, null, "OFFEN");
        ProjektChatNachricht n = new ProjektChatNachricht();
        n.setId(1L);
        n.setProjekt(p);
        n.setAutor(ersteller);
        n.setText("ups");

        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        when(userRepository.findByBenutzername("chef")).thenReturn(Optional.of(ersteller));
        when(chatRepository.findById(1L)).thenReturn(Optional.of(n));

        service.loeschen(10L, 1L, "chef");

        assertThat(n.isGeloescht()).isTrue();
        assertThat(n.getText()).isEmpty();
    }

    @Test
    void fremdeNachrichtDarfNichtGeloeschtWerden() {
        User ersteller = user(1, "chef");
        User mitglied = user(5, "stud");
        Projekt p = projekt(10, ersteller, null, "OFFEN");
        ProjektChatNachricht n = new ProjektChatNachricht();
        n.setId(1L);
        n.setProjekt(p);
        n.setAutor(ersteller); // von jemand anderem

        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(mitglied));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 5L)).thenReturn(true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.loeschen(10L, 1L, "stud"))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(n.isGeloescht()).isFalse();
    }

    @Test
    void listeLiefertGesamtenVerlauf() {
        User ersteller = user(1, "chef");
        Projekt p = projekt(10, ersteller, null, "OFFEN");

        ProjektChatNachricht n = new ProjektChatNachricht();
        n.setId(1L);
        n.setProjekt(p);
        n.setAutor(ersteller);
        n.setText("erste Nachricht");

        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        when(userRepository.findByBenutzername("chef")).thenReturn(Optional.of(ersteller));
        when(betreuungsEinladungRepository.istCoBetreuer(10L, 1L)).thenReturn(false);
        when(chatRepository.findByProjektIdOrderByErstelltAmAsc(10L)).thenReturn(List.of(n));

        List<ProjektChatService.NachrichtAnsicht> liste = service.liste(10L, "chef", null);

        assertThat(liste).hasSize(1);
        assertThat(liste.get(0).text()).isEqualTo("erste Nachricht");
        assertThat(liste.get(0).autor().rolle()).isEqualTo("Ersteller:in");
        assertThat(liste.get(0).autor().seite()).isEqualTo("links");
    }
}
