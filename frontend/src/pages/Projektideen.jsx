import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import AuswahlFeld from "../components/AuswahlFeld";
import {
    FACHBEREICHE,
    studiengaengeFuer,
    projektartenFuer,
    nachFachbereichWechsel,
    nachStudiengangWechsel,
} from "../components/ProjektOptionen";
import { getAuth } from "../services/api";
import { schlageIdeenVor } from "../services/ideenService";
import "./Projektideen.css";

// ---------------------------------------------------------------- Auswahl-Listen

const SCHRITTE = ["Rahmen", "Themenfeld", "Technologien", "Wünsche"];

const SCHWIERIGKEITEN = [
    { wert: "Einsteiger", text: "Einsteiger", info: "Wenige Funktionen, eine Weboberfläche und eine Datenbank." },
    { wert: "Mittel", text: "Mittel", info: "Anmeldung, Rollen und bis zu einer externen Schnittstelle." },
    { wert: "Anspruchsvoll", text: "Anspruchsvoll", info: "Darf KI, Echtzeit, Hardware oder mehrere Schnittstellen enthalten." },
];

const THEMENFELDER = [
    "Hochschule & Campus",
    "Gesundheit",
    "Nachhaltigkeit & Umwelt",
    "Mobilität & Verkehr",
    "Bildung & Lernen",
    "Finanzen",
    "Spiele & Unterhaltung",
    "Soziales & Ehrenamt",
    "Sport & Freizeit",
    "Smart Home & IoT",
    "Wirtschaft & Handel",
    "Kultur & Medien",
];

const TECHNOLOGIEN = {
    "Frontend": ["React", "Angular", "Vue.js", "HTML/CSS/JavaScript", "TypeScript", "Bootstrap", "Tailwind CSS"],
    "Backend": ["Java / Spring Boot", "Node.js / Express", "Python / Django", "Python / Flask", "PHP / Laravel", "C# / .NET"],
    "Datenbank": ["PostgreSQL", "MySQL", "MongoDB", "SQLite", "Firebase"],
    "Mobile": ["Flutter", "React Native", "Android (Kotlin)", "iOS (Swift)"],
    "KI & Daten": ["Python (Data Science)", "Machine Learning", "KI-Sprachmodelle (LLM)", "Datenvisualisierung"],
    "IoT & Hardware": ["Arduino", "Raspberry Pi", "Sensoren", "MQTT"],
};

const TOOLS = ["GitHub", "GitLab", "Docker", "Figma", "Jira", "Trello", "Postman", "Swagger/OpenAPI", "JUnit", "Selenium"];

const TECH_BEREICH_LABEL = { frontend: "Frontend", backend: "Backend", datenbank: "Datenbank", sonstige: "Sonstiges" };

// Zwischenspeicher, damit Auswahl und Ideen nach "Als Projekt übernehmen" -> "Zurück" noch da sind
const SPEICHER_SCHLUESSEL = "sophub_ideenfinder";

const leereAuswahl = {
    fachbereich: "",
    studiengang: "",
    projektart: "",
    gruppengroesse: "3",
    schwierigkeit: "Mittel",
    themenfelder: [],
    zielgruppe: "",
    technologien: {}, // Name -> "kenne" | "lernen"
    tools: [],
    wuensche: "",
};

function ladeSpeicher() {
    try {
        const roh = sessionStorage.getItem(SPEICHER_SCHLUESSEL);
        return roh ? JSON.parse(roh) : null;
    } catch {
        return null;
    }
}

function schreibeSpeicher(daten) {
    try {
        sessionStorage.setItem(SPEICHER_SCHLUESSEL, JSON.stringify(daten));
    } catch {
        // Speichern ist nur eine Bequemlichkeit
    }
}

const alsListe = (wert) => (Array.isArray(wert) ? wert.filter(Boolean) : []);

