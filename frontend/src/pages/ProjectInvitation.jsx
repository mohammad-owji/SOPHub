import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import Navbar, { EINLADUNGEN_GEAENDERT } from "../components/Navbar";
import ProjektStatusBadge from "../components/ProjektStatus";
import {
    acceptProject,
    getAuth,
    getProjektById
} from "../services/api";
import { ABLEHNUNGS_GRUENDE, lehneProjektAb } from "../services/betreuungService";
import "./Einladungen.css";


function ProjectInvitation() {

    const { projektId } = useParams();
    const navigate = useNavigate();

    // Eingeloggter Benutzer (wird beim Login in localStorage gespeichert)
    const auth = getAuth();

    const [projekt, setProjekt] = useState(null);
    const [decision, setDecision] = useState("");
    const [error, setError] = useState("");
    // Ablehnung: erst Grund auswaehlen (Radio-Buttons) und optional erklaeren
    const [ablehnenOffen, setAblehnenOffen] = useState(false);
    const [grund, setGrund] = useState("");
    const [kommentar, setKommentar] = useState("");
    const [sendet, setSendet] = useState(false);


    useEffect(() => {

        // Nicht eingeloggt? Dann zur Login-Seite und danach wieder hierher zurueck.
        if (!auth?.token) {
            navigate(`/?redirect=/project-invitation/${projektId}`);
            return;
        }

        async function loadProject() {
            try {
                const data = await getProjektById(projektId);
                setProjekt(data);
            } catch (err) {
                console.error(err);
                setError("Projekt konnte nicht geladen werden.");
            }
        }

        loadProject();

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projektId]);


    const handleAccept = async () => {
        setError("");
        try {
            const updatedProjekt = await acceptProject(projektId, auth.id);
            setProjekt(updatedProjekt);
            setDecision("accepted");
            // Zahl der offenen Einladungen in der Navbar aktualisieren
            window.dispatchEvent(new Event(EINLADUNGEN_GEAENDERT));
        } catch (err) {
            console.error(err);
            setError(err.message);
        }
    };


    const kommentarPflicht = grund === "SONSTIGES";

    const handleDecline = async (event) => {
        event.preventDefault();
        setError("");

        if (!grund) {
            setError("Bitte wählen Sie einen Grund für die Ablehnung aus.");
            return;
        }
        if (kommentarPflicht && !kommentar.trim()) {
            setError("Bitte beschreiben Sie den Grund für die Ablehnung.");
            return;
        }

        setSendet(true);
        try {
            const updatedProjekt = await lehneProjektAb(projektId, auth.id, grund, kommentar.trim());
            setProjekt(updatedProjekt);
            setDecision("declined");
            setAblehnenOffen(false);
            window.dispatchEvent(new Event(EINLADUNGEN_GEAENDERT));
        } catch (err) {
            console.error(err);
            setError(err.message);
        } finally {
            setSendet(false);
        }
    };

    const ablehnenAbbrechen = () => {
        setAblehnenOffen(false);
        setGrund("");
        setKommentar("");
        setError("");
    };


    // Waehrend der Weiterleitung zum Login nichts anzeigen
    if (!auth?.token) {
        return null;
    }


    if (!projekt) {
        return (
            <>
                <Navbar />
                <div className="container py-4 anfrage-seite">
                    {error ? (
                        <div className="alert alert-danger text-center">{error}</div>
                    ) : (
                        <p className="text-muted">Projektanfrage wird geladen...</p>
                    )}
                </div>
            </>
        );
    }


    // Ist der eingeloggte Benutzer der eingeladene Betreuer dieses Projekts?
    const istEingeladenerBetreuer =
        projekt.betreuer != null && projekt.betreuer.id === auth.id;


    return (
        <div>
            <Navbar />

            <div className="container py-4 anfrage-seite">

                <Link to="/einladungen" className="einl-zurueck">← Meine Einladungen</Link>

                <div className="card overflow-hidden">

                    {/* Kopf */}
                    <div className="anfrage-kopf">
                        <div className="d-flex align-items-center gap-3">
                            <span className="anfrage-kopf-symbol">✉</span>
                            <div>
                                <h2>Projektanfrage</h2>
                                <div>Sie wurden eingeladen, folgendes Projekt zu betreuen.</div>
                            </div>
                        </div>
                    </div>

                    <div className="card-body p-4 p-md-5">

                        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2">
                            <h3 className="anfrage-titel">{projekt.titel}</h3>
                            <ProjektStatusBadge status={projekt.status} />
                        </div>

                        <p className="text-muted mb-4" style={{ whiteSpace: "pre-line" }}>
                            {projekt.beschreibung || "Keine Beschreibung hinterlegt."}
                        </p>

                        <dl className="anfrage-infos mb-4">
                            <div>
                                <dt>Eingeladen von</dt>
                                <dd>
                                    {projekt.student
                                        ? `${projekt.student.vorname} ${projekt.student.name}`
                                        : "–"}
                                </dd>
                            </div>
                            <div>
                                <dt>Fachbereich</dt>
                                <dd>{projekt.fachbereich || "–"}</dd>
                            </div>
                            <div>
                                <dt>Studiengang</dt>
                                <dd>{projekt.studiengang || "–"}</dd>
                            </div>
                            <div>
                                <dt>Projektart</dt>
                                <dd>{projekt.projektart || "–"}</dd>
                            </div>
                            <div>
                                <dt>Semester</dt>
                                <dd>{projekt.semester || "–"}</dd>
                            </div>
                            <div>
                                <dt>Teamgröße</dt>
                                <dd>{projekt.gruppenanzahl != null ? `${projekt.gruppenanzahl} Personen` : "–"}</dd>
                            </div>
                            <div>
                                <dt>Status</dt>
                                <dd><ProjektStatusBadge status={projekt.status} /></dd>
                            </div>
                        </dl>


                        {/* Buttons nur fuer den eingeladenen Betreuer und nur solange das Projekt OFFEN ist */}
                        {projekt.status === "OFFEN" && !decision && istEingeladenerBetreuer && !ablehnenOffen && (
                            <div className="anfrage-entscheidung">
                                <p>Möchten Sie dieses Projekt betreuen?</p>
                                <div className="d-flex flex-column flex-sm-row justify-content-center gap-3">
                                    <button
                                        className="btn btn-primary px-4"
                                        onClick={handleAccept}
                                    >
                                        ✓ Projekt annehmen
                                    </button>
                                    <button
                                        className="btn btn-outline-danger px-4"
                                        onClick={() => { setError(""); setAblehnenOffen(true); }}
                                    >
                                        ✕ Projekt ablehnen
                                    </button>
                                </div>
                                <div className="form-text mt-3">
                                    Das Team wird über Ihre Entscheidung per E-Mail informiert.
                                </div>
                            </div>
                        )}


                        {/* Ablehnen: Grund auswaehlen und erklaeren */}
                        {projekt.status === "OFFEN" && !decision && istEingeladenerBetreuer && ablehnenOffen && (
                            <form className="anfrage-ablehnung" onSubmit={handleDecline}>
                                <h4 className="anfrage-ablehnung-titel">Warum lehnen Sie die Anfrage ab?</h4>
                                <p className="anfrage-ablehnung-hinweis">
                                    Der Grund wird dem Team auf der Projektseite und per E-Mail angezeigt.
                                </p>

                                <fieldset className="mb-3">
                                    <legend className="visually-hidden">Grund der Ablehnung</legend>
                                    {ABLEHNUNGS_GRUENDE.map((eintrag) => (
                                        <div className="form-check anfrage-grund" key={eintrag.wert}>
                                            <input
                                                className="form-check-input"
                                                type="radio"
                                                name="ablehnungsgrund"
                                                id={`grund-${eintrag.wert}`}
                                                value={eintrag.wert}
                                                checked={grund === eintrag.wert}
                                                onChange={(e) => setGrund(e.target.value)}
                                            />
                                            <label className="form-check-label" htmlFor={`grund-${eintrag.wert}`}>
                                                {eintrag.text}
                                            </label>
                                        </div>
                                    ))}
                                </fieldset>

                                <label className="form-label" htmlFor="ablehnung-kommentar">
                                    Erklärung für das Team{" "}
                                    <span className="text-muted fw-normal">
                                        {kommentarPflicht ? "(Pflicht bei „Sonstiger Grund“)" : "(optional)"}
                                    </span>
                                </label>
                                <textarea
                                    id="ablehnung-kommentar"
                                    className="form-control"
                                    rows="4"
                                    maxLength={1000}
                                    value={kommentar}
                                    onChange={(e) => setKommentar(e.target.value)}
                                    placeholder="z. B. was das Team ändern könnte oder wen es stattdessen anfragen kann"
                                />
                                <div className="form-text text-end">{kommentar.length} / 1000</div>

                                <div className="d-flex flex-column flex-sm-row justify-content-end gap-2 mt-3">
                                    <button type="button" className="btn btn-outline-secondary" onClick={ablehnenAbbrechen} disabled={sendet}>
                                        Abbrechen
                                    </button>
                                    <button type="submit" className="btn btn-danger" disabled={sendet}>
                                        {sendet ? "Wird gesendet..." : "Ablehnung senden"}
                                    </button>
                                </div>
                            </form>
                        )}


                        {/* Eingeloggt, aber nicht der eingeladene Betreuer */}
                        {projekt.status === "OFFEN" && !istEingeladenerBetreuer && (
                            <div className="alert alert-secondary text-center mb-0">
                                Diese Projektanfrage ist an eine andere Person gerichtet.
                                Bitte melden Sie sich mit dem eingeladenen Betreuer-Konto an.
                            </div>
                        )}


                        {decision === "accepted" && (
                            <div className="alert alert-success text-center mb-0">
                                <strong>Vielen Dank!</strong> Sie haben das Projekt angenommen.
                            </div>
                        )}

                        {decision === "declined" && (
                            <div className="alert alert-warning text-center mb-0">
                                Sie haben das Projekt abgelehnt. Das Team wurde mit Ihrem Grund informiert.
                            </div>
                        )}

                        {projekt.status === "ANGENOMMEN" && decision !== "accepted" && (
                            <div className="alert alert-info text-center mb-0">
                                Dieses Projekt wurde bereits angenommen.
                            </div>
                        )}

                        {projekt.status === "ABGELEHNT" && decision !== "declined" && (
                            <div className="alert alert-warning text-center mb-0">
                                Dieses Projekt wurde bereits abgelehnt.
                            </div>
                        )}

                        {/* Gespeicherter Grund der Ablehnung */}
                        {projekt.status === "ABGELEHNT" && projekt.ablehnungsGrund && (
                            <div className="anfrage-grund-anzeige mt-3">
                                <strong>Grund:</strong> {projekt.ablehnungsGrund}
                                {projekt.ablehnungsKommentar && (
                                    <div className="mt-1" style={{ whiteSpace: "pre-line" }}>
                                        <strong>Erklärung:</strong> {projekt.ablehnungsKommentar}
                                    </div>
                                )}
                            </div>
                        )}

                        {error && (
                            <div className="alert alert-danger text-center mt-3 mb-0">
                                {error}
                            </div>
                        )}

                        <div className="d-flex flex-wrap justify-content-center gap-2 mt-4">
                            {/* Nach der Annahme: direkt zu den Mitteilungen des Projekts */}
                            {istEingeladenerBetreuer && (decision === "accepted"
                                || ["ANGENOMMEN", "ABGESCHLOSSEN"].includes((projekt.status || "").toUpperCase())) && (
                                    <Link to={`/projectdetails/${projekt.id}?reiter=mitteilungen`} className="btn btn-primary">
                                        Mitteilungen öffnen
                                    </Link>
                                )}
                            <Link to={`/projectdetails/${projekt.id}`} className="btn btn-outline-secondary">
                                Zur Projektseite
                            </Link>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    );
}


export default ProjectInvitation;