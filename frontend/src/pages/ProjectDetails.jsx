import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
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

const DOKUMENT_TYP_LABEL = {
    PFLICHTENHEFT: "Pflichtenheft",
    LASTENHEFT: "Lastenheft",
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

                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <h4 className="mb-0">KI-Zusammenfassung</h4>

                                    <button
                                        className="btn btn-sm btn-outline-primary"
                                        onClick={handleZusammenfassungErzeugen}
                                        disabled={kiLaedt}
                                    >
                                        {kiLaedt ? "Wird verarbeitet..." : "Zusammenfassung erzeugen"}
                                    </button>
                                </div>

                                {kiFehler && <div className="alert alert-warning py-2 mb-2">{kiFehler}</div>}

                                {kiLaedt ? (
                                    <p className="text-muted mb-0">Wird verarbeitet...</p>
                                ) : projekt.kiZusammenfassung ? (
                                    <p className="mb-0">{projekt.kiZusammenfassung}</p>
                                ) : (
                                    <p className="text-muted mb-0">Noch keine Zusammenfassung erzeugt.</p>
                                )}

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

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h4>Tags</h4>

                                {projekt.tags?.length > 0 ? (
                                    projekt.tags.map((tag) => (
                                        <span className="badge bg-info text-dark me-2" key={tag.id}>
                                            {tag.name}
                                        </span>
                                    ))
                                ) : (
                                    <p className="text-muted mb-0">Noch keine Tags vergeben.</p>
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

                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <h5 className="mb-0">Team</h5>

                                    {projekt.gruppenanzahl != null && (
                                        <span className={`badge ${maxErreicht ? "bg-secondary" : "bg-primary"}`}>
                                            {aktuelleGroesse} / {projekt.gruppenanzahl}
                                        </span>
                                    )}
                                </div>

                                <ul className="list-group mb-3">
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

                                    {mitglieder.map((m) => (
                                        <li
                                            className="list-group-item d-flex justify-content-between align-items-center"
                                            key={m.studentId}
                                        >
                                            {m.vorname} {m.name}

                                            {istErsteller && (
                                                <button
                                                    className="btn btn-sm btn-outline-danger"
                                                    onClick={() => handleEntfernen(m.studentId)}
                                                    title="Entfernen"
                                                >
                                                    ✕
                                                </button>
                                            )}
                                        </li>
                                    ))}

                                    {mitglieder.length === 0 && (
                                        <li className="list-group-item text-muted">
                                            Noch keine weiteren Teammitglieder.
                                        </li>
                                    )}
                                </ul>

                                {istErsteller && (
                                    <>
                                        {mitgliedFehler && (
                                            <div className="alert alert-danger py-2">{mitgliedFehler}</div>
                                        )}

                                        {maxErreicht ? (
                                            <p className="text-muted mb-0">
                                                Maximale Gruppengröße erreicht ({aktuelleGroesse}/{projekt.gruppenanzahl}).
                                            </p>
                                        ) : (
                                            <>
                                                <label className="form-label">Studierende hinzufügen</label>
                                                <input
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
                                    </>
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
