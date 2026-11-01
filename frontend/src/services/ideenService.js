import { authHeaders, parseOrThrow } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

// KI-Ideenfinder: schickt die Auswahl an das Backend und bekommt { ideen: [...] } zurueck
export async function schlageIdeenVor(angaben) {
    const response = await fetch(`${API_BASE_URL}/ideen/vorschlagen`, {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify(angaben),
    });
    return parseOrThrow(response);
}