import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProjektStatusBadge from "../components/ProjektStatus";
import {
    getAuth,
    getProjektDetails,
    aktualisiereProjekt,
    getDokumenteFuerProjekt,
    downloadDokument,
    uploadDokument,
    loescheDokument,
    getStudenten,
    getProjektMitglieder,
    mitgliedHinzufuegen,
    mitgliedEntfernen,
} from "../services/api";
import { getKiUebersicht } from "../services/aiService";
import "./ProjectDetails.css";

// Systemkonto der Beispielprojekte (siehe BeispielprojekteInitializer im Backend)
const BEISPIEL_KONTO = "sophub.beispielprojekte";

// Anfangsbuchstaben fuer die runden Namens-Symbole, z.B. "Max Mustermann" -> "MM"
const initialen = (vorname, name) =>
    `${(vorname || "").charAt(0)}${(name || "").charAt(0)}`.toUpperCase() || "?";

const DOKUMENT_TYP_LABEL = {
    PFLICHTENHEFT: "Pflichtenheft",
    LASTENHEFT: "Lastenheft",
    DATENBANKMODELL: "Datenbankmodell",
    DOKUMENTATION: "Dokumentation",
    SONSTIGES: "Sonstiges",
};

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
    return (
        <div className="d-flex flex-wrap gap-2">
            {items.map((item, index) => (
                <span className="badge bg-primary" key={`${item.wert || item}-${index}`}>
                    {typeof item === "string" ? item : item.wert}
                </span>
            ))}
        </div>
    );
}

