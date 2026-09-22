import { authHeaders, parseOrThrow } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

export async function generiereProjektZusammenfassung(projektId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/zusammenfassung`, {
        method: "POST",
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}
