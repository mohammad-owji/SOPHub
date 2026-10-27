import { authHeaders, parseOrThrow } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

export async function generiereProjektZusammenfassung(projektId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/zusammenfassung`, {
        method: "POST",
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}

// KI-Übersicht (Projektzusammenfassung + kategorisierte Stichwörter).
// Wird beim ersten Aufruf erzeugt und danach gecacht. neu=true erzwingt Neuerzeugung.
export async function getKiUebersicht(projektId, neu = false) {
    const query = neu ? "?neu=true" : "";
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/ki-uebersicht${query}`, {
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}

// --- SOP-Assistent (Chat mit Verlauf) ---

// Wie parseOrThrow, hängt aber den HTTP-Status an den Fehler, damit die
// Chat-Oberfläche 404 (nicht gefunden), 409 (Limit) und 503 (KI) unterscheiden kann.
async function parseChatAntwort(response) {
    const text = await response.text();
    if (!response.ok) {
        const fehler = new Error(text || "Anfrage fehlgeschlagen");
        fehler.status = response.status;
        throw fehler;
    }
    return text ? JSON.parse(text) : null;
}

// Liste der eigenen Chats (id, titel, angepinnt, aktualisiertAm).
export async function getChats(q = "") {
    const query = q && q.trim() ? `?q=${encodeURIComponent(q.trim())}` : "";
    const response = await fetch(`${API_BASE_URL}/ai/chats${query}`, {
        headers: authHeaders(),
    });
    return parseChatAntwort(response);
}

// Ein Chat mit allen Nachrichten.
export async function getChat(chatId) {
    const response = await fetch(`${API_BASE_URL}/ai/chats/${chatId}`, {
        headers: authHeaders(),
    });
    return parseChatAntwort(response);
}

// Erste Nachricht eines neuen Chats -> Chat wird im Backend lazy angelegt.
// Antwort enthält die neue chatId.
export async function sendeNeueNachricht(inhalt) {
    const response = await fetch(`${API_BASE_URL}/ai/chats/messages`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: JSON.stringify({ inhalt }),
    });
    return parseChatAntwort(response);
}

// Nachricht an einen bestehenden Chat.
export async function sendeNachricht(chatId, inhalt) {
    const response = await fetch(`${API_BASE_URL}/ai/chats/${chatId}/messages`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: JSON.stringify({ inhalt }),
    });
    return parseChatAntwort(response);
}
