const API_BASE_URL = "http://localhost:8080/sop/api";


// Alle Projekte laden
export async function getProjects() {
    const response = await fetch(`${API_BASE_URL}/projekte`);

    if (!response.ok) {
        throw new Error("Fehler beim Laden der Projekte");
    }

    return response.json();
}


// Einzelnes Projekt laden
export async function getProjectById(projectId) {
    const response = await fetch(
        `${API_BASE_URL}/projekte/${projectId}`
    );

    if (!response.ok) {
        throw new Error("Projekt konnte nicht geladen werden");
    }

    return response.json();
}


// Projekt annehmen
export async function acceptProject(projectId, betreuerId) {
    const response = await fetch(
        `${API_BASE_URL}/projekte/${projectId}/annehmen/${betreuerId}`,
        {
            method: "PUT",
        }
    );

    if (!response.ok) {
        throw new Error("Projekt konnte nicht angenommen werden");
    }

    return response.json();
}


// Projekt ablehnen
export async function declineProject(projectId, betreuerId) {
    const response = await fetch(
        `${API_BASE_URL}/projekte/${projectId}/ablehnen/${betreuerId}`,
        {
            method: "PUT",
        }
    );

    if (!response.ok) {
        throw new Error("Projekt konnte nicht abgelehnt werden");
    }

    return response.json();
}