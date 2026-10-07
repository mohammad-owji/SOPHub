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

export async function getMitteilungen(projektId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/mitteilungen`, {
        headers: authHeaders(),
    });
    return pruefe(response);
}

export async function sendeMitteilung(projektId, daten) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/mitteilungen`, {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify(daten),
    });
    return pruefe(response);
}

export async function sendeAntwort(projektId, mitteilungId, text) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/mitteilungen/${mitteilungId}/antworten`, {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ text }),
    });
    return pruefe(response);
}

export async function loescheMitteilung(projektId, mitteilungId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/mitteilungen/${mitteilungId}`, {
        method: "DELETE",
        headers: authHeaders(),
    });
    return pruefe(response);
}