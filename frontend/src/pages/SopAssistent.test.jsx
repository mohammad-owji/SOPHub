import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import SopAssistent from "./SopAssistent";

// aiService komplett mocken (keine echten HTTP-Aufrufe).
vi.mock("../services/aiService", () => ({
    getChats: vi.fn(),
    getChat: vi.fn(),
    sendeNeueNachricht: vi.fn(),
    sendeNachricht: vi.fn(),
    benenneChatUm: vi.fn(),
    setzeAngepinnt: vi.fn(),
    loescheChat: vi.fn(),
}));

// api nur so weit mocken, wie Seite und Navbar es brauchen.
vi.mock("../services/api", () => ({
    getAuth: () => ({ token: "t", rolle: "STUDENT", vorname: "Test" }),
    getMeineEinladungen: vi.fn(() => Promise.resolve([])),
}));

import {
    getChats,
    getChat,
    sendeNeueNachricht,
    sendeNachricht,
    benenneChatUm,
    loescheChat,
} from "../services/aiService";

function renderSeite(initial = "/sop-assistent") {
    return render(
        <MemoryRouter initialEntries={[initial]}>
            <Routes>
                <Route path="/sop-assistent" element={<SopAssistent />} />
                <Route path="/sop-assistent/:chatId" element={<SopAssistent />} />
                <Route path="/create-project" element={<div>CREATE-SEITE</div>} />
                <Route path="/projectdetails/:id" element={<div>DETAILS-SEITE</div>} />
            </Routes>
        </MemoryRouter>
    );
}

const jetztISO = new Date().toISOString();

beforeEach(() => {
    vi.clearAllMocks();
    try { localStorage.clear(); } catch { /* ignorieren */ }
    getChats.mockResolvedValue([]);
    getChat.mockResolvedValue({ nachrichten: [] });
    benenneChatUm.mockResolvedValue({});
    loescheChat.mockResolvedValue(true);
});

