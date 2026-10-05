import { useEffect, useState } from "react";
import Navbar from "../components/Navbar";
import { getAlleProjekte } from "../services/api";
import { getKiUebersicht } from "../services/aiService";

const KATEGORIE_LABEL = {
    themenbereich: "Themenbereich",
    fachliche_kernfunktionen: "Fachliche Kernfunktionen",
    technologien: "Technologien",
    architektur_muster: "Architektur & Muster",
    vorgehensmodell: "Vorgehensmodell",
    verfuegbare_dokumente: "Verfügbare Dokumente",
    zielgruppen_rollen: "Zielgruppen & Rollen",
    nichtfunktionale_schwerpunkte: "Nichtfunktionale Schwerpunkte",
    externe_systeme_integrationen: "Externe Systeme",
    erforderliche_kompetenzen: "Erforderliche Kompetenzen",
    projektstatus_reifegrad: "Projektstatus",
};

const TECH_LABEL = {
    sprachen: "Sprachen",
    frontend: "Frontend",
    backend: "Backend",
    datenbank: "Datenbank",
    schnittstellen_apis: "Schnittstellen & APIs",
    sicherheit_auth: "Sicherheit & Auth",
    testwerkzeuge: "Testwerkzeuge",
    devops_infrastruktur: "DevOps & Infrastruktur",
    modellierung_diagramme: "Modellierung & Diagramme",
    dokumentation_werkzeuge: "Dokumentation",
    ki_ml: "KI / ML",
    sonstige: "Sonstige",
};

function StichwortBadges({ items }) {
    if (!Array.isArray(items) || items.length === 0) return null;
    return items.map((item, index) => (
        <span className="badge bg-primary me-1 mb-1" key={`${item.wert || item}-${index}`}>
            {typeof item === "string" ? item : item.wert}
        </span>
    ));
}

