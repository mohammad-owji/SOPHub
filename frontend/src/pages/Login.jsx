import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import "./Auth.css";

// startModus: "login" (Standard) oder "register"
function Login({ startModus = "login" }) {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    // Nur interne Pfade als Weiterleitungsziel erlauben.
    const redirectParam = searchParams.get("redirect");
    const zielNachLogin =
        redirectParam &&
            redirectParam.startsWith("/") &&
            !redirectParam.startsWith("//")
            ? redirectParam
            : "/dashboard";

    const [mode, setMode] = useState(startModus);
    const [email, setEmail] = useState("");
    const [passwort, setPasswort] = useState("");
    const [passwortBestaetigen, setPasswortBestaetigen] = useState("");
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("");

    const switchMode = () => {
        setMode(mode === "login" ? "register" : "login");
        setEmail("");
        setPasswort("");
        setPasswortBestaetigen("");
        setMessage("");
        setMessageType("");
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!email.trim() || !passwort) {
            setMessage("Bitte E-Mail und Passwort eingeben.");
            setMessageType("error");
            return;
        }

        if (mode === "register") {
            if (!passwortBestaetigen) {
                setMessage("Bitte alle Felder ausfüllen.");
                setMessageType("error");
                return;
            }

            if (passwort.length < 8) {
                setMessage("Das Passwort muss mindestens 8 Zeichen lang sein.");
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

            const body = {
                email: email.trim().toLowerCase(),
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
                    setMessage(text || "Registrierung erfolgreich. Bitte bestätigen Sie Ihre E-Mail-Adresse.");
                    setMessageType("success");
                } else {
                    const daten = JSON.parse(text);

                    if (!daten?.token) {
                        throw new Error(
                            "Die Serverantwort enthält keinen Login-Token."
                        );
                    }

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
            console.error("Authentifizierung fehlgeschlagen:", error);
            setMessageType("error");
            setMessage(
                error instanceof TypeError
                    ? "Backend nicht erreichbar oder Anfrage durch CORS blockiert."
                    : `Authentifizierung fehlgeschlagen: ${error.message}`
            );
        }
    };

    const istLogin = mode === "login";

    return (
        <div className="auth-seite">
            {/* Linke Seite: Marke und Vorteile */}
            <aside className="auth-marke d-none d-lg-flex">
                <div className="auth-logo">
                    <span className="auth-logo-zeichen">S</span>
                    <span>
                        SOP<span className="auth-akzent">hub</span>
                    </span>
                </div>

                <div>
                    <h1 className="auth-titel">
                        Von der Idee bis zum Abschluss –
                        <br />
                        <span className="auth-akzent">
                            alles an einem Ort.
                        </span>
                    </h1>

                    <p className="auth-untertitel">
                        Das Projektportal für Softwareprojekte an der
                        Hochschule Bochum.
                    </p>

                    <ul className="auth-vorteile">
                        <li>
                            <span className="auth-haken">✓</span>
                            Projekte anlegen, Teams bilden und den Status im
                            Blick behalten
                        </li>
                        <li>
                            <span className="auth-haken">✓</span>
                            Betreuer:innen per E-Mail einladen – Annahme mit
                            einem Klick
                        </li>
                        <li>
                            <span className="auth-haken">✓</span>
                            Lastenheft, Pflichtenheft und Datenbankmodell
                            zentral ablegen
                        </li>
                    </ul>
                </div>

                <p className="auth-fusszeile">
                    Hochschule Bochum · Softwareprojekt SOPhub
                </p>
            </aside>

            {/* Rechte Seite: Formular */}
            <main className="auth-bereich">
                <div className="auth-karte">
                    {/* Logo nur auf kleinen Bildschirmen */}
                    <div className="auth-logo auth-logo-dunkel d-lg-none mb-4">
                        <span className="auth-logo-zeichen">S</span>
                        <span>
                            SOP
                            <span className="auth-akzent-dunkel">hub</span>
                        </span>
                    </div>

                    <h2 className="mb-1">
                        {istLogin ? "Willkommen zurück" : "Konto erstellen"}
                    </h2>

                    <p className="text-muted mb-4">
                        {istLogin
                            ? "Melden Sie sich mit Ihrer E-Mail-Adresse und Ihrem Passwort an."
                            : "Registrieren Sie sich mit Ihrer Hochschul-E-Mail-Adresse."}
                    </p>

                    {redirectParam && !message && (
                        <div className="alert alert-info py-2 small">
                            Bitte melden Sie sich an, um die Projektanfrage
                            zu öffnen.
                        </div>
                    )}

                    {message && (
                        <div
                            className={`alert py-2 small ${messageType === "error"
                                    ? "alert-danger"
                                    : "alert-success"
                                }`}
                        >
                            {message}
                        </div>
                    )}

                    <form
                        onSubmit={handleSubmit}
                        onKeyDown={(event) => {
                            if (
                                event.key === "Enter" &&
                                event.target.tagName === "INPUT" &&
                                !event.nativeEvent.isComposing
                            ) {
                                event.preventDefault();
                                event.currentTarget.requestSubmit();
                            }
                        }}
                        noValidate
                    >
                        <div className="mb-3">
                            <label
                                className="form-label"
                                htmlFor="auth-email"
                            >
                                E-Mail-Adresse
                            </label>

                            <input
                                id="auth-email"
                                type="email"
                                className="form-control"
                                placeholder="name@stud.hs-bochum.de"
                                autoComplete="username"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />

                            {!istLogin && (
                                <div className="form-text">
                                    Studierende: @stud.hs-bochum.de ·
                                    Lehrende: @hs-bochum.de
                                </div>
                            )}
                        </div>

                        <div className="mb-3">
                            <label
                                className="form-label"
                                htmlFor="auth-passwort"
                            >
                                Passwort
                            </label>

                            <input
                                id="auth-passwort"
                                type="password"
                                className="form-control"
                                placeholder="••••••••"
                                autoComplete={
                                    istLogin
                                        ? "current-password"
                                        : "new-password"
                                }
                                value={passwort}
                                onChange={(e) => setPasswort(e.target.value)}
                            />
                            {!istLogin && (
                                <div className="form-text">Mindestens 8 Zeichen.</div>
                            )}
                        </div>

                        {!istLogin && (
                            <div className="mb-3">
                                <label
                                    className="form-label"
                                    htmlFor="auth-passwort2"
                                >
                                    Passwort bestätigen
                                </label>

                                <input
                                    id="auth-passwort2"
                                    type="password"
                                    className="form-control"
                                    placeholder="••••••••"
                                    autoComplete="new-password"
                                    value={passwortBestaetigen}
                                    onChange={(e) =>
                                        setPasswortBestaetigen(e.target.value)
                                    }
                                />
                            </div>
                        )}

                        <button
                            className="btn btn-primary w-100 py-2 mt-2"
                            type="submit"
                        >
                            {istLogin ? "Anmelden" : "Registrieren"}
                        </button>
                    </form>

                    <p className="text-center text-muted mt-4 mb-0">
                        {istLogin
                            ? "Noch kein Konto?"
                            : "Bereits registriert?"}{" "}
                        <button
                            className="auth-wechsel"
                            type="button"
                            onClick={switchMode}
                        >
                            {istLogin
                                ? "Jetzt registrieren"
                                : "Zur Anmeldung"}
                        </button>
                    </p>
                </div>
            </main>
        </div>
    );
}

export default Login;