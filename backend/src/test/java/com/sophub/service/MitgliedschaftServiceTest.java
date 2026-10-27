package com.sophub.service;

import com.sophub.model.Projekt;
import com.sophub.model.Rolle;
import com.sophub.model.User;
import com.sophub.repository.ProjektMitgliedRepository;
import com.sophub.repository.ProjektRepository;
import com.sophub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Berechtigung für interne Projektdetails/-inhalte (Grundlage des Tools
 * get_project_details): Admin, Betreuer und Teammitglied dürfen; Fremde nicht.
 */
@ExtendWith(MockitoExtension.class)
class MitgliedschaftServiceTest {

    @Mock ProjektMitgliedRepository projektMitgliedRepository;
    @Mock ProjektRepository projektRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @InjectMocks MitgliedschaftService service;

    private User user(String benutzername, long id, String rolle) {
        User u = new User();
        u.setId(id);
        u.setBenutzername(benutzername);
        Rolle r = new Rolle();
        r.setName(rolle);
        u.setRolle(r);
        return u;
    }

    @Test
    void adminDarfImmer() {
        when(userRepository.findByBenutzername("admin")).thenReturn(Optional.of(user("admin", 1, "ADMIN")));
        assertThat(service.darfInterneDetails(10L, "admin")).isTrue();
    }

    @Test
    void zugewiesenerBetreuerDarf() {
        when(userRepository.findByBenutzername("prof")).thenReturn(Optional.of(user("prof", 5, "PROFESSOR")));
        Projekt p = new Projekt();
        p.setBetreuer(user("prof", 5, "PROFESSOR"));
        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        assertThat(service.darfInterneDetails(10L, "prof")).isTrue();
    }

    @Test
    void teammitgliedDarf() {
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(user("stud", 6, "STUDENT")));
        when(projektRepository.findById(10L)).thenReturn(Optional.of(new Projekt()));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 6L)).thenReturn(true);
        assertThat(service.darfInterneDetails(10L, "stud")).isTrue();
    }

    @Test
    void fremderDarfNicht() {
        when(userRepository.findByBenutzername("fremd")).thenReturn(Optional.of(user("fremd", 7, "STUDENT")));
        Projekt p = new Projekt();
        p.setBetreuer(user("prof", 9, "PROFESSOR"));
        p.setStudent(user("owner", 8, "STUDENT"));
        when(projektRepository.findById(10L)).thenReturn(Optional.of(p));
        when(projektMitgliedRepository.existsByProjektIdAndStudentId(10L, 7L)).thenReturn(false);
        assertThat(service.darfInterneDetails(10L, "fremd")).isFalse();
    }
}
