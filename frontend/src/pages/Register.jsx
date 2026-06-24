function Register() {
    return (
        <div className="container d-flex justify-content-center align-items-center min-vh-100">
            <div className="card shadow p-4" style={{ width: "450px" }}>
                <h2 className="text-center mb-3">SOPhub</h2>
                <p className="text-center text-muted">
                    Account erstellen und Rolle auswählen.
                </p>

                <h4 className="mb-3">Registrierung</h4>

                <form>
                    <div className="row">
                        <div className="col-md-6 mb-3">
                            <label className="form-label">Vorname</label>
                            <input type="text" className="form-control" placeholder="Vorname" />
                        </div>

                        <div className="col-md-6 mb-3">
                            <label className="form-label">Nachname</label>
                            <input type="text" className="form-control" placeholder="Nachname" />
                        </div>
                    </div>

                    <div className="mb-3">
                        <label className="form-label">E-Mail-Adresse</label>
                        <input
                            type="email"
                            className="form-control"
                            placeholder="name@hs-bochum.de"
                        />
                    </div>

                    <div className="mb-3">
                        <label className="form-label">Passwort</label>
                        <input
                            type="password"
                            className="form-control"
                            placeholder="Passwort eingeben"
                        />
                    </div>

                    <div className="mb-3">
                        <label className="form-label">Passwort bestätigen</label>
                        <input
                            type="password"
                            className="form-control"
                            placeholder="Passwort erneut eingeben"
                        />
                    </div>

                    <div className="mb-3">
                        <label className="form-label">Rolle</label>
                        <select className="form-select" defaultValue="">
                            <option value="" disabled>
                                Rolle auswählen
                            </option>
                            <option value="student">Student</option>
                            <option value="lehrender">Lehrender</option>
                            <option value="admin">Admin</option>
                        </select>
                    </div>

                    <button type="submit" className="btn btn-success w-100">
                        Registrieren
                    </button>
                </form>

                <p className="text-center mt-3">
                    Schon registriert? <a href="/">Zum Login</a>
                </p>
            </div>
        </div>
    );
}

export default Register;