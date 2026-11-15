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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjektPflichtfelderTest {

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
    void allePflichtfelderMussenVorDerErstellungAusgefuelltSein() {
        List<Consumer<Projekt>> fehlendeFelder = List.of(
                projekt -> projekt.setTitel("  "),
                projekt -> projekt.setFachbereich(" "),
                projekt -> projekt.setStudiengang(""),
                projekt -> projekt.setProjektart(null)
        );

        for (Consumer<Projekt> feldLeeren : fehlendeFelder) {
            Projekt projekt = new Projekt();
            projekt.setTitel("Titel");
            projekt.setFachbereich("Fachbereich");
            projekt.setStudiengang("Studiengang");
            projekt.setProjektart("Projektart");
            feldLeeren.accept(projekt);

            assertThrows(IllegalArgumentException.class, () -> service.erstellen(projekt, 1L, null));
        }

        verifyNoInteractions(projektRepository, userRepository, betreuerService, emailService);
    }

    @Test
    void pflichtfelderWerdenVorDemSpeichernGetrimmt() {
        Projekt projekt = new Projekt();
        projekt.setTitel(" Titel ");
        projekt.setFachbereich(" Fachbereich ");
        projekt.setStudiengang(" Studiengang ");
        projekt.setProjektart(" Projektart ");
        User student = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(projektRepository.save(projekt)).thenReturn(projekt);

        service.erstellen(projekt, 1L, null);

        assertEquals("Titel", projekt.getTitel());
        assertEquals("Fachbereich", projekt.getFachbereich());
        assertEquals("Studiengang", projekt.getStudiengang());
        assertEquals("Projektart", projekt.getProjektart());
        verify(projektRepository).save(projekt);
    }
}
