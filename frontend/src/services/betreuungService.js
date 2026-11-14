import { authHeaders } from "./api";

const API_BASE_URL = "http://localhost:8080/sop/api";

// Antwort lesen: JSON, einfacher Text oder Fehlermeldung des Backends
async function antwortLesen(response) {
    const text = await response.text();
    if (!response.ok) {
        const fehler = new Error(text || "Anfrage fehlgeschlagen");
        fehler.status = response.status;
        throw fehler;
    }
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

// ---------------------------------------------------------------- Ablehnung einer Projektanfrage

// Gruende fuer die Ablehnung einer Projektanfrage.
// Die Schluessel muessen zu ProjektService.ABLEHNUNGS_GRUENDE im Backend passen.
export const ABLEHNUNGS_GRUENDE = [
    { wert: "KEINE_KAPAZITAET", text: "Keine freie Betreuungskapazität in diesem Semester" },
    { wert: "FACHGEBIET", text: "Das Thema passt nicht zu meinem Fachgebiet" },
    { wert: "UMFANG", text: "Der Umfang passt nicht zur Projektart oder Gruppengröße" },
    { wert: "UNKLAR", text: "Die Projektbeschreibung ist noch zu unklar" },
    { wert: "SONSTIGES", text: "Sonstiger Grund" },
];

// Projektanfrage mit Grund ablehnen (Erklaerung ist bei "Sonstiges" Pflicht)
export async function lehneProjektAb(projektId, betreuerId, grund, kommentar) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/ablehnen/${betreuerId}`, {
        method: "PUT",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ grund, kommentar }),
    });
    return antwortLesen(response);
}

// ---------------------------------------------------------------- Co-Betreuung

// Betreuung und Co-Betreuung eines Projekts (403 = keine Berechtigung)
export async function getBetreuung(projektId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/betreuung`, {
        headers: authHeaders(),
    });
    return antwortLesen(response);
}

// Person ueber ihre Hochschul-E-Mail zur Co-Betreuung einladen
export async function ladeCoBetreuungEin(projektId, email, nachricht) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/betreuung/co`, {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ email, nachricht }),
    });
    return antwortLesen(response);
}

// Einladung zurueckziehen bzw. Co-Betreuung beenden
export async function entferneCoBetreuung(projektId, einladungId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/betreuung/co/${einladungId}`, {
        method: "DELETE",
        headers: authHeaders(),
    });
    return antwortLesen(response);
}

// Eigene Einladungen (Co-Betreuung und Uebergabe)
export async function getMeineCoEinladungen() {
    const response = await fetch(`${API_BASE_URL}/betreuung/einladungen`, {
        headers: authHeaders(),
    });
    return antwortLesen(response);
}

// Einladung (Co-Betreuung oder Uebergabe) annehmen (true) oder ablehnen (false)
export async function beantworteCoEinladung(einladungId, annehmen) {
    const aktion = annehmen ? "annehmen" : "ablehnen";
    const response = await fetch(`${API_BASE_URL}/betreuung/einladungen/${einladungId}/${aktion}`, {
        method: "PUT",
        headers: authHeaders(),
    });
    return antwortLesen(response);
}

// ---------------------------------------------------------------- Uebergabe der Betreuung

// Betreuung an eine andere Person uebergeben (die Person muss annehmen)
export async function uebergebeProjekt(projektId, email, grund, alsCoBleiben) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/betreuung/uebergabe`, {
        method: "POST",
        headers: { ...authHeaders(), "Content-Type": "application/json" },
        body: JSON.stringify({ email, grund, alsCoBleiben }),
    });
    return antwortLesen(response);
}

// Offene Uebergabe zurueckziehen
export async function zieheUebergabeZurueck(projektId, einladungId) {
    const response = await fetch(`${API_BASE_URL}/projekte/${projektId}/betreuung/uebergabe/${einladungId}`, {
        method: "DELETE",
        headers: authHeaders(),
    });
    return antwortLesen(response);
}