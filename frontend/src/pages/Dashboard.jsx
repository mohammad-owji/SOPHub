import Navbar from "../components/Navbar";

function Dashboard() {
    return (
        <div>
            <Navbar />

            <div className="container mt-4">

                {/* Begrüßung */}
                <div className="p-5 bg-light rounded shadow-sm mb-4 text-center">
                    <h1 className="fw-bold text-dark">
                        Willkommen zurück, Salma 👋
                    </h1>

                    <p className="text-muted">
                        Von der Idee bis zum erfolgreichen SOP-Projekt.
                    </p>
                </div>

                {/* Suchleiste */}
                <div className="card shadow-sm border-0 mb-4">
                    <div className="card-body">
                        <input
                            type="text"
                            className="form-control"
                            placeholder="🔍 Projekt suchen..."
                        />
                    </div>
                </div>

                {/* Statistiken */}
                <div className="row mb-4">

                    <div className="col-md-3 mb-3">
                        <div className="card text-center shadow-sm border-0">
                            <div className="card-body">
                                <h3 className="text-primary">3</h3>
                                <p className="mb-0">Meine Projekte</p>
                            </div>
                        </div>
                    </div>

                    <div className="col-md-3 mb-3">
                        <div className="card text-center shadow-sm border-0">
                            <div className="card-body">
                                <h3 className="text-success">12</h3>
                                <p className="mb-0">Projektideen</p>
                            </div>
                        </div>
                    </div>

                    <div className="col-md-3 mb-3">
                        <div className="card text-center shadow-sm border-0">
                            <div className="card-body">
                                <h3 className="text-warning">7</h3>
                                <p className="mb-0">Teams</p>
                            </div>
                        </div>
                    </div>

                    <div className="col-md-3 mb-3">
                        <div className="card text-center shadow-sm border-0">
                            <div className="card-body">
                                <h3 className="text-danger">58</h3>
                                <p className="mb-0">Dokumente</p>
                            </div>
                        </div>
                    </div>

                </div>

                {/* Funktionen */}
                <div className="row">

                    <div className="col-md-4 mb-4">
                        <div className="card shadow-sm h-100 border-0">
                            <div className="card-body text-center">
                                <h5 className="card-title">Meine Projekte</h5>
                                <p className="card-text text-muted">
                                    Projekte ansehen, an denen du beteiligt bist.
                                </p>

                                <a href="/my-projects" className="btn btn-primary">
                                    Öffnen
                                </a>
                            </div>
                        </div>
                    </div>

                    <div className="col-md-4 mb-4">
                        <div className="card shadow-sm h-100 border-0">
                            <div className="card-body text-center">
                                <h5 className="card-title">Alle Projekte</h5>
                                <p className="card-text text-muted">
                                    Laufende und abgeschlossene SOP-Projekte durchsuchen.
                                </p>

                                <a href="/projects" className="btn btn-primary">
                                    Anzeigen
                                </a>
                            </div>
                        </div>
                    </div>

                    <div className="col-md-4 mb-4">
                        <div className="card shadow-sm h-100 border-0">
                            <div className="card-body text-center">
                                <h5 className="card-title">Projektideen</h5>
                                <p className="card-text text-muted">
                                    Neue Themen entdecken oder eigene Ideen einreichen.
                                </p>

                                <a href="/ideas" className="btn btn-primary">
                                    Ideen ansehen
                                </a>
                            </div>
                        </div>
                    </div>

                    <div className="col-md-4 mb-4">
                        <div className="card shadow-sm h-100 border-0">
                            <div className="card-body text-center">
                                <h5 className="card-title">Projekt erstellen</h5>

                                <p className="card-text text-muted">
                                    Erstelle ein neues Projekt mit Beschreibung und Technologien.
                                </p>

                                <a href="/create-project" className="btn btn-outline-primary">
                                    Erstellen
                                </a>
                            </div>
                        </div>
                    </div>

                    <div className="col-md-4 mb-4">
                        <div className="card shadow-sm h-100 border-0">
                            <div className="card-body text-center">
                                <h5 className="card-title">KI-Unterstützung</h5>

                                <p className="card-text text-muted">
                                    Projektinhalte zusammenfassen und ähnliche Projekte finden.
                                </p>

                                <a href="/ai" className="btn btn-outline-primary">
                                    KI öffnen
                                </a>
                            </div>
                        </div>
                    </div>

                </div>

            </div>
        </div>
    );
}

export default Dashboard;