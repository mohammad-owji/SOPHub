import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Auth.css";

function Login() {
    const navigate = useNavigate();

    const [mode, setMode] = useState("login");
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
                    setMessage(text || "Registrierung erfolgreich.");
                    setMessageType("success");

                    setTimeout(() => {
                        switchMode();
                    }, 1500);
                } else {
                    navigate("/dashboard");
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

    return (
        <div className="auth-page">
            <div className="login-box">
                <div className="logo-circle">SOP</div>

                {message && <div className={`message ${messageType}`}>{message}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="input-group-auth">
                        <span className="icon">👤</span>
                        <input
                            type="text"
                            placeholder="Benutzername"
                            value={benutzername}
                            onChange={(e) => setBenutzername(e.target.value)}
                        />
                    </div>

                    {mode === "register" && (
                        <>
                            <div className="input-group-auth">
                                <span className="icon">✉</span>
                                <input
                                    type="email"
                                    placeholder="E-Mail (@stud.hs-bochum.de oder @hs-bochum.de)"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                />
                            </div>

                            <div className="input-group-auth">
                                <span className="icon">👤</span>
                                <input
                                    type="text"
                                    placeholder="Vorname"
                                    value={vorname}
                                    onChange={(e) => setVorname(e.target.value)}
                                />
                            </div>

                            <div className="input-group-auth">
                                <span className="icon">👤</span>
                                <input
                                    type="text"
                                    placeholder="Name"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                />
                            </div>
                        </>
                    )}

                    <div className="input-group-auth">
                        <span className="icon">🔒</span>
                        <input
                            type="password"
                            placeholder="Passwort"
                            value={passwort}
                            onChange={(e) => setPasswort(e.target.value)}
                        />
                    </div>

                    {mode === "register" && (
                        <div className="input-group-auth">
                            <span className="icon">🔒</span>
                            <input
                                type="password"
                                placeholder="Passwort bestätigen"
                                value={passwortBestaetigen}
                                onChange={(e) => setPasswortBestaetigen(e.target.value)}
                            />
                        </div>
                    )}

                    <button className="main-btn" type="submit">
                        {mode === "login" ? "Anmelden" : "Registrieren"}
                    </button>
                </form>

                <button className="switch-link" type="button" onClick={switchMode}>
                    {mode === "login"
                        ? "Noch kein Konto? Registrieren"
                        : "Bereits registriert? Anmelden"}
                </button>
            </div>
        </div>
    );
}

export default Login;