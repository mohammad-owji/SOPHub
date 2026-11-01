package com.sophub.controller;

import com.sophub.model.ChatConversation;
import com.sophub.model.ChatMessage;
import com.sophub.model.ChatRolle;
import com.sophub.model.User;
import com.sophub.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-Ebene (ohne Spring-Context, per standaloneSetup): Der Besitzer erreicht
 * seinen Chat (200), fremde Nutzer und Admins bekommen 404 (NICHT 403). Der
 * eingeloggte Nutzer wird als Principal in die Anfrage gelegt; {@link ChatService}
 * ist gemockt, sodass "fremd/Admin" genau das leere Ergebnis liefern, das der
 * Controller zu 404 machen muss.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    ChatService chatService;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatController(chatService, JsonMapper.builder().build()))
                .build();
    }

    private Authentication als(String benutzername) {
        return new UsernamePasswordAuthenticationToken(benutzername, null, List.of());
    }

    private ChatConversation chatVon(String benutzername, UUID id) {
        User besitzer = new User();
        besitzer.setBenutzername(benutzername);
        ChatConversation chat = new ChatConversation();
        chat.setId(id);
        chat.setUser(besitzer);
        chat.setTitel("Mein Chat");
        return chat;
    }

    @Test
    void besitzerBekommtSeinenChat() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatService.eigenerChat(eq("stud"), eq(id))).thenReturn(Optional.of(chatVon("stud", id)));
        when(chatService.nachrichten(any())).thenReturn(List.of());

        mockMvc.perform(get("/sop/api/ai/chats/{id}", id).principal(als("stud")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titel").value("Mein Chat"));
    }

    @Test
    void fremderBekommt404() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatService.eigenerChat(eq("fremd"), eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/sop/api/ai/chats/{id}", id).principal(als("fremd")))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminBekommt404() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatService.eigenerChat(eq("admin"), eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/sop/api/ai/chats/{id}", id).principal(als("admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminKannFremdenChatNichtLoeschen() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatService.loeschen(eq("admin"), eq(id))).thenReturn(false);

        mockMvc.perform(delete("/sop/api/ai/chats/{id}", id).principal(als("admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void fremderKannFremdenChatNichtUmbenennen() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatService.aktualisieren(eq("fremd"), eq(id), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(patch("/sop/api/ai/chats/{id}", id)
                        .principal(als("fremd"))
                        .contentType("application/json")
                        .content("{\"titel\":\"Hack\"}"))
                .andExpect(status().isNotFound());
    }

    // --- Schritt 3: Nachricht senden ---

    @Test
    void neueNachrichtInNeuemChatGibt200MitAntwort() throws Exception {
        UUID chatId = UUID.randomUUID();
        ChatConversation chat = chatVon("stud", chatId);
        ChatMessage antwort = new ChatMessage();
        antwort.setId(UUID.randomUUID());
        antwort.setRolle(ChatRolle.ASSISTANT);
        antwort.setInhalt("Hallo, womit kann ich helfen?");
        when(chatService.nachrichtSenden(eq("stud"), isNull(), any()))
                .thenReturn(new ChatService.NachrichtErgebnis(chat, antwort));

        mockMvc.perform(post("/sop/api/ai/chats/messages")
                        .principal(als("stud"))
                        .contentType("application/json")
                        .content("{\"inhalt\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatId").value(chatId.toString()))
                .andExpect(jsonPath("$.nachricht.rolle").value("ASSISTANT"))
                .andExpect(jsonPath("$.nachricht.inhalt").value("Hallo, womit kann ich helfen?"));
    }

    @Test
    void nachrichtAnFremdenChatGibt404() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatService.nachrichtSenden(eq("fremd"), eq(id), any()))
                .thenThrow(new ChatService.ChatNichtGefundenException());

        mockMvc.perform(post("/sop/api/ai/chats/{id}/messages", id)
                        .principal(als("fremd"))
                        .contentType("application/json")
                        .content("{\"inhalt\":\"Hi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void zuLangeNachrichtGibt400() throws Exception {
        when(chatService.nachrichtSenden(eq("stud"), isNull(), any()))
                .thenThrow(new IllegalArgumentException("Die Nachricht ist zu lang (maximal 2000 Zeichen)."));

        mockMvc.perform(post("/sop/api/ai/chats/messages")
                        .principal(als("stud"))
                        .contentType("application/json")
                        .content("{\"inhalt\":\"...\"}"))
                .andExpect(status().isBadRequest());
    }
}
