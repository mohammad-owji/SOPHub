import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProjektStatusBadge from "../components/ProjektStatus";
import { getAuth, getMeineEinladungen } from "../services/api";
import "./Einladungen.css";

function MyInvitations() {

    const navigate = useNavigate();
    const auth = getAuth();

    const [einladungen, setEinladungen] = useState([]);
    const [laedt, setLaedt] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {

        // Nicht eingeloggt? Dann zum Login und danach wieder hierher zurueck.
        if (!auth?.token) {
            navigate("/?redirect=/einladungen");
            return;
        }

        getMeineEinladungen()
            .then((data) => setEinladungen(data || []))
            .catch((err) => {
                console.error(err);
                setError("Einladungen konnten nicht geladen werden.");
            })
            .finally(() => setLaedt(false));

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Offene Anfragen oben, bereits entschiedene darunter
    const offene = einladungen.filter((p) => p.status === "OFFEN");
    const erledigte = einladungen.filter((p) => p.status !== "OFFEN");

    if (!auth?.token) {
        return null;
    }

    const einladungsKarte = (projekt) => {
        const istOffen = projekt.status === "OFFEN";
        // Nach der Annahme kann die Betreuung die Mitteilungen des Projekts lesen und beantworten
        const istAngenommen = ["ANGENOMMEN", "ABGESCHLOSSEN"].includes((projekt.status || "").toUpperCase());

        return (
            <div className="col-md-6 col-xl-4" key={projekt.id}>
                <div className={`card einl-karte ${istOffen ? "einl-karte-offen" : ""}`}>
                    <div className="card-body">

                        <div className="d-flex justify-content-between align-items-start gap-2">
                            <h3 className="einl-titel">{projekt.titel}</h3>
                            <ProjektStatusBadge status={projekt.status} className="flex-shrink-0" />
                        </div>

                        <div className="einl-von">
                            Eingeladen von{" "}
                            <strong>
                                {projekt.student
                                    ? `${projekt.student.vorname} ${projekt.student.name}`
                                    : "–"}
                            </strong>
                        </div>

                        <div className="einl-meta">
                            <span><strong>Fachbereich:</strong> {projekt.fachbereich || "–"}</span>
                            <span><strong>Projektart:</strong> {projekt.projektart || "–"}</span>
                            <span><strong>Semester:</strong> {projekt.semester || "–"}</span>
                        </div>

                        <div className="mt-auto pt-2 d-flex flex-column gap-2">
                            {istAngenommen && (
                                <button
                                    className="btn btn-primary w-100"
                                    onClick={() => navigate(`/projectdetails/${projekt.id}?reiter=mitteilungen`)}
                                >
                                    Mitteilungen öffnen
                                </button>
                            )}
                            <button
                                className={`btn w-100 ${istOffen ? "btn-primary" : "btn-outline-secondary"}`}
                                onClick={() => navigate(`/project-invitation/${projekt.id}`)}
                            >
                                {istOffen ? "Anfrage öffnen" : "Details ansehen"}
                            </button>
                        </div>

                    </div>
                </div>
            </div>
        );
    };

    return (
        <div>
            <Navbar />

            <div className="container py-4">

                <div className="mb-4">
                    <h2 className="mb-1">Meine Einladungen</h2>
                    <p className="text-muted mb-0">
                        Projektanfragen von Studierenden, die Sie als Betreuer:in angefragt haben.
                    </p>
                </div>

                {laedt && <p className="text-muted">Einladungen werden geladen...</p>}

                {error && <div className="alert alert-danger">{error}</div>}

                {!laedt && !error && einladungen.length === 0 && (
                    <div className="card">
                        <div className="einl-leer">
                            <div className="einl-leer-symbol">✉</div>
                            <h4>Keine Projektanfragen</h4>
                            <p className="text-muted mb-0">
                                Sie haben noch keine Projektanfragen erhalten. Sobald Studierende Sie
                                als Betreuer:in anfragen, erscheinen die Anfragen hier.
                            </p>
                        </div>
                    </div>
                )}

                {offene.length > 0 && (
                    <section className="mb-5">
                        <h4 className="einl-abschnitt">
                            Offene Anfragen <span className="einl-zaehler">{offene.length}</span>
                        </h4>
                        <div className="row g-4">{offene.map(einladungsKarte)}</div>
                    </section>
                )}

                {erledigte.length > 0 && (
                    <section>
                        <h4 className="einl-abschnitt">Bereits entschieden</h4>
                        <div className="row g-4">{erledigte.map(einladungsKarte)}</div>
                    </section>
                )}

            </div>
        </div>
    );
}

export default MyInvitations;