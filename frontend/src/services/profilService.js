import { authHeaders, parseOrThrow } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

// Benutzername aendern. Antwort enthaelt einen neuen Login-Token (wie beim Login).
export async function aendereBenutzername(neuerBenutzername, passwort) {
    const response = await fetch(`${API_BASE_URL}/benutzer/profil/benutzername`, {
        method: "PUT",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ neuerBenutzername, passwort }),
    });
    return parseOrThrow(response);
}

// Passwort aendern. Bei Erfolg kommt keine Antwort (204).
export async function aenderePasswort(aktuellesPasswort, neuesPasswort) {
    const response = await fetch(`${API_BASE_URL}/benutzer/profil/passwort`, {
        method: "PUT",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ aktuellesPasswort, neuesPasswort }),
    });
    return parseOrThrow(response);
}