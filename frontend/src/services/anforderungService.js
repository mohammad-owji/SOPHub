import { authHeaders, parseOrThrow } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

const basis = (projektId) => `${API_BASE_URL}/projekte/${projektId}/anforderungen`;

// Fehler mit HTTP-Status, damit die Seite "kein Zugriff" (403) erkennen kann
async function pruefe(response) {
    if (response.status === 403) {
        const fehler = new Error((await response.text()) || "Kein Zugriff.");
        fehler.status = 403;
        throw fehler;
    }
    return parseOrThrow(response);
}

// Liefert { istTeam, istBetreuer, anforderungen: [...] }
export async function getAnforderungen(projektId) {
    const response = await fetch(basis(projektId), { headers: authHeaders() });
    return pruefe(response);
}

// daten = { typ, titel, beschreibung, frist } (frist als "JJJJ-MM-TT" oder leer)
export async function erstelleAnforderung(projektId, daten) {
    const response = await fetch(basis(projektId), {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify(daten),
    });
    return pruefe(response);
}

export async function reicheEin(projektId, anforderungId, datei, kommentar) {
    const formData = new FormData();
    formData.append("datei", datei);
    if (kommentar && kommentar.trim()) {
        formData.append("kommentar", kommentar.trim());
    }
    const response = await fetch(`${basis(projektId)}/${anforderungId}/einreichen`, {
        method: "POST",
        headers: authHeaders(),
        body: formData,
    });
    return pruefe(response);
}

// ergebnis = "OK" oder "UEBERARBEITEN"
export async function pruefeAbgabe(projektId, anforderungId, ergebnis, kommentar) {
    const response = await fetch(`${basis(projektId)}/${anforderungId}/pruefen`, {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ ergebnis, kommentar }),
    });
    return pruefe(response);
}

export async function loescheAnforderung(projektId, anforderungId) {
    const response = await fetch(`${basis(projektId)}/${anforderungId}`, {
        method: "DELETE",
        headers: authHeaders(),
    });
    return pruefe(response);
}