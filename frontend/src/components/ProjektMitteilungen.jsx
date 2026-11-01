import { useEffect, useState } from "react";
import { getMitteilungen, sendeMitteilung, sendeAntwort, loescheMitteilung } from "../services/mitteilungService";
import "./ProjektMitteilungen.css";

// Bewusst nur "wichtige" Arten von Mitteilungen - kein Chat fuer Small Talk
const KATEGORIEN = {
    WICHTIG: { text: "Wichtig", klasse: "pm-kat-wichtig" },
    FRAGE: { text: "Frage", klasse: "pm-kat-frage" },
    TERMIN: { text: "Termin", klasse: "pm-kat-termin" },
    ABGABE: { text: "Abgabe", klasse: "pm-kat-abgabe" },
    INFO: { text: "Info", klasse: "pm-kat-info" },
};

const MAX_TEXT = 2000;

const leeresFormular = { kategorie: "WICHTIG", betreff: "", text: "", nurTeam: false };

function formatZeit(wert) {
    if (!wert) return "";
    const datum = new Date(wert);
    if (Number.isNaN(datum.getTime())) return "";
    return datum.toLocaleString("de-DE", { dateStyle: "medium", timeStyle: "short" });
}

const initialen = (person) =>
    `${(person?.vorname || "").charAt(0)}${(person?.name || "").charAt(0)}`.toUpperCase() || "?";

/**
 * Mitteilungsbrett eines Projekts.
 * Sichtbar fuer Team und Betreuung. Fuer alle anderen antwortet das Backend mit 403,
 * dann zeigt diese Komponente nichts an.
 */
