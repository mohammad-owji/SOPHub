package com.sophub.service;

import com.sophub.model.ChatConversation;
import com.sophub.model.ChatMessage;
import com.sophub.model.User;
import com.sophub.repository.ChatConversationRepository;
import com.sophub.repository.ChatMessageRepository;
import com.sophub.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Verwaltet die gespeicherten Chats des SOP-Assistenten (Anlegen, Auflisten,
 * Umbenennen/Anpinnen, Löschen).
 * <p>
 * Zentrale Sicherheitsregel: Jeder Zugriff auf einen einzelnen Chat läuft über
 * {@link #eigenerChat(String, UUID)} und ist an den eingeloggten Nutzer
 * gebunden - es gibt bewusst KEIN Admin-Sonderrecht. Gehört der Chat einem
 * anderen Nutzer (oder existiert nicht), kommt ein leeres {@link Optional}
 * zurück, das der Controller zu 404 (nicht 403) macht, damit die Existenz
 * fremder Chats nicht verraten wird.
 */
@Service
public class ChatService {

    /** Maximale Anzahl Zeichen im Titel (entspricht der Spaltenlänge). */
    public static final int MAX_TITEL_LAENGE = 120;
    /** Standard-Seitengröße für die Chat-Liste. */
    public static final int STANDARD_SEITENGROESSE = 30;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    private User nutzer(String benutzername) {
        return userRepository.findByBenutzername(benutzername)
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden."));
    }

    /** Chats des eingeloggten Nutzers (angepinnt zuerst, dann nach letzter Aktualisierung). */
    public List<ChatConversation> liste(String benutzername, String q, int page, int size) {
        Long userId = nutzer(benutzername).getId();
        int seite = Math.max(page, 0);
        int groesse = size <= 0 ? STANDARD_SEITENGROESSE : size;
        Pageable pageable = PageRequest.of(seite, groesse);
        if (q != null && !q.isBlank()) {
            return conversationRepository
                    .findByUserIdAndTitelContainingIgnoreCaseOrderByAngepinntDescAktualisiertAmDesc(
                            userId, q.trim(), pageable);
        }
        return conversationRepository.findByUserIdOrderByAngepinntDescAktualisiertAmDesc(userId, pageable);
    }

    /** Legt einen neuen Chat für den eingeloggten Nutzer an. */
    public ChatConversation erstellen(String benutzername, String titel) {
        ChatConversation chat = new ChatConversation();
        chat.setUser(nutzer(benutzername));
        chat.setTitel(kuerzeTitel(titel));
        return conversationRepository.save(chat);
    }

    /**
     * Liefert den Chat nur, wenn er dem eingeloggten Nutzer gehört - sonst leer.
     * Gilt ausdrücklich auch für Admins: Es wird ausschließlich nach der
     * Kombination (Chat-Id, eigene Nutzer-Id) gesucht.
     */
    public Optional<ChatConversation> eigenerChat(String benutzername, UUID id) {
        return conversationRepository.findByIdAndUserId(id, nutzer(benutzername).getId());
    }

    /** Nachrichten eines Chats in chronologischer Reihenfolge. */
    public List<ChatMessage> nachrichten(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByErstelltAmAsc(conversationId);
    }

    /** Titel ändern und/oder anpinnen - nur am eigenen Chat. Leer = 404 im Controller. */
    public Optional<ChatConversation> aktualisieren(String benutzername, UUID id, String titel, Boolean angepinnt) {
        return eigenerChat(benutzername, id).map(chat -> {
            if (titel != null) {
                chat.setTitel(kuerzeTitel(titel));
            }
            if (angepinnt != null) {
                chat.setAngepinnt(angepinnt);
            }
            return conversationRepository.save(chat);
        });
    }

    /** Löscht den eigenen Chat. {@code false} = kein eigener Chat gefunden (-> 404). */
    @Transactional
    public boolean loeschen(String benutzername, UUID id) {
        Optional<ChatConversation> chat = eigenerChat(benutzername, id);
        chat.ifPresent(conversationRepository::delete);
        return chat.isPresent();
    }

    private String kuerzeTitel(String titel) {
        if (titel == null) {
            return null;
        }
        String bereinigt = titel.trim();
        if (bereinigt.isEmpty()) {
            return null;
        }
        return bereinigt.length() > MAX_TITEL_LAENGE ? bereinigt.substring(0, MAX_TITEL_LAENGE) : bereinigt;
    }
}
