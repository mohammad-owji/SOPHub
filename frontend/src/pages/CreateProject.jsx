import { useState } from "react";
import Navbar from "../components/Navbar";

function CreateProject() {
    const [title, setTitle] = useState("");
    const [shortDescription, setShortDescription] = useState("");
    const [semester, setSemester] = useState("SoSe 2026");
    const [status, setStatus] = useState("offen");
    const [teamSize, setTeamSize] = useState("3");
    const [technologies, setTechnologies] = useState([]);
    const [message, setMessage] = useState("");

    const availableTechnologies = [
        "React",
        "Spring Boot",
        "PostgreSQL",
        "KI",
        "Java",
        "Node.js",
        "Python",
    ];

    const toggleTechnology = (tech) => {
        if (technologies.includes(tech)) {
            setTechnologies(technologies.filter((item) => item !== tech));
        } else {
            setTechnologies([...technologies, tech]);
        }
    };

    const handleSubmit = (event) => {
        event.preventDefault();

        if (!title || !shortDescription || technologies.length === 0) {
            setMessage("Bitte Titel, Beschreibung und mindestens eine Technologie angeben.");
            return;
        }

        setMessage("Projekt wurde erfolgreich vorbereitet. Backend-Anbindung folgt später.");

        setTitle("");
        setShortDescription("");
        setTechnologies([]);
        setSemester("SoSe 2026");
        setStatus("offen");
        setTeamSize("3");
    };

    return (
        <div>
            <Navbar />

            <div className="container mt-4">
                <div className="mb-4">
                    <h2 className="fw-bold text-dark">Projekt erstellen</h2>
                    <p className="text-muted">
                        Erstelle eine neue Projektidee oder ein neues SOP-Projekt.
                    </p>
                </div>

                {message && (
                    <div className="alert alert-info">
                        {message}
                    </div>
                )}

                <div className="card shadow-sm border-0">
                    <div className="card-body p-4">
                        <form onSubmit={handleSubmit}>
                            <div className="mb-3">
                                <label className="form-label">Projekttitel</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="z. B. SOPhub"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                />
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Kurzbeschreibung</label>
                                <textarea
                                    className="form-control"
                                    rows="4"
                                    placeholder="Beschreibe kurz, worum es im Projekt geht..."
                                    value={shortDescription}
                                    onChange={(e) => setShortDescription(e.target.value)}
                                ></textarea>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Technologien</label>

                                <div className="d-flex flex-wrap gap-2">
                                    {availableTechnologies.map((tech) => (
                                        <button
                                            type="button"
                                            key={tech}
                                            className={
                                                technologies.includes(tech)
                                                    ? "btn btn-primary btn-sm"
                                                    : "btn btn-outline-primary btn-sm"
                                            }
                                            onClick={() => toggleTechnology(tech)}
                                        >
                                            {tech}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Semester</label>
                                    <select
                                        className="form-select"
                                        value={semester}
                                        onChange={(e) => setSemester(e.target.value)}
                                    >
                                        <option>SoSe 2026</option>
                                        <option>WiSe 2025</option>
                                        <option>SoSe 2025</option>
                                    </select>
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Status</label>
                                    <select
                                        className="form-select"
                                        value={status}
                                        onChange={(e) => setStatus(e.target.value)}
                                    >
                                        <option value="offen">Offen</option>
                                        <option value="laufend">Laufend</option>
                                        <option value="abgeschlossen">Abgeschlossen</option>
                                    </select>
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Maximale Teamgröße</label>
                                    <select
                                        className="form-select"
                                        value={teamSize}
                                        onChange={(e) => setTeamSize(e.target.value)}
                                    >
                                        <option value="2">2 Personen</option>
                                        <option value="3">3 Personen</option>
                                        <option value="4">4 Personen</option>
                                        <option value="5">5 Personen</option>
                                    </select>
                                </div>
                            </div>

                            <button type="submit" className="btn btn-primary">
                                Projekt erstellen
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default CreateProject;