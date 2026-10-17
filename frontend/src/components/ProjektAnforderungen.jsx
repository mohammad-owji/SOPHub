import { useEffect, useState } from "react";
import {
    getAnforderungen,
    erstelleAnforderung,
    reicheEin,
    pruefeAbgabe,
    loescheAnforderung,
} from "../services/anforderungService";
import { downloadDokument } from "../services/api";
import DokumentAnsicht from "./DokumentAnsicht";
import "./ProjektAnforderungen.css";

// Waehlbare Dokumentarten (muss zur Liste im AnforderungService passen)
const DOKUMENTARTEN = {
    LASTENHEFT: "Lastenheft",
    PFLICHTENHEFT: "Pflichtenheft",
    DATENBANKMODELL: "Datenbankmodell",
    DOKUMENTATION: "Dokumentation",
    TESTKONZEPT: "Testkonzept",
    PRAESENTATION: "Präsentation",
    SONSTIGES: "Sonstiges (eigener Name)",
};

const STATUS = {
    ANGEFORDERT: { text: "Angefordert", klasse: "pa-status-angefordert" },
    EINGEREICHT: { text: "Eingereicht", klasse: "pa-status-eingereicht" },
    UEBERARBEITEN: { text: "Überarbeiten", klasse: "pa-status-ueberarbeiten" },
    AKZEPTIERT: { text: "Akzeptiert", klasse: "pa-status-akzeptiert" },
};

const MAX_TEXT = 2000;

const leeresFormular = { typ: "LASTENHEFT", titel: "", beschreibung: "", frist: "" };

// Heutiges Datum als "JJJJ-MM-TT" (Ortszeit), z.B. fuer min im Datumsfeld
function heute() {
    const d = new Date();
    const zwei = (zahl) => String(zahl).padStart(2, "0");
    return `${d.getFullYear()}-${zwei(d.getMonth() + 1)}-${zwei(d.getDate())}`;
}

// "2026-11-15" -> "15.11.2026"
function formatDatum(wert) {
    if (!wert) return "";
    const text = Array.isArray(wert)
        ? `${wert[0]}-${String(wert[1]).padStart(2, "0")}-${String(wert[2]).padStart(2, "0")}`
        : String(wert);
    const [jahr, monat, tag] = text.split("-");
    return tag && monat && jahr ? `${tag}.${monat}.${jahr}` : text;
}

function datumAlsText(wert) {
    if (!wert) return "";
    return Array.isArray(wert)
        ? `${wert[0]}-${String(wert[1]).padStart(2, "0")}-${String(wert[2]).padStart(2, "0")}`
        : String(wert);
}

function formatZeit(wert) {
    if (!wert) return "";
    const datum = new Date(wert);
    if (Number.isNaN(datum.getTime())) return "";
    return datum.toLocaleString("de-DE", { dateStyle: "medium", timeStyle: "short" });
}

const vollerName = (person) => `${person?.vorname || ""} ${person?.name || ""}`.trim() || "Unbekannt";

const istPdf = (dateiName) => (dateiName || "").toLowerCase().endsWith(".pdf");

/** Wie viele Abgaben warten gerade auf mich? (fuer die Zahl im Reiter) */
function zaehleOffene(daten) {
    const liste = daten?.anforderungen || [];
    if (daten?.istBetreuer) {
        return liste.filter((a) => a.status === "EINGEREICHT").length;
    }
    return liste.filter((a) => a.status === "ANGEFORDERT" || a.status === "UEBERARBEITEN").length;
}

/**
 * Reiter "Abgaben": Die Betreuung fordert Dokumente an (z.B. Lastenheft),
 * das Team laedt sie hoch, die Betreuung akzeptiert sie oder bittet um Ueberarbeitung.
 * Sichtbar nur fuer Team und Betreuung - fuer alle anderen antwortet das Backend mit 403.
 */
