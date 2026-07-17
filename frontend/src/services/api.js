const API_BASE_URL = "http://localhost:8080/sop/api";

export function getAuth() {
    const raw = localStorage.getItem("sophub_auth");
    return raw ? JSON.parse(raw) : null;
}

function authHeaders() {
    const auth = getAuth();
    return auth?.token ? { Authorization: `Bearer ${auth.token}` } : {};
}

async function parseOrThrow(response) {
    const text = await response.text();
    if (!response.ok) {
        throw new Error(text || "Anfrage fehlgeschlagen");
    }
    return text ? JSON.parse(text) : null;
}

export async function getAlleProjekte() {
    const response = await fetch(`${API_BASE_URL}/projekte/alle`, {
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}

export async function getMeineProjekte(studentId) {
    const response = await fetch(`${API_BASE_URL}/projekte/student/${studentId}`, {
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}

export async function createProjekt(studentId, daten, betreuerId) {
    const query = betreuerId ? `?betreuerId=${betreuerId}` : "";
    const response = await fetch(`${API_BASE_URL}/projekte/student/${studentId}${query}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: JSON.stringify(daten),
    });
    return parseOrThrow(response);
}

export async function getProfessoren() {
    const response = await fetch(`${API_BASE_URL}/benutzer/professoren`, {
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}

export async function uploadDokument(formData) {
    const response = await fetch(`${API_BASE_URL}/dokumente/upload`, {
        method: "POST",
        headers: authHeaders(),
        body: formData,
    });
    return parseOrThrow(response);
}

export async function getProjektById(id) {
    const response = await fetch(`${API_BASE_URL}/projekte/${id}`, {
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}

export async function getDokumenteFuerProjekt(projektId) {
    const response = await fetch(`${API_BASE_URL}/dokumente/projekt/${projektId}`, {
        headers: authHeaders(),
    });
    return parseOrThrow(response);
}

export async function downloadDokument(dokumentId, dateiName) {
    const response = await fetch(`${API_BASE_URL}/dokumente/${dokumentId}/download`, {
        headers: authHeaders(),
    });
    if (!response.ok) {
        throw new Error("Download fehlgeschlagen");
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = dateiName || "dokument";
    link.click();
    URL.revokeObjectURL(url);
}
