import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import Navbar from "../components/Navbar";
import { getAuth } from "../services/api";
import { getChats, getChat, sendeNeueNachricht, sendeNachricht } from "../services/aiService";
import "./SopAssistent.css";

const MAX_ZEICHEN = 2000;
const SIDEBAR_KEY = "sophub_sop_sidebar_ein";

// Beispiel-Chips im leeren Chat (klick = direkt senden).
const BEISPIELE = [
    "Was ist der Unterschied zwischen Lastenheft und Pflichtenheft?",
    "Erstelle mir eine Roadmap für mein SOP über zwei Semester.",
    "Wie läuft ein Softwareprojekt typischerweise ab?",
    "Ich habe eine Idee für eine App – wie grenze ich sie ab?",
    "Hilf mir, eine Projektidee zu finden.",
    "Welche Technologien eignen sich für ein Web-Projekt im Team?",
];

function fehlerText(err) {
    if (!err) return "Es ist ein Fehler aufgetreten.";
    if (err.status === 503) return "Die KI ist gerade nicht erreichbar. Bitte versuche es erneut.";
    if (err.status === 409) return err.message || "Limit erreicht. Bitte starte einen neuen Chat.";
    if (err.status === 404) return "Chat nicht gefunden.";
    return err.message || "Es ist ein Fehler aufgetreten.";
}

function sidebarStartwert() {
    try {
        return localStorage.getItem(SIDEBAR_KEY) !== "zu";
    } catch {
        return true;
    }
}

