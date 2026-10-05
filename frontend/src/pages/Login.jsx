import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import "./Auth.css";

// startModus: "login" (Standard) oder "register" (z.B. ueber die Adresse /register)
function Login({ startModus = "login" }) {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    // Ziel nach dem Login, z.B. /?redirect=/project-invitation/3 (Link aus der Projektanfrage-Mail).
    // Nur interne Pfade erlauben (beginnt mit "/", aber nicht mit "//"),
    // damit niemand ueber den Link auf eine fremde Webseite weiterleiten kann.
    const redirectParam = searchParams.get("redirect");
    const zielNachLogin =
        redirectParam && redirectParam.startsWith("/") && !redirectParam.startsWith("//")
            ? redirectParam
            : "/dashboard";

    const [mode, setMode] = useState(startModus);
    const [benutzername, setBenutzername] = useState("");
    const [email, setEmail] = useState("");
    const [name, setName] = useState("");
    const [vorname, setVorname] = useState("");
    const [passwort, setPasswort] = useState("");
    const [passwortBestaetigen, setPasswortBestaetigen] = useState("");
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("");

    const switchMode = () => {
        setMode(mode === "login" ? "register" : "login");
        setBenutzername("");
        setEmail("");
        setName("");
        setVorname("");
        setPasswort("");
        setPasswortBestaetigen("");
        setMessage("");
        setMessageType("");
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!benutzername || !passwort) {
            setMessage("Bitte Benutzername und Passwort eingeben.");
            setMessageType("error");
            return;
        }

        if (mode === "register") {
            if (!email || !name || !vorname || !passwortBestaetigen) {
                setMessage("Bitte alle Felder ausfüllen.");
                setMessageType("error");
                return;
            }

            if (passwort !== passwortBestaetigen) {
                setMessage("Die Passwörter stimmen nicht überein.");
                setMessageType("error");
                return;
            }
        }

        try {
            const endpoint = mode === "login" ? "login" : "register";

            const body =
                mode === "login"
                    ? { benutzername, passwort }
                    : {
                        benutzername,
                        email,
                        name,
                        vorname,
                        passwort,
                    };

            const response = await fetch(
                `http://localhost:8080/sop/api/auth/${endpoint}`,
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(body),
                }
            );

            const text = await response.text();

            if (response.ok) {
                if (mode === "register") {
                    // Hinweis "Bitte E-Mail bestaetigen" stehen lassen und zur Anmeldung wechseln.
                    // (Nicht switchMode() benutzen: das wuerde die Meldung wieder loeschen.)
                    setMode("login");
                    setPasswort("");
                    setPasswortBestaetigen("");
                    setEmail("");
                    setName("");
                    setVorname("");
                    setMessage(text || "Registrierung erfolgreich. Bitte bestätigen Sie Ihre E-Mail-Adresse.");
                    setMessageType("success");
                } else {
                    const daten = JSON.parse(text);
                    localStorage.setItem(
                        "sophub_auth",
                        JSON.stringify({
                            token: daten.token,
                            id: daten.id,
                            benutzername: daten.benutzername,
                            vorname: daten.vorname,
                            name: daten.name,
                            rolle: daten.rolle,
                        })
                    );
                    navigate(zielNachLogin);
                }
            } else {
                setMessage(text || "Fehler bei der Anfrage.");
                setMessageType("error");
            }
        } catch (error) {
            if (mode === "login") {
                setMessage("Server nicht erreichbar. Dummy-Login wird verwendet.");
                setMessageType("error");

                setTimeout(() => {
                    navigate("/dashboard");
                }, 800);
            } else {
                setMessage("Server nicht erreichbar. Registrierung aktuell nicht möglich.");
                setMessageType("error");
            }
        }
    };

    const istLogin = mode === "login";

    return (
        <div className="auth-seite">

            {/* Linke Seite: Marke und Vorteile (nur auf grossen Bildschirmen) */}
            <aside className="auth-marke d-none d-lg-flex">
                <div className="auth-logo">
                    <span className="auth-logo-zeichen">S</span>
                    <span>SOP<span className="auth-akzent">hub</span></span>
                </div>

                <div>
                    <h1 className="auth-titel">
                        Von der Idee bis zum Abschluss –<br />
                        <span className="auth-akzent">alles an einem Ort.</span>
                    </h1>

                    <p className="auth-untertitel">
                        Das Projektportal für Softwareprojekte an der Hochschule Bochum.
                    </p>

                    <ul className="auth-vorteile">
                        <li>
                            <span className="auth-haken">✓</span>
                            Projekte anlegen, Teams bilden und den Status im Blick behalten
                        </li>
                        <li>
                            <span className="auth-haken">✓</span>
                            Betreuer:innen per E-Mail einladen – Annahme mit einem Klick
                        </li>
                        <li>
                            <span className="auth-haken">✓</span>
                            Lastenheft, Pflichtenheft und Datenbankmodell zentral ablegen
                        </li>
                    </ul>
                </div>

                <p className="auth-fusszeile">Hochschule Bochum · Softwareprojekt SOPhub</p>
            </aside>

            {/* Rechte Seite: Formular */}
            <main className="auth-bereich">
                <div className="auth-karte">

                    {/* Logo nur auf kleinen Bildschirmen */}
                    <div className="auth-logo auth-logo-dunkel d-lg-none mb-4">
                        <span className="auth-logo-zeichen">S</span>
                        <span>SOP<span className="auth-akzent-dunkel">hub</span></span>
                    </div>

                    <h2 className="mb-1">{istLogin ? "Willkommen zurück" : "Konto erstellen"}</h2>
                    <p className="text-muted mb-4">
                        {istLogin
                            ? "Melden Sie sich mit Ihrem SOPhub-Konto an."
                            : "Registrieren Sie sich mit Ihrer Hochschul-E-Mail-Adresse."}
                    </p>

                    {redirectParam && !message && (
                        <div className="alert alert-info py-2 small">
                            Bitte melden Sie sich an, um die Projektanfrage zu öffnen.
                        </div>
                    )}

                    {message && (
                        <div className={`alert py-2 small ${messageType === "error" ? "alert-danger" : "alert-success"}`}>
                            {message}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} noValidate>
                        <div className="mb-3">
                            <label className="form-label" htmlFor="auth-benutzername">Benutzername</label>
                            <input
                                id="auth-benutzername"
                                type="text"
                                className="form-control"
                                placeholder="z. B. s123456"
                                autoComplete="username"
                                value={benutzername}
                                onChange={(e) => setBenutzername(e.target.value)}
                            />
                        </div>

                        {!istLogin && (
                            <>
                                <div className="mb-3">
                                    <label className="form-label" htmlFor="auth-email">E-Mail-Adresse</label>
                                    <input
                                        id="auth-email"
                                        type="email"
                                        className="form-control"
                                        placeholder="name@stud.hs-bochum.de"
                                        autoComplete="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                    />
                                    <div className="form-text">
                                        Studierende: @stud.hs-bochum.de · Lehrende: @hs-bochum.de
                                    </div>
                                </div>

                                <div className="row">
                                    <div className="col-6 mb-3">
                                        <label className="form-label" htmlFor="auth-vorname">Vorname</label>
                                        <input
                                            id="auth-vorname"
                                            type="text"
                                            className="form-control"
                                            autoComplete="given-name"
                                            value={vorname}
                                            onChange={(e) => setVorname(e.target.value)}
                                        />
                                    </div>

                                    <div className="col-6 mb-3">
                                        <label className="form-label" htmlFor="auth-name">Nachname</label>
                                        <input
                                            id="auth-name"
                                            type="text"
                                            className="form-control"
                                            autoComplete="family-name"
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                        />
                                    </div>
                                </div>
                            </>
                        )}

                        <div className="mb-3">
                            <label className="form-label" htmlFor="auth-passwort">Passwort</label>
                            <input
                                id="auth-passwort"
                                type="password"
                                className="form-control"
                                placeholder="••••••••"
                                autoComplete={istLogin ? "current-password" : "new-password"}
                                value={passwort}
                                onChange={(e) => setPasswort(e.target.value)}
                            />
                        </div>

                        {!istLogin && (
                            <div className="mb-3">
                                <label className="form-label" htmlFor="auth-passwort2">Passwort bestätigen</label>
                                <input
                                    id="auth-passwort2"
                                    type="password"
                                    className="form-control"
                                    placeholder="••••••••"
                                    autoComplete="new-password"
                                    value={passwortBestaetigen}
                                    onChange={(e) => setPasswortBestaetigen(e.target.value)}
                                />
                            </div>
                        )}

                        <button className="btn btn-primary w-100 py-2 mt-2" type="submit">
                            {istLogin ? "Anmelden" : "Registrieren"}
                        </button>
                    </form>

                    <p className="text-center text-muted mt-4 mb-0">
                        {istLogin ? "Noch kein Konto?" : "Bereits registriert?"}{" "}
                        <button className="auth-wechsel" type="button" onClick={switchMode}>
                            {istLogin ? "Jetzt registrieren" : "Zur Anmeldung"}
                        </button>
                    </p>
                </div>
            </main>
        </div>
    );
}

export default Login;