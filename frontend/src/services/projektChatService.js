import { authHeaders, parseOrThrow } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

// Fehler mit HTTP-Status, damit die Seite "kein Zugriff" (403) erkennen kann
async function pruefe(response) {
    if (response.status === 403) {
        const fehler = new Error((await response.text()) || "Kein Zugriff.");
        fehler.status = 403;
        throw fehler;
    }
    return parseOrThrow(response);
}

/** Chat-Verlauf laden. Mit nachId werden nur neuere Nachrichten geholt (Polling). */
export async function getChatNachrichten(projektId, nachId) {
    const query = nachId ? `?nach=${encodeURIComponent(nachId)}` : "";
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/chat${query}`, {
        headers: authHeaders(),
    });
    return pruefe(response);
}

/** Neue Nachricht senden; liefert die gespeicherte Nachricht zurück. */
export async function sendeChatNachricht(projektId, text) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/chat`, {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ text }),
    });
    return pruefe(response);
}

/** Eigene Nachricht löschen (Soft-Delete: bleibt als "gelöscht" im Verlauf). */
export async function loescheChatNachricht(projektId, nachrichtId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/chat/${nachrichtId}`, {
        method: "DELETE",
        headers: authHeaders(),
    });
    return pruefe(response);
}
