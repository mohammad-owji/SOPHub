import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getProjects } from "../services/api";

function Projects() {
    const navigate = useNavigate();

    const [projects, setProjects] = useState([]);
    const [error, setError] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const [statusFilter, setStatusFilter] = useState("alle");
    const [technologyFilter, setTechnologyFilter] = useState("alle");

    useEffect(() => {
        getProjects()
            .then((data) => {
                setProjects(data);
            })
            .catch(() => {
                setError("Backend ist noch nicht erreichbar. Es werden Testdaten angezeigt.");

                setProjects([
                    {
                        id: 1,
                        titel: "SOPhub",
                        status: "laufend",
                        semester: "SoSe 2026",
                        kurzbeschreibung:
                            "Zentrale Plattform für SOP-Projekte, Teamorganisation und KI-gestützte Suche.",
                        technologies: ["React", "Spring Boot", "PostgreSQL", "KI"],
                    },
                    {
                        id: 2,
                        titel: "Campus Connect",
                        status: "abgeschlossen",
                        semester: "WiSe 2025",
                        kurzbeschreibung:
                            "Plattform zur Vernetzung von Studierenden auf dem Campus.",
                        technologies: ["React", "Node.js"],
                    },
                    {
                        id: 3,
                        titel: "Study Planner",
                        status: "offen",
                        semester: "SoSe 2026",
                        kurzbeschreibung:
                            "App zur Planung von Lernzeiten, Aufgaben und Prüfungen.",
                        technologies: ["Vue", "Spring Boot", "PostgreSQL"],
                    },
                    {
                        id: 4,
                        titel: "KI Lernassistent",
                        status: "laufend",
                        semester: "WiSe 2025",
                        kurzbeschreibung:
                            "KI-gestützte Lernhilfe für Programmiermodule.",
                        technologies: ["React", "KI", "Python"],
                    },
                ]);
            });
    }, []);

    const allTechnologies = [
        "alle",
        ...new Set(projects.flatMap((project) => project.technologies || [])),
    ];

    const filteredProjects = projects.filter((project) => {
        const matchesSearch =
            project.titel?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            project.kurzbeschreibung?.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesStatus =
            statusFilter === "alle" || project.status === statusFilter;

        const matchesTechnology =
            technologyFilter === "alle" ||
            project.technologies?.includes(technologyFilter);

        return matchesSearch && matchesStatus && matchesTechnology;
    });

    return (
        <div>
            <Navbar />

            <div className="container mt-4">
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h2 className="fw-bold text-dark">Alle Projekte</h2>
                        <p className="text-muted mb-0">
                            Suche nach laufenden, offenen und abgeschlossenen SOP-Projekten.
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
                                    <option value="offen">Offen</option>
                                    <option value="laufend">Laufend</option>
                                    <option value="abgeschlossen">Abgeschlossen</option>
                                </select>
                            </div>

                            <div className="col-md-3">
                                <label className="form-label">Technologie</label>
                                <select
                                    className="form-select"
                                    value={technologyFilter}
                                    onChange={(e) => setTechnologyFilter(e.target.value)}
                                >
                                    {allTechnologies.map((tech) => (
                                        <option value={tech} key={tech}>
                                            {tech === "alle" ? "Alle" : tech}
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
                                        {project.kurzbeschreibung}
                                    </p>

                                    <p className="mb-2">
                                        <strong>Semester:</strong> {project.semester}
                                    </p>

                                    <div className="mb-3">
                                        {project.technologies?.map((tech) => (
                                            <span
                                                className="badge bg-primary me-1 mb-1"
                                                key={tech}
                                            >
                                                {tech}
                                            </span>
                                        ))}
                                    </div>

                                    <button
                                        className="btn btn-outline-primary"
                                        onClick={() => navigate("/projectdetails")}
                                    >
                                        Details ansehen
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {filteredProjects.length === 0 && (
                    <div className="alert alert-info">
                        Keine Projekte gefunden. Bitte ändere deine Such- oder Filterkriterien.
                    </div>
                )}
            </div>
        </div>
    );
}

export default Projects;