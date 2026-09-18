import { useEffect, useState } from "react";
import Navbar from "../components/Navbar";
import { getAuth, getProfil } from "../services/api";

function Profile() {
    const [message, setMessage] = useState("");
    const [profil, setProfil] = useState(null);

    useEffect(() => {
        getProfil()
            .then(setProfil)
            .catch(() => setProfil(null));
    }, []);

    const auth = getAuth();
    const vorname = profil?.vorname || auth?.vorname || "";
    const name = profil?.name || auth?.name || "";
    const email = profil?.email || "";
    const rolle = profil?.rolle || auth?.rolle || "";

    const handleSave = (e) => {
        e.preventDefault();

        setMessage(
            "Profil erfolgreich gespeichert. Backend-Anbindung folgt später."
        );
    };

    return (
        <div>
            <Navbar />

            <div className="container mt-4">

                <div className="mb-4">
                    <h2 className="fw-bold text-dark">
                        Mein Profil
                    </h2>

                    <p className="text-muted">
                        Verwalte deine persönlichen Daten und Kontoeinstellungen.
                    </p>
                </div>

                {message && (
                    <div className="alert alert-success">
                        {message}
                    </div>
                )}

                <div className="row">

                    {/* Linke Seite */}
                    <div className="col-lg-4 mb-4">

                        <div className="card shadow-sm border-0 text-center">
                            <div className="card-body">

                                <div
                                    className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-bold mx-auto mb-3"
                                    style={{
                                        width: "100px",
                                        height: "100px",
                                        fontSize: "36px",
                                    }}
                                >
                                    {vorname ? vorname.charAt(0).toUpperCase() : "?"}
                                </div>

                                <h4>{vorname} {name}</h4>

                                <p className="text-muted">
                                    {rolle}
                                </p>

                                <hr />

                                <div className="row text-center">

                                    <div className="col-4">
                                        <h5 className="text-primary">3</h5>
                                        <small>Projekte</small>
                                    </div>

                                    <div className="col-4">
                                        <h5 className="text-success">12</h5>
                                        <small>Ideen</small>
                                    </div>

                                    <div className="col-4">
                                        <h5 className="text-warning">7</h5>
                                        <small>Teams</small>
                                    </div>

                                </div>

                            </div>
                        </div>

                    </div>

                    {/* Rechte Seite */}
                    <div className="col-lg-8">

                        <div className="card shadow-sm border-0">
                            <div className="card-body">

                                <form onSubmit={handleSave} key={profil ? "loaded" : "loading"}>

                                    <h4 className="mb-4">
                                        Persönliche Daten
                                    </h4>

                                    <div className="row">

                                        <div className="col-md-6 mb-3">
                                            <label className="form-label">
                                                Vorname
                                            </label>

                                            <input
                                                type="text"
                                                className="form-control"
                                                defaultValue={vorname}
                                            />
                                        </div>

                                        <div className="col-md-6 mb-3">
                                            <label className="form-label">
                                                Nachname
                                            </label>

                                            <input
                                                type="text"
                                                className="form-control"
                                                defaultValue={name}
                                            />
                                        </div>

                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label">
                                            E-Mail-Adresse
                                        </label>

                                        <input
                                            type="email"
                                            className="form-control"
                                            defaultValue={email}
                                        />
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label">
                                            Rolle
                                        </label>

                                        <select
                                            className="form-select"
                                            defaultValue={rolle === "PROFESSOR" ? "lehrender" : "student"}
                                        >
                                            <option value="student">
                                                Student:in
                                            </option>

                                            <option value="lehrender">
                                                Lehrende:r
                                            </option>

                                            <option value="admin">
                                                Administrator
                                            </option>
                                        </select>
                                    </div>

                                    <div className="mb-4">
                                        <label className="form-label">
                                            Profilbild hochladen
                                        </label>

                                        <input
                                            type="file"
                                            className="form-control"
                                        />
                                    </div>

                                    <hr className="my-4" />

                                    <h4 className="mb-4">
                                        Passwort ändern
                                    </h4>

                                    <div className="mb-3">
                                        <label className="form-label">
                                            Aktuelles Passwort
                                        </label>

                                        <input
                                            type="password"
                                            className="form-control"
                                        />
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label">
                                            Neues Passwort
                                        </label>

                                        <input
                                            type="password"
                                            className="form-control"
                                        />
                                    </div>

                                    <div className="mb-4">
                                        <label className="form-label">
                                            Passwort bestätigen
                                        </label>

                                        <input
                                            type="password"
                                            className="form-control"
                                        />
                                    </div>

                                    <button
                                        type="submit"
                                        className="btn btn-primary"
                                    >
                                        Änderungen speichern
                                    </button>

                                </form>

                            </div>
                        </div>

                    </div>

                </div>

            </div>
        </div>
    );
}

export default Profile;