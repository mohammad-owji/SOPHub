import Navbar from "../components/Navbar";

function ProjectDetails() {
    return (
        <div>
            <Navbar />

            <div className="container mt-4">

                <div className="card shadow-sm border-0 mb-4">
                    <div className="card-body">

                        <div className="d-flex justify-content-between align-items-center">
                            <div>
                                <h2 className="fw-bold">SOPhub</h2>

                                <p className="text-muted">
                                    Plattform zur Verwaltung von SOP-Projekten,
                                    Teams, Dokumenten und KI-Unterstützung.
                                </p>
                            </div>

                            <span className="badge bg-success fs-6">
                                laufend
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
                                    SOPhub ist eine Plattform zur Verwaltung von
                                    Softwareprojekten an Hochschulen.
                                    Studierende können Projekte suchen,
                                    Projektideen veröffentlichen,
                                    Teams bilden und Dokumente verwalten.
                                </p>

                            </div>
                        </div>

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h4>Technologien</h4>

                                <span className="badge bg-primary me-2">
                                    React
                                </span>

                                <span className="badge bg-primary me-2">
                                    Spring Boot
                                </span>

                                <span className="badge bg-primary me-2">
                                    PostgreSQL
                                </span>

                                <span className="badge bg-primary">
                                    KI
                                </span>

                            </div>
                        </div>

                        <div className="card shadow-sm border-0">
                            <div className="card-body">

                                <h4>Dokumente</h4>

                                <ul className="list-group">
                                    <li className="list-group-item">
                                        Lastenheft.pdf
                                    </li>

                                    <li className="list-group-item">
                                        Pflichtenheft.pdf
                                    </li>

                                    <li className="list-group-item">
                                        Datenbankmodell.pdf
                                    </li>
                                </ul>

                            </div>
                        </div>

                    </div>

                    <div className="col-md-4">

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h5>Projektinformationen</h5>

                                <p>
                                    <strong>Semester:</strong><br />
                                    SoSe 2026
                                </p>

                                <p>
                                    <strong>Status:</strong><br />
                                    Laufend
                                </p>

                                <p>
                                    <strong>Teamgröße:</strong><br />
                                    3 / 5
                                </p>

                            </div>
                        </div>

                        <div className="card shadow-sm border-0 mb-4">
                            <div className="card-body">

                                <h5>Teammitglieder</h5>

                                <ul className="list-group">
                                    <li className="list-group-item">
                                        Salma
                                    </li>

                                    <li className="list-group-item">
                                        Mohammad
                                    </li>

                                    <li className="list-group-item">
                                        Dalieh
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