function KI() {
    const [projekte, setProjekte] = useState([]);
    const [projektId, setProjektId] = useState("");
    const [projekteLaden, setProjekteLaden] = useState(true);
    const [kiUebersicht, setKiUebersicht] = useState(null);
    const [kiLaden, setKiLaden] = useState(false);
    const [fehler, setFehler] = useState("");

    useEffect(() => {
        let aktiv = true;

        getAlleProjekte()
            .then((daten) => {
                if (aktiv) setProjekte(Array.isArray(daten) ? daten : []);
            })
            .catch((error) => {
                if (aktiv) setFehler(error.message || "Projekte konnten nicht geladen werden.");
            })
            .finally(() => {
                if (aktiv) setProjekteLaden(false);
            });

        return () => {
            aktiv = false;
        };
    }, []);

    useEffect(() => {
        if (!projektId) {
            setKiUebersicht(null);
            setKiLaden(false);
            return;
        }

        let aktiv = true;
        setKiUebersicht(null);
        setFehler("");
        setKiLaden(true);

        getKiUebersicht(projektId)
            .then((daten) => {
                if (aktiv) setKiUebersicht(daten);
            })
            .catch((error) => {
                if (aktiv) setFehler(error.message || "KI-Übersicht konnte nicht geladen werden.");
            })
            .finally(() => {
                if (aktiv) setKiLaden(false);
            });

        return () => {
            aktiv = false;
        };
    }, [projektId]);

    const neuErzeugen = async () => {
        setKiLaden(true);
        setFehler("");
        try {
            setKiUebersicht(await getKiUebersicht(projektId, true));
        } catch (error) {
            setFehler(error.message || "KI-Übersicht konnte nicht neu erzeugt werden.");
        } finally {
            setKiLaden(false);
        }
    };

    const zusammenfassung = kiUebersicht?.projektZusammenfassung;
    const kategorien = kiUebersicht?.stichwoerter?.kategorien || {};
    const kategorienEintraege = Object.entries(kategorien).filter(([, wert]) => {
        if (Array.isArray(wert)) return wert.length > 0;
        if (!wert || typeof wert !== "object") return false;
        return Boolean(wert.wert) || Object.values(wert).some((liste) => Array.isArray(liste) && liste.length > 0);
    });
    const schwierigkeitsstufe = kiUebersicht?.stichwoerter?.schwierigkeitsanalyse?.stufe;

    return (
        <div>
            <Navbar />
            <main className="container py-4">
                <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-end gap-3 mb-4">
                    <div>
                        <h1 className="mb-1">KI-Übersicht</h1>
                    </div>
                    {kiUebersicht?.generiertAm && (
                        <small className="text-muted">
                            Erstellt am {new Date(kiUebersicht.generiertAm).toLocaleString("de-DE")}
                        </small>
                    )}
                </div>

                <div className="row align-items-end g-3 mb-4">
                    <div className="col-md-8">
                        <label className="form-label fw-semibold" htmlFor="ki-projekt">Projekt</label>
                        <select
                            id="ki-projekt"
                            className="form-select"
                            value={projektId}
                            onChange={(event) => setProjektId(event.target.value)}
                            disabled={projekteLaden || kiLaden}
                        >
                            <option value="">
                                {projekteLaden ? "Projekte werden geladen..." : "Projekt auswählen..."}
                            </option>
                            {projekte.map((projekt) => (
                                <option key={projekt.id} value={projekt.id}>
                                    {projekt.titel}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="col-md-4">
                        <button
                            type="button"
                            className="btn btn-outline-primary w-100"
                            onClick={neuErzeugen}
                            disabled={!projektId || kiLaden}
                        >
                            {kiLaden ? "Wird verarbeitet..." : "Übersicht neu erzeugen"}
                        </button>
                    </div>
                </div>

                {fehler && <div className="alert alert-warning">{fehler}</div>}

                {!fehler && !projektId && !projekteLaden && projekte.length === 0 && (
                    <p className="text-muted">Keine Projekte verfügbar.</p>
                )}
                {!fehler && !projektId && projekte.length > 0 && (
                    <p className="text-muted">Wähle ein Projekt aus.</p>
                )}
                {kiLaden && <p className="text-muted">Die KI wertet die Projektdokumente aus...</p>}

                {!kiLaden && kiUebersicht && (
                    <div className="d-flex flex-column gap-4">
                        <section className="card">
                            <div className="card-body">
                                <h2 className="h4 mb-3">Projektzusammenfassung</h2>
                                {zusammenfassung ? (
                                    <>
                                        {zusammenfassung.kurzfassung && (
                                            <p className="fw-semibold">{zusammenfassung.kurzfassung}</p>
                                        )}
                                        {zusammenfassung.einleitung && <p>{zusammenfassung.einleitung}</p>}
                                        {Array.isArray(zusammenfassung.hauptteil) && zusammenfassung.hauptteil.map((abschnitt, index) => (
                                            <div className="mb-3" key={index}>
                                                <h3 className="h6 fw-bold">{abschnitt.titel}</h3>
                                                {abschnitt.inhalt && <p>{abschnitt.inhalt}</p>}
                                                {Array.isArray(abschnitt.stichpunkte) && abschnitt.stichpunkte.length > 0 && (
                                                    <ul className="mb-0">
                                                        {abschnitt.stichpunkte.map((punkt, punktIndex) => (
                                                            <li key={punktIndex}>{punkt}</li>
                                                        ))}
                                                    </ul>
                                                )}
                                            </div>
                                        ))}
                                        {zusammenfassung.schluss?.fazit && (
                                            <p className="mb-0">{zusammenfassung.schluss.fazit}</p>
                                        )}
                                    </>
                                ) : (
                                    <p className="text-muted mb-0">Keine Zusammenfassung verfügbar.</p>
                                )}
                            </div>
                        </section>

                        <section className="card">
                            <div className="card-body">
                                <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                                    <h2 className="h4 mb-0">Stichwörter</h2>
                                    {schwierigkeitsstufe && (
                                        <span className="badge bg-dark">Schwierigkeit: {schwierigkeitsstufe}</span>
                                    )}
                                </div>

                                {kategorienEintraege.length > 0 ? kategorienEintraege.map(([key, wert]) => (
                                    <div className="mb-3" key={key}>
                                        <h3 className="h6 fw-bold">{KATEGORIE_LABEL[key] || key}</h3>
                                        {key === "technologien" && wert && typeof wert === "object" && !Array.isArray(wert) ? (
                                            Object.entries(wert).map(([unterkey, liste]) => (
                                                Array.isArray(liste) && liste.length > 0 && (
                                                    <div className="mb-2" key={unterkey}>
                                                        <small className="text-muted d-block">{TECH_LABEL[unterkey] || unterkey}</small>
                                                        <StichwortBadges items={liste} />
                                                    </div>
                                                )
                                            ))
                                        ) : Array.isArray(wert) ? (
                                            <StichwortBadges items={wert} />
                                        ) : wert?.wert ? (
                                            <span className="badge bg-primary">{wert.wert}</span>
                                        ) : null}
                                    </div>
                                )) : (
                                    <p className="text-muted mb-0">Keine Stichwörter verfügbar.</p>
                                )}
                            </div>
                        </section>
                    </div>
                )}
            </main>
        </div>
    );
}

export default KI;