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
