import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import AuswahlFeld from "../components/AuswahlFeld";
import {
    getAuth,
    createProjekt,
    uploadDokument,
    getProfessoren,
    getStudenten,
    mitgliedHinzufuegen,
} from "../services/api";

const DOKUMENT_TYPEN = [
    { value: "PFLICHTENHEFT", label: "Pflichtenheft" },
    { value: "LASTENHEFT", label: "Lastenheft" },
    { value: "DOKUMENTATION", label: "Dokumentation" },
    { value: "SONSTIGES", label: "Sonstiges" },
];

// Vorschlaege fuer die Auswahlfelder. Man kann auswaehlen, danach suchen
// (einfach lostippen) oder einen eigenen Wert eintragen, falls etwas fehlt.
const SEMESTER = [
    "SoSe 2027",
    "WiSe 2026/27",
    "SoSe 2026",
    "WiSe 2025/26",
    "SoSe 2025",
];

// Aktuelles Semester als Vorauswahl
const STANDARD_SEMESTER = "WiSe 2026/27";

const FACHBEREICHE = [
    "Informatik",
    "Wirtschaftsinformatik",
    "Medieninformatik",
    "Elektrotechnik",
    "Mechatronik",
    "Maschinenbau",
    "Bauingenieurwesen",
    "Architektur",
    "Wirtschaft",
    "Geodäsie",
];

const PROJEKTARTEN = [
    "Softwareprojekt",
    "Studienprojekt",
    "Praxisprojekt",
    "Forschungsprojekt",
    "Bachelorarbeit",
    "Masterarbeit",
    "Seminararbeit",
    "Hackathon",
];

const HOCHSCHUL_DOMAIN = "@hs-bochum.de";

// Zerlegt einen eingetippten Namen in Vorname und Nachname.
// Titel wie "Prof.", "Dr." oder "Dr.-Ing." werden ignoriert.
// Beispiel: "Prof. Dr. Max Beispiel" -> { vorname: "Max", name: "Beispiel" }
function teileNamen(text) {
    const woerter = text
        .trim()
        .split(/\s+/)
        .filter((wort) => !/^(prof|dr|dr\.-ing|rer|nat|ing)\.?$/i.test(wort));

    if (woerter.length < 2) {
        return null;
    }

    return {
        vorname: woerter.slice(0, -1).join(" "),
        name: woerter[woerter.length - 1],
    };
}

