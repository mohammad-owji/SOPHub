import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import Navbar from "../components/Navbar";
import { getAuth } from "../services/api";
import {
    getChats,
    getChat,
    sendeNeueNachricht,
    sendeNachricht,
    benenneChatUm,
    setzeAngepinnt,
    loescheChat,
} from "../services/aiService";
import "./SopAssistent.css";

const MAX_ZEICHEN = 2000;
const SEITE_GROESSE = 30;
const SIDEBAR_KEY = "sophub_sop_sidebar_ein";
const PLATZHALTER_TITEL = "Neuer Chat";

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

// Chats in Datumsgruppen einteilen (Backend liefert bereits sortiert: angepinnt, dann neueste zuerst).
function tagesStart(datum) {
    const d = new Date(datum);
    d.setHours(0, 0, 0, 0);
    return d;
}

function gruppiere(chats) {
    const heute = tagesStart(new Date());
    const tag = 86400000;
    const eimer = { angepinnt: [], heute: [], gestern: [], woche: [], monat: [], aelter: [] };

    for (const c of chats) {
        if (c.angepinnt) {
            eimer.angepinnt.push(c);
            continue;
        }
        const diff = Math.round((heute - tagesStart(c.aktualisiertAm)) / tag);
        if (diff <= 0) eimer.heute.push(c);
        else if (diff === 1) eimer.gestern.push(c);
        else if (diff <= 7) eimer.woche.push(c);
        else if (diff <= 30) eimer.monat.push(c);
        else eimer.aelter.push(c);
    }

    return [
        ["angepinnt", "Angepinnt"],
        ["heute", "Heute"],
        ["gestern", "Gestern"],
        ["woche", "Letzte 7 Tage"],
        ["monat", "Letzte 30 Tage"],
        ["aelter", "Älter"],
    ]
        .filter(([key]) => eimer[key].length > 0)
        .map(([key, label]) => ({ label, items: eimer[key] }));
}

