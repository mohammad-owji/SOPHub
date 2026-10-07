import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProjektStatusBadge from "../components/ProjektStatus";
import {
    acceptProject,
    declineProject,
    getAuth,
    getProjektById
} from "../services/api";
import "./Einladungen.css";


function ProjectInvitation() {

    const { projektId } = useParams();
    const navigate = useNavigate();

    // Eingeloggter Benutzer (wird beim Login in localStorage gespeichert)
    const auth = getAuth();

    const [projekt, setProjekt] = useState(null);
    const [decision, setDecision] = useState("");
    const [error, setError] = useState("");


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
        } catch (err) {
            console.error(err);
            setError(err.message);
        }
    };


    const handleDecline = async () => {
        setError("");
        try {
            const updatedProjekt = await declineProject(projektId, auth.id);
            setProjekt(updatedProjekt);
            setDecision("declined");
        } catch (err) {
            console.error(err);
            setError(err.message);
        }
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
                        {projekt.status === "OFFEN" && !decision && istEingeladenerBetreuer && (
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
                                        onClick={handleDecline}
                                    >
                                        ✕ Projekt ablehnen
                                    </button>
                                </div>
                                <div className="form-text mt-3">
                                    Der/die Student:in wird über Ihre Entscheidung per E-Mail informiert.
                                </div>
                            </div>
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
                                Sie haben das Projekt abgelehnt.
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