function CreateProject() {
    const navigate = useNavigate();
    const auth = getAuth();

    const [titel, setTitel] = useState("");
    const [beschreibung, setBeschreibung] = useState("");
    const [semester, setSemester] = useState(STANDARD_SEMESTER);
    const [fachbereich, setFachbereich] = useState("");
    const [projektart, setProjektart] = useState("");
    const [gruppenanzahl, setGruppenanzahl] = useState("3");
    // Betreuer: Text im Auswahlfeld (Name aus der Liste oder selbst eingetippt)
    const [betreuerText, setBetreuerText] = useState("");
    // Nur noetig, wenn der Betreuer nicht in der Liste steht
    const [neuerBetreuerEmail, setNeuerBetreuerEmail] = useState("");
    const [professoren, setProfessoren] = useState([]);
    const [alleStudenten, setAlleStudenten] = useState([]);
    const [teamSuche, setTeamSuche] = useState("");
    const [ausgewaehlteMitglieder, setAusgewaehlteMitglieder] = useState([]);
    const [dokumente, setDokumente] = useState([
        { typ: DOKUMENT_TYPEN[0].value, datei: null },
    ]);
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("");
    const [wirdGespeichert, setWirdGespeichert] = useState(false);

    useEffect(() => {
        getProfessoren()
            .then((data) => setProfessoren(data || []))
            .catch(() => setProfessoren([]));

        getStudenten()
            .then((data) => setAlleStudenten(data || []))
            .catch(() => setAlleStudenten([]));
    }, []);

    // Betreuer-Liste fuer das Auswahlfeld, z.B. "Anja Tenberge"
    const betreuerNamen = professoren.map((prof) => `${prof.vorname} ${prof.name}`);

    // Passt der eingegebene Text genau zu einem Betreuer aus der Liste?
    const gefundenerBetreuer = professoren.find(
        (prof) =>
            `${prof.vorname} ${prof.name}`.toLowerCase() === betreuerText.trim().toLowerCase()
    );

    // Text eingegeben, aber nicht in der Liste -> neuer Betreuer, E-Mail wird gebraucht
    const istNeuerBetreuer = betreuerText.trim() !== "" && !gefundenerBetreuer;

    const maxGroesse = gruppenanzahl ? Number(gruppenanzahl) : null;
    const aktuelleGroesse = 1 + ausgewaehlteMitglieder.length;
    const maxErreicht = maxGroesse != null && aktuelleGroesse >= maxGroesse;

    const gefilterteStudenten = teamSuche.trim()
        ? alleStudenten.filter((s) => {
            const vollerName = `${s.vorname} ${s.name}`.toLowerCase();
            return (
                vollerName.includes(teamSuche.toLowerCase()) &&
                s.id !== auth?.id &&
                !ausgewaehlteMitglieder.some((m) => m.id === s.id)
            );
        })
        : [];

    const mitgliedAuswaehlen = (student) => {
        if (maxErreicht) return;
        setAusgewaehlteMitglieder([...ausgewaehlteMitglieder, student]);
        setTeamSuche("");
    };

    const mitgliedAbwaehlen = (studentId) => {
        setAusgewaehlteMitglieder(ausgewaehlteMitglieder.filter((m) => m.id !== studentId));
    };

    const resetForm = () => {
        setTitel("");
        setBeschreibung("");
        setSemester(STANDARD_SEMESTER);
        setFachbereich("");
        setProjektart("");
        setGruppenanzahl("3");
        setBetreuerText("");
        setNeuerBetreuerEmail("");
        setTeamSuche("");
        setAusgewaehlteMitglieder([]);
        setDokumente([{ typ: DOKUMENT_TYPEN[0].value, datei: null }]);
    };

    const dokumentHinzufuegen = () => {
        setDokumente([...dokumente, { typ: DOKUMENT_TYPEN[0].value, datei: null }]);
    };

    const dokumentEntfernen = (index) => {
        setDokumente(dokumente.filter((_, i) => i !== index));
    };

    const dokumentAendern = (index, feld, wert) => {
        setDokumente(
            dokumente.map((eintrag, i) =>
                i === index ? { ...eintrag, [feld]: wert } : eintrag
            )
        );
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!titel) {
            setMessage("Bitte mindestens einen Projekttitel angeben.");
            setMessageType("error");
            return;
        }

        if (!auth?.id) {
            setMessage("Bitte zuerst einloggen.");
            setMessageType("error");
            return;
        }

        // Selbst eingetippter Betreuer: Name und Hochschul-E-Mail pruefen
        let neuerBetreuer = null;
        if (istNeuerBetreuer) {
            const namensTeile = teileNamen(betreuerText);
            if (!namensTeile) {
                setMessage("Bitte Vor- und Nachnamen des Betreuers eingeben.");
                setMessageType("error");
                return;
            }

            const email = neuerBetreuerEmail.trim().toLowerCase();
            if (!email.endsWith(HOCHSCHUL_DOMAIN)) {
                setMessage(`Bitte die Hochschul-E-Mail des Betreuers angeben (endet auf ${HOCHSCHUL_DOMAIN}).`);
                setMessageType("error");
                return;
            }

            neuerBetreuer = { ...namensTeile, email };
        }

        setWirdGespeichert(true);
        setMessage("");

        try {
            const projekt = await createProjekt(
                auth.id,
                {
                    titel,
                    beschreibung,
                    semester,
                    fachbereich,
                    projektart,
                    gruppenanzahl: gruppenanzahl ? Number(gruppenanzahl) : null,
                },
                gefundenerBetreuer ? gefundenerBetreuer.id : null,
                neuerBetreuer
            );

            const hochzuladen = dokumente.filter((eintrag) => eintrag.datei);
            for (const eintrag of hochzuladen) {
                const formData = new FormData();
                formData.append("datei", eintrag.datei);
                formData.append("benutzerId", auth.id);
                formData.append("projektId", projekt.id);
                formData.append("typ", eintrag.typ);
                await uploadDokument(formData);
            }

            for (const mitglied of ausgewaehlteMitglieder) {
                await mitgliedHinzufuegen(projekt.id, mitglied.id);
            }

            setMessage("Projekt wurde erfolgreich erstellt.");
            setMessageType("success");
            resetForm();

            setTimeout(() => {
                navigate("/my-projects");
            }, 1200);
        } catch (error) {
            setMessage(error.message || "Fehler beim Erstellen des Projekts.");
            setMessageType("error");
        } finally {
            setWirdGespeichert(false);
        }
    };

    return (
        <div>
            <Navbar />

            <div className="container mt-4">
                <div className="mb-4">
                    <h2 className="fw-bold text-dark">Projekt erstellen</h2>
                    <p className="text-muted">
                        Erstelle eine neue Projektidee oder ein neues SOP-Projekt.
                    </p>
                </div>

                {message && (
                    <div className={`alert ${messageType === "error" ? "alert-danger" : "alert-success"}`}>
                        {message}
                    </div>
                )}

                <div className="card shadow-sm border-0">
                    <div className="card-body p-4">
                        <form onSubmit={handleSubmit}>
                            <div className="mb-3">
                                <label className="form-label">Projekttitel *</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="z. B. SOPhub"
                                    value={titel}
                                    onChange={(e) => setTitel(e.target.value)}
                                />
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Beschreibung</label>
                                <textarea
                                    className="form-control"
                                    rows="4"
                                    placeholder="Beschreibe kurz, worum es im Projekt geht..."
                                    value={beschreibung}
                                    onChange={(e) => setBeschreibung(e.target.value)}
                                ></textarea>
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Semester</label>
                                    <AuswahlFeld
                                        value={semester}
                                        onChange={setSemester}
                                        optionen={SEMESTER}
                                        placeholder="Auswählen oder eintippen"
                                    />
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Fachbereich</label>
                                    <AuswahlFeld
                                        value={fachbereich}
                                        onChange={setFachbereich}
                                        optionen={FACHBEREICHE}
                                        placeholder="Auswählen oder eintippen"
                                    />
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Projektart</label>
                                    <AuswahlFeld
                                        value={projektart}
                                        onChange={setProjektart}
                                        optionen={PROJEKTARTEN}
                                        placeholder="Auswählen oder eintippen"
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Gruppengröße</label>
                                    <select
                                        className="form-select"
                                        value={gruppenanzahl}
                                        onChange={(e) => setGruppenanzahl(e.target.value)}
                                    >
                                        <option value="1">1 Person</option>
                                        <option value="2">2 Personen</option>
                                        <option value="3">3 Personen</option>
                                        <option value="4">4 Personen</option>
                                        <option value="5">5 Personen</option>
                                    </select>
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Betreuer:in</label>
                                    <AuswahlFeld
                                        value={betreuerText}
                                        onChange={setBetreuerText}
                                        optionen={betreuerNamen}
                                        placeholder="Auswählen oder Namen eintippen"
                                    />
                                    <div className="form-text">
                                        Leer lassen, wenn noch kein Betreuer feststeht.
                                    </div>
                                </div>

                                {/* Nur sichtbar, wenn der Betreuer nicht in der Liste steht */}
                                {istNeuerBetreuer && (
                                    <div className="col-md-4 mb-3">
                                        <label className="form-label">E-Mail des Betreuers *</label>
                                        <input
                                            type="email"
                                            className="form-control"
                                            placeholder={`vorname.nachname${HOCHSCHUL_DOMAIN}`}
                                            value={neuerBetreuerEmail}
                                            onChange={(e) => setNeuerBetreuerEmail(e.target.value)}
                                        />
                                        <div className="form-text">
                                            Nicht in der Liste? Dann wird der Betreuer neu angelegt
                                            und bekommt die Projektanfrage an diese Adresse.
                                        </div>
                                    </div>
                                )}
                            </div>

                            <hr className="my-4" />

                            <div className="d-flex justify-content-between align-items-center mb-2">
                                <h5 className="mb-0">Teammitglieder (optional)</h5>
                                <span className={`badge ${maxErreicht ? "bg-secondary" : "bg-primary"}`}>
                                    {aktuelleGroesse} / {maxGroesse ?? "-"}
                                </span>
                            </div>

                            {ausgewaehlteMitglieder.length > 0 && (
                                <ul className="list-group mb-2">
                                    {ausgewaehlteMitglieder.map((m) => (
                                        <li
                                            className="list-group-item d-flex justify-content-between align-items-center"
                                            key={m.id}
                                        >
                                            {m.vorname} {m.name}
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-outline-danger"
                                                onClick={() => mitgliedAbwaehlen(m.id)}
                                                title="Entfernen"
                                            >
                                                ✕
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}

                            {maxErreicht ? (
                                <p className="text-muted mb-4">
                                    Maximale Gruppengröße erreicht ({aktuelleGroesse}/{maxGroesse}).
                                </p>
                            ) : (
                                <div className="mb-4">
                                    <input
                                        type="text"
                                        className="form-control mb-2"
                                        placeholder="Studierende suchen..."
                                        value={teamSuche}
                                        onChange={(e) => setTeamSuche(e.target.value)}
                                    />

                                    {gefilterteStudenten.length > 0 && (
                                        <ul className="list-group">
                                            {gefilterteStudenten.map((s) => (
                                                <li
                                                    className="list-group-item d-flex justify-content-between align-items-center"
                                                    key={s.id}
                                                >
                                                    {s.vorname} {s.name}
                                                    <button
                                                        type="button"
                                                        className="btn btn-sm btn-outline-primary"
                                                        onClick={() => mitgliedAuswaehlen(s)}
                                                    >
                                                        + Hinzufügen
                                                    </button>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                            )}

                            <hr className="my-4" />

                            <h5 className="mb-3">Dokumente hochladen (optional)</h5>
                            {dokumente.map((eintrag, index) => (
                                <div className="row align-items-end" key={index}>
                                    <div className="col-md-4 mb-3">
                                        <label className="form-label">Dokumententyp</label>
                                        <select
                                            className="form-select"
                                            value={eintrag.typ}
                                            onChange={(e) => dokumentAendern(index, "typ", e.target.value)}
                                        >
                                            {DOKUMENT_TYPEN.map((typ) => (
                                                <option key={typ.value} value={typ.value}>
                                                    {typ.label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="col-md-7 mb-3">
                                        <label className="form-label">PDF-Datei</label>
                                        <input
                                            type="file"
                                            className="form-control"
                                            accept="application/pdf"
                                            onChange={(e) =>
                                                dokumentAendern(index, "datei", e.target.files[0] ?? null)
                                            }
                                        />
                                    </div>

                                    <div className="col-md-1 mb-3">
                                        {dokumente.length > 1 && (
                                            <button
                                                type="button"
                                                className="btn btn-outline-danger"
                                                onClick={() => dokumentEntfernen(index)}
                                                title="Dokument entfernen"
                                            >
                                                ✕
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))}

                            <div className="d-flex align-items-center gap-3 mb-4">
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    onClick={dokumentHinzufuegen}
                                >
                                    + Weiteres Dokument
                                </button>

                                <button type="submit" className="btn btn-primary" disabled={wirdGespeichert}>
                                    {wirdGespeichert ? "Wird erstellt..." : "Projekt erstellen"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default CreateProject;