function ProjektAnforderungen({ projektId, onGeladen, onKeinZugriff, onEingereicht }) {
    const [daten, setDaten] = useState({ istTeam: false, istBetreuer: false, anforderungen: [] });
    const [laedt, setLaedt] = useState(true);
    const [keinZugriff, setKeinZugriff] = useState(false);
    const [fehler, setFehler] = useState("");
    const [erfolg, setErfolg] = useState("");
    const [sendet, setSendet] = useState(false);

    // Betreuung: neues Dokument anfordern
    const [formularOffen, setFormularOffen] = useState(false);
    const [formular, setFormular] = useState(leeresFormular);

    // Team: Datei einreichen (nur bei einer Anforderung gleichzeitig geoeffnet)
    const [einreichenFuer, setEinreichenFuer] = useState(null);
    const [datei, setDatei] = useState(null);
    const [einreichKommentar, setEinreichKommentar] = useState("");

    // Betreuung: Einreichung pruefen
    const [pruefenFuer, setPruefenFuer] = useState(null);
    const [pruefKommentar, setPruefKommentar] = useState("");

    const [ansichtDokument, setAnsichtDokument] = useState(null);

    const laden = () =>
        getAnforderungen(projektId)
            .then((antwort) => {
                const ergebnis = antwort || { istTeam: false, istBetreuer: false, anforderungen: [] };
                setDaten(ergebnis);
                setKeinZugriff(false);
                if (onGeladen) onGeladen(zaehleOffene(ergebnis));
            })
            .catch((error) => {
                if (error.status === 403) {
                    setKeinZugriff(true);
                    if (onKeinZugriff) onKeinZugriff();
                } else {
                    setFehler(error.message || "Abgaben konnten nicht geladen werden.");
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

    const { istTeam, istBetreuer, anforderungen } = daten;
    const heutigesDatum = heute();

    const meldungenZuruecksetzen = () => {
        setFehler("");
        setErfolg("");
    };

    const anfordern = async (event) => {
        event.preventDefault();
        meldungenZuruecksetzen();
        if (formular.typ === "SONSTIGES" && !formular.titel.trim()) {
            setFehler("Bitte geben Sie einen Namen für das Dokument ein.");
            return;
        }
        setSendet(true);
        try {
            await erstelleAnforderung(projektId, {
                typ: formular.typ,
                titel: formular.typ === "SONSTIGES" ? formular.titel.trim() : "",
                beschreibung: formular.beschreibung,
                frist: formular.frist,
            });
            setFormular(leeresFormular);
            setFormularOffen(false);
            setErfolg("Dokument angefordert. Das Team wird per E-Mail informiert.");
            await laden();
        } catch (error) {
            setFehler(error.message || "Das Dokument konnte nicht angefordert werden.");
        } finally {
            setSendet(false);
        }
    };

    const einreichen = async (event, anforderungId) => {
        event.preventDefault();
        meldungenZuruecksetzen();
        if (!datei) {
            setFehler("Bitte wählen Sie eine Datei aus.");
            return;
        }
        setSendet(true);
        try {
            await reicheEin(projektId, anforderungId, datei, einreichKommentar);
            setEinreichenFuer(null);
            setDatei(null);
            setEinreichKommentar("");
            setErfolg("Dokument eingereicht. Die Betreuung wird per E-Mail informiert.");
            await laden();
            // Die Datei steht jetzt auch in der Dokumentenliste der Uebersicht
            if (onEingereicht) onEingereicht();
        } catch (error) {
            setFehler(error.message || "Das Dokument konnte nicht eingereicht werden.");
        } finally {
            setSendet(false);
        }
    };

    const pruefen = async (anforderungId, ergebnis) => {
        meldungenZuruecksetzen();
        if (ergebnis === "UEBERARBEITEN" && !pruefKommentar.trim()) {
            setFehler("Bitte beschreiben Sie, was überarbeitet werden soll.");
            return;
        }
        setSendet(true);
        try {
            await pruefeAbgabe(projektId, anforderungId, ergebnis, pruefKommentar);
            setPruefenFuer(null);
            setPruefKommentar("");
            setErfolg(
                ergebnis === "OK"
                    ? "Dokument akzeptiert. Das Team wird per E-Mail informiert."
                    : "Überarbeitung angefordert. Das Team wird per E-Mail informiert."
            );
            await laden();
        } catch (error) {
            setFehler(error.message || "Die Prüfung konnte nicht gespeichert werden.");
        } finally {
            setSendet(false);
        }
    };

    const loeschen = async (anforderung) => {
        if (!window.confirm(`Anforderung „${anforderung.titel}“ wirklich löschen? Bereits hochgeladene Dateien bleiben im Projekt.`)) {
            return;
        }
        meldungenZuruecksetzen();
        try {
            await loescheAnforderung(projektId, anforderung.id);
            await laden();
        } catch (error) {
            setFehler(error.message || "Die Anforderung konnte nicht gelöscht werden.");
        }
    };

    const dateiOeffnen = (eintrag) => {
        const dokument = { id: eintrag.dokumentId, dateiName: eintrag.dateiName };
        if (istPdf(eintrag.dateiName)) {
            setAnsichtDokument(dokument);
        } else {
            downloadDokument(dokument.id, dokument.dateiName).catch(() =>
                setFehler("Die Datei konnte nicht heruntergeladen werden.")
            );
        }
    };

    // Kleine Zusammenfassung oben: wie viele in welchem Zustand
    const anzahl = (status) => anforderungen.filter((a) => a.status === status).length;
    const zusammenfassung = [
        { status: "ANGEFORDERT", zahl: anzahl("ANGEFORDERT") },
        { status: "EINGEREICHT", zahl: anzahl("EINGEREICHT") },
        { status: "UEBERARBEITEN", zahl: anzahl("UEBERARBEITEN") },
        { status: "AKZEPTIERT", zahl: anzahl("AKZEPTIERT") },
    ].filter((eintrag) => eintrag.zahl > 0);

    return (
        <div className="card">
            <DokumentAnsicht dokument={ansichtDokument} onSchliessen={() => setAnsichtDokument(null)} />

            <div className="card-body">
                <div className="d-flex justify-content-between align-items-center gap-2 mb-1">
                    <h4 className="pd-abschnitt mb-0">Abgaben</h4>
                    {istBetreuer && !formularOffen && (
                        <button
                            type="button"
                            className="btn btn-sm btn-primary"
                            onClick={() => {
                                meldungenZuruecksetzen();
                                setFormularOffen(true);
                            }}
                        >
                            + Dokument anfordern
                        </button>
                    )}
                </div>
                <p className="pa-hinweis">
                    {istBetreuer
                        ? "Fordern Sie hier Dokumente beim Team an und prüfen Sie die Einreichungen. Das Team wird bei jedem Schritt per E-Mail informiert."
                        : "Hier sehen Sie, welche Dokumente Ihre Betreuung angefordert hat. Laden Sie sie hier hoch – die Betreuung wird per E-Mail informiert."}
                </p>

                {fehler && <div className="alert alert-danger py-2">{fehler}</div>}
                {erfolg && <div className="alert alert-success py-2">{erfolg}</div>}

                {formularOffen && (
                    <form className="pa-formular" onSubmit={anfordern}>
                        <div className="row g-3">
                            <div className="col-md-6">
                                <label className="form-label" htmlFor="pa-typ">Dokument</label>
                                <select
                                    id="pa-typ"
                                    className="form-select"
                                    value={formular.typ}
                                    onChange={(e) => setFormular({ ...formular, typ: e.target.value })}
                                >
                                    {Object.entries(DOKUMENTARTEN).map(([wert, text]) => (
                                        <option key={wert} value={wert}>{text}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="col-md-6">
                                <label className="form-label" htmlFor="pa-frist">
                                    Abgabe bis <span className="text-muted fw-normal">(optional)</span>
                                </label>
                                <input
                                    id="pa-frist"
                                    type="date"
                                    className="form-control"
                                    min={heutigesDatum}
                                    value={formular.frist}
                                    onChange={(e) => setFormular({ ...formular, frist: e.target.value })}
                                />
                            </div>
                            {formular.typ === "SONSTIGES" && (
                                <div className="col-12">
                                    <label className="form-label" htmlFor="pa-titel">Name des Dokuments</label>
                                    <input
                                        id="pa-titel"
                                        className="form-control"
                                        maxLength={150}
                                        value={formular.titel}
                                        onChange={(e) => setFormular({ ...formular, titel: e.target.value })}
                                        placeholder="z. B. Zwischenbericht"
                                        autoFocus
                                    />
                                </div>
                            )}
                            <div className="col-12">
                                <label className="form-label" htmlFor="pa-beschreibung">
                                    Hinweise für das Team <span className="text-muted fw-normal">(optional)</span>
                                </label>
                                <textarea
                                    id="pa-beschreibung"
                                    className="form-control"
                                    rows="3"
                                    maxLength={MAX_TEXT}
                                    value={formular.beschreibung}
                                    onChange={(e) => setFormular({ ...formular, beschreibung: e.target.value })}
                                    placeholder="z. B. Bitte nach der Vorlage aus der Vorlesung, max. 10 Seiten."
                                />
                            </div>
                        </div>
                        <div className="d-flex gap-2 mt-3">
                            <button className="btn btn-primary" type="submit" disabled={sendet}>
                                {sendet ? "Sendet..." : "Anfordern"}
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

                {zusammenfassung.length > 0 && (
                    <div className="pa-zusammenfassung">
                        {zusammenfassung.map((eintrag) => (
                            <span key={eintrag.status} className={`pa-status ${STATUS[eintrag.status].klasse}`}>
                                {eintrag.zahl} {STATUS[eintrag.status].text}
                            </span>
                        ))}
                    </div>
                )}

                {laedt ? (
                    <p className="text-muted mb-0">Abgaben werden geladen...</p>
                ) : anforderungen.length === 0 ? (
                    <p className="text-muted mb-0">
                        {istBetreuer
                            ? "Sie haben noch keine Dokumente angefordert."
                            : "Ihre Betreuung hat noch keine Dokumente angefordert."}
                    </p>
                ) : (
                    <div className="d-flex flex-column gap-3 mt-2">
                        {anforderungen.map((a) => {
                            const status = STATUS[a.status] || STATUS.ANGEFORDERT;
                            const frist = datumAlsText(a.frist);
                            const offen = a.status === "ANGEFORDERT" || a.status === "UEBERARBEITEN";
                            const ueberfaellig = offen && frist && frist < heutigesDatum;
                            const darfEinreichen = istTeam && a.status !== "AKZEPTIERT";
                            const darfPruefen = istBetreuer && a.status === "EINGEREICHT";
                            let fassung = 0;

                            return (
                                <article className={`pa-anforderung pa-rand-${a.status.toLowerCase()}`} key={a.id}>
                                    <div className="pa-kopf">
                                        <h5 className="pa-titel">{a.titel}</h5>
                                        <span className={`pa-status ${status.klasse}`}>{status.text}</span>
                                    </div>

                                    <div className="pa-meta">
                                        Angefordert von {vollerName(a.angefordertVon)} am {formatZeit(a.erstelltAm)}
                                        {frist && (
                                            <span className={`pa-frist ${ueberfaellig ? "pa-frist-ueberfaellig" : ""}`}>
                                                Abgabe bis {formatDatum(frist)}
                                                {ueberfaellig && " – überfällig"}
                                            </span>
                                        )}
                                    </div>

                                    {a.beschreibung && <p className="pa-text">{a.beschreibung}</p>}

                                    {a.verlauf?.length > 0 && (
                                        <ol className="pa-verlauf">
                                            {a.verlauf.map((e) => {
                                                if (e.art === "EINGEREICHT") fassung += 1;
                                                return (
                                                    <li className={`pa-eintrag pa-eintrag-${e.art.toLowerCase()}`} key={e.id}>
                                                        <div className="pa-eintrag-kopf">
                                                            {e.art === "EINGEREICHT" && (
                                                                <>
                                                                    <strong>Fassung {fassung}</strong> eingereicht von {vollerName(e.person)}
                                                                </>
                                                            )}
                                                            {e.art === "UEBERARBEITEN" && (
                                                                <>
                                                                    <strong>Überarbeitung angefordert</strong> von {vollerName(e.person)}
                                                                </>
                                                            )}
                                                            {e.art === "AKZEPTIERT" && (
                                                                <>
                                                                    <strong>Akzeptiert</strong> von {vollerName(e.person)}
                                                                </>
                                                            )}
                                                            <span className="text-muted"> · {formatZeit(e.erstelltAm)}</span>
                                                        </div>

                                                        {e.art === "EINGEREICHT" && (
                                                            e.dokumentId ? (
                                                                <button
                                                                    type="button"
                                                                    className="pa-datei"
                                                                    onClick={() => dateiOeffnen(e)}
                                                                    title={istPdf(e.dateiName) ? "Ansehen" : "Herunterladen"}
                                                                >
                                                                    <span className="pa-datei-endung">
                                                                        {e.dateiName?.split(".").pop()?.toUpperCase() || "DOC"}
                                                                    </span>
                                                                    <span className="text-truncate">{e.dateiName}</span>
                                                                </button>
                                                            ) : (
                                                                <div className="text-muted small">Datei wurde gelöscht.</div>
                                                            )
                                                        )}

                                                        {e.kommentar && <p className="pa-text mb-0">{e.kommentar}</p>}
                                                    </li>
                                                );
                                            })}
                                        </ol>
                                    )}

                                    {/* Team: Datei hochladen */}
                                    {darfEinreichen && einreichenFuer === a.id && (
                                        <form className="pa-aktion" onSubmit={(e) => einreichen(e, a.id)}>
                                            <label className="form-label" htmlFor={`pa-datei-${a.id}`}>Datei</label>
                                            <input
                                                id={`pa-datei-${a.id}`}
                                                type="file"
                                                className="form-control mb-2"
                                                accept=".pdf,.docx,.txt"
                                                onChange={(e) => setDatei(e.target.files?.[0] || null)}
                                            />
                                            <textarea
                                                className="form-control"
                                                rows="2"
                                                maxLength={MAX_TEXT}
                                                value={einreichKommentar}
                                                onChange={(e) => setEinreichKommentar(e.target.value)}
                                                placeholder="Kommentar an die Betreuung (optional), z. B. was geändert wurde"
                                            />
                                            <div className="d-flex gap-2 mt-2">
                                                <button className="btn btn-sm btn-primary" type="submit" disabled={sendet || !datei}>
                                                    {sendet ? "Lädt hoch..." : "Einreichen"}
                                                </button>
                                                <button
                                                    className="btn btn-sm btn-outline-secondary"
                                                    type="button"
                                                    onClick={() => {
                                                        setEinreichenFuer(null);
                                                        setDatei(null);
                                                        setEinreichKommentar("");
                                                    }}
                                                >
                                                    Abbrechen
                                                </button>
                                            </div>
                                        </form>
                                    )}

                                    {/* Betreuung: Einreichung pruefen */}
                                    {darfPruefen && pruefenFuer === a.id && (
                                        <div className="pa-aktion">
                                            <label className="form-label" htmlFor={`pa-pruefung-${a.id}`}>
                                                Rückmeldung an das Team
                                            </label>
                                            <textarea
                                                id={`pa-pruefung-${a.id}`}
                                                className="form-control"
                                                rows="3"
                                                maxLength={MAX_TEXT}
                                                value={pruefKommentar}
                                                onChange={(e) => setPruefKommentar(e.target.value)}
                                                placeholder="Bei „Überarbeiten“ Pflicht: Was soll geändert werden? Bei „Akzeptieren“ optional."
                                                autoFocus
                                            />
                                            <div className="d-flex flex-wrap gap-2 mt-2">
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-success"
                                                    disabled={sendet}
                                                    onClick={() => pruefen(a.id, "OK")}
                                                >
                                                    ✓ Akzeptieren
                                                </button>
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-warning"
                                                    disabled={sendet}
                                                    onClick={() => pruefen(a.id, "UEBERARBEITEN")}
                                                >
                                                    Überarbeitung anfordern
                                                </button>
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-outline-secondary"
                                                    onClick={() => {
                                                        setPruefenFuer(null);
                                                        setPruefKommentar("");
                                                    }}
                                                >
                                                    Abbrechen
                                                </button>
                                            </div>
                                        </div>
                                    )}

                                    {/* Knoepfe, solange kein Formular offen ist */}
                                    {einreichenFuer !== a.id && pruefenFuer !== a.id && (
                                        <div className="d-flex flex-wrap align-items-center gap-2 mt-2">
                                            {darfEinreichen && (
                                                <button
                                                    type="button"
                                                    className={`btn btn-sm ${offen ? "btn-primary" : "btn-outline-primary"}`}
                                                    onClick={() => {
                                                        meldungenZuruecksetzen();
                                                        setPruefenFuer(null);
                                                        setEinreichenFuer(a.id);
                                                        setDatei(null);
                                                        setEinreichKommentar("");
                                                    }}
                                                >
                                                    {a.status === "ANGEFORDERT" ? "Dokument hochladen" : "Neue Fassung hochladen"}
                                                </button>
                                            )}
                                            {istTeam && a.status === "EINGEREICHT" && (
                                                <span className="text-muted small">Wartet auf Prüfung durch die Betreuung.</span>
                                            )}
                                            {darfPruefen && (
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-primary"
                                                    onClick={() => {
                                                        meldungenZuruecksetzen();
                                                        setEinreichenFuer(null);
                                                        setPruefenFuer(a.id);
                                                        setPruefKommentar("");
                                                    }}
                                                >
                                                    Prüfen
                                                </button>
                                            )}
                                            {istBetreuer && offen && (
                                                <span className="text-muted small">Wartet auf Einreichung durch das Team.</span>
                                            )}
                                            {istBetreuer && (
                                                <button
                                                    type="button"
                                                    className="btn btn-link btn-sm pa-loeschen ms-auto"
                                                    onClick={() => loeschen(a)}
                                                >
                                                    Anforderung löschen
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

export default ProjektAnforderungen;