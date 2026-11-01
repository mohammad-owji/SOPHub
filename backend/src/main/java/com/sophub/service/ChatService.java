package com.sophub.service;

import com.sophub.model.ChatConversation;
import com.sophub.model.ChatMessage;
import com.sophub.model.ChatRolle;
import com.sophub.model.User;
import com.sophub.repository.ChatConversationRepository;
import com.sophub.repository.ChatMessageRepository;
import com.sophub.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    /** Maximale Länge einer Nutzernachricht (Token-/Missbrauchsschutz). */
    public static final int MAX_NACHRICHT_LAENGE = 2000;
    /** Maximale Anzahl gespeicherter Chats pro Nutzer. */
    public static final int MAX_CHATS_PRO_NUTZER = 200;
    /** Maximale Anzahl Nachrichten pro Chat. */
    public static final int MAX_NACHRICHTEN_PRO_CHAT = 100;
    /** Wie viele vergangene Nachrichten als KI-Kontext geladen werden. */
    public static final int KONTEXT_NACHRICHTEN = 10;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final SopAssistentPrompt sopAssistentPrompt;
    private final ChatTitelService chatTitelService;
    private final com.sophub.service.werkzeug.WerkzeugRegistry werkzeugRegistry;
    private final tools.jackson.databind.ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       UserRepository userRepository,
                       AIService aiService,
                       SopAssistentPrompt sopAssistentPrompt,
                       ChatTitelService chatTitelService,
                       com.sophub.service.werkzeug.WerkzeugRegistry werkzeugRegistry,
                       tools.jackson.databind.ObjectMapper objectMapper,
                       PlatformTransactionManager transactionManager) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.sopAssistentPrompt = sopAssistentPrompt;
        this.chatTitelService = chatTitelService;
        this.werkzeugRegistry = werkzeugRegistry;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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

    /**
     * Verarbeitet eine Nutzernachricht: lädt die letzten {@value #KONTEXT_NACHRICHTEN}
     * Nachrichten als KI-Kontext (das Frontend schickt keinen Verlauf mehr mit),
     * ruft die KI auf und speichert anschließend Nutzer- UND KI-Nachricht.
     * <p>
     * {@code chatId == null} legt lazy einen neuen Chat an (keine leeren Chats).
     * Persistiert wird erst NACH erfolgreicher KI-Antwort - schlägt die KI fehl,
     * bleibt kein verwaister Eintrag zurück, und das Frontend kann es erneut
     * versuchen.
     *
     * @throws ChatNichtGefundenException wenn ein {@code chatId} angegeben ist, der
     *                                    nicht dem Nutzer gehört (Controller -> 404)
     * @throws IllegalArgumentException   bei leerer oder zu langer Nachricht (-> 400)
     * @throws ChatLimitException         bei erreichtem Chat-/Nachrichtenlimit (-> 409)
     */
    public NachrichtErgebnis nachrichtSenden(String benutzername, UUID chatId, String inhalt) {
        String text = inhalt == null ? "" : inhalt.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Die Nachricht darf nicht leer sein.");
        }
        if (text.length() > MAX_NACHRICHT_LAENGE) {
            throw new IllegalArgumentException(
                    "Die Nachricht ist zu lang (maximal " + MAX_NACHRICHT_LAENGE + " Zeichen).");
        }

        User user = nutzer(benutzername);

        // Phase 1: Vorhandenen Chat prüfen und Kontext laden - nur kurze Lesezugriffe,
        // KEINE Transaktion, die über den (langsamen) KI-Aufruf offen bliebe.
        List<AIService.Nachricht> verlauf = ladeKontext(user, chatId);
        verlauf.add(new AIService.Nachricht("user", text));

        // Phase 2: KI-Aufruf OHNE offene DB-Transaktion - hält keine Verbindung aus dem
        // Pool, während bis zu mehrere Minuten auf die externe KI gewartet wird.
        // Wirft bei Fehler, bevor Phase 3 etwas speichert (-> kein verwaister Chat).
        com.sophub.service.werkzeug.Werkzeugkasten kasten = werkzeugRegistry.fuer(benutzername);
        String antwortText = aiService.chatMitWerkzeugen(sopAssistentPrompt.systemPrompt(), verlauf, kasten);
        String bloeckeJson = serialisiereBloecke(kasten == null ? List.of() : kasten.gesammelteBloecke());

        // Phase 3: Nutzer- UND KI-Nachricht in einer kurzen Schreib-Transaktion speichern.
        return transactionTemplate.execute(status ->
                speichereNachrichten(user, chatId, text, antwortText, bloeckeJson));
    }

    /**
     * Lädt den KI-Kontext (letzte Nachrichten chronologisch) für einen bestehenden Chat.
     * Prüft dabei Eigentümerschaft und Nachrichtenlimit. Bei {@code chatId == null} ist
     * der Verlauf leer (neuer Chat wird erst in Phase 3 angelegt).
     */
    private List<AIService.Nachricht> ladeKontext(User user, UUID chatId) {
        List<AIService.Nachricht> verlauf = new ArrayList<>();
        if (chatId == null) {
            return verlauf;
        }
        ChatConversation chat = conversationRepository.findByIdAndUserId(chatId, user.getId())
                .orElseThrow(ChatNichtGefundenException::new);
        if (messageRepository.countByConversationId(chat.getId()) >= MAX_NACHRICHTEN_PRO_CHAT) {
            throw new ChatLimitException(
                    "Dieser Chat hat das Limit von " + MAX_NACHRICHTEN_PRO_CHAT
                            + " Nachrichten erreicht. Bitte starte einen neuen Chat.");
        }
        List<ChatMessage> letzte = messageRepository
                .findTop10ByConversationIdOrderByErstelltAmDesc(chat.getId());
        for (int i = letzte.size() - 1; i >= 0; i--) {
            ChatMessage m = letzte.get(i);
            verlauf.add(new AIService.Nachricht(rolleFuerKi(m.getRolle()), m.getInhalt()));
        }
        return verlauf;
    }

    /**
     * Phase 3: speichert Nutzer- und KI-Nachricht in einer kurzen Transaktion und legt
     * bei Bedarf lazy einen neuen Chat an. Läuft erst NACH erfolgreichem KI-Aufruf, damit
     * bei einem KI-Fehler kein verwaister (leerer) Chat zurückbleibt.
     */
    private NachrichtErgebnis speichereNachrichten(User user, UUID chatId, String text,
                                                   String antwortText, String bloeckeJson) {
        ChatConversation chat = (chatId == null)
                ? neuenChatAnlegen(user)
                : conversationRepository.findByIdAndUserId(chatId, user.getId())
                        .orElseThrow(ChatNichtGefundenException::new);

        LocalDateTime jetzt = LocalDateTime.now();
        ChatMessage nutzerNachricht = neueNachricht(chat, ChatRolle.USER, text, jetzt);
        // 1 ms später, damit die zeitliche Reihenfolge auch bei grober Zeitauflösung stabil ist.
        ChatMessage antwort = neueNachricht(chat, ChatRolle.ASSISTANT, antwortText, jetzt.plusNanos(1_000_000));
        antwort.setBloecke(bloeckeJson);
        messageRepository.save(nutzerNachricht);
        antwort = messageRepository.save(antwort);

        chat.setAktualisiertAm(jetzt);
        chat = conversationRepository.save(chat);

        // Nach der ersten KI-Antwort einmalig einen Titel erzeugen - asynchron und
        // erst NACH dem Commit, damit die KI-Antwort nicht verzögert wird und die
        // Nachrichten für die Titelgenerierung bereits persistiert sind.
        if (chat.getTitel() == null || chat.getTitel().isBlank()) {
            titelErzeugenNachCommit(chat.getId());
        }

        return new NachrichtErgebnis(chat, antwort);
    }

    private void titelErzeugenNachCommit(UUID chatId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    chatTitelService.titelGenerieren(chatId);
                }
            });
        } else {
            // Kein aktiver Transaktionskontext (z.B. im Unit-Test) -> direkt aufrufen.
            chatTitelService.titelGenerieren(chatId);
        }
    }

    /** Legt einen neuen Chat an und setzt dabei das Chat-Limit pro Nutzer durch. */
    private ChatConversation neuenChatAnlegen(User user) {
        if (conversationRepository.countByUserId(user.getId()) >= MAX_CHATS_PRO_NUTZER) {
            // Ältesten nicht angepinnten Chat löschen, um Platz zu schaffen.
            ChatConversation aeltester = conversationRepository
                    .findFirstByUserIdAndAngepinntFalseOrderByAktualisiertAmAsc(user.getId())
                    .orElseThrow(() -> new ChatLimitException(
                            "Du hast das Maximum von " + MAX_CHATS_PRO_NUTZER
                                    + " Chats erreicht und alle sind angepinnt. Bitte löse eine Anpinnung oder lösche einen Chat."));
            conversationRepository.delete(aeltester);
        }
        ChatConversation chat = new ChatConversation();
        chat.setUser(user);
        return conversationRepository.save(chat);
    }

    private ChatMessage neueNachricht(ChatConversation chat, ChatRolle rolle, String inhalt, LocalDateTime erstelltAm) {
        ChatMessage nachricht = new ChatMessage();
        nachricht.setConversation(chat);
        nachricht.setRolle(rolle);
        nachricht.setInhalt(inhalt);
        nachricht.setErstelltAm(erstelltAm);
        return nachricht;
    }

    private String rolleFuerKi(ChatRolle rolle) {
        return rolle == ChatRolle.USER ? "user" : "assistant";
    }

    /** Serialisiert die gesammelten Karten-Blöcke als JSON-Array (null, wenn keine). */
    private String serialisiereBloecke(List<tools.jackson.databind.JsonNode> bloecke) {
        if (bloecke == null || bloecke.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(bloecke);
        } catch (Exception e) {
            return null; // defekte Blöcke werden ignoriert; der Fließtext bleibt erhalten
        }
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

    /** Ergebnis einer verarbeiteten Nachricht: der (ggf. neue) Chat und die KI-Antwort. */
    public record NachrichtErgebnis(ChatConversation chat, ChatMessage antwort) {}

    /** Chat gehört nicht dem Nutzer oder existiert nicht - Controller macht daraus 404. */
    public static class ChatNichtGefundenException extends RuntimeException {
        public ChatNichtGefundenException() {
            super("Chat nicht gefunden.");
        }
    }

    /** Ein Chat-/Nachrichtenlimit wurde erreicht - Controller macht daraus 409. */
    public static class ChatLimitException extends RuntimeException {
        public ChatLimitException(String nachricht) {
            super(nachricht);
        }
    }
}