function ProjectDetails() {
    const { id } = useParams();

    const auth = getAuth();

    const [projekt, setProjekt] = useState(null);
    const [dokumente, setDokumente] = useState([]);
    const [fehler, setFehler] = useState("");
    const [mitglieder, setMitglieder] = useState([]);
    const [alleStudenten, setAlleStudenten] = useState([]);
    const [suche, setSuche] = useState("");
    const [mitgliedFehler, setMitgliedFehler] = useState("");
    const [kiLaedt, setKiLaedt] = useState(false);
    const [kiFehler, setKiFehler] = useState("");
    const [kiUebersicht, setKiUebersicht] = useState(null);
    const [bearbeitung, setBearbeitung] = useState(false);
    const [speichertProjekt, setSpeichertProjekt] = useState(false);
    const [projektForm, setProjektForm] = useState({ titel: "", beschreibung: "" });
    const [projektFehler, setProjektFehler] = useState("");
    const [datei, setDatei] = useState(null);
    const [dateiTyp, setDateiTyp] = useState("DOKUMENTATION");
    const [dokumentFehler, setDokumentFehler] = useState("");
    const [dokumentLaedt, setDokumentLaedt] = useState(false);

    const ladeMitglieder = () => {
        getProjektMitglieder(id)
            .then((data) => setMitglieder(data || []))
            .catch(() => setMitglieder([]));
    };

    useEffect(() => {
        if (!id) return;

        setFehler("");
        setProjekt(null);
        getProjektDetails(id)
            .then((daten) => {
                setProjekt(daten);
                setProjektForm({ titel: daten.titel || "", beschreibung: daten.beschreibung || "" });

                if (daten.istOwner || daten.istTeammitglied) {
                    getDokumenteFuerProjekt(id)
                        .then((liste) => setDokumente(liste || []))
                        .catch(() => setDokumente([]));
                    ladeMitglieder();
                } else {
                    setDokumente([]);
                    setMitglieder([]);
                }

                if (daten.istOwner) {
                    getStudenten()
                        .then((liste) => setAlleStudenten(liste || []))
                        .catch(() => setAlleStudenten([]));
                } else {
                    setAlleStudenten([]);
                }
            })
            .catch(() => setFehler("Projekt konnte nicht geladen werden."));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    useEffect(() => {
        if (!id) return;

        let aktiv = true;
        setKiUebersicht(null);
        setKiLaedt(true);
        setKiFehler("");

        getKiUebersicht(id)
            .then((daten) => {
                if (aktiv) setKiUebersicht(daten);
            })
            .catch((error) => {
                if (aktiv) setKiFehler(error.message || "Zusammenfassung konnte nicht geladen werden.");
            })
            .finally(() => {
                if (aktiv) setKiLaedt(false);
            });

        return () => {
            aktiv = false;
        };
    }, [id]);

    const istErsteller = Boolean(projekt?.istOwner);
    const istTeammitglied = Boolean(projekt?.istTeammitglied);
    const darfBearbeiten = istErsteller || istTeammitglied;
    const aktuelleGroesse = 1 + mitglieder.length;
    const maxErreicht = projekt?.gruppenanzahl != null && aktuelleGroesse >= projekt.gruppenanzahl;

    const gefilterteStudenten = suche.trim()
        ? alleStudenten.filter((s) => {
            const vollerName = `${s.vorname} ${s.name}`.toLowerCase();
            return (
                vollerName.includes(suche.toLowerCase()) &&
                s.id !== projekt?.student?.id &&
                !mitglieder.some((m) => m.studentId === s.id)
            );
        })
        : [];

    const handleHinzufuegen = async (studentId) => {
        setMitgliedFehler("");
        try {
            await mitgliedHinzufuegen(id, studentId);
            setSuche("");
            ladeMitglieder();
        } catch (error) {
            setMitgliedFehler(error.message || "Teammitglied konnte nicht hinzugefügt werden.");
        }
    };

    const handleEntfernen = async (studentId) => {
        try {
            await mitgliedEntfernen(id, studentId);
            ladeMitglieder();
        } catch {
            setMitgliedFehler("Teammitglied konnte nicht entfernt werden.");
        }
    };

    const speichereProjekt = async (event) => {
        event.preventDefault();
        setProjektFehler("");
        setSpeichertProjekt(true);
        try {
            const aktualisiert = await aktualisiereProjekt(id, projektForm);
            setProjekt((vorher) => ({ ...vorher, ...aktualisiert }));
            setBearbeitung(false);
            if (dokumente.length > 0) {
                setKiLaedt(true);
                try {
                    setKiUebersicht(await getKiUebersicht(id, true));
                } catch (error) {
                    setKiFehler(error.message || "KI-Übersicht konnte nicht aktualisiert werden.");
                } finally {
                    setKiLaedt(false);
                }
            }
        } catch (error) {
            setProjektFehler(error.message || "Projekt konnte nicht gespeichert werden.");
        } finally {
            setSpeichertProjekt(false);
        }
    };

    const dokumentHochladen = async (event) => {
        event.preventDefault();
        if (!datei) return;
        const form = event.currentTarget;

        setDokumentFehler("");
        setDokumentLaedt(true);
        try {
            const formData = new FormData();
            formData.append("datei", datei);
            formData.append("benutzerId", auth.id);
            formData.append("projektId", id);
            formData.append("typ", dateiTyp);
            await uploadDokument(formData);
            setDatei(null);
            form.reset();
            const liste = await getDokumenteFuerProjekt(id);
            setDokumente(liste || []);
            setKiLaedt(true);
            try {
                setKiUebersicht(await getKiUebersicht(id));
                setKiFehler("");
            } catch (error) {
                setKiFehler(error.message || "KI-Übersicht konnte nicht aktualisiert werden.");
            } finally {
                setKiLaedt(false);
            }
        } catch (error) {
            setDokumentFehler(error.message || "Dokument konnte nicht hinzugefügt werden.");
        } finally {
            setDokumentLaedt(false);
        }
    };

    const dokumentLoeschen = async (dokumentId) => {
        setDokumentFehler("");
        setDokumentLaedt(true);
        try {
            await loescheDokument(dokumentId);
            const liste = await getDokumenteFuerProjekt(id);
            setDokumente(liste || []);
            if (liste.length > 0) {
                setKiLaedt(true);
                try {
                    setKiUebersicht(await getKiUebersicht(id));
                    setKiFehler("");
                } catch (error) {
                    setKiFehler(error.message || "KI-Übersicht konnte nicht aktualisiert werden.");
                } finally {
                    setKiLaedt(false);
                }
            } else {
                setKiUebersicht(null);
            }
        } catch (error) {
            setDokumentFehler(error.message || "Dokument konnte nicht gelöscht werden.");
        } finally {
            setDokumentLaedt(false);
        }
    };

    const schlagwoerterListe = projekt?.schlagwoerter
        ? projekt.schlagwoerter.split(",").map((s) => s.trim()).filter(Boolean)
        : [];
    const kategorien = kiUebersicht?.stichwoerter?.kategorien || {};
    const kategorienEintraege = Object.entries(kategorien).filter(([, wert]) => {
        if (Array.isArray(wert)) return wert.length > 0;
        if (!wert || typeof wert !== "object") return false;
        return Boolean(wert.wert) || Object.values(wert).some((liste) => Array.isArray(liste) && liste.length > 0);
    });
    const schwierigkeitsstufe = kiUebersicht?.stichwoerter?.schwierigkeitsanalyse?.stufe;
    const projektZusammenfassung = kiUebersicht?.projektZusammenfassung;
    const zusammenfassung = typeof projektZusammenfassung === "string"
        ? projektZusammenfassung
        : projektZusammenfassung?.kurzfassung || projektZusammenfassung?.einleitung;

    // Einfacher Rahmen fuer die Sonderfaelle (kein Projekt gewaehlt, Fehler, Laden)
    const Hinweis = ({ art, text }) => (
        <div>
            <Navbar />
            <div className="container py-4">
                <div className={`alert alert-${art}`}>{text}</div>
                <Link to="/projects" className="btn btn-outline-secondary">← Zu allen Projekten</Link>
            </div>
        </div>
    );

    if (!id) {
        return <Hinweis art="info" text='Bitte ein Projekt aus "Meine Projekte" oder "Alle Projekte" auswählen.' />;
    }

    if (fehler) {
        return <Hinweis art="danger" text={fehler} />;
    }

    if (!projekt) {
        return (
            <div>
                <Navbar />
                <div className="container py-4">
                    <p className="text-muted">Projekt wird geladen...</p>
                </div>
            </div>
        );
    }

    const istBeispiel = projekt.student?.benutzername === BEISPIEL_KONTO;
    const hatSchlagwoerterOderTags = kategorienEintraege.length > 0
        || schlagwoerterListe.length > 0
        || projekt.tags?.length > 0;

    return (
        <div>
            <Navbar />

            <div className="container py-4">

                <Link to="/projects" className="pd-zurueck">← Alle Projekte</Link>

                {/* Kopfbereich */}
                <section className="card pd-kopf mb-4">
                    <div className="card-body">
                        <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-start gap-3">
                            <div>
                                {darfBearbeiten && (
                                    <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
                                        <ProjektStatusBadge status={projekt.status} />
                                        {istBeispiel && <span className="pd-beispiel">Beispielprojekt</span>}
                                    </div>
                                )}
                                <h1 className="pd-titel">{projekt.titel}</h1>
                                {darfBearbeiten && (
                                    <div className="pd-meta">
                                        {[projekt.projektart, projekt.fachbereich, projekt.semester]
                                            .filter(Boolean)
                                            .join(" · ") || "Keine weiteren Angaben"}
                                    </div>
                                )}
                            </div>

                            {darfBearbeiten && (
                                <div className="pd-kennzahlen">
                                    <div>
                                        <span className="pd-kennzahl">{dokumente.length}</span>
                                        <span className="pd-kennzahl-text">Dokumente</span>
                                    </div>
                                    <div>
                                        <span className="pd-kennzahl">
                                            {aktuelleGroesse}
                                            {projekt.gruppenanzahl != null && <small>/{projekt.gruppenanzahl}</small>}
                                        </span>
                                        <span className="pd-kennzahl-text">Teammitglieder</span>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </section>

                <div className="row g-4">

                    {/* Linke Spalte */}
                    <div className="col-lg-8 d-flex flex-column gap-4">

                        <div className="card">
                            <div className="card-body">
                                <div className="d-flex justify-content-between align-items-center gap-2 mb-3">
                                    <h4 className="pd-abschnitt mb-0">Projektbeschreibung</h4>
                                    {darfBearbeiten && !bearbeitung && (
                                        <button className="btn btn-sm btn-outline-primary" onClick={() => setBearbeitung(true)}>
                                            Bearbeiten
                                        </button>
                                    )}
                                </div>
                                {bearbeitung ? (
                                    <form onSubmit={speichereProjekt}>
                                        {projektFehler && <div className="alert alert-danger py-2">{projektFehler}</div>}
                                        <label className="form-label" htmlFor="pd-projekt-titel">Projekttitel</label>
                                        <input
                                            id="pd-projekt-titel"
                                            className="form-control mb-3"
                                            value={projektForm.titel}
                                            onChange={(event) => setProjektForm({ ...projektForm, titel: event.target.value })}
                                            required
                                        />
                                        <label className="form-label" htmlFor="pd-projekt-beschreibung">Beschreibung</label>
                                        <textarea
                                            id="pd-projekt-beschreibung"
                                            className="form-control mb-3"
                                            rows="4"
                                            value={projektForm.beschreibung}
                                            onChange={(event) => setProjektForm({ ...projektForm, beschreibung: event.target.value })}
                                        />
                                        <div className="d-flex gap-2">
                                            <button className="btn btn-primary" type="submit" disabled={speichertProjekt}>
                                                {speichertProjekt ? "Speichert..." : "Speichern"}
                                            </button>
                                            <button className="btn btn-outline-secondary" type="button" onClick={() => {
                                                setProjektForm({ titel: projekt.titel || "", beschreibung: projekt.beschreibung || "" });
                                                setBearbeitung(false);
                                                setProjektFehler("");
                                            }}>
                                                Abbrechen
                                            </button>
                                        </div>
                                    </form>
                                ) : (
                                    <p className="pd-text mb-0">
                                        {projekt.beschreibung || "Keine Beschreibung hinterlegt."}
                                    </p>
                                )}
                            </div>
                        </div>

                        <div className={`card pd-ki ${kiLaedt ? "pd-ki-laedt" : ""}`}>
                            <div className="card-body">
                                <div className="mb-3">
                                    <h4 className="pd-abschnitt mb-0">
                                        <span className="pd-ki-symbol">✦</span> KI-Zusammenfassung
                                    </h4>
                                </div>

                                {kiFehler && <div className="alert alert-warning py-2 mb-2">{kiFehler}</div>}

                                {kiLaedt ? (
                                    <p className="text-muted mb-0">Die KI liest die Projektunterlagen...</p>
                                ) : projektZusammenfassung && typeof projektZusammenfassung === "object" ? (
                                    <>
                                        {projektZusammenfassung.kurzfassung && (
                                            <p className="pd-text fw-semibold">{projektZusammenfassung.kurzfassung}</p>
                                        )}
                                        {projektZusammenfassung.einleitung && (
                                            <p className="pd-text">{projektZusammenfassung.einleitung}</p>
                                        )}
                                        {Array.isArray(projektZusammenfassung.hauptteil) && projektZusammenfassung.hauptteil.map((abschnitt, index) => (
                                            <section className="mb-3" key={abschnitt.schluessel || index}>
                                                {abschnitt.titel && <h5 className="h6 fw-bold">{abschnitt.titel}</h5>}
                                                {abschnitt.inhalt && <p className="pd-text">{abschnitt.inhalt}</p>}
                                                {Array.isArray(abschnitt.stichpunkte) && abschnitt.stichpunkte.length > 0 && (
                                                    <ul className="mb-0">
                                                        {abschnitt.stichpunkte.map((punkt, punktIndex) => (
                                                            <li key={punktIndex}>{punkt}</li>
                                                        ))}
                                                    </ul>
                                                )}
                                            </section>
                                        ))}
                                        {projektZusammenfassung.schluss?.fazit && (
                                            <p className="pd-text mb-0">{projektZusammenfassung.schluss.fazit}</p>
                                        )}
                                    </>
                                ) : zusammenfassung ? (
                                    <p className="pd-text mb-0">{zusammenfassung}</p>
                                ) : (
                                    <p className="text-muted mb-0">
                                        Noch keine Zusammenfassung verfügbar. Die KI erstellt sie aus den
                                        Projektunterlagen.
                                    </p>
                                )}
                            </div>
                        </div>

                        {darfBearbeiten && (
                            <div className="card">
                                <div className="card-body">
                                    <h4 className="pd-abschnitt">Dokumente</h4>
                                    {dokumentFehler && <div className="alert alert-warning py-2">{dokumentFehler}</div>}

                                    {dokumente.length > 0 ? (
                                        <div className="d-flex flex-column gap-2 mb-3">
                                            {dokumente.map((dok) => (
                                                <div className="pd-dokument" key={dok.id}>
                                                    <span className="pd-dokument-symbol">
                                                        {dok.dateiName?.split(".").pop()?.toUpperCase() || "DOC"}
                                                    </span>

                                                    <div className="flex-grow-1 min-w-0">
                                                        <div className="pd-dokument-name text-truncate" title={dok.dateiName}>
                                                            {dok.dateiName}
                                                        </div>
                                                        <span className="badge bg-secondary">
                                                            {DOKUMENT_TYP_LABEL[dok.typ] || dok.typ}
                                                        </span>
                                                    </div>

                                                    <button
                                                        className="btn btn-sm btn-outline-primary flex-shrink-0"
                                                        onClick={() => downloadDokument(dok.id, dok.dateiName)}
                                                    >
                                                        Herunterladen
                                                    </button>
                                                    <button
                                                        className="btn btn-sm btn-outline-danger flex-shrink-0"
                                                        onClick={() => {
                                                            if (window.confirm(`Dokument „${dok.dateiName}“ wirklich löschen?`)) {
                                                                dokumentLoeschen(dok.id);
                                                            }
                                                        }}
                                                        disabled={dokumentLaedt}
                                                    >
                                                        Löschen
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    ) : (
                                        <p className="text-muted">Noch keine Dokumente hochgeladen.</p>
                                    )}

                                    <form className="row g-2 align-items-end" onSubmit={dokumentHochladen}>
                                        <div className="col-md-5">
                                            <label className="form-label" htmlFor="pd-dokument">Datei hinzufügen</label>
                                            <input
                                                id="pd-dokument"
                                                className="form-control"
                                                type="file"
                                                accept=".pdf,.docx,.txt"
                                                onChange={(event) => setDatei(event.target.files?.[0] || null)}
                                                required
                                            />
                                        </div>
                                        <div className="col-md-4">
                                            <label className="form-label" htmlFor="pd-dokument-typ">Dokumenttyp</label>
                                            <select
                                                id="pd-dokument-typ"
                                                className="form-select"
                                                value={dateiTyp}
                                                onChange={(event) => setDateiTyp(event.target.value)}
                                            >
                                                {Object.entries(DOKUMENT_TYP_LABEL).map(([wert, label]) => (
                                                    <option key={wert} value={wert}>{label}</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div className="col-md-3">
                                            <button className="btn btn-primary w-100" type="submit" disabled={dokumentLaedt || !datei}>
                                                {dokumentLaedt ? "Verarbeitet..." : "Hinzufügen"}
                                            </button>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        )}

                        {/* Schlagwoerter/Tags nur anzeigen, wenn vorhanden */}
                        {hatSchlagwoerterOderTags && (
                            <div className={`card ${kiLaedt ? "pd-ki-laedt" : ""}`}>
                                <div className="card-body">
                                    <h4 className="pd-abschnitt">Schlagwörter & Tags</h4>
                                    {kategorienEintraege.length > 0 && (
                                        <>
                                            {schwierigkeitsstufe && (
                                                <p className="mb-3">
                                                    <strong>Schwierigkeit:</strong> {schwierigkeitsstufe}
                                                </p>
                                            )}
                                            {kategorienEintraege.map(([key, wert]) => (
                                                <details className="mb-2" key={key}>
                                                    <summary className="h6 fw-bold">{KATEGORIE_LABEL[key] || key}</summary>
                                                    <div className="pt-2">
                                                        {key === "technologien" && wert && typeof wert === "object" && !Array.isArray(wert) ? (
                                                            Object.entries(wert).map(([unterkey, liste]) => (
                                                                Array.isArray(liste) && liste.length > 0 && (
                                                                    <div className="mb-2" key={unterkey}>
                                                                        <small className="text-muted d-block mb-1">{TECH_LABEL[unterkey] || unterkey}</small>
                                                                        <StichwortBadges items={liste} />
                                                                    </div>
                                                                )
                                                            ))
                                                        ) : Array.isArray(wert) ? (
                                                            <StichwortBadges items={wert} />
                                                        ) : wert?.wert ? (
                                                            <StichwortBadges items={[wert]} />
                                                        ) : null}
                                                    </div>
                                                </details>
                                            ))}
                                        </>
                                    )}
                                    {(schlagwoerterListe.length > 0 || projekt.tags?.length > 0) && (
                                        <div className={kategorienEintraege.length > 0 ? "mt-3 pt-3 border-top" : ""}>
                                            {kategorienEintraege.length > 0 && <h5 className="h6 fw-bold">Projekt-Tags</h5>}
                                            {schlagwoerterListe.map((wort) => (
                                                <span className="badge bg-info me-2 mb-1" key={wort}>
                                                    {wort}
                                                </span>
                                            ))}
                                            {projekt.tags?.map((tag) => (
                                                <span className="badge bg-info me-2 mb-1" key={tag.id}>
                                                    {tag.name}
                                                </span>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Rechte Spalte */}
                    <div className="col-lg-4 d-flex flex-column gap-4">
                        {darfBearbeiten && (
                            <>
                        <div className="card">
                            <div className="card-body">
                                <h5 className="pd-abschnitt">Projektinformationen</h5>

                                <dl className="pd-infos mb-0">
                                    <dt>Status</dt>
                                    <dd><ProjektStatusBadge status={projekt.status} /></dd>

                                    <dt>Semester</dt>
                                    <dd>{projekt.semester || "–"}</dd>

                                    <dt>Fachbereich</dt>
                                    <dd>{projekt.fachbereich || "–"}</dd>

                                    <dt>Projektart</dt>
                                    <dd>{projekt.projektart || "–"}</dd>

                                    <dt>Gruppengröße</dt>
                                    <dd>{projekt.gruppenanzahl != null ? `${projekt.gruppenanzahl} Personen` : "–"}</dd>
                                </dl>
                            </div>
                        </div>

                        <div className="card">
                            <div className="card-body">
                                <div className="d-flex justify-content-between align-items-center mb-3">
                                    <h5 className="pd-abschnitt mb-0">Team</h5>

                                    {projekt.gruppenanzahl != null && (
                                        <span className={`badge ${maxErreicht ? "bg-secondary" : "bg-primary"}`}>
                                            {aktuelleGroesse} / {projekt.gruppenanzahl}
                                        </span>
                                    )}
                                </div>

                                <div className="pd-person">
                                    <span className="pd-avatar">
                                        {initialen(projekt.student?.vorname, projekt.student?.name)}
                                    </span>
                                    <div>
                                        <div className="pd-person-name">
                                            {projekt.student
                                                ? `${projekt.student.vorname} ${projekt.student.name}`
                                                : "–"}
                                        </div>
                                        <div className="pd-person-rolle">Ersteller:in</div>
                                    </div>
                                </div>

                                <div className="pd-person">
                                    <span className={`pd-avatar ${projekt.betreuer ? "pd-avatar-betreuer" : "pd-avatar-leer"}`}>
                                        {projekt.betreuer
                                            ? initialen(projekt.betreuer.vorname, projekt.betreuer.name)
                                            : "?"}
                                    </span>
                                    <div>
                                        <div className="pd-person-name">
                                            {projekt.betreuer
                                                ? `${projekt.betreuer.vorname} ${projekt.betreuer.name}`
                                                : "Noch nicht zugewiesen"}
                                        </div>
                                        <div className="pd-person-rolle">Betreuer:in</div>
                                    </div>
                                </div>

                                {mitglieder.map((m) => (
                                    <div className="pd-person" key={m.studentId}>
                                        <span className="pd-avatar">{initialen(m.vorname, m.name)}</span>
                                        <div className="flex-grow-1">
                                            <div className="pd-person-name">{m.vorname} {m.name}</div>
                                            <div className="pd-person-rolle">Teammitglied</div>
                                        </div>

                                        {istErsteller && (
                                            <button
                                                className="btn btn-sm btn-outline-danger"
                                                onClick={() => handleEntfernen(m.studentId)}
                                                title="Aus dem Team entfernen"
                                            >
                                                ✕
                                            </button>
                                        )}
                                    </div>
                                ))}

                                {mitglieder.length === 0 && (
                                    <p className="text-muted small mt-2 mb-0">Noch keine weiteren Teammitglieder.</p>
                                )}

                                {istErsteller && (
                                    <div className="mt-3 pt-3 border-top">
                                        {mitgliedFehler && (
                                            <div className="alert alert-danger py-2">{mitgliedFehler}</div>
                                        )}

                                        {maxErreicht ? (
                                            <p className="text-muted small mb-0">
                                                Maximale Gruppengröße erreicht ({aktuelleGroesse}/{projekt.gruppenanzahl}).
                                            </p>
                                        ) : (
                                            <>
                                                <label className="form-label" htmlFor="pd-team-suche">
                                                    Studierende hinzufügen
                                                </label>
                                                <input
                                                    id="pd-team-suche"
                                                    type="text"
                                                    className="form-control mb-2"
                                                    placeholder="Name suchen..."
                                                    value={suche}
                                                    onChange={(e) => setSuche(e.target.value)}
                                                />

                                                {gefilterteStudenten.length > 0 && (
                                                    <ul className="list-group">
                                                        {gefilterteStudenten.map((s) => (
                                                            <li
                                                                className="list-group-item d-flex justify-content-between align-items-center"
                                                                key={s.id}
                                                            >
                                                                {s.vorname} {s.name}

                                                                <button
                                                                    className="btn btn-sm btn-outline-primary"
                                                                    onClick={() => handleHinzufuegen(s.id)}
                                                                >
                                                                    + Hinzufügen
                                                                </button>
                                                            </li>
                                                        ))}
                                                    </ul>
                                                )}
                                            </>
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default ProjectDetails;