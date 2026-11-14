import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar, { EINLADUNGEN_GEAENDERT } from "../components/Navbar";
import ProjektStatusBadge from "../components/ProjektStatus";
import { getAuth, getMeineEinladungen } from "../services/api";
import { getMeineCoEinladungen, beantworteCoEinladung } from "../services/betreuungService";
import "./Einladungen.css";

function MyInvitations() {

    const navigate = useNavigate();
    const auth = getAuth();

    const [einladungen, setEinladungen] = useState([]);
    // Einladungen zur Co-Betreuung (an die eigene Hochschul-E-Mail)
    const [coEinladungen, setCoEinladungen] = useState([]);
    const [coFehler, setCoFehler] = useState("");
    const [coSendet, setCoSendet] = useState(null);
    const [laedt, setLaedt] = useState(true);
    const [error, setError] = useState("");
    // Angenommene und abgelehnte Anfragen sind standardmaessig eingeklappt
    const [aufgeklappt, setAufgeklappt] = useState({ angenommen: false, abgelehnt: false, co: false });

    const ladeCoEinladungen = () =>
        getMeineCoEinladungen()
            .then((data) => setCoEinladungen(data || []))
            .catch(() => setCoEinladungen([]));

    useEffect(() => {

        // Nicht eingeloggt? Dann zum Login und danach wieder hierher zurueck.
        if (!auth?.token) {
            navigate("/?redirect=/einladungen");
            return;
        }

        ladeCoEinladungen();

        getMeineEinladungen()
            .then((data) => setEinladungen(data || []))
            .catch((err) => {
                console.error(err);
                setError("Einladungen konnten nicht geladen werden.");
            })
            .finally(() => setLaedt(false));

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Offene Anfragen oben, angenommene darunter, abgelehnte in einem eingeklappten Bereich
    const statusVon = (p) => (p.status || "").toUpperCase();
    const offene = einladungen.filter((p) => statusVon(p) === "OFFEN");
    const abgelehnte = einladungen.filter((p) => statusVon(p) === "ABGELEHNT");
    const angenommene = einladungen.filter((p) => !["OFFEN", "ABGELEHNT"].includes(statusVon(p)));

    const offeneCo = coEinladungen.filter((e) => e.status === "OFFEN");
    const erledigteCo = coEinladungen.filter((e) => e.status !== "OFFEN");

    const coBeantworten = async (einladung, annehmen) => {
        const istUebergabe = einladung.art === "UEBERGABE";
        if (!annehmen && !window.confirm(istUebergabe
            ? `Übernahme der Betreuung von „${einladung.projekt.titel}“ ablehnen?`
            : `Einladung zur Co-Betreuung von „${einladung.projekt.titel}“ ablehnen?`)) {
            return;
        }
        if (annehmen && istUebergabe && !window.confirm(
            `Die Betreuung von „${einladung.projekt.titel}“ übernehmen? Sie sind danach die Betreuer:in des Projekts.`)) {
            return;
        }
        setCoFehler("");
        setCoSendet(einladung.id);
        try {
            await beantworteCoEinladung(einladung.id, annehmen);
            await ladeCoEinladungen();
            window.dispatchEvent(new Event(EINLADUNGEN_GEAENDERT));
            if (annehmen) {
                navigate(`/projectdetails/${einladung.projekt.id}`);
            }
        } catch (error) {
            setCoFehler(error.message || "Die Antwort konnte nicht gespeichert werden.");
        } finally {
            setCoSendet(null);
        }
    };

    if (!auth?.token) {
        return null;
    }

    const CO_STATUS = {
        OFFEN: "Offen",
        ANGENOMMEN: "Angenommen",
        ABGELEHNT: "Abgelehnt",
        BEENDET: "Beendet",
    };

    // Karte fuer eine Einladung zur Co-Betreuung oder eine Uebergabe der Betreuung
    const coKarte = (einladung) => {
        const istOffen = einladung.status === "OFFEN";
        const istUebergabe = einladung.art === "UEBERGABE";
        const p = einladung.projekt;
        return (
            <div className="col-md-6 col-xl-4" key={`co-${einladung.id}`}>
                <div className={`card einl-karte ${istOffen ? (istUebergabe ? "einl-karte-uebergabe" : "einl-karte-co") : ""}`}>
                    <div className="card-body">
                        <div className="d-flex justify-content-between align-items-start gap-2">
                            <h3 className="einl-titel">{p.titel}</h3>
                            <span className={`einl-co-badge einl-co-${einladung.status.toLowerCase()}`}>
                                {CO_STATUS[einladung.status] || einladung.status}
                            </span>
                        </div>

                        <span className={`einl-art ${istUebergabe ? "einl-art-uebergabe" : ""}`}>
                            {istUebergabe ? "Übergabe der Betreuung" : "Co-Betreuung"}
                        </span>

                        <div className="einl-von">
                            {istUebergabe ? "Angefragt von" : "Eingeladen von"}{" "}
                            <strong>
                                {einladung.eingeladenVon
                                    ? `${einladung.eingeladenVon.vorname} ${einladung.eingeladenVon.name}`
                                    : "–"}
                            </strong>
                        </div>

                        <div className="einl-meta">
                            <span><strong>Team:</strong> {p.student ? `${p.student.vorname} ${p.student.name}` : "–"}</span>
                            <span><strong>Projektart:</strong> {p.projektart || "–"}</span>
                            <span><strong>Semester:</strong> {p.semester || "–"}</span>
                        </div>

                        {einladung.nachricht && (
                            <div className="einl-nachricht">
                                {istUebergabe ? "Grund: " : ""}„{einladung.nachricht}“
                            </div>
                        )}

                        {istUebergabe && istOffen && (
                            <div className="einl-hinweis">
                                Wenn Sie annehmen, sind Sie die neue Betreuer:in. Mitteilungen, Abgaben und Dokumente
                                bleiben erhalten.
                                {einladung.alsCoBleiben && " Die bisherige Betreuung bleibt als Co-Betreuung im Projekt."}
                            </div>
                        )}

                        <div className="mt-auto pt-2 d-flex flex-column gap-2">
                            {istOffen ? (
                                <div className="d-flex gap-2">
                                    <button className="btn btn-primary flex-grow-1" disabled={coSendet === einladung.id}
                                        onClick={() => coBeantworten(einladung, true)}>
                                        ✓ Annehmen
                                    </button>
                                    <button className="btn btn-outline-danger flex-grow-1" disabled={coSendet === einladung.id}
                                        onClick={() => coBeantworten(einladung, false)}>
                                        ✕ Ablehnen
                                    </button>
                                </div>
                            ) : einladung.status === "ANGENOMMEN" ? (
                                <button className="btn btn-outline-secondary w-100"
                                    onClick={() => navigate(`/projectdetails/${p.id}`)}>
                                    Zum Projekt
                                </button>
                            ) : null}
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    const einladungsKarte = (projekt) => {
        const istOffen = projekt.status === "OFFEN";
        // Nach der Annahme kann die Betreuung die Mitteilungen des Projekts lesen und beantworten
        const istAngenommen = ["ANGENOMMEN", "ABGESCHLOSSEN"].includes((projekt.status || "").toUpperCase());

        return (
            <div className="col-md-6 col-xl-4" key={projekt.id}>
                <div className={`card einl-karte ${istOffen ? "einl-karte-offen" : ""}`}>
                    <div className="card-body">

                        <div className="d-flex justify-content-between align-items-start gap-2">
                            <h3 className="einl-titel">{projekt.titel}</h3>
                            <ProjektStatusBadge status={projekt.status} className="flex-shrink-0" />
                        </div>

                        <div className="einl-von">
                            Eingeladen von{" "}
                            <strong>
                                {projekt.student
                                    ? `${projekt.student.vorname} ${projekt.student.name}`
                                    : "–"}
                            </strong>
                        </div>

                        <div className="einl-meta">
                            <span><strong>Fachbereich:</strong> {projekt.fachbereich || "–"}</span>
                            {projekt.studiengang && (
                                <span><strong>Studiengang:</strong> {projekt.studiengang}</span>
                            )}
                            <span><strong>Projektart:</strong> {projekt.projektart || "–"}</span>
                            <span><strong>Semester:</strong> {projekt.semester || "–"}</span>
                        </div>

                        <div className="mt-auto pt-2 d-flex flex-column gap-2">
                            {istAngenommen && (
                                <button
                                    className="btn btn-primary w-100"
                                    onClick={() => navigate(`/projectdetails/${projekt.id}?reiter=mitteilungen`)}
                                >
                                    Mitteilungen öffnen
                                </button>
                            )}
                            <button
                                className={`btn w-100 ${istOffen ? "btn-primary" : "btn-outline-secondary"}`}
                                onClick={() => navigate(`/project-invitation/${projekt.id}`)}
                            >
                                {istOffen ? "Anfrage öffnen" : "Details ansehen"}
                            </button>
                        </div>

                    </div>
                </div>
            </div>
        );
    };

    // Aufklappbarer Bereich: zuerst nur ein Balken mit Zahl, ein Klick zeigt die Karten
    const klappBereich = (schluessel, titel, liste, zaehlerKlasse) => {
        if (liste.length === 0) return null;
        const istAuf = aufgeklappt[schluessel];
        const bereichId = `einl-${schluessel}`;

        return (
            <section className="mb-3">
                <button
                    type="button"
                    className="einl-klappe"
                    onClick={() => setAufgeklappt((vorher) => ({ ...vorher, [schluessel]: !vorher[schluessel] }))}
                    aria-expanded={istAuf}
                    aria-controls={bereichId}
                >
                    <span className={`einl-pfeil ${istAuf ? "einl-pfeil-offen" : ""}`} aria-hidden="true">▸</span>
                    {titel}
                    <span className={`einl-zaehler ${zaehlerKlasse}`}>{liste.length}</span>
                    <span className="einl-klappe-hinweis">{istAuf ? "Einklappen" : "Anzeigen"}</span>
                </button>

                {istAuf && (
                    <div className="row g-4 mt-1 mb-2" id={bereichId}>
                        {liste.map(einladungsKarte)}
                    </div>
                )}
            </section>
        );
    };

    return (
        <div>
            <Navbar />

            <div className="container py-4">

                <div className="mb-4">
                    <h2 className="mb-1">Meine Einladungen</h2>
                    <p className="text-muted mb-0">
                        Projektanfragen von Studierenden, Einladungen zur Co-Betreuung und Übergaben.
                    </p>
                </div>

                {laedt && <p className="text-muted">Einladungen werden geladen...</p>}

                {error && <div className="alert alert-danger">{error}</div>}

                {!laedt && !error && einladungen.length === 0 && coEinladungen.length === 0 && (
                    <div className="card">
                        <div className="einl-leer">
                            <div className="einl-leer-symbol">✉</div>
                            <h4>Keine Projektanfragen</h4>
                            <p className="text-muted mb-0">
                                Sie haben noch keine Projektanfragen erhalten. Sobald Studierende Sie
                                als Betreuer:in anfragen, erscheinen die Anfragen hier.
                            </p>
                        </div>
                    </div>
                )}

                {coFehler && <div className="alert alert-danger">{coFehler}</div>}

                {offeneCo.length > 0 && (
                    <section className="mb-5">
                        <h4 className="einl-abschnitt">
                            Einladungen zur Betreuung <span className="einl-zaehler">{offeneCo.length}</span>
                        </h4>
                        <div className="row g-4">{offeneCo.map(coKarte)}</div>
                    </section>
                )}

                {offene.length > 0 && (
                    <section className="mb-5">
                        <h4 className="einl-abschnitt">
                            Offene Anfragen <span className="einl-zaehler">{offene.length}</span>
                        </h4>
                        <div className="row g-4">{offene.map(einladungsKarte)}</div>
                    </section>
                )}

                {!laedt && !error && einladungen.length > 0 && offene.length === 0 && offeneCo.length === 0 && (
                    <div className="alert alert-light border mb-4">
                        Zurzeit gibt es keine offenen Anfragen.
                    </div>
                )}

                {klappBereich("angenommen", "Angenommene Anfragen", angenommene, "einl-zaehler-gruen")}
                {klappBereich("abgelehnt", "Abgelehnte Anfragen", abgelehnte, "einl-zaehler-grau")}

                {erledigteCo.length > 0 && (
                    <section className="mb-3">
                        <button
                            type="button"
                            className="einl-klappe"
                            onClick={() => setAufgeklappt((vorher) => ({ ...vorher, co: !vorher.co }))}
                            aria-expanded={aufgeklappt.co}
                            aria-controls="einl-co"
                        >
                            <span className={`einl-pfeil ${aufgeklappt.co ? "einl-pfeil-offen" : ""}`} aria-hidden="true">▸</span>
                            Co-Betreuungen und Übergaben
                            <span className="einl-zaehler einl-zaehler-gruen">{erledigteCo.length}</span>
                            <span className="einl-klappe-hinweis">{aufgeklappt.co ? "Einklappen" : "Anzeigen"}</span>
                        </button>
                        {aufgeklappt.co && (
                            <div className="row g-4 mt-1 mb-2" id="einl-co">{erledigteCo.map(coKarte)}</div>
                        )}
                    </section>
                )}

            </div>
        </div>
    );
}

export default MyInvitations;