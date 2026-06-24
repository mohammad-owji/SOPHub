const API_BASE_URL = "http://localhost:8080/api";

export async function getProjects() {
    const response = await fetch(`${API_BASE_URL}/projects`);

    if (!response.ok) {
        throw new Error("Fehler beim Laden der Projekte");
    }

    return response.json();
}