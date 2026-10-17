import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getAuth, getProfil, getMeineProjekte, getMeineEinladungen } from "../services/api";
import { aenderePasswort } from "../services/profilService";
import "./Profile.css";

// Lesbare Bezeichnung fuer die Rolle aus der Datenbank
const ROLLEN_NAME = {
    STUDENT: "Student:in",
    PROFESSOR: "Betreuer:in",
    ADMIN: "Admin",
};

// Datum aus dem Backend (z.B. "2026-09-30T01:45:12") lesbar machen
function formatDatum(wert, mitUhrzeit = false) {
    if (!wert) return "–";
    const datum = new Date(wert);
    if (Number.isNaN(datum.getTime())) return "–";
    return mitUhrzeit
        ? datum.toLocaleString("de-DE", { dateStyle: "medium", timeStyle: "short" })
        : datum.toLocaleDateString("de-DE", { dateStyle: "long" });
}

function Profile() {
    const navigate = useNavigate();
    const auth = getAuth();
    const istBetreuer = auth?.rolle === "PROFESSOR";

    const [profil, setProfil] = useState(null);
    const [fehler, setFehler] = useState("");
    const [anzahlProjekte, setAnzahlProjekte] = useState(null);
    const [anzahlOffen, setAnzahlOffen] = useState(null);

    // Passwort aendern
    const [passwortOffen, setPasswortOffen] = useState(false);
    const [altesPasswort, setAltesPasswort] = useState("");
    const [neuesPasswort, setNeuesPasswort] = useState("");
    const [passwortWiederholung, setPasswortWiederholung] = useState("");
    const [passwortFehler, setPasswortFehler] = useState("");
    const [passwortErfolg, setPasswortErfolg] = useState("");
    const [passwortSpeichert, setPasswortSpeichert] = useState(false);

    useEffect(() => {
        if (!auth?.id) {
            navigate("/?redirect=/profile");
            return;
        }

        getProfil()
            .then(setProfil)
            .catch(() => setFehler("Profil konnte nicht geladen werden."));

        // Kennzahlen: Studierende -> eigene Projekte, Betreuer -> erhaltene Anfragen
        if (istBetreuer) {
            getMeineEinladungen()
                .then((liste) => {
                    const alle = liste || [];
                    setAnzahlProjekte(alle.filter((p) => p.status === "ANGENOMMEN").length);
                    setAnzahlOffen(alle.filter((p) => p.status === "OFFEN").length);
                })
                .catch(() => { });
        } else {
            getMeineProjekte(auth.id)
                .then((liste) => {
                    const alle = liste || [];
                    setAnzahlProjekte(alle.length);
                    setAnzahlOffen(alle.filter((p) => (p.status || "").toUpperCase() === "OFFEN").length);
                })
                .catch(() => { });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const passwortFormularSchliessen = () => {
        setPasswortOffen(false);
        setAltesPasswort("");
        setNeuesPasswort("");
        setPasswortWiederholung("");
        setPasswortFehler("");
    };

    const passwortSpeichern = async (event) => {
        event.preventDefault();
        setPasswortFehler("");
        setPasswortErfolg("");
        if (neuesPasswort.length < 8) {
            setPasswortFehler("Das neue Passwort muss mindestens 8 Zeichen lang sein.");
            return;
        }
        if (neuesPasswort !== passwortWiederholung) {
            setPasswortFehler("Die beiden neuen Passwörter stimmen nicht überein.");
            return;
        }
        setPasswortSpeichert(true);
        try {
            await aenderePasswort(altesPasswort, neuesPasswort);
            passwortFormularSchliessen();
            setPasswortErfolg("Ihr Passwort wurde geändert. Bitte melden Sie sich künftig mit dem neuen Passwort an.");
        } catch (error) {
            setPasswortFehler(error.message || "Das Passwort konnte nicht geändert werden.");
        } finally {
            setPasswortSpeichert(false);
        }
    };

    if (!auth?.id) {
        return null;
    }

    // Solange das Profil laedt, Werte aus dem Login verwenden
    const vorname = profil?.vorname || auth?.vorname || "";
    const name = profil?.name || auth?.name || "";
    const rolle = profil?.rolle || auth?.rolle || "";
    const initialen = `${vorname.charAt(0)}${name.charAt(0)}`.toUpperCase() || "?";

    return (
        <div>
            <Navbar />

            <div className="container py-4">

                <div className="mb-4">
                    <h2 className="mb-1">Mein Profil</h2>
                    <p className="text-muted mb-0">Ihre persönlichen Daten und Ihr SOPhub-Konto. Sie melden sich mit Ihrer E-Mail-Adresse an; Ihr Passwort können Sie hier ändern.</p>
                </div>

                {fehler && <div className="alert alert-warning">{fehler}</div>}

                <div className="row g-4">

                    {/* Linke Spalte: Profilkarte */}
                    <div className="col-lg-4">
                        <div className="card profil-kopf text-center">
                            <div className="profil-banner"></div>

                            <div className="px-4 pb-4">
                                <div className="profil-avatar">{initialen}</div>
                                <h3 className="profil-name">{vorname} {name}</h3>
                                <span className="badge bg-primary">{ROLLEN_NAME[rolle] || rolle}</span>
                                <div className="text-muted small mt-2">{profil?.email || ""}</div>
                            </div>

                            <div className="profil-zahlen">
                                <div>
                                    <span className="profil-zahl">{anzahlProjekte ?? "–"}</span>
                                    <span className="profil-zahl-text">
                                        {istBetreuer ? "Betreute Projekte" : "Eigene Projekte"}
                                    </span>
                                </div>
                                <div>
                                    <span className="profil-zahl">{anzahlOffen ?? "–"}</span>
                                    <span className="profil-zahl-text">
                                        {istBetreuer ? "Offene Anfragen" : "Warten auf Betreuer"}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Rechte Spalte: Angaben */}
                    <div className="col-lg-8 d-flex flex-column gap-4">

                        <div className="card">
                            <div className="card-body">
                                <h4 className="mb-2">Persönliche Daten</h4>

                                <dl className="profil-daten">
                                    <div>
                                        <dt>Vorname</dt>
                                        <dd>{vorname || "–"}</dd>
                                    </div>
                                    <div>
                                        <dt>Nachname</dt>
                                        <dd>{name || "–"}</dd>
                                    </div>
                                    <div>
                                        <dt>E-Mail-Adresse</dt>
                                        <dd>{profil?.email || "–"}</dd>
                                    </div>
                                </dl>
                            </div>
                        </div>

                        <div className="card">
                            <div className="card-body">
                                <h4 className="mb-2">Konto</h4>

                                <dl className="profil-daten">
                                    <div>
                                        <dt>Rolle</dt>
                                        <dd>{ROLLEN_NAME[rolle] || rolle || "–"}</dd>
                                    </div>
                                    <div>
                                        <dt>Mitglied seit</dt>
                                        <dd>{formatDatum(profil?.erstelltAm)}</dd>
                                    </div>
                                    <div>
                                        <dt>Letzte Anmeldung</dt>
                                        <dd>{formatDatum(profil?.lastLogin, true)}</dd>
                                    </div>
                                </dl>

                                <div className="form-text mt-2">
                                    Die Rolle wird automatisch über die E-Mail-Adresse vergeben
                                    (@hs-bochum.de = Betreuer:in, sonst Student:in).
                                </div>
                            </div>
                        </div>

                        <div className="card">
                            <div className="card-body">
                                <div className="d-flex justify-content-between align-items-center gap-2 mb-2">
                                    <h4 className="mb-0">Passwort</h4>
                                    {!passwortOffen && (
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-outline-primary"
                                            onClick={() => {
                                                setPasswortErfolg("");
                                                setPasswortOffen(true);
                                            }}
                                        >
                                            Passwort ändern
                                        </button>
                                    )}
                                </div>

                                {passwortErfolg && <div className="alert alert-success py-2 mb-0">{passwortErfolg}</div>}

                                {!passwortOffen && !passwortErfolg && (
                                    <p className="text-muted mb-0">
                                        Aus Sicherheitsgründen wird Ihr Passwort nicht angezeigt.
                                    </p>
                                )}

                                {passwortOffen && (
                                    <form className="profil-formular mt-2" onSubmit={passwortSpeichern}>
                                        {passwortFehler && <div className="alert alert-danger py-2">{passwortFehler}</div>}
                                        <div className="row g-3">
                                            <div className="col-12">
                                                <label className="form-label" htmlFor="profil-altes-passwort">Aktuelles Passwort</label>
                                                <input
                                                    id="profil-altes-passwort"
                                                    type="password"
                                                    className="form-control"
                                                    value={altesPasswort}
                                                    onChange={(e) => setAltesPasswort(e.target.value)}
                                                    autoComplete="current-password"
                                                    autoFocus
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label className="form-label" htmlFor="profil-neues-passwort">Neues Passwort</label>
                                                <input
                                                    id="profil-neues-passwort"
                                                    type="password"
                                                    className="form-control"
                                                    value={neuesPasswort}
                                                    onChange={(e) => setNeuesPasswort(e.target.value)}
                                                    autoComplete="new-password"
                                                />
                                                <div className="form-text">Mindestens 8 Zeichen.</div>
                                            </div>
                                            <div className="col-md-6">
                                                <label className="form-label" htmlFor="profil-passwort-wiederholung">Neues Passwort wiederholen</label>
                                                <input
                                                    id="profil-passwort-wiederholung"
                                                    type="password"
                                                    className="form-control"
                                                    value={passwortWiederholung}
                                                    onChange={(e) => setPasswortWiederholung(e.target.value)}
                                                    autoComplete="new-password"
                                                />
                                            </div>
                                        </div>
                                        <div className="d-flex gap-2 mt-3">
                                            <button className="btn btn-primary" type="submit" disabled={passwortSpeichert}>
                                                {passwortSpeichert ? "Speichert..." : "Passwort speichern"}
                                            </button>
                                            <button className="btn btn-outline-secondary" type="button" onClick={passwortFormularSchliessen}>
                                                Abbrechen
                                            </button>
                                        </div>
                                    </form>
                                )}
                            </div>
                        </div>

                        <div className="d-flex flex-wrap gap-2">
                            <Link className="btn btn-primary" to={istBetreuer ? "/einladungen" : "/my-projects"}>
                                {istBetreuer ? "Zu meinen Einladungen" : "Zu meinen Projekten"}
                            </Link>
                            <Link className="btn btn-outline-secondary" to="/dashboard">
                                Zum Dashboard
                            </Link>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
}

export default Profile;