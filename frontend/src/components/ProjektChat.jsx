import { useEffect, useRef, useState } from "react";
import { getChatNachrichten, sendeChatNachricht, loescheChatNachricht } from "../services/projektChatService";
import "./ProjektChat.css";

const MAX_TEXT = 2000;
const POLL_INTERVALL_MS = 4000;

function formatZeit(wert) {
    if (!wert) return "";
    const datum = new Date(wert);
    if (Number.isNaN(datum.getTime())) return "";
    return datum.toLocaleString("de-DE", { dateStyle: "medium", timeStyle: "short" });
}

const initialen = (person) =>
    `${(person?.vorname || "").charAt(0)}${(person?.name || "").charAt(0)}`.toUpperCase() || "?";

// Dezente (pastellige) Avatar-Farben. Jede Person bekommt stabil dieselbe Farbe.
const AVATAR_FARBEN = [
    { bg: "#e3f2fd", fg: "#1565c0" }, // Blau
    { bg: "#e8f5e9", fg: "#2e7d32" }, // Grün
    { bg: "#fff3e0", fg: "#e65100" }, // Orange
    { bg: "#f3e5f5", fg: "#6a1b9a" }, // Violett
    { bg: "#fce4ec", fg: "#ad1457" }, // Pink
    { bg: "#e0f7fa", fg: "#00838f" }, // Türkis
    { bg: "#fff8e1", fg: "#f9a825" }, // Amber
    { bg: "#efebe9", fg: "#4e342e" }, // Braun
    { bg: "#eceff1", fg: "#37474f" }, // Blaugrau
];

// Farbe deterministisch aus der Person ableiten (id bevorzugt, sonst Name).
function avatarFarbe(person) {
    const schluessel = String(person?.id ?? `${person?.vorname || ""}${person?.name || ""}`);
    let hash = 0;
    for (let i = 0; i < schluessel.length; i++) {
        hash = (hash * 31 + schluessel.charCodeAt(i)) >>> 0;
    }
    return AVATAR_FARBEN[hash % AVATAR_FARBEN.length];
}

/**
 * Interner Projekt-Chat zwischen Team und Betreuung.
 * Team-Nachrichten stehen links, Betreuung rechts (serverseitig über "seite" bestimmt).
 * Sichtbar für Team und Betreuung; für alle anderen antwortet das Backend mit 403.
 */
