import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import {
    acceptProject,
    declineProject,
    getAuth,
    getProjektById
} from "../services/api";


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
                <div className="container mt-5">
                    {error ? (
                        <div className="alert alert-danger text-center">{error}</div>
                    ) : (
                        "Laden..."
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

            <div className="container mt-5">
                <div
                    className="card shadow-sm border-0 mx-auto"
                    style={{ maxWidth: "750px" }}
                >
                    <div className="card-body p-5">

                        <div className="text-center mb-4">
                            <h2 className="fw-bold">Projektanfrage</h2>
                            <p className="text-muted">
                                Sie wurden eingeladen, folgendes Projekt zu betreuen.
                            </p>
                        </div>

                        <hr />

                        <h3 className="fw-bold">{projekt.titel}</h3>

                        <p className="text-muted">{projekt.beschreibung}</p>

                        <div className="row mt-4">

                            <div className="col-md-6 mb-3">
                                <strong>Eingeladen von</strong>
                                <div>
                                    {projekt.student
                                        ? `${projekt.student.vorname} ${projekt.student.name}`
                                        : "-"}
                                </div>
                            </div>

                            <div className="col-md-6 mb-3">
                                <strong>Fachbereich</strong>
                                <div>{projekt.fachbereich}</div>
                            </div>

                            <div className="col-md-6 mb-3">
                                <strong>Projektart</strong>
                                <div>{projekt.projektart}</div>
                            </div>

                            <div className="col-md-6 mb-3">
                                <strong>Semester</strong>
                                <div>{projekt.semester}</div>
                            </div>

                            <div className="col-md-6 mb-3">
                                <strong>Teamgröße</strong>
                                <div>{projekt.gruppenanzahl} Personen</div>
                            </div>

                            <div className="col-md-6 mb-3">
                                <strong>Status</strong>
                                <div>{projekt.status}</div>
                            </div>

                        </div>


                        {/* Buttons nur fuer den eingeladenen Betreuer und nur solange das Projekt OFFEN ist */}
                        {projekt.status === "OFFEN" && !decision && istEingeladenerBetreuer && (
                            <>
                                <hr />
                                <p className="text-center fw-semibold">
                                    Möchten Sie dieses Projekt betreuen?
                                </p>
                                <div className="d-flex justify-content-center gap-3">
                                    <button
                                        className="btn btn-success px-4"
                                        onClick={handleAccept}
                                    >
                                        Projekt annehmen
                                    </button>
                                    <button
                                        className="btn btn-outline-danger px-4"
                                        onClick={handleDecline}
                                    >
                                        Projekt ablehnen
                                    </button>
                                </div>
                            </>
                        )}


                        {/* Eingeloggt, aber nicht der eingeladene Betreuer */}
                        {projekt.status === "OFFEN" && !istEingeladenerBetreuer && (
                            <div className="alert alert-secondary mt-4 text-center">
                                Diese Projektanfrage ist an eine andere Person gerichtet.
                                Bitte melden Sie sich mit dem eingeladenen Betreuer-Konto an.
                            </div>
                        )}


                        {decision === "accepted" && (
                            <div className="alert alert-success mt-4 text-center">
                                Sie haben das Projekt angenommen.
                            </div>
                        )}

                        {decision === "declined" && (
                            <div className="alert alert-warning mt-4 text-center">
                                Sie haben das Projekt abgelehnt.
                            </div>
                        )}

                        {projekt.status === "ANGENOMMEN" && decision !== "accepted" && (
                            <div className="alert alert-info mt-4 text-center">
                                Dieses Projekt wurde bereits angenommen.
                            </div>
                        )}

                        {projekt.status === "ABGELEHNT" && decision !== "declined" && (
                            <div className="alert alert-warning mt-4 text-center">
                                Dieses Projekt wurde bereits abgelehnt.
                            </div>
                        )}

                        {error && (
                            <div className="alert alert-danger mt-4 text-center">
                                {error}
                            </div>
                        )}

                    </div>
                </div>
            </div>
        </div>
    );
}


export default ProjectInvitation;