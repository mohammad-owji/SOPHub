import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getProjektById, getDokumenteFuerProjekt, downloadDokument } from "../services/api";

const DOKUMENT_TYP_LABEL = {
    PFLICHTENHEFT: "Pflichtenheft",
    LASTENHEFT: "Lastenheft",
    DOKUMENTATION: "Dokumentation",
    SONSTIGES: "Sonstiges",
};

function ProjectDetails() {
    const { id } = useParams();

    const [projekt, setProjekt] = useState(null);
    const [dokumente, setDokumente] = useState([]);
    const [fehler, setFehler] = useState("");

    useEffect(() => {
        if (!id) return;

        setFehler("");
        getProjektById(id)
            .then(setProjekt)
            .catch(() => setFehler("Projekt konnte nicht geladen werden."));

        getDokumenteFuerProjekt(id)
            .then((data) => setDokumente(data || []))
            .catch(() => setDokumente([]));
    }, [id]);

    const schlagwoerterListe = projekt?.schlagwoerter
        ? projekt.schlagwoerter.split(",").map((s) => s.trim()).filter(Boolean)
        : [];

    if (!id) {
        return (
            <div>
                <Navbar />
                <div className="container mt-4">
                    <div className="alert alert-info">
                        Bitte ein Projekt aus "Meine Projekte" oder "Alle Projekte" auswählen.
                    </div>
                </div>
            </div>
        );
    }

    if (fehler) {
        return (
            <div>
                <Navbar />
                <div className="container mt-4">
                    <div className="alert alert-danger">{fehler}</div>
                </div>
            </div>
        );
    }

    if (!projekt) {
        return (
            <div>
                <Navbar />
                <div className="container mt-4">
                    <p className="text-muted">Lade Projekt...</p>
                </div>
            </div>
        );
    }

    return (
        <div>
            <Navbar />

            <div className="container mt-4">

                <div className="card shadow-sm border-0 mb-4">
                    <div className="card-body">

                        <div className="d-flex justify-content-between align-items-center">
                            <h2 className="fw-bold mb-0">{projekt.titel}</h2>

                            <span className="badge bg-success fs-6">
                                {projekt.status}
                            </span>
                        </div>

                    </div>
                </div>

                <div className="row">

                    <div className="col-md-8">

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h4>Projektbeschreibung</h4>

                                <p>
                                    {projekt.beschreibung || "Keine Beschreibung hinterlegt."}
                                </p>

                            </div>
                        </div>

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h4>Schlagwörter</h4>

                                {schlagwoerterListe.length > 0 ? (
                                    schlagwoerterListe.map((wort) => (
                                        <span className="badge bg-primary me-2" key={wort}>
                                            {wort}
                                        </span>
                                    ))
                                ) : (
                                    <p className="text-muted mb-0">Keine Schlagwörter hinterlegt.</p>
                                )}

                            </div>
                        </div>

                        <div className="card shadow-sm border-0">
                            <div className="card-body">

                                <h4>Dokumente</h4>

                                {dokumente.length > 0 ? (
                                    <ul className="list-group">
                                        {dokumente.map((dok) => (
                                            <li
                                                className="list-group-item d-flex justify-content-between align-items-center"
                                                key={dok.id}
                                            >
                                                <div>
                                                    {dok.dateiName}
                                                    <span className="badge bg-secondary ms-2">
                                                        {DOKUMENT_TYP_LABEL[dok.typ] || dok.typ}
                                                    </span>
                                                </div>

                                                <button
                                                    className="btn btn-sm btn-outline-primary"
                                                    onClick={() => downloadDokument(dok.id, dok.dateiName)}
                                                >
                                                    Herunterladen
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                ) : (
                                    <p className="text-muted mb-0">Noch keine Dokumente hochgeladen.</p>
                                )}

                            </div>
                        </div>

                    </div>

                    <div className="col-md-4">

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h5>Projektinformationen</h5>

                                <p>
                                    <strong>Semester:</strong><br />
                                    {projekt.semester || "-"}
                                </p>

                                <p>
                                    <strong>Status:</strong><br />
                                    {projekt.status || "-"}
                                </p>

                                <p>
                                    <strong>Fachbereich:</strong><br />
                                    {projekt.fachbereich || "-"}
                                </p>

                                <p className="mb-0">
                                    <strong>Gruppengröße:</strong><br />
                                    {projekt.gruppenanzahl ?? "-"}
                                </p>

                            </div>
                        </div>

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h5>Team</h5>

                                <ul className="list-group">
                                    <li className="list-group-item">
                                        <strong>Student:in:</strong>{" "}
                                        {projekt.student
                                            ? `${projekt.student.vorname} ${projekt.student.name}`
                                            : "-"}
                                    </li>

                                    <li className="list-group-item">
                                        <strong>Betreuer:in:</strong>{" "}
                                        {projekt.betreuer
                                            ? `${projekt.betreuer.vorname} ${projekt.betreuer.name}`
                                            : "Noch nicht zugewiesen"}
                                    </li>
                                </ul>

                            </div>
                        </div>

                        <div className="card shadow-sm border-0">
                            <div className="card-body text-center">

                                <button className="btn btn-success w-100">
                                    Team beitreten
                                </button>

                            </div>
                        </div>

                    </div>

                </div>

            </div>
        </div>
    );
}

export default ProjectDetails;
