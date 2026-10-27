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
}));

// api nur so weit mocken, wie Seite und Navbar es brauchen.
vi.mock("../services/api", () => ({
    getAuth: () => ({ token: "t", rolle: "STUDENT", vorname: "Test" }),
    getMeineEinladungen: vi.fn(() => Promise.resolve([])),
}));

import { getChats, getChat, sendeNeueNachricht } from "../services/aiService";

function renderSeite(initial = "/sop-assistent") {
    return render(
        <MemoryRouter initialEntries={[initial]}>
            <Routes>
                <Route path="/sop-assistent" element={<SopAssistent />} />
                <Route path="/sop-assistent/:chatId" element={<SopAssistent />} />
            </Routes>
        </MemoryRouter>
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    try { localStorage.clear(); } catch { /* ignorieren */ }
    getChats.mockResolvedValue([]);
    getChat.mockResolvedValue({ nachrichten: [] });
});

describe("SopAssistent", () => {
    it("zeigt im leeren Chat die Beispiel-Chips und den Datenschutz-Hinweis", async () => {
        renderSeite();
        expect(await screen.findByText("Hilf mir, eine Projektidee zu finden.")).toBeInTheDocument();
        expect(
            screen.getByText("Was ist der Unterschied zwischen Lastenheft und Pflichtenheft?")
        ).toBeInTheDocument();
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

        await user.type(screen.getByRole("textbox"), "Was ist ein Pflichtenheft?");
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

        await user.type(screen.getByRole("textbox"), "Hallo");
        await user.click(screen.getByRole("button", { name: "Senden" }));

        expect(await screen.findByText(/nicht erreichbar/i)).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Erneut versuchen" })).toBeInTheDocument();
    });

    it("zeigt vorhandene Chats in der Seitenleiste und klappt sie ein", async () => {
        getChats.mockResolvedValue([
            { id: "c1", titel: "Mein Chat", angepinnt: false, aktualisiertAm: "2026-10-27T10:00:00" },
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
