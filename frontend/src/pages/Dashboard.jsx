import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProjektStatusBadge from "../components/ProjektStatus";
import { getAuth, getAlleProjekte, getMeineProjekte, getMeineEinladungen } from "../services/api";
import "./Dashboard.css";

// Schnellzugriff rechts (nur Seiten, die es wirklich gibt)
const SCHNELLZUGRIFF = [
    { ziel: "/create-project", titel: "Projekt erstellen", text: "Neue Projektidee anlegen und Betreuer:in anfragen" },
    { ziel: "/my-projects", titel: "Meine Projekte", text: "Eigene Projekte ansehen und bearbeiten" },
    { ziel: "/projects", titel: "Alle Projekte", text: "Laufende und abgeschlossene Projekte durchsuchen" },
    { ziel: "/einladungen", titel: "Einladungen", text: "Projektanfragen annehmen oder ablehnen" },
    { ziel: "/profile", titel: "Profil", text: "Persönliche Daten verwalten" },
];

function Dashboard() {
    const navigate = useNavigate();
    const auth = getAuth();
    const istBetreuer = auth?.rolle === "PROFESSOR";

    const [alleProjekte, setAlleProjekte] = useState([]);
    const [meineProjekte, setMeineProjekte] = useState([]);
    const [einladungen, setEinladungen] = useState([]);
    const [laedt, setLaedt] = useState(true);
    const [fehler, setFehler] = useState("");

    useEffect(() => {
        if (!auth?.id) {
            navigate("/");
            return;
        }

        // Studierende: eigene Projekte / Betreuer: erhaltene Anfragen
        const eigene = istBetreuer ? Promise.resolve([]) : getMeineProjekte(auth.id);
        const anfragen = istBetreuer ? getMeineEinladungen() : Promise.resolve([]);

        Promise.all([getAlleProjekte(), eigene, anfragen])
            .then(([alle, meine, einl]) => {
                setAlleProjekte(alle || []);
                setMeineProjekte(meine || []);
                setEinladungen(einl || []);
            })
            .catch(() => setFehler("Daten konnten nicht geladen werden. Läuft das Backend?"))
            .finally(() => setLaedt(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const zaehle = (liste, status) =>
        liste.filter((p) => (p.status || "").toUpperCase() === status).length;

    // Kennzahlen je nach Rolle
    const kacheln = istBetreuer
        ? [
            { symbol: "✉", zahl: zaehle(einladungen, "OFFEN"), text: "Offene Anfragen", ziel: "/einladungen" },
            { symbol: "✓", zahl: zaehle(einladungen, "ANGENOMMEN"), text: "Betreute Projekte", ziel: "/einladungen" },
            { symbol: "✕", zahl: zaehle(einladungen, "ABGELEHNT"), text: "Abgelehnte Anfragen", ziel: "/einladungen" },
            { symbol: "▦", zahl: alleProjekte.length, text: "Projekte im Portal", ziel: "/projects" },
        ]
        : [
            { symbol: "▤", zahl: meineProjekte.length, text: "Meine Projekte", ziel: "/my-projects" },
            { symbol: "⏳", zahl: zaehle(meineProjekte, "OFFEN"), text: "Warten auf Betreuer", ziel: "/my-projects" },
            { symbol: "✓", zahl: zaehle(meineProjekte, "ANGENOMMEN"), text: "Angenommen", ziel: "/my-projects" },
            { symbol: "▦", zahl: alleProjekte.length, text: "Projekte im Portal", ziel: "/projects" },
        ];

    // Die 5 neuesten Eintraege (hoechste ID zuerst)
    const liste = (istBetreuer ? einladungen : meineProjekte)
        .slice()
        .sort((a, b) => b.id - a.id)
        .slice(0, 5);

    return (
        <div>
            <Navbar />

            <div className="container py-4">

                {/* Begruessung */}
                <section className="dash-hero mb-4">
                    <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                        <div>
                            <h1>Willkommen zurück, {auth?.vorname || "Gast"}</h1>
                            <p>
                                {istBetreuer
                                    ? "Hier sehen Sie Ihre Projektanfragen und betreuten Projekte auf einen Blick."
                                    : "Von der Idee bis zum erfolgreichen SOP-Projekt – hier ist Ihr aktueller Stand."}
                            </p>
                        </div>

                        <div className="d-flex gap-2 flex-wrap">
                            {istBetreuer ? (
                                <Link className="btn btn-light" to="/einladungen">Anfragen ansehen</Link>
                            ) : (
                                <Link className="btn btn-light" to="/create-project">+ Neues Projekt</Link>
                            )}
                            <Link className="btn btn-outline-light" to="/projects">Alle Projekte</Link>
                        </div>
                    </div>
                </section>

                {fehler && <div className="alert alert-warning">{fehler}</div>}

                {/* Kennzahlen */}
                <div className="row g-3 mb-4">
                    {kacheln.map((kachel) => (
                        <div className="col-6 col-lg-3" key={kachel.text}>
                            <Link className="card card-body dash-kachel" to={kachel.ziel}>
                                <span className="dash-kachel-symbol">{kachel.symbol}</span>
                                <div>
                                    <div className="dash-kachel-zahl">{laedt ? "–" : kachel.zahl}</div>
                                    <div className="dash-kachel-text">{kachel.text}</div>
                                </div>
                            </Link>
                        </div>
                    ))}
                </div>

                <div className="row g-4">

                    {/* Letzte Projekte bzw. Anfragen */}
                    <div className="col-lg-8">
                        <div className="card h-100">
                            <div className="card-body">
                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <h4 className="mb-0">
                                        {istBetreuer ? "Neueste Anfragen" : "Meine letzten Projekte"}
                                    </h4>
                                    <Link
                                        className="small fw-semibold text-decoration-none"
                                        to={istBetreuer ? "/einladungen" : "/my-projects"}
                                    >
                                        Alle anzeigen →
                                    </Link>
                                </div>

                                {laedt ? (
                                    <p className="text-muted mb-0 py-3">Wird geladen...</p>
                                ) : liste.length === 0 ? (
                                    <div className="text-center py-4">
                                        <p className="text-muted mb-3">
                                            {istBetreuer
                                                ? "Sie haben noch keine Projektanfragen erhalten."
                                                : "Sie haben noch keine eigenen Projekte."}
                                        </p>
                                        {!istBetreuer && (
                                            <Link className="btn btn-primary" to="/create-project">
                                                Erstes Projekt erstellen
                                            </Link>
                                        )}
                                    </div>
                                ) : (
                                    liste.map((projekt) => (
                                        <Link
                                            className="dash-eintrag"
                                            key={projekt.id}
                                            to={istBetreuer && (projekt.status || "").toUpperCase() === "OFFEN"
                                                ? `/project-invitation/${projekt.id}`
                                                : `/projectdetails/${projekt.id}`}
                                        >
                                            <div className="text-truncate">
                                                <div className="dash-eintrag-titel text-truncate">{projekt.titel}</div>
                                                <div className="dash-eintrag-info">
                                                    {[
                                                        projekt.semester,
                                                        istBetreuer && projekt.student
                                                            ? `von ${projekt.student.vorname} ${projekt.student.name}`
                                                            : projekt.betreuer
                                                                ? `Betreuung: ${projekt.betreuer.vorname} ${projekt.betreuer.name}`
                                                                : null,
                                                    ].filter(Boolean).join(" · ") || "–"}
                                                </div>
                                            </div>
                                            <ProjektStatusBadge status={projekt.status} className="flex-shrink-0" />
                                        </Link>
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Schnellzugriff */}
                    <div className="col-lg-4">
                        <div className="card h-100">
                            <div className="card-body">
                                <h4 className="mb-2">Schnellzugriff</h4>

                                {SCHNELLZUGRIFF.map((eintrag) => (
                                    <Link className="dash-eintrag" to={eintrag.ziel} key={eintrag.ziel}>
                                        <div>
                                            <div className="dash-eintrag-titel">{eintrag.titel}</div>
                                            <div className="dash-eintrag-info">{eintrag.text}</div>
                                        </div>
                                        <span className="dash-pfeil">›</span>
                                    </Link>
                                ))}
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
}

export default Dashboard;