import { useEffect, useState } from "react";
import { authHeaders } from "../services/api";
import "./DokumentAnsicht.css";

// Zeigt ein Dokument direkt auf der Seite an (PDF: blaettern, scrollen, zoomen).
// Die Datei wird mit dem Login-Token geladen. Deshalb sehen sie nur Personen,
// die das Backend fuer dieses Projekt freigibt (Ersteller, Teammitglieder, Betreuer, Admin).
const DOWNLOAD_URL = (id) => `http://localhost:8080/sop/api/dokumente/${id}/download`;

const istPdf = (dateiName) => (dateiName || "").toLowerCase().endsWith(".pdf");

function DokumentAnsicht({ dokument, onSchliessen }) {
    const [url, setUrl] = useState(null);
    const [fehler, setFehler] = useState("");

    // Datei laden und als lokale Adresse (blob:) fuer den eingebauten PDF-Betrachter bereitstellen
    useEffect(() => {
        if (!dokument) return;

        let aktiv = true;
        let lokaleUrl = null;
        setUrl(null);
        setFehler("");

        fetch(DOWNLOAD_URL(dokument.id), { headers: authHeaders() })
            .then((response) => {
                if (response.status === 401 || response.status === 403) {
                    throw new Error("Sie haben keinen Zugriff auf dieses Dokument.");
                }
                if (!response.ok) {
                    throw new Error("Das Dokument konnte nicht geladen werden.");
                }
                return response.blob();
            })
            .then((daten) => {
                if (!aktiv) return;
                // Das Backend schickt die Datei als "octet-stream".
                // Damit der Browser sie als PDF anzeigt, geben wir den Typ selbst an.
                const typ = istPdf(dokument.dateiName) ? "application/pdf" : daten.type;
                lokaleUrl = URL.createObjectURL(new Blob([daten], { type: typ }));
                setUrl(lokaleUrl);
            })
            .catch((error) => {
                if (aktiv) setFehler(error.message || "Das Dokument konnte nicht geladen werden.");
            });

        return () => {
            aktiv = false;
            if (lokaleUrl) URL.revokeObjectURL(lokaleUrl);
        };
    }, [dokument]);

    // Mit der Esc-Taste schliessen und Hintergrund nicht mitscrollen
    useEffect(() => {
        if (!dokument) return;
        const beiTaste = (event) => {
            if (event.key === "Escape") onSchliessen();
        };
        document.addEventListener("keydown", beiTaste);
        const vorher = document.body.style.overflow;
        document.body.style.overflow = "hidden";
        return () => {
            document.removeEventListener("keydown", beiTaste);
            document.body.style.overflow = vorher;
        };
    }, [dokument, onSchliessen]);

    if (!dokument) return null;

    const herunterladen = () => {
        if (!url) return;
        const link = document.createElement("a");
        link.href = url;
        link.download = dokument.dateiName || "dokument";
        link.click();
    };

    return (
        <div className="da-hintergrund" onClick={onSchliessen}>
            <div
                className="da-fenster"
                role="dialog"
                aria-modal="true"
                aria-label={`Dokument ${dokument.dateiName}`}
                onClick={(event) => event.stopPropagation()}
            >
                <div className="da-kopf">
                    <div className="da-titel text-truncate" title={dokument.dateiName}>
                        {dokument.dateiName}
                    </div>
                    <div className="d-flex gap-2 flex-shrink-0">
                        <button className="btn btn-sm btn-outline-primary" onClick={herunterladen} disabled={!url}>
                            Herunterladen
                        </button>
                        <button className="btn btn-sm btn-outline-secondary" onClick={onSchliessen}>
                            Schließen
                        </button>
                    </div>
                </div>

                <div className="da-inhalt">
                    {fehler ? (
                        <div className="da-hinweis">
                            <div className="alert alert-warning mb-0">{fehler}</div>
                        </div>
                    ) : !url ? (
                        <div className="da-hinweis">
                            <div className="spinner-border text-primary" role="status" aria-hidden="true" />
                            <span className="text-muted">Dokument wird geladen...</span>
                        </div>
                    ) : istPdf(dokument.dateiName) ? (
                        <iframe className="da-pdf" src={url} title={dokument.dateiName} />
                    ) : (
                        <div className="da-hinweis">
                            <p className="mb-2">Für diesen Dateityp gibt es keine Vorschau.</p>
                            <button className="btn btn-primary" onClick={herunterladen}>
                                Datei herunterladen
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default DokumentAnsicht;