function ProjektMitteilungen({ projektId, istTeam, onGeladen, onKeinZugriff }) {
    const [mitteilungen, setMitteilungen] = useState([]);
    const [laedt, setLaedt] = useState(true);
    const [keinZugriff, setKeinZugriff] = useState(false);
    const [fehler, setFehler] = useState("");

    const [formularOffen, setFormularOffen] = useState(false);
    const [formular, setFormular] = useState(leeresFormular);
    const [sendet, setSendet] = useState(false);
    const [erfolg, setErfolg] = useState("");

    // Antwort-Formular: welche Mitteilung gerade beantwortet wird
    const [antwortAuf, setAntwortAuf] = useState(null);
    const [antwortText, setAntwortText] = useState("");

    const laden = () =>
        getMitteilungen(projektId)
            .then((liste) => {
                setMitteilungen(liste || []);
                setKeinZugriff(false);
                // Anzahl an die Projektseite melden (fuer den Reiter "Mitteilungen (3)")
                if (onGeladen) onGeladen((liste || []).length);
            })
            .catch((error) => {
                if (error.status === 403) {
                    setKeinZugriff(true);
                    if (onKeinZugriff) onKeinZugriff();
                } else {
                    setFehler(error.message || "Mitteilungen konnten nicht geladen werden.");
                }
            })
            .finally(() => setLaedt(false));

    useEffect(() => {
        if (!projektId) return;
        setLaedt(true);
        setFehler("");
        laden();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projektId]);

    if (keinZugriff) return null;

    const abschicken = async (event) => {
        event.preventDefault();
        setFehler("");
        setErfolg("");
        if (!formular.betreff.trim() || !formular.text.trim()) {
            setFehler("Bitte geben Sie Betreff und Text ein.");
            return;
        }
        setSendet(true);
        try {
            await sendeMitteilung(projektId, { ...formular, nurTeam: istTeam && formular.nurTeam });
            setFormular(leeresFormular);
            setFormularOffen(false);
            setErfolg(
                formular.nurTeam && istTeam
                    ? "Mitteilung an das Team gesendet."
                    : "Mitteilung gesendet. Die anderen Beteiligten werden per E-Mail informiert."
            );
            await laden();
        } catch (error) {
            setFehler(error.message || "Mitteilung konnte nicht gesendet werden.");
        } finally {
            setSendet(false);
        }
    };

    const antworten = async (event, mitteilungId) => {
        event.preventDefault();
        setFehler("");
        if (!antwortText.trim()) {
            setFehler("Bitte geben Sie eine Antwort ein.");
            return;
        }
        setSendet(true);
        try {
            await sendeAntwort(projektId, mitteilungId, antwortText);
            setAntwortAuf(null);
            setAntwortText("");
            await laden();
        } catch (error) {
            setFehler(error.message || "Antwort konnte nicht gesendet werden.");
        } finally {
            setSendet(false);
        }
    };

    const loeschen = async (mitteilungId, istAntwort) => {
        const frage = istAntwort
            ? "Diese Antwort wirklich löschen?"
            : "Diese Mitteilung (mit allen Antworten) wirklich löschen?";
        if (!window.confirm(frage)) return;
        setFehler("");
        try {
            await loescheMitteilung(projektId, mitteilungId);
            await laden();
        } catch (error) {
            setFehler(error.message || "Löschen ist fehlgeschlagen.");
        }
    };

    return (
        <div className="card">
            <div className="card-body">
                <div className="d-flex justify-content-between align-items-center gap-2 mb-1">
                    <h4 className="pd-abschnitt mb-0">Mitteilungen</h4>
                    {!formularOffen && (
                        <button
                            type="button"
                            className="btn btn-sm btn-primary"
                            onClick={() => {
                                setErfolg("");
                                setFormularOffen(true);
                            }}
                        >
                            + Neue Mitteilung
                        </button>
                    )}
                </div>
                <p className="pm-hinweis">
                    Für wichtige Informationen an das Team und die Betreuung, z. B. Termine, Abgaben oder Fragen.
                    Bitte keine Unterhaltungen.
                </p>

                {fehler && <div className="alert alert-danger py-2">{fehler}</div>}
                {erfolg && <div className="alert alert-success py-2">{erfolg}</div>}

                {formularOffen && (
                    <form className="pm-formular" onSubmit={abschicken}>
                        <div className="row g-3">
                            <div className="col-md-6">
                                <label className="form-label" htmlFor="pm-kategorie">Art der Mitteilung</label>
                                <select
                                    id="pm-kategorie"
                                    className="form-select"
                                    value={formular.kategorie}
                                    onChange={(e) => setFormular({ ...formular, kategorie: e.target.value })}
                                >
                                    {Object.entries(KATEGORIEN).map(([wert, info]) => (
                                        <option key={wert} value={wert}>{info.text}</option>
                                    ))}
                                </select>
                            </div>
                            {istTeam && (
                                <div className="col-md-6">
                                    <label className="form-label" htmlFor="pm-empfaenger">Empfänger</label>
                                    <select
                                        id="pm-empfaenger"
                                        className="form-select"
                                        value={formular.nurTeam ? "team" : "alle"}
                                        onChange={(e) => setFormular({ ...formular, nurTeam: e.target.value === "team" })}
                                    >
                                        <option value="alle">Team + Betreuer:in</option>
                                        <option value="team">Nur Team</option>
                                    </select>
                                </div>
                            )}
                            <div className="col-12">
                                <label className="form-label" htmlFor="pm-betreff">Betreff</label>
                                <input
                                    id="pm-betreff"
                                    className="form-control"
                                    maxLength={150}
                                    value={formular.betreff}
                                    onChange={(e) => setFormular({ ...formular, betreff: e.target.value })}
                                    placeholder="z. B. Abgabe Pflichtenheft am Freitag"
                                    autoFocus
                                />
                            </div>
                            <div className="col-12">
                                <label className="form-label" htmlFor="pm-text">Mitteilung</label>
                                <textarea
                                    id="pm-text"
                                    className="form-control"
                                    rows="4"
                                    maxLength={MAX_TEXT}
                                    value={formular.text}
                                    onChange={(e) => setFormular({ ...formular, text: e.target.value })}
                                />
                                <div className="form-text text-end">{formular.text.length} / {MAX_TEXT}</div>
                            </div>
                        </div>
                        <div className="d-flex gap-2 mt-2">
                            <button className="btn btn-primary" type="submit" disabled={sendet}>
                                {sendet ? "Sendet..." : "Mitteilung senden"}
                            </button>
                            <button
                                className="btn btn-outline-secondary"
                                type="button"
                                onClick={() => {
                                    setFormularOffen(false);
                                    setFormular(leeresFormular);
                                    setFehler("");
                                }}
                            >
                                Abbrechen
                            </button>
                        </div>
                    </form>
                )}

                {laedt ? (
                    <p className="text-muted mb-0">Mitteilungen werden geladen...</p>
                ) : mitteilungen.length === 0 ? (
                    <p className="text-muted mb-0">Noch keine Mitteilungen.</p>
                ) : (
                    <div className="d-flex flex-column gap-3 mt-2">
                        {mitteilungen.map((m) => {
                            const kategorie = KATEGORIEN[m.kategorie] || KATEGORIEN.INFO;
                            return (
                                <article className="pm-mitteilung" key={m.id}>
                                    <div className="pm-kopf">
                                        <span className={`pm-kategorie ${kategorie.klasse}`}>{kategorie.text}</span>
                                        {m.nurTeam && <span className="pm-nur-team">Nur Team</span>}
                                        <h5 className="pm-betreff">{m.betreff}</h5>
                                    </div>

                                    <div className="pm-autor">
                                        <span className="pm-avatar">{initialen(m.autor)}</span>
                                        <span>
                                            <strong>{m.autor?.vorname} {m.autor?.name}</strong>
                                            <span className="text-muted"> · {m.autor?.rolle} · {formatZeit(m.erstelltAm)}</span>
                                        </span>
                                    </div>

                                    <p className="pm-text">{m.text}</p>

                                    {m.antworten?.length > 0 && (
                                        <div className="pm-antworten">
                                            {m.antworten.map((a) => (
                                                <div className="pm-antwort" key={a.id}>
                                                    <div className="pm-autor">
                                                        <span className="pm-avatar pm-avatar-klein">{initialen(a.autor)}</span>
                                                        <span>
                                                            <strong>{a.autor?.vorname} {a.autor?.name}</strong>
                                                            <span className="text-muted"> · {a.autor?.rolle} · {formatZeit(a.erstelltAm)}</span>
                                                        </span>
                                                        {a.eigene && (
                                                            <button
                                                                type="button"
                                                                className="btn btn-link btn-sm pm-loeschen ms-auto"
                                                                onClick={() => loeschen(a.id, true)}
                                                            >
                                                                Löschen
                                                            </button>
                                                        )}
                                                    </div>
                                                    <p className="pm-text mb-0">{a.text}</p>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {antwortAuf === m.id ? (
                                        <form className="pm-antwort-formular" onSubmit={(e) => antworten(e, m.id)}>
                                            <textarea
                                                className="form-control"
                                                rows="2"
                                                maxLength={MAX_TEXT}
                                                value={antwortText}
                                                onChange={(e) => setAntwortText(e.target.value)}
                                                placeholder="Ihre Antwort..."
                                                autoFocus
                                            />
                                            <div className="d-flex gap-2 mt-2">
                                                <button className="btn btn-sm btn-primary" type="submit" disabled={sendet}>
                                                    {sendet ? "Sendet..." : "Antwort senden"}
                                                </button>
                                                <button
                                                    className="btn btn-sm btn-outline-secondary"
                                                    type="button"
                                                    onClick={() => {
                                                        setAntwortAuf(null);
                                                        setAntwortText("");
                                                    }}
                                                >
                                                    Abbrechen
                                                </button>
                                            </div>
                                        </form>
                                    ) : (
                                        <div className="d-flex gap-2 mt-2">
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-outline-primary"
                                                onClick={() => {
                                                    setAntwortAuf(m.id);
                                                    setAntwortText("");
                                                }}
                                            >
                                                Antworten
                                            </button>
                                            {m.eigene && (
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-outline-danger"
                                                    onClick={() => loeschen(m.id, false)}
                                                >
                                                    Löschen
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </article>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}

export default ProjektMitteilungen;