import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProjektStatusBadge from "../components/ProjektStatus";
import {
    getAuth,
    getProjektById,
    getDokumenteFuerProjekt,
    downloadDokument,
    getStudenten,
    getProjektMitglieder,
    mitgliedHinzufuegen,
    mitgliedEntfernen,
} from "../services/api";
import { generiereProjektZusammenfassung } from "../services/aiService";
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

    const ladeMitglieder = () => {
        getProjektMitglieder(id)
            .then((data) => setMitglieder(data || []))
            .catch(() => setMitglieder([]));
    };

    useEffect(() => {
        if (!id) return;

        setFehler("");
        getProjektById(id)
            .then(setProjekt)
            .catch(() => setFehler("Projekt konnte nicht geladen werden."));

        getDokumenteFuerProjekt(id)
            .then((data) => setDokumente(data || []))
            .catch(() => setDokumente([]));

        ladeMitglieder();

        getStudenten()
            .then((data) => setAlleStudenten(data || []))
            .catch(() => setAlleStudenten([]));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    const istErsteller = auth?.id && projekt?.student?.id === auth.id;
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

    const handleZusammenfassungErzeugen = async () => {
        setKiLaedt(true);
        setKiFehler("");
        try {
            const antwort = await generiereProjektZusammenfassung(id);
            setProjekt((prev) => ({ ...prev, kiZusammenfassung: antwort.antwort }));
        } catch (error) {
            setKiFehler(error.message || "Zusammenfassung konnte nicht erzeugt werden.");
        } finally {
            setKiLaedt(false);
        }
    };

    const schlagwoerterListe = projekt?.schlagwoerter
        ? projekt.schlagwoerter.split(",").map((s) => s.trim()).filter(Boolean)
        : [];

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
    const hatSchlagwoerterOderTags = schlagwoerterListe.length > 0 || projekt.tags?.length > 0;

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
                                <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
                                    <ProjektStatusBadge status={projekt.status} />
                                    {istBeispiel && <span className="pd-beispiel">Beispielprojekt</span>}
                                </div>
                                <h1 className="pd-titel">{projekt.titel}</h1>
                                <div className="pd-meta">
                                    {[projekt.projektart, projekt.fachbereich, projekt.semester]
                                        .filter(Boolean)
                                        .join(" · ") || "Keine weiteren Angaben"}
                                </div>
                            </div>

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
                        </div>
                    </div>
                </section>

                <div className="row g-4">

                    {/* Linke Spalte */}
                    <div className="col-lg-8 d-flex flex-column gap-4">

                        <div className="card">
                            <div className="card-body">
                                <h4 className="pd-abschnitt">Projektbeschreibung</h4>
                                <p className="pd-text mb-0">
                                    {projekt.beschreibung || "Keine Beschreibung hinterlegt."}
                                </p>
                            </div>
                        </div>

                        <div className="card pd-ki">
                            <div className="card-body">
                                <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                                    <h4 className="pd-abschnitt mb-0">
                                        <span className="pd-ki-symbol">✦</span> KI-Zusammenfassung
                                    </h4>

                                    <button
                                        className="btn btn-sm btn-outline-primary"
                                        onClick={handleZusammenfassungErzeugen}
                                        disabled={kiLaedt}
                                    >
                                        {kiLaedt
                                            ? "Wird verarbeitet..."
                                            : projekt.kiZusammenfassung
                                                ? "Neu erzeugen"
                                                : "Zusammenfassung erzeugen"}
                                    </button>
                                </div>

                                {kiFehler && <div className="alert alert-warning py-2 mb-2">{kiFehler}</div>}

                                {kiLaedt ? (
                                    <p className="text-muted mb-0">Die KI liest die Projektunterlagen...</p>
                                ) : projekt.kiZusammenfassung ? (
                                    <p className="pd-text mb-0">{projekt.kiZusammenfassung}</p>
                                ) : (
                                    <p className="text-muted mb-0">
                                        Noch keine Zusammenfassung erzeugt. Die KI fasst Beschreibung und
                                        Dokumente des Projekts in wenigen Sätzen zusammen.
                                    </p>
                                )}
                            </div>
                        </div>

                        <div className="card">
                            <div className="card-body">
                                <h4 className="pd-abschnitt">Dokumente</h4>

                                {dokumente.length > 0 ? (
                                    <div className="d-flex flex-column gap-2">
                                        {dokumente.map((dok) => (
                                            <div className="pd-dokument" key={dok.id}>
                                                <span className="pd-dokument-symbol">PDF</span>

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
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="text-muted mb-0">Noch keine Dokumente hochgeladen.</p>
                                )}
                            </div>
                        </div>

                        {/* Schlagwoerter/Tags nur anzeigen, wenn vorhanden */}
                        {hatSchlagwoerterOderTags && (
                            <div className="card">
                                <div className="card-body">
                                    <h4 className="pd-abschnitt">Schlagwörter & Tags</h4>
                                    {schlagwoerterListe.map((wort) => (
                                        <span className="badge bg-primary me-2 mb-1" key={wort}>
                                            {wort}
                                        </span>
                                    ))}
                                    {projekt.tags?.map((tag) => (
                                        <span className="badge bg-info me-2 mb-1" key={tag.id}>
                                            {tag.name}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Rechte Spalte */}
                    <div className="col-lg-4 d-flex flex-column gap-4">

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

                    </div>
                </div>
            </div>
        </div>
    );
}

export default ProjectDetails;