// Beschreibung fuer "Projekt erstellen" aus einer Idee zusammensetzen
function beschreibungAusIdee(idee) {
    const teile = [];
    if (idee.kurzbeschreibung) teile.push(idee.kurzbeschreibung);
    if (idee.problem) teile.push(`Problem: ${idee.problem}`);
    if (idee.zielgruppe) teile.push(`Zielgruppe: ${idee.zielgruppe}`);
    const funktionen = alsListe(idee.kernfunktionen);
    if (funktionen.length) teile.push(`Kernfunktionen:\n${funktionen.map((f) => `- ${f}`).join("\n")}`);
    const technik = Object.values(idee.technologien || {}).flatMap(alsListe);
    if (technik.length) teile.push(`Technologien: ${technik.join(", ")}`);
    return teile.join("\n\n");
}

/**
 * Seite "Projektideen": KI-Ideenfinder.
 * In 4 Schritten auswaehlen (Rahmen, Themenfeld, Technologien, Wuensche),
 * die KI schlaegt 3 passende Ideen vor, eine davon kann direkt als Projekt uebernommen werden.
 */
function Projektideen() {
    const navigate = useNavigate();
    const auth = getAuth();
    const gespeichert = ladeSpeicher();

    const [schritt, setSchritt] = useState(0);
    const [auswahl, setAuswahl] = useState(gespeichert?.auswahl || leereAuswahl);
    const [ideen, setIdeen] = useState(gespeichert?.ideen || []);
    const [bisherige, setBisherige] = useState(gespeichert?.bisherige || []);
    const [laedt, setLaedt] = useState(false);
    const [fehler, setFehler] = useState("");

    useEffect(() => {
        if (!auth?.token) {
            navigate("/?redirect=/ideas");
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        schreibeSpeicher({ auswahl, ideen, bisherige });
    }, [auswahl, ideen, bisherige]);

    if (!auth?.token) {
        return null;
    }

    const setze = (feld, wert) => setAuswahl((vorher) => ({ ...vorher, [feld]: wert }));

    const umschalten = (feld, wert) =>
        setAuswahl((vorher) => {
            const liste = vorher[feld];
            return {
                ...vorher,
                [feld]: liste.includes(wert) ? liste.filter((x) => x !== wert) : [...liste, wert],
            };
        });

    // Technologie-Chip: aus -> "kenne ich" -> "will ich lernen" -> aus
    const technologieUmschalten = (name) =>
        setAuswahl((vorher) => {
            const neu = { ...vorher.technologien };
            if (!neu[name]) neu[name] = "kenne";
            else if (neu[name] === "kenne") neu[name] = "lernen";
            else delete neu[name];
            return { ...vorher, technologien: neu };
        });

    const bekannt = Object.keys(auswahl.technologien).filter((t) => auswahl.technologien[t] === "kenne");
    const lernen = Object.keys(auswahl.technologien).filter((t) => auswahl.technologien[t] === "lernen");

    const ideenAnfordern = async (variante = "") => {
        setFehler("");
        setLaedt(true);
        try {
            const antwort = await schlageIdeenVor({
                fachbereich: auswahl.fachbereich,
                studiengang: auswahl.studiengang,
                projektart: auswahl.projektart,
                gruppengroesse: Number(auswahl.gruppengroesse) || 3,
                schwierigkeit: auswahl.schwierigkeit,
                themenfelder: auswahl.themenfelder,
                zielgruppe: auswahl.zielgruppe,
                technologienBekannt: bekannt,
                technologienLernen: lernen,
                tools: auswahl.tools,
                wuensche: auswahl.wuensche,
                variante,
                bisherigeVorschlaege: variante ? bisherige : [],
            });
            const neueIdeen = alsListe(antwort?.ideen);
            setIdeen(neueIdeen);
            const titel = neueIdeen.map((i) => i.titel).filter(Boolean);
            setBisherige((vorher) => (variante ? [...vorher, ...titel] : titel).slice(-30));
            window.scrollTo({ top: 0, behavior: "smooth" });
        } catch (error) {
            setFehler(error.message || "Es konnten keine Ideen erzeugt werden.");
        } finally {
            setLaedt(false);
        }
    };

    const alsProjektUebernehmen = (idee) => {
        navigate("/create-project", {
            state: {
                vorlage: {
                    titel: idee.titel || "",
                    beschreibung: beschreibungAusIdee(idee),
                    fachbereich: auswahl.fachbereich,
                    studiengang: auswahl.studiengang,
                    projektart: auswahl.projektart,
                    gruppenanzahl: auswahl.gruppengroesse,
                },
            },
        });
    };

    const neuAnfangen = () => {
        setAuswahl(leereAuswahl);
        setIdeen([]);
        setBisherige([]);
        setSchritt(0);
        setFehler("");
    };

    // ---------------------------------------------------------------- Ergebnisse

    if (ideen.length > 0 || laedt) {
        return (
            <div>
                <Navbar />
                <div className="container py-4">
                    <div className="d-flex flex-wrap justify-content-between align-items-end gap-2 mb-3">
                        <div>
                            <h2 className="mb-1">Ihre Projektideen</h2>
                            <p className="text-muted mb-0">
                                Vorschläge der KI passend zu Ihrer Auswahl. Übernehmen Sie eine Idee als Projekt
                                oder lassen Sie sich neue Vorschläge machen.
                            </p>
                        </div>
                        <button type="button" className="btn btn-outline-secondary" onClick={() => setIdeen([])} disabled={laedt}>
                            ← Angaben ändern
                        </button>
                    </div>

                    {fehler && <div className="alert alert-danger">{fehler}</div>}

                    {laedt ? (
                        <div className="card">
                            <div className="card-body pi-laden">
                                <div className="spinner-border text-primary" role="status" aria-hidden="true" />
                                <div>
                                    <strong>Die KI sucht passende Ideen…</strong>
                                    <div className="text-muted small">Das dauert meist 20 bis 40 Sekunden.</div>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <>
                            <div className="row g-4">
                                {ideen.map((idee, index) => (
                                    <div className="col-lg-4" key={`${idee.titel}-${index}`}>
                                        <IdeeKarte idee={idee} onUebernehmen={() => alsProjektUebernehmen(idee)} />
                                    </div>
                                ))}
                            </div>

                            <div className="pi-weitere">
                                <span className="text-muted">Nicht das Richtige dabei?</span>
                                <div className="d-flex flex-wrap gap-2">
                                    <button type="button" className="btn btn-outline-primary" onClick={() => ideenAnfordern("einfacher")}>
                                        Einfacher
                                    </button>
                                    <button type="button" className="btn btn-outline-primary" onClick={() => ideenAnfordern("anspruchsvoller")}>
                                        Anspruchsvoller
                                    </button>
                                    <button type="button" className="btn btn-primary" onClick={() => ideenAnfordern("andere")}>
                                        ↻ Andere Ideen
                                    </button>
                                    <button type="button" className="btn btn-link pi-link" onClick={neuAnfangen}>
                                        Neu anfangen
                                    </button>
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </div>
        );
    }

    // ---------------------------------------------------------------- Assistent

    return (
        <div>
            <Navbar />
            <div className="container py-4 pi-seite">
                <div className="mb-4">
                    <h2 className="mb-1"><span className="pi-symbol">✦</span> Projektideen mit KI finden</h2>
                    <p className="text-muted mb-0">
                        Sie haben noch kein Thema? Wählen Sie aus, was Sie interessiert und womit Sie arbeiten möchten –
                        die KI schlägt Ihnen drei passende, in einem Semester umsetzbare Projektideen vor.
                    </p>
                </div>

                {/* Fortschritt */}
                <ol className="pi-schritte">
                    {SCHRITTE.map((name, index) => (
                        <li
                            key={name}
                            className={`pi-schritt ${index === schritt ? "pi-schritt-aktiv" : ""} ${index < schritt ? "pi-schritt-fertig" : ""}`}
                        >
                            <button type="button" onClick={() => setSchritt(index)}>
                                <span className="pi-schritt-nr">{index < schritt ? "✓" : index + 1}</span>
                                <span className="pi-schritt-name">{name}</span>
                            </button>
                        </li>
                    ))}
                </ol>

                {fehler && <div className="alert alert-danger">{fehler}</div>}

                <div className="card">
                    <div className="card-body">

                        {schritt === 0 && (
                            <>
                                <h4 className="pi-titel">Rahmen</h4>
                                <p className="pi-hinweis">Alle Angaben sind freiwillig – je mehr Sie angeben, desto passender die Ideen.</p>
                                <div className="row g-3">
                                    <div className="col-md-6">
                                        <label className="form-label">Fachbereich</label>
                                        <AuswahlFeld
                                            value={auswahl.fachbereich}
                                            onChange={(wert) => setAuswahl((v) => nachFachbereichWechsel(v, wert))}
                                            optionen={FACHBEREICHE}
                                            placeholder="Auswählen oder eintippen"
                                        />
                                    </div>
                                    <div className="col-md-6">
                                        <label className="form-label">Studiengang</label>
                                        <AuswahlFeld
                                            value={auswahl.studiengang}
                                            onChange={(wert) => setAuswahl((v) => nachStudiengangWechsel(v, wert))}
                                            optionen={studiengaengeFuer(auswahl.fachbereich)}
                                            placeholder="Auswählen oder eintippen"
                                        />
                                    </div>
                                    <div className="col-md-6">
                                        <label className="form-label">Projektart</label>
                                        <AuswahlFeld
                                            value={auswahl.projektart}
                                            onChange={(wert) => setze("projektart", wert)}
                                            optionen={projektartenFuer(auswahl.fachbereich, auswahl.studiengang)}
                                            placeholder="Auswählen oder eintippen"
                                        />
                                    </div>
                                    <div className="col-md-6">
                                        <label className="form-label" htmlFor="pi-gruppe">Gruppengröße</label>
                                        <select
                                            id="pi-gruppe"
                                            className="form-select"
                                            value={auswahl.gruppengroesse}
                                            onChange={(e) => setze("gruppengroesse", e.target.value)}
                                        >
                                            {[1, 2, 3, 4, 5].map((anzahl) => (
                                                <option key={anzahl} value={String(anzahl)}>
                                                    {anzahl} {anzahl === 1 ? "Person" : "Personen"}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                <label className="form-label mt-4">Schwierigkeit</label>
                                <div className="row g-3">
                                    {SCHWIERIGKEITEN.map((s) => (
                                        <div className="col-md-4" key={s.wert}>
                                            <button
                                                type="button"
                                                className={`pi-option ${auswahl.schwierigkeit === s.wert ? "pi-option-aktiv" : ""}`}
                                                onClick={() => setze("schwierigkeit", s.wert)}
                                                aria-pressed={auswahl.schwierigkeit === s.wert}
                                            >
                                                <strong>{s.text}</strong>
                                                <span>{s.info}</span>
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </>
                        )}

                        {schritt === 1 && (
                            <>
                                <h4 className="pi-titel">Themenfeld</h4>
                                <p className="pi-hinweis">Welche Bereiche interessieren Sie? Mehrfachauswahl möglich.</p>
                                <div className="pi-chips">
                                    {THEMENFELDER.map((thema) => (
                                        <button
                                            type="button"
                                            key={thema}
                                            className={`pi-chip ${auswahl.themenfelder.includes(thema) ? "pi-chip-an" : ""}`}
                                            onClick={() => umschalten("themenfelder", thema)}
                                            aria-pressed={auswahl.themenfelder.includes(thema)}
                                        >
                                            {thema}
                                        </button>
                                    ))}
                                </div>

                                <label className="form-label mt-4" htmlFor="pi-zielgruppe">
                                    Zielgruppe <span className="text-muted fw-normal">(optional)</span>
                                </label>
                                <input
                                    id="pi-zielgruppe"
                                    className="form-control"
                                    maxLength={150}
                                    value={auswahl.zielgruppe}
                                    onChange={(e) => setze("zielgruppe", e.target.value)}
                                    placeholder="z. B. Erstsemester, Lehrende, Vereine, ältere Menschen"
                                />
                            </>
                        )}

                        {schritt === 2 && (
                            <>
                                <h4 className="pi-titel">Technologien &amp; Tools</h4>
                                <p className="pi-hinweis">
                                    Klicken Sie einmal für <span className="pi-legende pi-chip-an">kenne ich</span> und
                                    ein zweites Mal für <span className="pi-legende pi-chip-lernen">möchte ich lernen</span>.
                                    Ein dritter Klick entfernt die Auswahl.
                                </p>

                                {Object.entries(TECHNOLOGIEN).map(([bereich, liste]) => (
                                    <div className="pi-gruppe" key={bereich}>
                                        <div className="pi-gruppe-name">{bereich}</div>
                                        <div className="pi-chips">
                                            {liste.map((name) => {
                                                const status = auswahl.technologien[name];
                                                return (
                                                    <button
                                                        type="button"
                                                        key={name}
                                                        className={`pi-chip ${status === "kenne" ? "pi-chip-an" : ""} ${status === "lernen" ? "pi-chip-lernen" : ""}`}
                                                        onClick={() => technologieUmschalten(name)}
                                                        title={status === "kenne" ? "Kenne ich" : status === "lernen" ? "Möchte ich lernen" : "Nicht ausgewählt"}
                                                    >
                                                        {status === "kenne" && "✓ "}
                                                        {status === "lernen" && "★ "}
                                                        {name}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    </div>
                                ))}

                                <div className="pi-gruppe">
                                    <div className="pi-gruppe-name">Tools</div>
                                    <div className="pi-chips">
                                        {TOOLS.map((tool) => (
                                            <button
                                                type="button"
                                                key={tool}
                                                className={`pi-chip ${auswahl.tools.includes(tool) ? "pi-chip-an" : ""}`}
                                                onClick={() => umschalten("tools", tool)}
                                                aria-pressed={auswahl.tools.includes(tool)}
                                            >
                                                {tool}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </>
                        )}

                        {schritt === 3 && (
                            <>
                                <h4 className="pi-titel">Weitere Wünsche</h4>
                                <label className="form-label" htmlFor="pi-wuensche">
                                    Was soll das Projekt noch können? <span className="text-muted fw-normal">(optional)</span>
                                </label>
                                <textarea
                                    id="pi-wuensche"
                                    className="form-control"
                                    rows="3"
                                    maxLength={500}
                                    value={auswahl.wuensche}
                                    onChange={(e) => setze("wuensche", e.target.value)}
                                    placeholder="z. B. soll auf dem Handy laufen, mit Kamera arbeiten oder Daten einer offenen Schnittstelle nutzen"
                                />
                                <div className="form-text text-end">{auswahl.wuensche.length} / 500</div>

                                <div className="pi-zusammenfassung">
                                    <h5>Ihre Auswahl</h5>
                                    <dl>
                                        <dt>Rahmen</dt>
                                        <dd>
                                            {[auswahl.studiengang || auswahl.fachbereich, auswahl.projektart,
                                            `${auswahl.gruppengroesse} ${auswahl.gruppengroesse === "1" ? "Person" : "Personen"}`,
                                            auswahl.schwierigkeit].filter(Boolean).join(" · ")}
                                        </dd>
                                        <dt>Themenfelder</dt>
                                        <dd>{auswahl.themenfelder.join(", ") || "–"}</dd>
                                        <dt>Kenne ich</dt>
                                        <dd>{bekannt.join(", ") || "–"}</dd>
                                        <dt>Möchte ich lernen</dt>
                                        <dd>{lernen.join(", ") || "–"}</dd>
                                        <dt>Tools</dt>
                                        <dd>{auswahl.tools.join(", ") || "–"}</dd>
                                    </dl>
                                </div>
                            </>
                        )}

                        {/* Navigation */}
                        <div className="pi-navigation">
                            <button
                                type="button"
                                className="btn btn-outline-secondary"
                                onClick={() => setSchritt((s) => s - 1)}
                                disabled={schritt === 0}
                            >
                                ← Zurück
                            </button>
                            {schritt < SCHRITTE.length - 1 ? (
                                <button type="button" className="btn btn-primary" onClick={() => setSchritt((s) => s + 1)}>
                                    Weiter →
                                </button>
                            ) : (
                                <button type="button" className="btn btn-primary" onClick={() => ideenAnfordern()}>
                                    ✦ Ideen vorschlagen
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

// ---------------------------------------------------------------- Eine Ideen-Karte

function IdeeKarte({ idee, onUebernehmen }) {
    const funktionen = alsListe(idee.kernfunktionen);
    const erweiterungen = alsListe(idee.erweiterungen);
    const rollen = alsListe(idee.rollen);
    const neu = alsListe(idee.neu_zu_lernen);
    const technik = Object.entries(idee.technologien || {}).filter(([, liste]) => alsListe(liste).length > 0);
    const tools = alsListe(idee.tools);

    return (
        <article className="card pi-idee">
            <div className="card-body">
                <div className="d-flex flex-wrap gap-2 mb-2">
                    {idee.schwierigkeit && <span className="badge bg-primary">{idee.schwierigkeit}</span>}
                </div>
                <h3 className="pi-idee-titel">{idee.titel}</h3>
                {idee.kurzbeschreibung && <p className="pi-text">{idee.kurzbeschreibung}</p>}

                {idee.problem && (
                    <p className="pi-text"><strong>Problem:</strong> {idee.problem}</p>
                )}
                {idee.zielgruppe && (
                    <p className="pi-text"><strong>Zielgruppe:</strong> {idee.zielgruppe}</p>
                )}

                {funktionen.length > 0 && (
                    <>
                        <div className="pi-abschnitt">Kernfunktionen</div>
                        <ul className="pi-liste">
                            {funktionen.map((f, i) => <li key={i}>{f}</li>)}
                        </ul>
                    </>
                )}

                {(technik.length > 0 || tools.length > 0) && (
                    <>
                        <div className="pi-abschnitt">Technologien</div>
                        {technik.map(([bereich, liste]) => (
                            <div className="pi-technik" key={bereich}>
                                <span className="pi-technik-bereich">{TECH_BEREICH_LABEL[bereich] || bereich}</span>
                                {alsListe(liste).map((t) => (
                                    <span key={t} className={`pi-tag ${neu.includes(t) ? "pi-tag-neu" : ""}`} title={neu.includes(t) ? "Neu zu lernen" : ""}>
                                        {t}
                                    </span>
                                ))}
                            </div>
                        ))}
                        {tools.length > 0 && (
                            <div className="pi-technik">
                                <span className="pi-technik-bereich">Tools</span>
                                {tools.map((t) => <span key={t} className="pi-tag">{t}</span>)}
                            </div>
                        )}
                        {neu.length > 0 && (
                            <div className="form-text">★ Neu zu lernen: {neu.join(", ")}</div>
                        )}
                    </>
                )}

                {idee.aufwand && (
                    <p className="pi-aufwand"><strong>Aufwand:</strong> {idee.aufwand}</p>
                )}

                {rollen.length > 0 && (
                    <details className="pi-details">
                        <summary>Rollen im Team ({rollen.length})</summary>
                        <ul className="pi-liste">
                            {rollen.map((r, i) => (
                                <li key={i}><strong>{r.rolle}:</strong> {r.aufgaben}</li>
                            ))}
                        </ul>
                    </details>
                )}

                {erweiterungen.length > 0 && (
                    <details className="pi-details">
                        <summary>Mögliche Erweiterungen</summary>
                        <ul className="pi-liste">
                            {erweiterungen.map((e, i) => <li key={i}>{e}</li>)}
                        </ul>
                    </details>
                )}

                {idee.aehnlich_zu && (
                    <div className="pi-aehnlich">Ähnlich wie das vorhandene Projekt „{idee.aehnlich_zu}“</div>
                )}

                <button type="button" className="btn btn-primary w-100 mt-auto" onClick={onUebernehmen}>
                    Als Projekt übernehmen
                </button>
            </div>
        </article>
    );
}

export default Projektideen;
