package com.sophub.controller;

import com.sophub.service.ProjektChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Interner Projekt-Chat zwischen Team und Betreuung.
 * Zugriff haben nur Ersteller:in, Teammitglieder und die Betreuung (siehe ProjektChatService).
 */
@RestController
@RequestMapping("/sop/api/projekte/{projektId}/chat")
public class ProjektChatController {

    private final ProjektChatService chatService;

    public ProjektChatController(ProjektChatService chatService) {
        this.chatService = chatService;
    }

    /** Verlauf laden; mit ?nach={id} nur neuere Nachrichten (Polling). */
    @GetMapping
    public ResponseEntity<?> liste(@PathVariable Long projektId,
                                   @RequestParam(value = "nach", required = false) Long nach,
                                   Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.liste(projektId, authentication.getName(), nach));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> senden(@PathVariable Long projektId, @RequestBody NeueNachricht anfrage,
                                    Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.senden(projektId, authentication.getName(), anfrage.text()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{nachrichtId}")
    public ResponseEntity<?> loeschen(@PathVariable Long projektId, @PathVariable Long nachrichtId,
                                      Authentication authentication) {
        try {
            chatService.loeschen(projektId, nachrichtId, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    record NeueNachricht(String text) {}
}
