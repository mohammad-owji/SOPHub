package com.sophub.service;

import com.sophub.model.ChatConversation;
import com.sophub.model.ChatMessage;
import com.sophub.model.ChatRolle;
import com.sophub.model.User;
import com.sophub.repository.ChatConversationRepository;
import com.sophub.repository.ChatMessageRepository;
import com.sophub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Prüft die zentrale Besitzerregel von {@link ChatService}: Ein Chat ist nur für
 * seinen Besitzer erreichbar. Fremde Nutzer UND Admins bekommen nichts, weil die
 * Suche immer an die eigene Nutzer-Id gebunden ist (kein Admin-Bypass).
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatConversationRepository conversationRepository;
    @Mock ChatMessageRepository messageRepository;
    @Mock UserRepository userRepository;
    @Mock AIService aiService;
    @Mock SopAssistentPrompt sopAssistentPrompt;
    @Mock ChatTitelService chatTitelService;
    @Mock com.sophub.service.werkzeug.WerkzeugRegistry werkzeugRegistry;
    @InjectMocks ChatService chatService;

    private User nutzer(String benutzername, long id) {
        User u = new User();
        u.setId(id);
        u.setBenutzername(benutzername);
        return u;
    }

    @Test
    void besitzerErhaeltEigenenChat() {
        User besitzer = nutzer("stud", 1L);
        UUID chatId = UUID.randomUUID();
        ChatConversation chat = new ChatConversation();
        chat.setId(chatId);
        chat.setUser(besitzer);
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(besitzer));
        when(conversationRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(chat));

        Optional<ChatConversation> ergebnis = chatService.eigenerChat("stud", chatId);

        assertThat(ergebnis).containsSame(chat);
        verify(conversationRepository).findByIdAndUserId(chatId, 1L);
    }

    @Test
    void fremderErhaeltKeinenChat() {
        User fremder = nutzer("fremd", 2L);
        UUID chatId = UUID.randomUUID();
        when(userRepository.findByBenutzername("fremd")).thenReturn(Optional.of(fremder));
        when(conversationRepository.findByIdAndUserId(chatId, 2L)).thenReturn(Optional.empty());

        assertThat(chatService.eigenerChat("fremd", chatId)).isEmpty();
        // Zugriff war an die eigene (fremde) Nutzer-Id gebunden -> kein Treffer.
        verify(conversationRepository).findByIdAndUserId(chatId, 2L);
    }

    @Test
    void adminErhaeltKeinenFremdenChat() {
        // Rolle wäre ADMIN - bewusst irrelevant: es zählt nur die eigene Nutzer-Id.
        User admin = nutzer("admin", 3L);
        UUID chatId = UUID.randomUUID();
        when(userRepository.findByBenutzername("admin")).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdAndUserId(chatId, 3L)).thenReturn(Optional.empty());

        assertThat(chatService.eigenerChat("admin", chatId)).isEmpty();
        verify(conversationRepository).findByIdAndUserId(chatId, 3L);
        // Kein Admin-Bypass über eine ungescopte Suche.
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    void loeschenAmEigenenChatLoeschtUndMeldetErfolg() {
        User besitzer = nutzer("stud", 1L);
        UUID chatId = UUID.randomUUID();
        ChatConversation chat = new ChatConversation();
        chat.setId(chatId);
        chat.setUser(besitzer);
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(besitzer));
        when(conversationRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(chat));

        assertThat(chatService.loeschen("stud", chatId)).isTrue();
        verify(conversationRepository).delete(chat);
    }

    @Test
    void loeschenEinesFremdenChatsTutNichts() {
        User fremder = nutzer("fremd", 2L);
        UUID chatId = UUID.randomUUID();
        when(userRepository.findByBenutzername("fremd")).thenReturn(Optional.of(fremder));
        when(conversationRepository.findByIdAndUserId(chatId, 2L)).thenReturn(Optional.empty());

        assertThat(chatService.loeschen("fremd", chatId)).isFalse();
        verify(conversationRepository, never()).delete(any());
    }

    @Test
    void aktualisierenEinesFremdenChatsAendertNichts() {
        User admin = nutzer("admin", 3L);
        UUID chatId = UUID.randomUUID();
        when(userRepository.findByBenutzername("admin")).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdAndUserId(chatId, 3L)).thenReturn(Optional.empty());

        assertThat(chatService.aktualisieren("admin", chatId, "Neuer Titel", true)).isEmpty();
        verify(conversationRepository, never()).save(any());
    }

    // --- Schritt 3: Nachricht senden ---

    @Test
    void neueNachrichtLegtChatAnUndSpeichertNutzerUndKiNachricht() {
        User user = nutzer("stud", 1L);
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(user));
        when(conversationRepository.countByUserId(1L)).thenReturn(0L);
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.countByConversationId(any())).thenReturn(0L);
        when(messageRepository.findTop10ByConversationIdOrderByErstelltAmDesc(any())).thenReturn(List.of());
        when(sopAssistentPrompt.systemPrompt()).thenReturn("SYS");
        when(aiService.chatMitWerkzeugen(eq("SYS"), any(), any())).thenReturn("KI-Antwort");
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatService.NachrichtErgebnis ergebnis = chatService.nachrichtSenden("stud", null, "Hallo KI");

        assertThat(ergebnis.antwort().getInhalt()).isEqualTo("KI-Antwort");
        assertThat(ergebnis.antwort().getRolle()).isEqualTo(ChatRolle.ASSISTANT);
        // Nutzer- UND KI-Nachricht wurden gespeichert.
        verify(messageRepository, times(2)).save(any());

        // Die aktuelle Frage steht als letzte user-Nachricht im KI-Verlauf.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AIService.Nachricht>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService).chatMitWerkzeugen(eq("SYS"), captor.capture(), any());
        List<AIService.Nachricht> verlauf = captor.getValue();
        assertThat(verlauf).isNotEmpty();
        assertThat(verlauf.get(verlauf.size() - 1).rolle()).isEqualTo("user");
        assertThat(verlauf.get(verlauf.size() - 1).inhalt()).isEqualTo("Hallo KI");

        // Nach der ersten Antwort wird (einmalig) die Titelgenerierung angestoßen.
        verify(chatTitelService).titelGenerieren(any());
    }

    @Test
    void bestehenderChatMitTitelLoestKeineTitelgenerierungAus() {
        User user = nutzer("stud", 1L);
        UUID chatId = UUID.randomUUID();
        ChatConversation chat = new ChatConversation();
        chat.setId(chatId);
        chat.setUser(user);
        chat.setTitel("Schon betitelt");
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(chat));
        when(messageRepository.countByConversationId(chatId)).thenReturn(4L);
        when(messageRepository.findTop10ByConversationIdOrderByErstelltAmDesc(chatId)).thenReturn(List.of());
        when(sopAssistentPrompt.systemPrompt()).thenReturn("SYS");
        when(aiService.chatMitWerkzeugen(any(), any(), any())).thenReturn("Antwort");
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chatService.nachrichtSenden("stud", chatId, "Noch eine Frage");

        verifyNoInteractions(chatTitelService);
    }

    @Test
    void leereNachrichtWirdAbgelehntOhneKiAufruf() {
        assertThatThrownBy(() -> chatService.nachrichtSenden("stud", null, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(aiService);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void zuLangeNachrichtWirdAbgelehntOhneKiAufruf() {
        String zuLang = "x".repeat(ChatService.MAX_NACHRICHT_LAENGE + 1);
        assertThatThrownBy(() -> chatService.nachrichtSenden("stud", null, zuLang))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(aiService);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void nachrichtAnFremdenChatWirftNichtGefunden() {
        User fremder = nutzer("fremd", 2L);
        UUID chatId = UUID.randomUUID();
        when(userRepository.findByBenutzername("fremd")).thenReturn(Optional.of(fremder));
        when(conversationRepository.findByIdAndUserId(chatId, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.nachrichtSenden("fremd", chatId, "Hallo"))
                .isInstanceOf(ChatService.ChatNichtGefundenException.class);
        verifyNoInteractions(aiService);
    }

    @Test
    void nachrichtenLimitProChatBlocktKiAufruf() {
        User user = nutzer("stud", 1L);
        UUID chatId = UUID.randomUUID();
        ChatConversation chat = new ChatConversation();
        chat.setId(chatId);
        chat.setUser(user);
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(chat));
        when(messageRepository.countByConversationId(chatId))
                .thenReturn((long) ChatService.MAX_NACHRICHTEN_PRO_CHAT);

        assertThatThrownBy(() -> chatService.nachrichtSenden("stud", chatId, "Noch eine Frage"))
                .isInstanceOf(ChatService.ChatLimitException.class);
        verifyNoInteractions(aiService);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void beiErreichtemChatLimitWirdAeltesterUngepinnterGeloescht() {
        User user = nutzer("stud", 1L);
        ChatConversation aeltester = new ChatConversation();
        aeltester.setId(UUID.randomUUID());
        aeltester.setUser(user);
        when(userRepository.findByBenutzername("stud")).thenReturn(Optional.of(user));
        when(conversationRepository.countByUserId(1L))
                .thenReturn((long) ChatService.MAX_CHATS_PRO_NUTZER);
        when(conversationRepository.findFirstByUserIdAndAngepinntFalseOrderByAktualisiertAmAsc(1L))
                .thenReturn(Optional.of(aeltester));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.countByConversationId(any())).thenReturn(0L);
        when(messageRepository.findTop10ByConversationIdOrderByErstelltAmDesc(any())).thenReturn(List.of());
        when(sopAssistentPrompt.systemPrompt()).thenReturn("SYS");
        when(aiService.chatMitWerkzeugen(any(), any(), any())).thenReturn("Antwort");
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chatService.nachrichtSenden("stud", null, "Erste Frage im neuen Chat");

        verify(conversationRepository).delete(aeltester);
    }
}
