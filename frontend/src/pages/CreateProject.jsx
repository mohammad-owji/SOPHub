import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getAuth, createProjekt, uploadDokument } from "../services/api";

const DOKUMENT_TYPEN = [
    { value: "PFLICHTENHEFT", label: "Pflichtenheft" },
    { value: "LASTENHEFT", label: "Lastenheft" },
    { value: "DOKUMENTATION", label: "Dokumentation" },
    { value: "SONSTIGES", label: "Sonstiges" },
];

function CreateProject() {
    const navigate = useNavigate();

    const [titel, setTitel] = useState("");
    const [beschreibung, setBeschreibung] = useState("");
    const [semester, setSemester] = useState("SoSe 2026");
    const [fachbereich, setFachbereich] = useState("");
    const [projektart, setProjektart] = useState("");
    const [sprache, setSprache] = useState("Deutsch");
    const [schlagwoerter, setSchlagwoerter] = useState("");
    const [gruppenanzahl, setGruppenanzahl] = useState("3");
    const [dokumente, setDokumente] = useState([
        { typ: DOKUMENT_TYPEN[0].value, datei: null },
    ]);
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("");
    const [wirdGespeichert, setWirdGespeichert] = useState(false);

    const resetForm = () => {
        setTitel("");
        setBeschreibung("");
        setSemester("SoSe 2026");
        setFachbereich("");
        setProjektart("");
        setSprache("Deutsch");
        setSchlagwoerter("");
        setGruppenanzahl("3");
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

        const auth = getAuth();
        if (!auth?.id) {
            setMessage("Bitte zuerst einloggen.");
            setMessageType("error");
            return;
        }

        setWirdGespeichert(true);
        setMessage("");

        try {
            const projekt = await createProjekt(auth.id, {
                titel,
                beschreibung,
                semester,
                fachbereich,
                projektart,
                sprache,
                schlagwoerter,
                gruppenanzahl: gruppenanzahl ? Number(gruppenanzahl) : null,
            });

            const hochzuladen = dokumente.filter((eintrag) => eintrag.datei);
            for (const eintrag of hochzuladen) {
                const formData = new FormData();
                formData.append("datei", eintrag.datei);
                formData.append("benutzerId", auth.id);
                formData.append("projektId", projekt.id);
                formData.append("typ", eintrag.typ);
                await uploadDokument(formData);
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
                                    <select
                                        className="form-select"
                                        value={semester}
                                        onChange={(e) => setSemester(e.target.value)}
                                    >
                                        <option>SoSe 2026</option>
                                        <option>WiSe 2025</option>
                                        <option>SoSe 2025</option>
                                    </select>
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Fachbereich</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="z. B. Informatik"
                                        value={fachbereich}
                                        onChange={(e) => setFachbereich(e.target.value)}
                                    />
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Projektart</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="z. B. Abschlussprojekt"
                                        value={projektart}
                                        onChange={(e) => setProjektart(e.target.value)}
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Sprache</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={sprache}
                                        onChange={(e) => setSprache(e.target.value)}
                                    />
                                </div>

                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Schlagwörter</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="z. B. React, KI, Web"
                                        value={schlagwoerter}
                                        onChange={(e) => setSchlagwoerter(e.target.value)}
                                    />
                                </div>

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
                            </div>

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
