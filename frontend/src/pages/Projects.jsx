import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getAlleProjekte, getMeineProjekte, getAuth } from "../services/api";

function Projects({ scope = "alle" }) {
    const navigate = useNavigate();

    const [projects, setProjects] = useState([]);
    const [error, setError] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const [statusFilter, setStatusFilter] = useState("alle");
    const [schlagwortFilter, setSchlagwortFilter] = useState("alle");

    const istMeine = scope === "meine";

    const mitSchlagwoerterArray = (project) => ({
        ...project,
        schlagwoerterListe: project.schlagwoerter
            ? project.schlagwoerter.split(",").map((s) => s.trim()).filter(Boolean)
            : [],
    });

    useEffect(() => {
        setError("");

        const auth = getAuth();
        if (istMeine && !auth?.id) {
            setError("Bitte zuerst einloggen, um deine Projekte zu sehen.");
            setProjects([]);
            return;
        }

        const ladeProjekte = istMeine ? getMeineProjekte(auth.id) : getAlleProjekte();

        ladeProjekte
            .then((data) => {
                setProjects((data || []).map(mitSchlagwoerterArray));
            })
            .catch((err) => {
                setError(err.message || "Backend ist nicht erreichbar.");
                setProjects([]);
            });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [scope]);

    const alleSchlagwoerter = [
        "alle",
        ...new Set(projects.flatMap((project) => project.schlagwoerterListe || [])),
    ];

    const filteredProjects = projects.filter((project) => {
        const matchesSearch =
            project.titel?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            project.beschreibung?.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesStatus =
            statusFilter === "alle" || project.status === statusFilter;

        const matchesSchlagwort =
            schlagwortFilter === "alle" ||
            project.schlagwoerterListe?.includes(schlagwortFilter);

        return matchesSearch && matchesStatus && matchesSchlagwort;
    });

    return (
        <div>
            <Navbar />

            <div className="container mt-4">
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h2 className="fw-bold text-dark">
                            {istMeine ? "Meine Projekte" : "Alle Projekte"}
                        </h2>
                        <p className="text-muted mb-0">
                            {istMeine
                                ? "Deine eigenen SOP-Projekte."
                                : "Suche nach laufenden, offenen und abgeschlossenen SOP-Projekten."}
                        </p>
                    </div>

                    <button
                        className="btn btn-primary"
                        onClick={() => navigate("/create-project")}
                    >
                        Neues Projekt
                    </button>
                </div>

                {error && <div className="alert alert-warning">{error}</div>}

                <div className="card shadow-sm border-0 mb-4">
                    <div className="card-body">
                        <div className="row g-3">
                            <div className="col-md-6">
                                <label className="form-label">Suche</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="🔍 Projektname oder Beschreibung suchen..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>

                            <div className="col-md-3">
                                <label className="form-label">Status</label>
                                <select
                                    className="form-select"
                                    value={statusFilter}
                                    onChange={(e) => setStatusFilter(e.target.value)}
                                >
                                    <option value="alle">Alle</option>
                                    <option value="ENTWURF">Entwurf</option>
                                    <option value="offen">Offen</option>
                                    <option value="laufend">Laufend</option>
                                    <option value="abgeschlossen">Abgeschlossen</option>
                                </select>
                            </div>

                            <div className="col-md-3">
                                <label className="form-label">Schlagwort</label>
                                <select
                                    className="form-select"
                                    value={schlagwortFilter}
                                    onChange={(e) => setSchlagwortFilter(e.target.value)}
                                >
                                    {alleSchlagwoerter.map((wort) => (
                                        <option value={wort} key={wort}>
                                            {wort === "alle" ? "Alle" : wort}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                <p className="text-muted">
                    {filteredProjects.length} Projekt(e) gefunden
                </p>

                <div className="row">
                    {filteredProjects.map((project) => (
                        <div className="col-md-6 mb-4" key={project.id}>
                            <div className="card shadow-sm border-0 h-100">
                                <div className="card-body">
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <h5 className="card-title">{project.titel}</h5>

                                        <span
                                            className={`badge ${project.status === "laufend"
                                                ? "bg-success"
                                                : project.status === "abgeschlossen"
                                                    ? "bg-secondary"
                                                    : "bg-warning text-dark"
                                                }`}
                                        >
                                            {project.status}
                                        </span>
                                    </div>

                                    <p className="text-muted">
                                        {project.beschreibung}
                                    </p>

                                    <p className="mb-2">
                                        <strong>Semester:</strong> {project.semester}
                                    </p>

                                    <div className="mb-3">
                                        {project.schlagwoerterListe?.map((wort) => (
                                            <span
                                                className="badge bg-primary me-1 mb-1"
                                                key={wort}
                                            >
                                                {wort}
                                            </span>
                                        ))}
                                        {project.tags?.map((tag) => (
                                            <span
                                                className="badge bg-info text-dark me-1 mb-1"
                                                key={tag.id}
                                            >
                                                {tag.name}
                                            </span>
                                        ))}
                                    </div>

                                    <button
                                        className="btn btn-outline-primary"
                                        onClick={() => navigate(`/projectdetails/${project.id}`)}
                                    >
                                        Details ansehen
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {filteredProjects.length === 0 && !error && (
                    <div className="alert alert-info">
                        Keine Projekte gefunden. Bitte ändere deine Such- oder Filterkriterien.
                    </div>
                )}
            </div>
        </div>
    );
}

export default Projects;