describe("SopAssistent", () => {
    it("zeigt im leeren Chat die Beispiel-Chips und den Datenschutz-Hinweis", async () => {
        renderSeite();
        expect(await screen.findByText("Hilf mir, eine Projektidee zu finden.")).toBeInTheDocument();
        expect(screen.getByText(/privat und nur für dich sichtbar/i)).toBeInTheDocument();
    });

    it("sendet eine neue Nachricht und zeigt Frage und KI-Antwort an", async () => {
        sendeNeueNachricht.mockResolvedValue({
            chatId: "neu-1",
            titel: null,
            nachricht: { id: "m1", rolle: "ASSISTANT", inhalt: "Hallo, womit kann ich helfen?", bloecke: null },
        });
        const user = userEvent.setup();
        renderSeite();

        await user.type(screen.getByRole("textbox", { name: "Nachricht" }), "Was ist ein Pflichtenheft?");
        await user.click(screen.getByRole("button", { name: "Senden" }));

        expect(await screen.findByText("Was ist ein Pflichtenheft?")).toBeInTheDocument();
        expect(await screen.findByText("Hallo, womit kann ich helfen?")).toBeInTheDocument();
        expect(sendeNeueNachricht).toHaveBeenCalledWith("Was ist ein Pflichtenheft?");
    });

    it("zeigt bei 503 eine KI-Fehlermeldung mit 'Erneut versuchen'", async () => {
        const fehler = new Error("Dienst nicht erreichbar");
        fehler.status = 503;
        sendeNeueNachricht.mockRejectedValue(fehler);
        const user = userEvent.setup();
        renderSeite();

        await user.type(screen.getByRole("textbox", { name: "Nachricht" }), "Hallo");
        await user.click(screen.getByRole("button", { name: "Senden" }));

        expect(await screen.findByText(/nicht erreichbar/i)).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Erneut versuchen" })).toBeInTheDocument();
    });

    it("gruppiert Chats nach Angepinnt und Datum", async () => {
        const alt = new Date(Date.now() - 40 * 86400000).toISOString();
        getChats.mockResolvedValue([
            { id: "p", titel: "Gepinnt-Chat", angepinnt: true, aktualisiertAm: jetztISO },
            { id: "h", titel: "Heute-Chat", angepinnt: false, aktualisiertAm: jetztISO },
            { id: "a", titel: "Alt-Chat", angepinnt: false, aktualisiertAm: alt },
        ]);
        renderSeite();

        expect(await screen.findByText("Angepinnt")).toBeInTheDocument();
        expect(screen.getByText("Heute")).toBeInTheDocument();
        expect(screen.getByText("Älter")).toBeInTheDocument();
        expect(screen.getByText("Gepinnt-Chat")).toBeInTheDocument();
        expect(screen.getByText("Alt-Chat")).toBeInTheDocument();
    });

    it("benennt einen Chat inline um (Enter speichert)", async () => {
        getChats.mockResolvedValue([
            { id: "c1", titel: "Mein Chat", angepinnt: false, aktualisiertAm: jetztISO },
        ]);
        const user = userEvent.setup();
        renderSeite();

        await screen.findByText("Mein Chat");
        await user.click(screen.getByRole("button", { name: "Chat-Menü" }));
        await user.click(screen.getByRole("button", { name: "Umbenennen" }));

        const input = screen.getByRole("textbox", { name: "Chat umbenennen" });
        await user.clear(input);
        await user.type(input, "Neuer Name{Enter}");

        expect(benenneChatUm).toHaveBeenCalledWith("c1", "Neuer Name");
    });

    it("löscht einen Chat erst nach Bestätigung im Dialog", async () => {
        getChats.mockResolvedValue([
            { id: "c1", titel: "Mein Chat", angepinnt: false, aktualisiertAm: jetztISO },
        ]);
        const user = userEvent.setup();
        renderSeite();

        await screen.findByText("Mein Chat");
        await user.click(screen.getByRole("button", { name: "Chat-Menü" }));
        await user.click(screen.getByRole("button", { name: "Löschen" }));

        // Bestätigungsdialog erscheint; erst dann wird gelöscht.
        expect(await screen.findByText("Chat löschen")).toBeInTheDocument();
        expect(loescheChat).not.toHaveBeenCalled();

        await user.click(screen.getByRole("button", { name: "Löschen" }));
        expect(loescheChat).toHaveBeenCalledWith("c1");
    });

    it("sucht mit Debounce über das Backend (?q=)", async () => {
        const user = userEvent.setup();
        renderSeite();
        await screen.findByPlaceholderText("Chats durchsuchen");

        await user.type(screen.getByPlaceholderText("Chats durchsuchen"), "roadmap");

        await waitFor(() => expect(getChats).toHaveBeenCalledWith("roadmap", 0, 30));
    });

    it("rendert eine Ideenkarte mit Speichern-Button und Chips", async () => {
        getChat.mockResolvedValue({
            nachrichten: [
                { id: "m1", rolle: "USER", inhalt: "Ich brauche eine Idee" },
                {
                    id: "m2", rolle: "ASSISTANT", inhalt: "Hier ein Vorschlag:",
                    bloecke: [{
                        typ: "idea_card", titel: "Lern-App", problem: "Lernen im Team",
                        mvp: ["Login", "Kurse"], technologien: ["React"], aufwand: "mittel", machbarkeit: "gruen",
                    }],
                },
            ],
        });
        sendeNachricht.mockResolvedValue({
            chatId: "abc", titel: "x",
            nachricht: { id: "m3", rolle: "ASSISTANT", inhalt: "Kleinere Variante", bloecke: null },
        });
        const user = userEvent.setup();
        renderSeite("/sop-assistent/abc");

        expect(await screen.findByText("Lern-App")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Als Projektidee speichern" })).toBeInTheDocument();

        // Chip "Kleiner" sendet eine Folgenachricht in den aktiven Chat.
        await user.click(screen.getByRole("button", { name: "Kleiner" }));
        await waitFor(() => expect(sendeNachricht).toHaveBeenCalled());
        expect(sendeNachricht.mock.calls[0][0]).toBe("abc");
        expect(sendeNachricht.mock.calls[0][1]).toMatch(/kleiner/i);
    });

    it("führt 'Als Projektidee speichern' zum Projektformular", async () => {
        getChat.mockResolvedValue({
            nachrichten: [
                { id: "m2", rolle: "ASSISTANT", inhalt: "Vorschlag", bloecke: [{ typ: "idea_card", titel: "Lern-App" }] },
            ],
        });
        const user = userEvent.setup();
        renderSeite("/sop-assistent/abc");

        await user.click(await screen.findByRole("button", { name: "Als Projektidee speichern" }));
        expect(await screen.findByText("CREATE-SEITE")).toBeInTheDocument();
    });

    it("rendert einen Projektverweis als Link zur Detailseite", async () => {
        getChat.mockResolvedValue({
            nachrichten: [
                {
                    id: "m2", rolle: "ASSISTANT", inhalt: "Ähnliche Projekte:",
                    bloecke: [{ typ: "project_ref", id: 5, titel: "Projekt X", aehnlichkeit: "hoch", begruendung: "Gleiches Thema" }],
                },
            ],
        });
        renderSeite("/sop-assistent/abc");

        const link = await screen.findByRole("link", { name: "Projekt X" });
        expect(link).toHaveAttribute("href", "/projectdetails/5");
        expect(screen.getByText(/Ähnlichkeit: hoch/)).toBeInTheDocument();
    });

    it("zeigt vorhandene Chats und klappt die Seitenleiste ein", async () => {
        getChats.mockResolvedValue([
            { id: "c1", titel: "Mein Chat", angepinnt: false, aktualisiertAm: jetztISO },
        ]);
        const user = userEvent.setup();
        const { container } = renderSeite();

        expect(await screen.findByText("Mein Chat")).toBeInTheDocument();

        const leiste = container.querySelector(".sop-seitenleiste");
        expect(leiste).not.toHaveClass("zu");
        await user.click(screen.getByRole("button", { name: "Seitenleiste einklappen" }));
        expect(leiste).toHaveClass("zu");
        expect(localStorage.getItem("sophub_sop_sidebar_ein")).toBe("zu");
    });
});
