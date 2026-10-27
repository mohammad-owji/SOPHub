package com.sophub.service;

import com.sophub.model.ChatConversation;
import com.sophub.model.User;
import com.sophub.repository.ChatConversationRepository;
import com.sophub.repository.ChatMessageRepository;
import com.sophub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