function ProjektChat({ projektId, onZugriff, onKeinZugriff }) {
    const [nachrichten, setNachrichten] = useState([]);
    const [laedt, setLaedt] = useState(true);
    const [keinZugriff, setKeinZugriff] = useState(false);
    const [fehler, setFehler] = useState("");
    const [eingabe, setEingabe] = useState("");
    const [sendet, setSendet] = useState(false);

    // Rechtsklick-Menü (welche Nachricht, Position) und die zu bestätigende Löschung.
    const [menuId, setMenuId] = useState(null);
    const [menuPos, setMenuPos] = useState({ x: 0, y: 0 });
    const [loeschId, setLoeschId] = useState(null);

    const verlaufEndeRef = useRef(null);
    const containerRef = useRef(null);

    // Erstes Laden + Polling, solange die Komponente sichtbar ist.
    // Es wird jeweils die GESAMTE Liste geladen, damit auch Löschungen (die eine
    // bestehende Nachricht verändern, nicht anhängen) bei allen Teilnehmern ankommen.
    useEffect(() => {
        if (!projektId) return;
        let aktiv = true;
        setNachrichten([]);
        setLaedt(true);
        setFehler("");

        const laden = (istErst) =>
            getChatNachrichten(projektId)
                .then((liste) => {
                    if (!aktiv) return;
                    setKeinZugriff(false);
                    if (onZugriff) onZugriff();
                    setNachrichten(liste || []);
                })
                .catch((error) => {
                    if (!aktiv) return;
                    if (error.status === 403) {
                        setKeinZugriff(true);
                        if (onKeinZugriff) onKeinZugriff();
                    } else if (istErst) {
                        setFehler(error.message || "Chat konnte nicht geladen werden.");
                    }
                })
                .finally(() => {
                    if (aktiv && istErst) setLaedt(false);
                });

        laden(true);
        const timer = setInterval(() => laden(false), POLL_INTERVALL_MS);

        return () => {
            aktiv = false;
            clearInterval(timer);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projektId]);

    // Nur ans Ende scrollen, wenn eine NEUE Nachricht dazukommt (höchste id steigt),
    // nicht bei einer Löschung - sonst springt die Ansicht beim Lesen.
    const letzteId = nachrichten.length ? nachrichten[nachrichten.length - 1].id : 0;
    useEffect(() => {
        verlaufEndeRef.current?.scrollIntoView?.({ behavior: "smooth", block: "end" });
    }, [letzteId]);

    if (keinZugriff) return null;

    const senden = async (event) => {
        event.preventDefault();
        const text = eingabe.trim();
        if (!text || sendet) return;
        setFehler("");
        setSendet(true);
        try {
            const gespeichert = await sendeChatNachricht(projektId, text);
            setEingabe("");
            // Sofort anzeigen; der nächste Poll liefert ohnehin die maßgebliche Liste.
            setNachrichten((prev) =>
                prev.some((n) => n.id === gespeichert.id) ? prev : [...prev, gespeichert]);
        } catch (error) {
            setFehler(error.message || "Nachricht konnte nicht gesendet werden.");
        } finally {
            setSendet(false);
        }
    };

    // Rechtsklick auf eine eigene, noch nicht gelöschte Nachricht öffnet das Menü.
    const kontextMenue = (event, n) => {
        if (!n.eigene || n.geloescht) return;
        event.preventDefault();
        setMenuId(n.id);
        setMenuPos({ x: event.clientX, y: event.clientY });
    };

    const loeschenBestaetigen = async () => {
        const id = loeschId;
        setLoeschId(null);
        if (!id) return;
        setFehler("");
        try {
            await loescheChatNachricht(projektId, id);
            setNachrichten((prev) =>
                prev.map((n) => (n.id === id ? { ...n, geloescht: true, text: "" } : n)));
        } catch (error) {
            setFehler(error.message || "Nachricht konnte nicht gelöscht werden.");
        }
    };

    const beiTaste = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            senden(e);
        }
    };

    return (
        <div className="card">
            <div className="card-body">
                <h4 className="pd-abschnitt mb-1">Projekt-Chat</h4>
                <p className="pc-hinweis">
                    Interner Chat für Team und Betreuung. Neue Teammitglieder sehen den gesamten Verlauf.
                </p>

                {fehler && <div className="alert alert-danger py-2">{fehler}</div>}

                <div className="pc-verlauf" ref={containerRef}>
                    {laedt ? (
                        <p className="text-muted mb-0">Chat wird geladen...</p>
                    ) : nachrichten.length === 0 ? (
                        <p className="text-muted mb-0">Noch keine Nachrichten. Schreib die erste.</p>
                    ) : (
                        nachrichten.map((n) => {
                            const rechts = n.autor?.seite === "rechts";
                            const farbe = avatarFarbe(n.autor);
                            return (
                                <div
                                    key={n.id}
                                    className={`pc-zeile ${rechts ? "rechts" : "links"}`}
                                    onContextMenu={(e) => kontextMenue(e, n)}
                                >
                                    {n.autor?.profilbild ? (
                                        <img
                                            className="pc-avatar pc-avatar-bild"
                                            src={n.autor.profilbild}
                                            alt={`${n.autor?.vorname || ""} ${n.autor?.name || ""}`.trim()}
                                        />
                                    ) : (
                                        <span
                                            className="pc-avatar"
                                            style={{ backgroundColor: farbe.bg, color: farbe.fg }}
                                        >
                                            {initialen(n.autor)}
                                        </span>
                                    )}
                                    <div className="pc-bubble-spalte">
                                        <div className="pc-meta">
                                            <strong>{n.autor?.vorname} {n.autor?.name}</strong>
                                            <span className="text-muted"> · {n.autor?.rolle} · {formatZeit(n.erstelltAm)}</span>
                                            {n.eigene && !n.geloescht && (
                                                <button
                                                    type="button"
                                                    className="pc-loeschen"
                                                    onClick={() => setLoeschId(n.id)}
                                                    title="Nachricht löschen (oder Rechtsklick)"
                                                >
                                                    Löschen
                                                </button>
                                            )}
                                        </div>
                                        {n.geloescht ? (
                                            <div className="pc-bubble pc-bubble-geloescht">
                                                🚫 {n.eigene
                                                    ? "Du hast diese Nachricht gelöscht"
                                                    : "Diese Nachricht wurde gelöscht"}
                                            </div>
                                        ) : (
                                            <div className={`pc-bubble ${rechts ? "betreuung" : "team"}`}>
                                                {n.text}
                                            </div>
                                        )}
                                        {loeschId === n.id && (
                                            <div className="pc-confirm">
                                                <span>Nachricht löschen?</span>
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-danger"
                                                    onClick={loeschenBestaetigen}
                                                >
                                                    Löschen
                                                </button>
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-outline-secondary"
                                                    onClick={() => setLoeschId(null)}
                                                >
                                                    Abbrechen
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })
                    )}
                    <div ref={verlaufEndeRef} />
                </div>

                {/* Rechtsklick-Menü einer eigenen Nachricht */}
                {menuId !== null && (
                    <>
                        <div className="pc-menu-fang" onClick={() => setMenuId(null)} />
                        <div className="pc-menu" style={{ top: menuPos.y, left: menuPos.x }}>
                            <button
                                type="button"
                                className="pc-menu-eintrag text-danger"
                                onClick={() => {
                                    setLoeschId(menuId);
                                    setMenuId(null);
                                }}
                            >
                                Löschen
                            </button>
                        </div>
                    </>
                )}

                <form className="pc-eingabe" onSubmit={senden}>
                    <textarea
                        className="form-control"
                        rows="1"
                        maxLength={MAX_TEXT}
                        value={eingabe}
                        onChange={(e) => setEingabe(e.target.value)}
                        onKeyDown={beiTaste}
                        placeholder="Nachricht schreiben..."
                        aria-label="Nachricht"
                    />
                    <button className="btn btn-primary" type="submit" disabled={sendet || !eingabe.trim()}>
                        {sendet ? "Sendet..." : "Senden"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default ProjektChat;
