package com.sophub.service;

import com.sophub.model.ChatConversation;
import com.sophub.model.ChatMessage;
import com.sophub.model.ChatRolle;
import com.sophub.repository.ChatConversationRepository;
import com.sophub.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatTitelServiceTest {

    @Mock ChatConversationRepository conversationRepository;
    @Mock ChatMessageRepository messageRepository;
    @Mock AIService aiService;
    @InjectMocks ChatTitelService chatTitelService;

    private ChatConversation chatOhneTitel(UUID id) {
        ChatConversation chat = new ChatConversation();
        chat.setId(id);
        return chat;
    }

    private ChatMessage userNachricht(String inhalt) {
        ChatMessage m = new ChatMessage();
        m.setRolle(ChatRolle.USER);
        m.setInhalt(inhalt);
        return m;
    }

    @Test
    void setztBereinigtenKiTitel() {
        UUID id = UUID.randomUUID();
        ChatConversation chat = chatOhneTitel(id);
        when(conversationRepository.findById(id)).thenReturn(Optional.of(chat));
        when(messageRepository.findByConversationIdOrderByErstelltAmAsc(id))
                .thenReturn(List.of(userNachricht("Wie plane ich mein SOP Projekt?")));
        when(aiService.generiereAntwort(any())).thenReturn("\"SOP Projekt planen\"");

        chatTitelService.titelGenerieren(id);

        assertThat(chat.getTitel()).isEqualTo("SOP Projekt planen");
        verify(conversationRepository).save(chat);
    }

    @Test
    void kuerztAufFuenfWoerter() {
        UUID id = UUID.randomUUID();
        ChatConversation chat = chatOhneTitel(id);
        when(conversationRepository.findById(id)).thenReturn(Optional.of(chat));
        when(messageRepository.findByConversationIdOrderByErstelltAmAsc(id))
                .thenReturn(List.of(userNachricht("Frage")));
        when(aiService.generiereAntwort(any())).thenReturn("Eins Zwei Drei Vier Fuenf Sechs Sieben");

        chatTitelService.titelGenerieren(id);

        assertThat(chat.getTitel()).isEqualTo("Eins Zwei Drei Vier Fuenf");
    }

    @Test
    void nutztFallbackBeiKiFehler() {
        UUID id = UUID.randomUUID();
        ChatConversation chat = chatOhneTitel(id);
        String frage = "Ich moechte eine Plattform fuer Lerngruppen bauen bitte helfen";
        when(conversationRepository.findById(id)).thenReturn(Optional.of(chat));
        when(messageRepository.findByConversationIdOrderByErstelltAmAsc(id))
                .thenReturn(List.of(userNachricht(frage)));
        when(aiService.generiereAntwort(any())).thenThrow(new RuntimeException("KI weg"));

        chatTitelService.titelGenerieren(id);

        // Fallback = erste 40 Zeichen der Nutzerfrage.
        assertThat(chat.getTitel()).isEqualTo(frage.substring(0, 40).trim());
        verify(conversationRepository).save(chat);
    }

    @Test
    void ueberspringtWennTitelSchonGesetzt() {
        UUID id = UUID.randomUUID();
        ChatConversation chat = chatOhneTitel(id);
        chat.setTitel("Bereits vorhanden");
        when(conversationRepository.findById(id)).thenReturn(Optional.of(chat));

        chatTitelService.titelGenerieren(id);

        verifyNoInteractions(aiService);
        verify(conversationRepository, never()).save(any());
        verify(messageRepository, never()).findByConversationIdOrderByErstelltAmAsc(any());
    }
}
