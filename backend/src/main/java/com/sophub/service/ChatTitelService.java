package com.sophub.service;

import com.sophub.model.ChatConversation;
import com.sophub.model.ChatMessage;
import com.sophub.model.ChatRolle;
import com.sophub.repository.ChatConversationRepository;
import com.sophub.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

/**
 * Erzeugt einmalig einen kurzen Titel für einen Chat - asynchron, damit die
 * eigentliche KI-Antwort nicht verzögert wird. Eigene Bean, weil {@code @Async}
 * über Selbstaufruf innerhalb von {@link ChatService} nicht greifen würde.
 * <p>
 * Der Aufruf erfolgt bewusst erst nach dem Commit der Nachrichten (siehe
 * {@link ChatService}), damit die erste Nutzerfrage hier bereits gelesen werden
 * kann. Schlägt der KI-Aufruf fehl, wird auf die ersten 40 Zeichen der Frage
 * zurückgefallen.
 */
@Service
public class ChatTitelService {

    private static final Logger log = LoggerFactory.getLogger(ChatTitelService.class);
    private static final int FALLBACK_LAENGE = 40;
    private static final int MAX_WOERTER = 5;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final AIService aiService;

    public ChatTitelService(ChatConversationRepository conversationRepository,
                            ChatMessageRepository messageRepository,
                            AIService aiService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiService = aiService;
    }

    @Async
    public void titelGenerieren(UUID chatId) {
        try {
            ChatConversation chat = conversationRepository.findById(chatId).orElse(null);
            if (chat == null || (chat.getTitel() != null && !chat.getTitel().isBlank())) {
                return; // nicht gefunden oder schon betitelt -> nichts tun (einmalig)
            }

            String ersteFrage = messageRepository.findByConversationIdOrderByErstelltAmAsc(chatId).stream()
                    .filter(m -> m.getRolle() == ChatRolle.USER)
                    .map(ChatMessage::getInhalt)
                    .filter(inhalt -> inhalt != null && !inhalt.isBlank())
                    .findFirst()
                    .orElse(null);
            if (ersteFrage == null) {
                return;
            }

            String titel;
            try {
                titel = bereinigeTitel(aiService.generiereAntwort(titelPrompt(ersteFrage)));
                if (titel.isBlank()) {
                    titel = fallback(ersteFrage);
                }
            } catch (Exception e) {
                log.warn("Titelgenerierung per KI fehlgeschlagen, nutze Fallback: {}", e.getMessage());
                titel = fallback(ersteFrage);
            }

            chat.setTitel(titel);
            conversationRepository.save(chat);
        } catch (Exception e) {
            log.warn("Titel für Chat {} konnte nicht gesetzt werden: {}", chatId, e.getMessage());
        }
    }

    private String titelPrompt(String frage) {
        return """
                Formuliere einen sehr kurzen, treffenden Titel für einen Chat, der mit der folgenden Frage beginnt.
                Regeln: maximal 5 Wörter, auf Deutsch, keine Anführungszeichen, kein Satzzeichen am Ende.
                Gib ausschließlich den Titel zurück, sonst nichts.

                Frage:
                %s
                """.formatted(frage);
    }

    /** Entfernt Anführungszeichen/Codefences, kürzt auf 5 Wörter und entfernt Satzzeichen am Ende. */
    private String bereinigeTitel(String roh) {
        if (roh == null) {
            return "";
        }
        String t = roh.trim();
        int umbruch = t.indexOf('\n');
        if (umbruch >= 0) {
            t = t.substring(0, umbruch).trim();
        }
        t = t.replaceAll("^[\"'`*_#\\s]+", "").replaceAll("[\"'`\\s]+$", "").trim();

        String[] worte = t.split("\\s+");
        if (worte.length > MAX_WOERTER) {
            t = String.join(" ", Arrays.copyOfRange(worte, 0, MAX_WOERTER));
        }
        t = t.replaceAll("[.!?…,:;]+$", "").trim();

        if (t.length() > ChatService.MAX_TITEL_LAENGE) {
            t = t.substring(0, ChatService.MAX_TITEL_LAENGE);
        }
        return t;
    }

    private String fallback(String frage) {
        String f = frage.trim();
        return f.length() > FALLBACK_LAENGE ? f.substring(0, FALLBACK_LAENGE).trim() : f;
    }
}
