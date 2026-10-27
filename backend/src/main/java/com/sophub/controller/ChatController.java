package com.sophub.controller;

import com.sophub.model.ChatConversation;
import com.sophub.model.ChatMessage;
import com.sophub.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST-Endpunkte für die Chat-Verwaltung des SOP-Assistenten.
 * <p>
 * Jeder Zugriff ist an den eingeloggten Nutzer gebunden (Benutzername aus dem
 * JWT über {@link Authentication#getName()}). Ein Chat, der dem Nutzer nicht
 * gehört, liefert {@code 404} - ohne Admin-Ausnahme und bewusst nicht
 * {@code 403}, damit die Existenz fremder Chats nicht preisgegeben wird.
 */
@RestController
@RequestMapping("/sop/api/ai/chats")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<ChatZusammenfassung>> liste(
            Authentication authentication,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        List<ChatZusammenfassung> liste = chatService.liste(authentication.getName(), q, page, size)
                .stream().map(ChatZusammenfassung::new).toList();
        return ResponseEntity.ok(liste);
    }

    @PostMapping
    public ResponseEntity<ChatDetail> erstellen(Authentication authentication,
                                                @RequestBody(required = false) ChatErstellenRequest body) {
        String titel = body == null ? null : body.titel();
        ChatConversation chat = chatService.erstellen(authentication.getName(), titel);
        return ResponseEntity.status(HttpStatus.CREATED).body(detail(chat));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> holen(Authentication authentication, @PathVariable UUID id) {
        return chatService.eigenerChat(authentication.getName(), id)
                .<ResponseEntity<?>>map(chat -> ResponseEntity.ok(detail(chat)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> aktualisieren(Authentication authentication, @PathVariable UUID id,
                                           @RequestBody ChatPatchRequest body) {
        return chatService.aktualisieren(authentication.getName(), id, body.titel(), body.angepinnt())
                .<ResponseEntity<?>>map(chat -> ResponseEntity.ok(detail(chat)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> loeschen(Authentication authentication, @PathVariable UUID id) {
        boolean geloescht = chatService.loeschen(authentication.getName(), id);
        return geloescht ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Baut die Detail-Ansicht eines Chats inkl. seiner Nachrichten. */
    private ChatDetail detail(ChatConversation chat) {
        List<ChatNachricht> nachrichten = chatService.nachrichten(chat.getId())
                .stream().map(this::nachricht).toList();
        return new ChatDetail(chat.getId(), chat.getTitel(), chat.isAngepinnt(),
                chat.getErstelltAm(), chat.getAktualisiertAm(), nachrichten);
    }

    private ChatNachricht nachricht(ChatMessage m) {
        JsonNode bloecke = null;
        if (m.getBloecke() != null && !m.getBloecke().isBlank()) {
            try {
                bloecke = objectMapper.readTree(m.getBloecke());
            } catch (Exception e) {
                bloecke = null; // Defekte Blöcke werden ignoriert; der Fließtext bleibt erhalten.
            }
        }
        return new ChatNachricht(m.getId(), m.getRolle().name(), m.getInhalt(), bloecke, m.getErstelltAm());
    }

    // --- DTOs ---

    record ChatErstellenRequest(String titel) {}

    record ChatPatchRequest(String titel, Boolean angepinnt) {}

    record ChatZusammenfassung(UUID id, String titel, boolean angepinnt, LocalDateTime aktualisiertAm) {
        ChatZusammenfassung(ChatConversation c) {
            this(c.getId(), c.getTitel(), c.isAngepinnt(), c.getAktualisiertAm());
        }
    }

    record ChatNachricht(UUID id, String rolle, String inhalt, JsonNode bloecke, LocalDateTime erstelltAm) {}

    record ChatDetail(UUID id, String titel, boolean angepinnt, LocalDateTime erstelltAm,
                      LocalDateTime aktualisiertAm, List<ChatNachricht> nachrichten) {}
}
