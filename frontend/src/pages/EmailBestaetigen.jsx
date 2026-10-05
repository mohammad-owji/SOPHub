import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import "./Auth.css";

const API_URL = "http://localhost:8080/sop/api/auth/bestaetigen";

/**
 * Seite fuer den Link aus der Bestaetigungs-Mail:
 * /email-bestaetigen?token=...
 * Schickt den Token ans Backend und zeigt das Ergebnis an.
 */
function EmailBestaetigen() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get("token");

    // "laedt" | "erfolg" | "fehler"
    const [zustand, setZustand] = useState("laedt");
    const [meldung, setMeldung] = useState("");

    // React ruft useEffect im Entwicklungsmodus (StrictMode) absichtlich ZWEIMAL auf.
    // Der Token funktioniert aber nur einmal - der zweite Aufruf wuerde sonst
    // "bereits verwendet" melden. useRef merkt sich, dass wir schon gesendet haben.
    const schonGesendet = useRef(false);

    useEffect(() => {
        if (schonGesendet.current) return;
        schonGesendet.current = true;

        if (!token) {
            setZustand("fehler");
            setMeldung("Der Bestätigungslink ist unvollständig. Bitte öffnen Sie den Link direkt aus der E-Mail.");
            return;
        }

        fetch(`${API_URL}?token=${encodeURIComponent(token)}`)
            .then(async (response) => {
                const text = await response.text();
                setZustand(response.ok ? "erfolg" : "fehler");
                setMeldung(text || (response.ok ? "E-Mail-Adresse bestätigt." : "Bestätigung fehlgeschlagen."));
            })
            .catch(() => {
                setZustand("fehler");
                setMeldung("Der Server ist nicht erreichbar. Bitte versuchen Sie es später erneut.");
            });
    }, [token]);

    return (
        <div className="auth-seite" style={{ gridTemplateColumns: "1fr" }}>
            <main className="auth-bereich">
                <div className="auth-karte text-center">

                    <div className="auth-logo auth-logo-dunkel justify-content-center mb-4">
                        <span className="auth-logo-zeichen">S</span>
                        <span>SOP<span className="auth-akzent-dunkel">hub</span></span>
                    </div>

                    {zustand === "laedt" && (
                        <>
                            <div className="spinner-border text-primary mb-3" role="status" aria-hidden="true"></div>
                            <h2 className="mb-2">E-Mail wird bestätigt …</h2>
                            <p className="text-muted mb-0">Einen Moment bitte.</p>
                        </>
                    )}

                    {zustand === "erfolg" && (
                        <>
                            <div className="auth-status-symbol auth-status-erfolg">✓</div>
                            <h2 className="mb-2">E-Mail bestätigt</h2>
                            <p className="text-muted mb-4">{meldung}</p>
                            <Link to="/" className="btn btn-primary w-100 py-2">Zur Anmeldung</Link>
                        </>
                    )}

                    {zustand === "fehler" && (
                        <>
                            <div className="auth-status-symbol auth-status-fehler">!</div>
                            <h2 className="mb-2">Bestätigung nicht möglich</h2>
                            <p className="text-muted mb-4">{meldung}</p>
                            <Link to="/register" className="btn btn-primary w-100 py-2 mb-2">Erneut registrieren</Link>
                            <Link to="/" className="btn btn-outline-secondary w-100 py-2">Zur Anmeldung</Link>
                        </>
                    )}
                </div>
            </main>
        </div>
    );
}

export default EmailBestaetigen;