function SopAssistent() {
    const { chatId } = useParams();
    const navigate = useNavigate();

    const [chats, setChats] = useState([]);
    const [seite, setSeite] = useState(0);
    const [hatMehr, setHatMehr] = useState(false);
    const [mehrLaedt, setMehrLaedt] = useState(false);
    const [suche, setSuche] = useState("");
    const [sucheAktiv, setSucheAktiv] = useState("");

    const [nachrichten, setNachrichten] = useState([]);
    const [eingabe, setEingabe] = useState("");
    const [senden, setSenden] = useState(false);
    const [ladeChat, setLadeChat] = useState(false);
    const [fehler, setFehler] = useState(null);

    const [sidebarEin, setSidebarEin] = useState(sidebarStartwert);
    const [mobileOffen, setMobileOffen] = useState(false);
    const [menuId, setMenuId] = useState(null);
    const [umbenennenId, setUmbenennenId] = useState(null);
    const [umbenennenWert, setUmbenennenWert] = useState("");
    const [loeschenChat, setLoeschenChat] = useState(null);

    const aktivRef = useRef(chatId || null);
    const schonGeladenRef = useRef(null);
    const letzteEingabeRef = useRef("");
    const verlaufEndeRef = useRef(null);
    const titelTimerRef = useRef(null);

    const angemeldet = Boolean(getAuth()?.token);

    const ladeListe = async (q, seiteNr, append) => {
        try {
            const liste = await getChats(q, seiteNr, SEITE_GROESSE);
            const arr = Array.isArray(liste) ? liste : [];
            setChats((prev) => (append ? [...prev, ...arr] : arr));
            setHatMehr(arr.length === SEITE_GROESSE);
            setSeite(seiteNr);
        } catch { /* Liste ist nebensächlich */ }
    };

    // Suche entprellen.
    useEffect(() => {
        const t = setTimeout(() => setSucheAktiv(suche.trim()), 300);
        return () => clearTimeout(t);
    }, [suche]);

    // Liste bei Suche (und initial) neu laden.
    useEffect(() => {
        if (angemeldet) ladeListe(sucheAktiv, 0, false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [sucheAktiv]);

    // Timer beim Verlassen aufräumen.
    useEffect(() => () => {
        if (titelTimerRef.current) clearTimeout(titelTimerRef.current);
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
                if (err.status === 404) navigate("/sop-assistent", { replace: true });
            })
            .finally(() => {
                if (aktiv) setLadeChat(false);
            });
        return () => {
            aktiv = false;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [chatId]);

    useEffect(() => {
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

    const chatOeffnen = (id) => {
        setMobileOffen(false);
        navigate(`/sop-assistent/${id}`);
    };

    const neuerChat = () => {
        setFehler(null);
        setEingabe("");
        setMobileOffen(false);
        navigate("/sop-assistent");
    };

    // Nach dem ersten Senden den asynchron erzeugten Titel nachladen:
    // bis zu 3 Versuche im Abstand von ~2 s, Abbruch sobald ein Titel gesetzt ist.
    const titelNachladen = (neueChatId) => {
        if (titelTimerRef.current) clearTimeout(titelTimerRef.current);
        let versuche = 0;
        const tick = async () => {
            versuche += 1;
            let arr = [];
            try {
                const liste = await getChats(sucheAktiv, 0, SEITE_GROESSE);
                arr = Array.isArray(liste) ? liste : [];
            } catch { /* ignorieren */ }
            setChats(arr);
            setHatMehr(arr.length === SEITE_GROESSE);
            setSeite(0);
            const chat = arr.find((c) => c.id === neueChatId);
            if (!(chat && chat.titel) && versuche < 3) {
                titelTimerRef.current = setTimeout(tick, 2000);
            }
        };
        titelTimerRef.current = setTimeout(tick, 2000);
    };

    const absenden = async (text) => {
        const inhalt = (text ?? eingabe).trim();
        if (!inhalt || senden) return;
        if (inhalt.length > MAX_ZEICHEN) return;

        const zielId = aktivRef.current;
        const tempId = `tmp-${Date.now()}`;
        letzteEingabeRef.current = inhalt;

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

            const nochAktiv = zielId ? aktivRef.current === zielId : aktivRef.current === null;
            if (nochAktiv) {
                setNachrichten((prev) => [...prev, antwortNachricht]);
                if (!zielId) {
                    schonGeladenRef.current = antwort.chatId;
                    aktivRef.current = antwort.chatId;
                    navigate(`/sop-assistent/${antwort.chatId}`);
                }
            }
            ladeListe(sucheAktiv, 0, false);
            if (!zielId) titelNachladen(antwort.chatId);
        } catch (err) {
            const nochAktiv = zielId ? aktivRef.current === zielId : aktivRef.current === null;
            if (nochAktiv) {
                setNachrichten((prev) => prev.map((m) => (m.id === tempId ? { ...m, fehler: true } : m)));
                setFehler({ status: err.status, text: fehlerText(err) });
                if (err.status === 404) navigate("/sop-assistent", { replace: true });
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

    // --- Seitenleisten-Aktionen ---
    const umbenennenStarten = (c) => {
        setMenuId(null);
        setUmbenennenId(c.id);
        setUmbenennenWert(c.titel || "");
    };

    const umbenennenSpeichern = async () => {
        const neu = umbenennenWert.trim();
        const id = umbenennenId;
        setUmbenennenId(null);
        if (!id || !neu) return;
        try {
            await benenneChatUm(id, neu);
        } catch { /* ignorieren */ }
        ladeListe(sucheAktiv, 0, false);
    };

    const umbenennenTaste = (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            umbenennenSpeichern();
        } else if (e.key === "Escape") {
            e.preventDefault();
            setUmbenennenId(null);
        }
    };

    const anpinnenUmschalten = async (c) => {
        setMenuId(null);
        try {
            await setzeAngepinnt(c.id, !c.angepinnt);
        } catch { /* ignorieren */ }
        ladeListe(sucheAktiv, 0, false);
    };

    const loeschenBestaetigen = async () => {
        const ziel = loeschenChat;
        setLoeschenChat(null);
        if (!ziel) return;
        try {
            await loescheChat(ziel.id);
        } catch { /* ignorieren */ }
        if (ziel.id === chatId) navigate("/sop-assistent");
        ladeListe(sucheAktiv, 0, false);
    };

    const gruppen = gruppiere(chats);
    // "Als Projektidee speichern": Formular mit Werten aus der Ideenkarte vorbefüllen.
    const ideeSpeichern = (karte) => {
        const beschreibung = [
            karte.problem && `Problem: ${karte.problem}`,
            karte.zielgruppe && `Zielgruppe: ${karte.zielgruppe}`,
            Array.isArray(karte.mvp) && karte.mvp.length ? `MVP: ${karte.mvp.join(", ")}` : null,
            Array.isArray(karte.technologien) && karte.technologien.length
                ? `Technologien: ${karte.technologien.join(", ")}` : null,
        ].filter(Boolean).join("\n");
        navigate("/create-project", { state: { vorlage: { titel: karte.titel || "", beschreibung } } });
    };

    const chipSenden = (titel, art) => {
        const name = titel || "diese Idee";
        const texte = {
            kleiner: `Mach die Idee „${name}" kleiner – weniger Umfang, kleineres MVP.`,
            groesser: `Mach die Idee „${name}" größer – mehr Funktionen und Anspruch.`,
            tech: `Schlage für die Idee „${name}" andere Technologien vor.`,
            fokus: `Gib der Idee „${name}" einen anderen fachlichen Fokus.`,
        };
        absenden(texte[art]);
    };

    const renderBlock = (block, index) => {
        if (!block || typeof block !== "object") return null;
        if (block.typ === "idea_card") {
            return (
                <div className="card sop-ideenkarte" key={index}>
                    <div className="card-body">
                        <div className="d-flex justify-content-between align-items-start gap-2">
                            <h3 className="h6 mb-1">{block.titel}</h3>
                            <div className="d-flex gap-1 flex-shrink-0">
                                {block.aufwand && <span className="badge bg-secondary">Aufwand: {block.aufwand}</span>}
                                {block.machbarkeit && (
                                    <span className={`badge sop-ampel-${block.machbarkeit}`}>Machbarkeit</span>
                                )}
                            </div>
                        </div>
                        {block.problem && <p className="mb-1"><strong>Problem:</strong> {block.problem}</p>}
                        {block.zielgruppe && <p className="mb-1"><strong>Zielgruppe:</strong> {block.zielgruppe}</p>}
                        {Array.isArray(block.mvp) && block.mvp.length > 0 && (
                            <p className="mb-1"><strong>MVP:</strong> {block.mvp.join(", ")}</p>
                        )}
                        {Array.isArray(block.technologien) && block.technologien.length > 0 && (
                            <div className="mb-1">
                                {block.technologien.map((t, i) => (
                                    <span className="badge bg-info me-1 mb-1" key={i}>{t}</span>
                                ))}
                            </div>
                        )}
                        {Array.isArray(block.risiken) && block.risiken.length > 0 && (
                            <p className="mb-1 text-muted"><strong>Risiken:</strong> {block.risiken.join(", ")}</p>
                        )}
                        {Array.isArray(block.teamrollen) && block.teamrollen.length > 0 && (
                            <p className="mb-2 text-muted"><strong>Teamrollen:</strong> {block.teamrollen.join(", ")}</p>
                        )}
                        <div className="d-flex flex-wrap gap-1 mb-2">
                            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => chipSenden(block.titel, "kleiner")}>Kleiner</button>
                            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => chipSenden(block.titel, "groesser")}>Größer</button>
                            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => chipSenden(block.titel, "tech")}>Andere Technologie</button>
                            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => chipSenden(block.titel, "fokus")}>Anderer Fokus</button>
                        </div>
                        <button type="button" className="btn btn-sm btn-primary" onClick={() => ideeSpeichern(block)}>
                            Als Projektidee speichern
                        </button>
                    </div>
                </div>
            );
        }
        if (block.typ === "project_ref") {
            const grad = block.aehnlichkeit || "mittel";
            return (
                <div className="card sop-projektref" key={index}>
                    <div className="card-body py-2">
                        <div className="d-flex justify-content-between align-items-center gap-2">
                            <Link to={`/projectdetails/${block.id}`} className="fw-semibold">{block.titel}</Link>
                            <span className={`badge sop-grad-${grad}`}>Ähnlichkeit: {grad}</span>
                        </div>
                        {block.begruendung && <p className="mb-0 small text-muted mt-1">{block.begruendung}</p>}
                    </div>
                </div>
            );
        }
        return null;
    };

    const leer = nachrichten.length === 0 && !ladeChat;
    const zuLang = eingabe.length > MAX_ZEICHEN;

    const chatEintrag = (c) => {
        if (umbenennenId === c.id) {
            return (
                <input
                    type="text"
                    className="form-control form-control-sm sop-umbenennen"
                    value={umbenennenWert}
                    autoFocus
                    onChange={(e) => setUmbenennenWert(e.target.value)}
                    onKeyDown={umbenennenTaste}
                    onBlur={() => setUmbenennenId(null)}
                    aria-label="Chat umbenennen"
                />
            );
        }
        return (
            <div className={`sop-chat-eintrag${c.id === chatId ? " aktiv" : ""}`}>
                <button
                    type="button"
                    className="sop-chat-titel"
                    onClick={() => chatOeffnen(c.id)}
                    title={c.titel || PLATZHALTER_TITEL}
                >
                    <span key={c.titel || "neu"} className="sop-titel-fade">
                        {c.titel || PLATZHALTER_TITEL}
                    </span>
                </button>
                <div className="sop-menu-anker">
                    <button
                        type="button"
                        className="sop-menu-btn"
                        onClick={() => setMenuId(menuId === c.id ? null : c.id)}
                        aria-label="Chat-Menü"
                        aria-haspopup="true"
                        aria-expanded={menuId === c.id}
                    >
                        ⋯
                    </button>
                    {menuId === c.id && (
                        <div className="sop-menu">
                            <button type="button" className="sop-menu-eintrag" onClick={() => umbenennenStarten(c)}>
                                Umbenennen
                            </button>
                            <button type="button" className="sop-menu-eintrag" onClick={() => anpinnenUmschalten(c)}>
                                {c.angepinnt ? "Anpinnen lösen" : "Anpinnen"}
                            </button>
                            <button
                                type="button"
                                className="sop-menu-eintrag text-danger"
                                onClick={() => {
                                    setMenuId(null);
                                    setLoeschenChat({ id: c.id, titel: c.titel || PLATZHALTER_TITEL });
                                }}
                            >
                                Löschen
                            </button>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    return (
        <div className="sop-assistent-seite">
            <Navbar />
            <div className="sop-assistent-layout">
                {mobileOffen && <div className="sop-mobile-backdrop" onClick={() => setMobileOffen(false)} />}

                <aside
                    className={`sop-seitenleiste${sidebarEin ? "" : " zu"}${mobileOffen ? " offen" : ""}`}
                    aria-label="Chat-Verlauf"
                >
                    <div className="sop-seitenleiste-kopf">
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary sop-icon-btn d-none d-md-inline-block"
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
                            <div className="sop-suche">
                                <input
                                    type="search"
                                    className="form-control form-control-sm"
                                    placeholder="Chats durchsuchen"
                                    value={suche}
                                    onChange={(e) => setSuche(e.target.value)}
                                    aria-label="Chats durchsuchen"
                                />
                            </div>

                            <div className="sop-chatliste">
                                {chats.length === 0 ? (
                                    <p className="sop-chatliste-leer text-muted">
                                        {sucheAktiv ? "Keine Treffer." : "Noch keine Chats. Stell deine erste Frage."}
                                    </p>
                                ) : (
                                    <>
                                        {gruppen.map((gruppe) => (
                                            <div className="sop-gruppe" key={gruppe.label}>
                                                <div className="sop-gruppe-titel">{gruppe.label}</div>
                                                {gruppe.items.map((c) => (
                                                    <div key={c.id}>{chatEintrag(c)}</div>
                                                ))}
                                            </div>
                                        ))}
                                        {hatMehr && (
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-outline-secondary w-100 mt-2"
                                                onClick={async () => {
                                                    setMehrLaedt(true);
                                                    await ladeListe(sucheAktiv, seite + 1, true);
                                                    setMehrLaedt(false);
                                                }}
                                                disabled={mehrLaedt}
                                            >
                                                {mehrLaedt ? "Lädt..." : "Mehr laden"}
                                            </button>
                                        )}
                                    </>
                                )}
                            </div>

                            <p className="sop-datenschutz text-muted">
                                Deine Chats sind privat und nur für dich sichtbar.
                            </p>
                        </>
                    )}
                </aside>

                <main className="sop-chat">
                    <div className="sop-chat-topbar d-md-none">
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => setMobileOffen(true)}
                            aria-label="Chats anzeigen"
                        >
                            ☰ Chats
                        </button>
                    </div>

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

                        {nachrichten.map((m) => {
                            const istUser = m.rolle === "USER";
                            const bloecke = Array.isArray(m.bloecke) ? m.bloecke : [];
                            return (
                                <div key={m.id} className={`sop-bubble-zeile ${istUser ? "user" : "assistant"}`}>
                                    <div className="sop-bubble-spalte">
                                        {(istUser || (m.inhalt && m.inhalt.trim())) && (
                                            <div className={`sop-bubble ${istUser ? "user" : "assistant"}${m.fehler ? " fehler" : ""}`}>
                                                {istUser ? (
                                                    <span className="sop-text">{m.inhalt}</span>
                                                ) : (
                                                    <div className="sop-markdown">
                                                        <ReactMarkdown remarkPlugins={[remarkGfm]}>{m.inhalt}</ReactMarkdown>
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                        {!istUser && bloecke.length > 0 && (
                                            <div className="sop-bloecke">
                                                {bloecke.map((b, i) => renderBlock(b, i))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })}

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

            {/* Klick-Fänger: schließt ein offenes "..."-Menü. */}
            {menuId && <div className="sop-menu-fang" onClick={() => setMenuId(null)} />}

            {/* Löschen-Bestätigung im Bootstrap-Dialogstil. */}
            {loeschenChat && (
                <>
                    <div className="modal-backdrop fade show" />
                    <div className="modal fade show sop-modal" style={{ display: "block" }} role="dialog" aria-modal="true">
                        <div className="modal-dialog modal-dialog-centered">
                            <div className="modal-content">
                                <div className="modal-header">
                                    <h5 className="modal-title">Chat löschen</h5>
                                    <button type="button" className="btn-close" aria-label="Schließen" onClick={() => setLoeschenChat(null)} />
                                </div>
                                <div className="modal-body">
                                    <p className="mb-0">
                                        Chat „{loeschenChat.titel}" wirklich endgültig löschen?
                                        Das kann nicht rückgängig gemacht werden.
                                    </p>
                                </div>
                                <div className="modal-footer">
                                    <button type="button" className="btn btn-outline-secondary" onClick={() => setLoeschenChat(null)}>
                                        Abbrechen
                                    </button>
                                    <button type="button" className="btn btn-danger" onClick={loeschenBestaetigen}>
                                        Löschen
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

export default SopAssistent;
