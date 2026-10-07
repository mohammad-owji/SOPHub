import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProjektStatusBadge, { PROJEKT_STATUS } from "../components/ProjektStatus";
import { getAlleProjekte, getMeineProjekte, getAuth } from "../services/api";
import "./Projects.css";

// Systemkonto der Beispielprojekte (siehe BeispielprojekteInitializer im Backend)
const BEISPIEL_KONTO = "sophub.beispielprojekte";

function Projects({ scope = "alle" }) {
    const navigate = useNavigate();

    const [projects, setProjects] = useState([]);
    const [error, setError] = useState("");
    const [laedt, setLaedt] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [statusFilter, setStatusFilter] = useState("alle");
    const [semesterFilter, setSemesterFilter] = useState("alle");

    const istMeine = scope === "meine";

    useEffect(() => {
        setError("");
        setLaedt(true);
        // Filter zuruecksetzen, wenn zwischen "Meine" und "Alle" gewechselt wird
        setSearchTerm("");
        setStatusFilter("alle");
        setSemesterFilter("alle");

        const auth = getAuth();
        if (istMeine && !auth?.id) {
            setError("Bitte zuerst einloggen, um Ihre Projekte zu sehen.");
            setProjects([]);
            setLaedt(false);
            return;
        }

        const ladeProjekte = istMeine ? getMeineProjekte(auth.id) : getAlleProjekte();

        ladeProjekte
            .then((data) => {
                // Neueste zuerst
                const liste = (data || []).slice().sort((a, b) => b.id - a.id);
                setProjects(liste);
            })
            .catch((err) => {
                setError(err.message || "Backend ist nicht erreichbar.");
                setProjects([]);
            })
            .finally(() => setLaedt(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [scope]);

    // Auswahllisten fuer die Filter aus den vorhandenen Projekten bilden
    const alleSemester = [...new Set(projects.map((p) => p.semester).filter(Boolean))].sort().reverse();

    const suche = searchTerm.trim().toLowerCase();

    const filteredProjects = projects.filter((project) => {
        const matchesSearch =
            !suche ||
            project.titel?.toLowerCase().includes(suche) ||
            project.beschreibung?.toLowerCase().includes(suche);

        const matchesStatus =
            statusFilter === "alle" || (project.status || "").toUpperCase() === statusFilter;

        const matchesSemester =
            semesterFilter === "alle" || project.semester === semesterFilter;

        return matchesSearch && matchesStatus && matchesSemester;
    });

    const filterAktiv =
        suche || statusFilter !== "alle" || semesterFilter !== "alle";

    const filterZuruecksetzen = () => {
        setSearchTerm("");
        setStatusFilter("alle");
        setSemesterFilter("alle");
    };

    return (
        <div>
            <Navbar />

            <div className="container py-4">
                <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
                    <div>
                        <h2 className="mb-1">{istMeine ? "Meine Projekte" : "Alle Projekte"}</h2>
                        <p className="text-muted mb-0">
                            {istMeine
                                ? "Ihre eigenen SOP-Projekte und deren aktueller Stand."
                                : "Laufende, offene und abgeschlossene SOP-Projekte durchsuchen."}
                        </p>
                    </div>

                    <button className="btn btn-primary" onClick={() => navigate("/create-project")}>
                        + Neues Projekt
                    </button>
                </div>

                {error && <div className="alert alert-warning">{error}</div>}

                {/* Filter */}
                <div className="card mb-4">
                    <div className="card-body">
                        <div className="row g-3 align-items-end">
                            <div className="col-lg-6">
                                <label className="form-label" htmlFor="proj-suche">Suche</label>
                                <input
                                    id="proj-suche"
                                    type="search"
                                    className="form-control"
                                    placeholder="Projektname oder Beschreibung..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>

                            <div className="col-sm-6 col-lg-3">
                                <label className="form-label" htmlFor="proj-status">Status</label>
                                <select
                                    id="proj-status"
                                    className="form-select"
                                    value={statusFilter}
                                    onChange={(e) => setStatusFilter(e.target.value)}
                                >
                                    <option value="alle">Alle Status</option>
                                    {Object.entries(PROJEKT_STATUS).map(([wert, info]) => (
                                        <option value={wert} key={wert}>{info.text}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="col-sm-6 col-lg-3">
                                <label className="form-label" htmlFor="proj-semester">Semester</label>
                                <select
                                    id="proj-semester"
                                    className="form-select"
                                    value={semesterFilter}
                                    onChange={(e) => setSemesterFilter(e.target.value)}
                                >
                                    <option value="alle">Alle</option>
                                    {alleSemester.map((sem) => (
                                        <option value={sem} key={sem}>{sem}</option>
                                    ))}
                                </select>
                            </div>

                        </div>
                    </div>
                </div>

                {!laedt && (
                    <div className="d-flex justify-content-between align-items-center mb-3">
                        <p className="text-muted mb-0">
                            <strong className="text-dark">{filteredProjects.length}</strong>{" "}
                            {filteredProjects.length === 1 ? "Projekt" : "Projekte"} gefunden
                        </p>
                        {filterAktiv && (
                            <button className="btn btn-sm btn-outline-secondary" onClick={filterZuruecksetzen}>
                                Filter zurücksetzen
                            </button>
                        )}
                    </div>
                )}

                {laedt ? (
                    <p className="text-muted">Projekte werden geladen...</p>
                ) : (
                    <div className="row g-4">
                        {filteredProjects.map((project) => (
                            <div className="col-md-6 col-xl-4" key={project.id}>
                                <Link className="card proj-karte" to={`/projectdetails/${project.id}`}>
                                    <div className="card-body">
                                        <div className="d-flex justify-content-between align-items-start gap-2">
                                            <h3 className="proj-titel">{project.titel}</h3>
                                            <ProjektStatusBadge status={project.status} className="flex-shrink-0" />
                                        </div>

                                        <p className="proj-beschreibung">
                                            {project.beschreibung || "Keine Beschreibung hinterlegt."}
                                        </p>

                                        <div className="proj-meta">
                                            {project.semester && (
                                                <span><strong>Semester:</strong> {project.semester}</span>
                                            )}
                                            {project.fachbereich && (
                                                <span><strong>Fachbereich:</strong> {project.fachbereich}</span>
                                            )}
                                            <span>
                                                <strong>Betreuung:</strong>{" "}
                                                {project.betreuer
                                                    ? `${project.betreuer.vorname} ${project.betreuer.name}`
                                                    : "–"}
                                            </span>
                                        </div>

                                        {/* KI-Tags (werden automatisch aus den Dokumenten erzeugt) */}
                                        {project.tags?.length > 0 && (
                                            <div>
                                                {project.tags.map((tag) => (
                                                    <span className="badge bg-info me-1 mb-1" key={tag.id}>
                                                        {tag.name}
                                                    </span>
                                                ))}
                                            </div>
                                        )}

                                        <div className="proj-fuss">
                                            {project.student?.benutzername === BEISPIEL_KONTO ? (
                                                <span className="proj-beispiel">Beispielprojekt</span>
                                            ) : (
                                                <span className="text-muted text-truncate">
                                                    {project.student
                                                        ? `${project.student.vorname} ${project.student.name}`
                                                        : ""}
                                                </span>
                                            )}
                                            <span className="proj-link flex-shrink-0">Details ansehen →</span>
                                        </div>
                                    </div>
                                </Link>
                            </div>
                        ))}
                    </div>
                )}

                {!laedt && filteredProjects.length === 0 && !error && (
                    <div className="card">
                        <div className="proj-leer">
                            <div className="proj-leer-symbol">▤</div>
                            {projects.length === 0 ? (
                                <>
                                    <h4>Noch keine Projekte</h4>
                                    <p className="text-muted">
                                        {istMeine
                                            ? "Sie haben noch kein eigenes Projekt angelegt."
                                            : "Im Portal gibt es noch keine Projekte."}
                                    </p>
                                    <button className="btn btn-primary" onClick={() => navigate("/create-project")}>
                                        Projekt erstellen
                                    </button>
                                </>
                            ) : (
                                <>
                                    <h4>Keine Treffer</h4>
                                    <p className="text-muted">Bitte ändern Sie Ihre Such- oder Filterkriterien.</p>
                                    <button className="btn btn-outline-secondary" onClick={filterZuruecksetzen}>
                                        Filter zurücksetzen
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default Projects;