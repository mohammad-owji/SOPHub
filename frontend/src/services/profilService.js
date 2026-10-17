import { authHeaders, parseOrThrow } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

// Passwort aendern. Bei Erfolg kommt keine Antwort (204).
export async function aenderePasswort(aktuellesPasswort, neuesPasswort) {
    const response = await fetch(`${API_BASE_URL}/benutzer/profil/passwort`, {
        method: "PUT",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ aktuellesPasswort, neuesPasswort }),
    });
    return parseOrThrow(response);
}