function SopAssistent() {
    const { chatId } = useParams();
    const navigate = useNavigate();

    const [chats, setChats] = useState([]);
    const [nachrichten, setNachrichten] = useState([]);
    const [eingabe, setEingabe] = useState("");
    const [senden, setSenden] = useState(false);
    const [ladeChat, setLadeChat] = useState(false);
    const [fehler, setFehler] = useState(null);
    const [sidebarEin, setSidebarEin] = useState(sidebarStartwert);

    const aktivRef = useRef(chatId || null);        // welcher Chat gerade sichtbar ist
    const schonGeladenRef = useRef(null);           // Chat, dessen Nachrichten bereits im State sind
    const letzteEingabeRef = useRef("");            // für "Erneut versuchen"
    const verlaufEndeRef = useRef(null);

    const ladeChats = () => {
        getChats()
            .then((liste) => setChats(Array.isArray(liste) ? liste : []))
            .catch(() => { /* Liste ist nebensächlich; Fehler still ignorieren */ });
    };

    useEffect(() => {
        if (getAuth()?.token) ladeChats();
    }, []);

    // Aktiven Chat aus der URL laden.
    useEffect(() => {
        aktivRef.current = chatId || null;
        if (!chatId) {
            setNachrichten([]);
            setFehler(null);
            return;
        }
        if (schonGeladenRef.current === chatId) {
            // Gerade selbst erstellt -> State ist aktuell, kein Nachladen (kein Flackern).
            schonGeladenRef.current = null;
            return;
        }
        let aktiv = true;
        setLadeChat(true);
        setFehler(null);
        getChat(chatId)
            .then((daten) => {
                if (aktiv) setNachrichten(daten?.nachrichten || []);
            })
            .catch((err) => {
                if (!aktiv) return;
                setFehler({ status: err.status, text: fehlerText(err) });
                if (err.status === 404) {
                    // Ungültige/fremde ID -> zurück auf neuen Chat.
                    navigate("/sop-assistent", { replace: true });
                }
            })
            .finally(() => {
                if (aktiv) setLadeChat(false);
            });
        return () => {
            aktiv = false;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [chatId]);

    // Immer ans Ende scrollen.
    useEffect(() => {
        // scrollIntoView fehlt in Testumgebungen (jsdom) -> optional aufrufen.
        verlaufEndeRef.current?.scrollIntoView?.({ behavior: "smooth" });
    }, [nachrichten, senden]);

    const sidebarUmschalten = () => {
        setSidebarEin((ein) => {
            const neu = !ein;
            try {
                localStorage.setItem(SIDEBAR_KEY, neu ? "auf" : "zu");
            } catch { /* localStorage kann blockiert sein */ }
            return neu;
        });
    };

    const neuerChat = () => {
        setFehler(null);
        setEingabe("");
        navigate("/sop-assistent");
    };

    const absenden = async (text) => {
        const inhalt = (text ?? eingabe).trim();
        if (!inhalt || senden) return;
        if (inhalt.length > MAX_ZEICHEN) return;

        const zielId = aktivRef.current; // null = neuer Chat
        const tempId = `tmp-${Date.now()}`;
        letzteEingabeRef.current = inhalt;

        // Optimistic UI: Nutzernachricht sofort anzeigen.
        setFehler(null);
        setEingabe("");
        setNachrichten((prev) => [...prev, { id: tempId, rolle: "USER", inhalt }]);
        setSenden(true);

        try {
            const antwort = zielId
                ? await sendeNachricht(zielId, inhalt)
                : await sendeNeueNachricht(inhalt);

            const antwortNachricht = {
                id: antwort.nachricht.id,
                rolle: "ASSISTANT",
                inhalt: antwort.nachricht.inhalt,
                bloecke: antwort.nachricht.bloecke,
            };

            // Nur in die Ansicht übernehmen, wenn der Nutzer noch denselben Chat sieht.
            // Sonst ist die Antwort trotzdem serverseitig gespeichert (kein Verlust, keine Vermischung).
            const nochAktiv = zielId ? aktivRef.current === zielId : aktivRef.current === null;
            if (nochAktiv) {
                setNachrichten((prev) => [...prev, antwortNachricht]);
                if (!zielId) {
                    // Neuer Chat: URL per Router wechseln, ohne die Seite neu zu laden und ohne Nachladen.
                    schonGeladenRef.current = antwort.chatId;
                    aktivRef.current = antwort.chatId;
                    navigate(`/sop-assistent/${antwort.chatId}`);
                }
            }
            ladeChats();
        } catch (err) {
            const nochAktiv = zielId ? aktivRef.current === zielId : aktivRef.current === null;
            if (nochAktiv) {
                setNachrichten((prev) =>
                    prev.map((m) => (m.id === tempId ? { ...m, fehler: true } : m)));
                setFehler({ status: err.status, text: fehlerText(err) });
                if (err.status === 404) {
                    navigate("/sop-assistent", { replace: true });
                }
            }
        } finally {
            setSenden(false);
        }
    };

    const erneutVersuchen = () => {
        setNachrichten((prev) => {
            const kopie = [...prev];
            if (kopie.length && kopie[kopie.length - 1].fehler) kopie.pop();
            return kopie;
        });
        absenden(letzteEingabeRef.current);
    };

    const beiTaste = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            absenden();
        }
    };

    const leer = nachrichten.length === 0 && !ladeChat;
    const zuLang = eingabe.length > MAX_ZEICHEN;

    return (
        <div className="sop-assistent-seite">
            <Navbar />
            <div className="sop-assistent-layout">
                <aside className={`sop-seitenleiste${sidebarEin ? "" : " zu"}`} aria-label="Chat-Verlauf">
                    <div className="sop-seitenleiste-kopf">
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary sop-icon-btn"
                            onClick={sidebarUmschalten}
                            title={sidebarEin ? "Einklappen" : "Ausklappen"}
                            aria-label={sidebarEin ? "Seitenleiste einklappen" : "Seitenleiste ausklappen"}
                        >
                            {sidebarEin ? "«" : "»"}
                        </button>
                        {sidebarEin ? (
                            <button type="button" className="btn btn-sm btn-primary flex-grow-1" onClick={neuerChat}>
                                + Neuer Chat
                            </button>
                        ) : (
                            <button
                                type="button"
                                className="btn btn-sm btn-primary sop-icon-btn"
                                onClick={neuerChat}
                                title="Neuer Chat"
                                aria-label="Neuer Chat"
                            >
                                +
                            </button>
                        )}
                    </div>

                    {sidebarEin && (
                        <>
                            <ul className="sop-chatliste">
                                {chats.length === 0 ? (
                                    <li className="sop-chatliste-leer text-muted">
                                        Noch keine Chats. Stell deine erste Frage.
                                    </li>
                                ) : (
                                    chats.map((c) => (
                                        <li key={c.id}>
                                            <button
                                                type="button"
                                                className={`sop-chat-eintrag${c.id === chatId ? " aktiv" : ""}`}
                                                onClick={() => navigate(`/sop-assistent/${c.id}`)}
                                                title={c.titel || "Neuer Chat"}
                                            >
                                                {c.angepinnt && <span className="sop-pin" aria-hidden="true">📌 </span>}
                                                {c.titel || "Neuer Chat"}
                                            </button>
                                        </li>
                                    ))
                                )}
                            </ul>
                            <p className="sop-datenschutz text-muted">
                                Deine Chats sind privat und nur für dich sichtbar.
                            </p>
                        </>
                    )}
                </aside>

                <main className="sop-chat">
                    <div className="sop-verlauf">
                        {ladeChat && <p className="text-muted">Chat wird geladen...</p>}

                        {leer && (
                            <div className="sop-leerzustand">
                                <h1 className="h4 mb-2">SOP-Assistent</h1>
                                <p className="text-muted mb-4">
                                    Stell deine erste Frage – z.&nbsp;B. zu Ablauf, Lasten-/Pflichtenheft,
                                    Roadmap oder Projektideen.
                                </p>
                                <div className="sop-chips">
                                    {BEISPIELE.map((text) => (
                                        <button
                                            key={text}
                                            type="button"
                                            className="btn btn-outline-primary btn-sm sop-chip"
                                            onClick={() => absenden(text)}
                                            disabled={senden}
                                        >
                                            {text}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        {nachrichten.map((m) => (
                            <div
                                key={m.id}
                                className={`sop-bubble-zeile ${m.rolle === "USER" ? "user" : "assistant"}`}
                            >
                                <div
                                    className={`sop-bubble ${m.rolle === "USER" ? "user" : "assistant"}${m.fehler ? " fehler" : ""}`}
                                >
                                    {m.rolle === "USER" ? (
                                        <span className="sop-text">{m.inhalt}</span>
                                    ) : (
                                        <div className="sop-markdown">
                                            <ReactMarkdown remarkPlugins={[remarkGfm]}>{m.inhalt}</ReactMarkdown>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}

                        {senden && (
                            <div className="sop-bubble-zeile assistant">
                                <div className="sop-bubble assistant sop-typing" aria-label="Die KI schreibt...">
                                    <span></span><span></span><span></span>
                                </div>
                            </div>
                        )}

                        {fehler && (
                            <div className="alert alert-warning sop-fehler" role="alert">
                                <span>{fehler.text}</span>
                                <span className="sop-fehler-aktionen">
                                    {fehler.status === 409 ? (
                                        <button type="button" className="btn btn-sm btn-outline-primary" onClick={neuerChat}>
                                            Neuen Chat starten
                                        </button>
                                    ) : fehler.status !== 404 ? (
                                        <button type="button" className="btn btn-sm btn-outline-primary" onClick={erneutVersuchen} disabled={senden}>
                                            Erneut versuchen
                                        </button>
                                    ) : null}
                                </span>
                            </div>
                        )}

                        <div ref={verlaufEndeRef} />
                    </div>

                    <div className="sop-eingabe">
                        <div className="sop-eingabe-feld">
                            <textarea
                                className="form-control"
                                rows={1}
                                placeholder="Nachricht an den SOP-Assistenten... (Enter = senden, Shift+Enter = neue Zeile)"
                                value={eingabe}
                                onChange={(e) => setEingabe(e.target.value)}
                                onKeyDown={beiTaste}
                                aria-label="Nachricht"
                            />
                            <button
                                type="button"
                                className="btn btn-primary"
                                onClick={() => absenden()}
                                disabled={senden || !eingabe.trim() || zuLang}
                            >
                                Senden
                            </button>
                        </div>
                        <div className={`sop-zaehler${zuLang ? " text-danger" : " text-muted"}`}>
                            {eingabe.length}/{MAX_ZEICHEN}
                        </div>
                    </div>
                </main>
            </div>
        </div>
    );
}

export default SopAssistent;
