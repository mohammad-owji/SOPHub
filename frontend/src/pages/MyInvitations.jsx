import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getAuth, getMeineEinladungen } from "../services/api";

// Farbe des Status-Abzeichens (Bootstrap-Klassen)
const STATUS_FARBE = {
    OFFEN: "bg-warning text-dark",
    ANGENOMMEN: "bg-success",
    ABGELEHNT: "bg-secondary",
};

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

    const einladungsKarte = (projekt) => (
        <div className="col-md-6 mb-3" key={projekt.id}>
            <div className="card shadow-sm border-0 h-100">
                <div className="card-body p-4 d-flex flex-column">

                    <div className="d-flex justify-content-between align-items-start mb-2">
                        <h5 className="fw-bold mb-0">{projekt.titel}</h5>
                        <span className={`badge ${STATUS_FARBE[projekt.status] || "bg-light text-dark"}`}>
                            {projekt.status}
                        </span>
                    </div>

                    <div className="text-muted small mb-3">
                        Eingeladen von{" "}
                        {projekt.student
                            ? `${projekt.student.vorname} ${projekt.student.name}`
                            : "-"}
                    </div>

                    <div className="mb-1">
                        <strong>Fachbereich:</strong>
                        <span className="text-muted ms-2">{projekt.fachbereich || "-"}</span>
                    </div>
                    <div className="mb-1">
                        <strong>Projektart:</strong>
                        <span className="text-muted ms-2">{projekt.projektart || "-"}</span>
                    </div>
                    <div className="mb-3">
                        <strong>Semester:</strong>
                        <span className="text-muted ms-2">{projekt.semester || "-"}</span>
                    </div>

                    <div className="mt-auto">
                        <button
                            className={`btn ${projekt.status === "OFFEN" ? "btn-primary" : "btn-outline-secondary"}`}
                            onClick={() => navigate(`/project-invitation/${projekt.id}`)}
                        >
                            {projekt.status === "OFFEN" ? "Anfrage öffnen" : "Details ansehen"}
                        </button>
                    </div>

                </div>
            </div>
        </div>
    );

    return (
        <div>
            <Navbar />

            <div className="container mt-5">

                <h2 className="fw-bold mb-4">Meine Einladungen</h2>

                {laedt && <p className="text-muted">Laden...</p>}

                {error && <div className="alert alert-danger">{error}</div>}

                {!laedt && !error && einladungen.length === 0 && (
                    <div className="alert alert-light border">
                        Sie haben noch keine Projektanfragen erhalten.
                    </div>
                )}

                {offene.length > 0 && (
                    <>
                        <h5 className="mb-3">
                            Offene Anfragen <span className="badge bg-warning text-dark">{offene.length}</span>
                        </h5>
                        <div className="row">{offene.map(einladungsKarte)}</div>
                    </>
                )}

                {erledigte.length > 0 && (
                    <>
                        <h5 className="mt-4 mb-3">Bereits entschieden</h5>
                        <div className="row">{erledigte.map(einladungsKarte)}</div>
                    </>
                )}

            </div>
        </div>
    );
}

export default MyInvitations;