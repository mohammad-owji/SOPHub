import { useEffect, useState } from "react";
import {
    getBetreuung,
    ladeCoBetreuungEin,
    entferneCoBetreuung,
    uebergebeProjekt,
    zieheUebergabeZurueck,
} from "../services/betreuungService";
import "./ProjektBetreuung.css";

// Datum wie "13.11.2026"
const datum = (wert) => {
    if (!wert) return "";
    const d = new Date(wert);
    return Number.isNaN(d.getTime()) ? "" : d.toLocaleDateString("de-DE");
};

const initialen = (vorname, name) =>
    `${(vorname || "").charAt(0)}${(name || "").charAt(0)}`.toUpperCase() || "?";

// Status einer Co-Betreuung als Text
const STATUS_TEXT = {
    OFFEN: "Eingeladen",
    ANGENOMMEN: "Aktiv",
    ABGELEHNT: "Abgelehnt",
    ZURUECKGEZOGEN: "Zurückgezogen",
    BEENDET: "Beendet",
};

const HOCHSCHUL_DOMAIN = "@hs-bochum.de";

// Gruende fuer eine Uebergabe der Betreuung ("Sonstiges" = eigener Text, dann Pflicht)
const UEBERGABE_GRUENDE = [
    { wert: "KRANKHEIT", text: "Längere Krankheit" },
    { wert: "FREISEMESTER", text: "Forschungs- oder Freisemester" },
    { wert: "ELTERNZEIT", text: "Elternzeit" },
    { wert: "AUSLASTUNG", text: "Zu hohe Auslastung" },
    { wert: "FACHLICH", text: "Fachlich passt eine andere Betreuung besser" },
    { wert: "WECHSEL", text: "Wechsel der Stelle oder der Hochschule" },
    { wert: "SONSTIGES", text: "Sonstiger Grund" },
];

/**
 * Bereich "Betreuung" auf der Projektseite.
 * - Team: sieht den Grund einer Ablehnung sowie Betreuung und Co-Betreuung.
 * - Betreuung (Betreuer:in und Co-Betreuung): laedt Co-Betreuung per Hochschul-E-Mail ein.
 * - Nur die Betreuer:in zieht Einladungen zurueck, beendet eine Co-Betreuung oder uebergibt das Projekt
 *   (z.B. wegen Krankheit) an eine Co-Betreuung oder eine andere Person. Der Verlauf bleibt erhalten.
 * Was die angemeldete Person sehen darf, entscheidet das Backend (403 = Bereich ausblenden).
 */
function ProjektBetreuung({ projekt, projektId, istTeam }) {
    const status = (projekt?.status || "").toUpperCase();
    const istAbgelehnt = status === "ABGELEHNT";

    const [daten, setDaten] = useState(null);
    const [email, setEmail] = useState("");
    const [nachricht, setNachricht] = useState("");
    const [formOffen, setFormOffen] = useState(false);
    const [sendet, setSendet] = useState(false);
    const [fehler, setFehler] = useState("");
    const [meldung, setMeldung] = useState("");
    // Uebergabe: an eine Co-Betreuung (deren E-Mail) oder an eine andere Person ("ANDERE")
    const [uebergabeOffen, setUebergabeOffen] = useState(false);
    const [uebergabeZiel, setUebergabeZiel] = useState("");
    const [uebergabeEmail, setUebergabeEmail] = useState("");
    const [uebergabeGrundArt, setUebergabeGrundArt] = useState("");
    const [uebergabeGrund, setUebergabeGrund] = useState("");
    const [alsCoBleiben, setAlsCoBleiben] = useState(true);

    const laden = () => {
        getBetreuung(projektId)
            .then((antwort) => setDaten(antwort))
            .catch(() => setDaten(null));
    };

    // Immer fragen: Die Betreuung bekommt auf der Projektseite nur die oeffentliche Ansicht
    // (ohne Status), das Backend weiss aber, ob sie den Bereich sehen darf.
    useEffect(() => {
        if (!projektId) {
            setDaten(null);
            return;
        }
        laden();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projektId]);

    const einladen = async (event) => {
        event.preventDefault();
        setFehler("");
        setMeldung("");

        const adresse = email.trim().toLowerCase();
        if (!adresse.endsWith(HOCHSCHUL_DOMAIN)) {
            setFehler(`Bitte geben Sie eine Hochschul-E-Mail-Adresse an (endet auf ${HOCHSCHUL_DOMAIN}).`);
            return;
        }

        setSendet(true);
        try {
            await ladeCoBetreuungEin(projektId, adresse, nachricht.trim());
            setMeldung(`Die Einladung wurde an ${adresse} geschickt.`);
            setEmail("");
            setNachricht("");
            setFormOffen(false);
            laden();
        } catch (error) {
            setFehler(error.message || "Die Einladung konnte nicht verschickt werden.");
        } finally {
            setSendet(false);
        }
    };

    // Nur die Betreuer:in: Einladung zurueckziehen bzw. Co-Betreuung beenden
    const entfernen = async (eintrag) => {
        const name = eintrag.vorname ? `${eintrag.vorname} ${eintrag.name}` : eintrag.email;
        const frage = eintrag.status === "OFFEN"
            ? `Einladung an ${name} zurückziehen?`
            : `Co-Betreuung von ${name} beenden?`;
        if (!window.confirm(frage)) return;

        setFehler("");
        setMeldung("");
        try {
            await entferneCoBetreuung(projektId, eintrag.id);
            laden();
        } catch (error) {
            setFehler(error.message || "Das hat nicht geklappt.");
        }
    };

    const uebergabeAbbrechen = () => {
        setUebergabeOffen(false);
        setUebergabeZiel("");
        setUebergabeEmail("");
        setUebergabeGrundArt("");
        setUebergabeGrund("");
        setAlsCoBleiben(true);
        setFehler("");
    };

    const uebergeben = async (event) => {
        event.preventDefault();
        setFehler("");
        setMeldung("");

        const adresse = (uebergabeZiel === "ANDERE" ? uebergabeEmail : uebergabeZiel).trim().toLowerCase();
        if (!adresse) {
            setFehler("Bitte wählen Sie aus, an wen Sie das Projekt übergeben möchten.");
            return;
        }
        if (!adresse.endsWith(HOCHSCHUL_DOMAIN)) {
            setFehler(`Bitte geben Sie eine Hochschul-E-Mail-Adresse an (endet auf ${HOCHSCHUL_DOMAIN}).`);
            return;
        }
        if (!uebergabeGrundArt) {
            setFehler("Bitte wählen Sie einen Grund für die Übergabe.");
            return;
        }
        const eigenerText = uebergabeGrund.trim();
        if (uebergabeGrundArt === "SONSTIGES" && !eigenerText) {
            setFehler("Bitte beschreiben Sie den Grund für die Übergabe.");
            return;
        }
        // Gespeichert (und dem Team angezeigt) wird der Text des Grundes bzw. der eigene Text
        const grundText = uebergabeGrundArt === "SONSTIGES"
            ? eigenerText
            : UEBERGABE_GRUENDE.find((g) => g.wert === uebergabeGrundArt)?.text || "";

        if (!window.confirm(`Die Betreuung an ${adresse} übergeben? Die Person muss die Übergabe noch annehmen.`)) {
            return;
        }

        setSendet(true);
        try {
            await uebergebeProjekt(projektId, adresse, grundText, alsCoBleiben);
            setMeldung(`Die Übergabe wurde an ${adresse} geschickt. Sie bleiben Betreuer:in, bis die Person annimmt.`);
            uebergabeAbbrechen();
            laden();
        } catch (error) {
            setFehler(error.message || "Die Übergabe konnte nicht angefragt werden.");
        } finally {
            setSendet(false);
        }
    };

    const uebergabeZurueckziehen = async (eintrag) => {
        if (!window.confirm("Die offene Übergabe zurückziehen?")) return;
        setFehler("");
        setMeldung("");
        try {
            await zieheUebergabeZurueck(projektId, eintrag.id);
            laden();
        } catch (error) {
            setFehler(error.message || "Das hat nicht geklappt.");
        }
    };

    // ---------------------------------------------------------------- Ablehnung (nur Team)

    const ablehnung = istTeam && istAbgelehnt && projekt.ablehnungsGrund ? (
        <section className="pb-ablehnung mb-4" role="status">
            <div className="pb-ablehnung-symbol" aria-hidden="true">✕</div>
            <div>
                <h5 className="pb-ablehnung-titel">Projektanfrage abgelehnt</h5>
                <p className="pb-ablehnung-text">
                    {projekt.betreuer ? `${projekt.betreuer.vorname} ${projekt.betreuer.name}` : "Die Betreuung"} hat die Betreuung
                    {projekt.abgelehntAm ? ` am ${datum(projekt.abgelehntAm)}` : ""} abgelehnt.
                </p>
                <dl className="pb-ablehnung-grund">
                    <dt>Grund</dt>
                    <dd>{projekt.ablehnungsGrund}</dd>
                    {projekt.ablehnungsKommentar && (
                        <>
                            <dt>Erklärung</dt>
                            <dd>{projekt.ablehnungsKommentar}</dd>
                        </>
                    )}
                </dl>
                <p className="pb-ablehnung-hinweis">
                    Sie können die Projektidee überarbeiten und eine andere Betreuerin oder einen anderen Betreuer anfragen.
                </p>
            </div>
        </section>
    ) : null;

    // Die Karte "Betreuung" gibt es nur bei laufenden oder abgeschlossenen Projekten
    const projektStatus = (daten?.projektStatus || "").toUpperCase();
    if (!daten || (projektStatus !== "ANGENOMMEN" && projektStatus !== "ABGESCHLOSSEN")) {
        return ablehnung;
    }

    // ---------------------------------------------------------------- Betreuung und Co-Betreuung

    const aktive = daten.coBetreuung.filter((e) => e.status === "OFFEN" || e.status === "ANGENOMMEN");
    const aktiveCo = daten.coBetreuung.filter((e) => e.status === "ANGENOMMEN");
    const uebergaben = daten.uebergaben || [];
    const offeneUebergabe = uebergaben.find((u) => u.status === "OFFEN");
    const istBetreuung = daten.istBetreuer || daten.istCoBetreuer;
    const personName = (e) => (e.vorname ? `${e.vorname} ${e.name}` : e.email);
    const vonName = (e) => (e.eingeladenVon ? `${e.eingeladenVon.vorname} ${e.eingeladenVon.name}` : "Die Betreuung");

    // Verlauf: beendete Co-Betreuungen und alle abgeschlossenen Uebergaben, nach Datum sortiert
    const verlauf = [
        ...daten.coBetreuung
            .filter((e) => e.status !== "OFFEN" && e.status !== "ANGENOMMEN")
            .map((e) => ({
                id: `co-${e.id}`,
                datum: e.beantwortetAm || e.eingeladenAm,
                text: `Co-Betreuung ${personName(e)}: ${STATUS_TEXT[e.status] || e.status}`,
            })),
        ...uebergaben
            .filter((u) => u.status !== "OFFEN")
            .map((u) => ({
                id: `ue-${u.id}`,
                datum: u.beantwortetAm || u.eingeladenAm,
                text: u.status === "ANGENOMMEN"
                    ? `Betreuung übergeben von ${vonName(u)} an ${personName(u)}${u.nachricht ? ` (Grund: ${u.nachricht})` : ""}`
                    : `Übergabe an ${personName(u)}: ${STATUS_TEXT[u.status] || u.status}`,
            })),
    ].sort((a, b) => String(b.datum || "").localeCompare(String(a.datum || "")));

    return (
        <>
            {ablehnung}

            <section className="card pb-karte mb-4">
                <div className="card-body">
                    <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                        <h5 className="pb-titel mb-0">Betreuung</h5>
                        <div className="d-flex flex-wrap gap-2">
                            {daten.darfEinladen && !formOffen && !uebergabeOffen && (
                                <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => {
                                    setFormOffen(true);
                                    setMeldung("");
                                }}>
                                    + Co-Betreuung einladen
                                </button>
                            )}
                            {daten.darfUebergeben && !uebergabeOffen && !formOffen && (
                                <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => {
                                    setUebergabeOffen(true);
                                    setUebergabeZiel(aktiveCo[0]?.email || "ANDERE");
                                    setMeldung("");
                                }}>
                                    ⇄ Betreuung übergeben
                                </button>
                            )}
                        </div>
                    </div>

                    {meldung && <div className="alert alert-success py-2">{meldung}</div>}
                    {fehler && <div className="alert alert-danger py-2">{fehler}</div>}

                    {/* Offene Uebergabe: wartet auf die Antwort der neuen Person */}
                    {offeneUebergabe && (
                        <div className="pb-uebergabe-hinweis">
                            <span>
                                <strong>Übergabe angefragt:</strong> {vonName(offeneUebergabe)} möchte die Betreuung an{" "}
                                <strong>{personName(offeneUebergabe)}</strong> übergeben
                                {offeneUebergabe.eingeladenAm && ` (seit ${datum(offeneUebergabe.eingeladenAm)})`}.
                                Die Übergabe wird wirksam, sobald die Person annimmt.
                            </span>
                            {daten.istBetreuer && (
                                <button type="button" className="btn btn-sm btn-outline-secondary flex-shrink-0"
                                    onClick={() => uebergabeZurueckziehen(offeneUebergabe)}>
                                    Zurückziehen
                                </button>
                            )}
                        </div>
                    )}

                    <div className="pb-personen">
                        {daten.betreuer && (
                            <div className="pb-person">
                                <span className="pb-avatar pb-avatar-betreuer">
                                    {initialen(daten.betreuer.vorname, daten.betreuer.name)}
                                </span>
                                <div className="flex-grow-1">
                                    <div className="pb-name">
                                        {daten.betreuer.vorname} {daten.betreuer.name}
                                        {daten.istBetreuer && <span className="pb-ich">Sie</span>}
                                    </div>
                                    <div className="pb-rolle">Betreuer:in</div>
                                </div>
                            </div>
                        )}

                        {aktive.map((e) => {
                            const name = e.vorname ? `${e.vorname} ${e.name}` : e.email;
                            return (
                                <div className="pb-person" key={e.id}>
                                    <span className={`pb-avatar ${e.status === "OFFEN" ? "pb-avatar-offen" : "pb-avatar-co"}`}>
                                        {e.vorname ? initialen(e.vorname, e.name) : "@"}
                                    </span>
                                    <div className="flex-grow-1 min-w-0">
                                        <div className="pb-name text-truncate" title={e.email}>{name}</div>
                                        <div className="pb-rolle">
                                            Co-Betreuer:in ·{" "}
                                            <span className={`pb-status pb-status-${e.status.toLowerCase()}`}>
                                                {STATUS_TEXT[e.status] || e.status}
                                            </span>
                                            {e.status === "OFFEN" && e.eingeladenAm && ` seit ${datum(e.eingeladenAm)}`}
                                        </div>
                                    </div>
                                    {daten.istBetreuer && (
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-outline-danger flex-shrink-0"
                                            onClick={() => entfernen(e)}
                                            title={e.status === "OFFEN" ? "Einladung zurückziehen" : "Co-Betreuung beenden"}
                                        >
                                            {e.status === "OFFEN" ? "Zurückziehen" : "Entfernen"}
                                        </button>
                                    )}
                                </div>
                            );
                        })}

                        {aktive.length === 0 && (
                            <p className="pb-leer">
                                {istBetreuung
                                    ? "Noch keine Co-Betreuung. Sie können wissenschaftliche Mitarbeitende oder Kolleg:innen über ihre Hochschul-E-Mail einladen."
                                    : "Keine Co-Betreuung."}
                            </p>
                        )}
                    </div>

                    {/* Einladung per Hochschul-E-Mail */}
                    {daten.darfEinladen && formOffen && (
                        <form className="pb-form" onSubmit={einladen}>
                            <label className="form-label" htmlFor="pb-email">Hochschul-E-Mail *</label>
                            <input
                                id="pb-email"
                                type="email"
                                className="form-control"
                                placeholder={`vorname.nachname${HOCHSCHUL_DOMAIN}`}
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                            <div className="form-text mb-2">
                                Die Person bekommt eine E-Mail. Nach der Annahme hat sie dieselben Rechte wie die
                                Betreuung (Mitteilungen, Dokumente, Abgaben anfordern und prüfen).
                            </div>

                            <label className="form-label" htmlFor="pb-nachricht">
                                Nachricht <span className="text-muted fw-normal">(optional)</span>
                            </label>
                            <textarea
                                id="pb-nachricht"
                                className="form-control"
                                rows="2"
                                maxLength={1000}
                                value={nachricht}
                                onChange={(e) => setNachricht(e.target.value)}
                                placeholder="z. B. Bitte übernehmen Sie die fachliche Begleitung der Datenbank."
                            />

                            <div className="d-flex justify-content-end gap-2 mt-3">
                                <button type="button" className="btn btn-outline-secondary" disabled={sendet}
                                    onClick={() => { setFormOffen(false); setFehler(""); }}>
                                    Abbrechen
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={sendet}>
                                    {sendet ? "Wird gesendet..." : "Einladung senden"}
                                </button>
                            </div>
                        </form>
                    )}

                    {/* Uebergabe an eine Co-Betreuung oder eine andere Person (nur Betreuer:in) */}
                    {daten.darfUebergeben && uebergabeOffen && (
                        <form className="pb-form" onSubmit={uebergeben}>
                            <h6 className="pb-form-titel">Betreuung übergeben</h6>
                            <p className="form-text mt-0">
                                Zum Beispiel bei Krankheit oder aus persönlichen Gründen. Mitteilungen, Abgaben und
                                Dokumente bleiben vollständig erhalten, damit die neue Betreuung den Verlauf nachvollziehen kann.
                            </p>

                            <fieldset className="mb-2">
                                <legend className="form-label">An wen? *</legend>
                                {aktiveCo.map((e) => (
                                    <div className="form-check" key={e.id}>
                                        <input
                                            className="form-check-input"
                                            type="radio"
                                            name="uebergabe-ziel"
                                            id={`ue-${e.id}`}
                                            value={e.email}
                                            checked={uebergabeZiel === e.email}
                                            onChange={(ev) => setUebergabeZiel(ev.target.value)}
                                        />
                                        <label className="form-check-label" htmlFor={`ue-${e.id}`}>
                                            {personName(e)} <span className="text-muted">(Co-Betreuung)</span>
                                        </label>
                                    </div>
                                ))}
                                <div className="form-check">
                                    <input
                                        className="form-check-input"
                                        type="radio"
                                        name="uebergabe-ziel"
                                        id="ue-andere"
                                        value="ANDERE"
                                        checked={uebergabeZiel === "ANDERE"}
                                        onChange={(ev) => setUebergabeZiel(ev.target.value)}
                                    />
                                    <label className="form-check-label" htmlFor="ue-andere">Andere Person (Hochschul-E-Mail)</label>
                                </div>
                            </fieldset>

                            {uebergabeZiel === "ANDERE" && (
                                <input
                                    type="email"
                                    className="form-control mb-2"
                                    placeholder={`vorname.nachname${HOCHSCHUL_DOMAIN}`}
                                    value={uebergabeEmail}
                                    onChange={(e) => setUebergabeEmail(e.target.value)}
                                    aria-label="E-Mail der neuen Betreuung"
                                    required
                                />
                            )}

                            <fieldset className="mb-2">
                                <legend className="form-label">
                                    Grund * <span className="text-muted fw-normal">(sieht auch das Team)</span>
                                </legend>
                                {UEBERGABE_GRUENDE.map((g) => (
                                    <div className="form-check pb-grund" key={g.wert}>
                                        <input
                                            className="form-check-input"
                                            type="radio"
                                            name="uebergabe-grund"
                                            id={`ue-grund-${g.wert}`}
                                            value={g.wert}
                                            checked={uebergabeGrundArt === g.wert}
                                            onChange={(ev) => setUebergabeGrundArt(ev.target.value)}
                                        />
                                        <label className="form-check-label" htmlFor={`ue-grund-${g.wert}`}>
                                            {g.text}
                                        </label>
                                    </div>
                                ))}
                            </fieldset>

                            {uebergabeGrundArt === "SONSTIGES" && (
                                <>
                                    <label className="form-label" htmlFor="pb-ue-grund">Genauer Grund *</label>
                                    <textarea
                                        id="pb-ue-grund"
                                        className="form-control"
                                        rows="2"
                                        maxLength={1000}
                                        value={uebergabeGrund}
                                        onChange={(e) => setUebergabeGrund(e.target.value)}
                                        placeholder="Bitte beschreiben Sie kurz, warum Sie die Betreuung übergeben."
                                        required
                                    />
                                </>
                            )}

                            <div className="form-check mt-2">
                                <input
                                    className="form-check-input"
                                    type="checkbox"
                                    id="pb-ue-co"
                                    checked={alsCoBleiben}
                                    onChange={(e) => setAlsCoBleiben(e.target.checked)}
                                />
                                <label className="form-check-label" htmlFor="pb-ue-co">
                                    Ich bleibe nach der Übergabe als Co-Betreuung im Projekt
                                </label>
                            </div>

                            <div className="d-flex justify-content-end gap-2 mt-3">
                                <button type="button" className="btn btn-outline-secondary" disabled={sendet}
                                    onClick={uebergabeAbbrechen}>
                                    Abbrechen
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={sendet}>
                                    {sendet ? "Wird gesendet..." : "Übergabe anfragen"}
                                </button>
                            </div>
                        </form>
                    )}

                    {/* Verlauf: Uebergaben sowie abgelehnte, zurueckgezogene und beendete Co-Betreuungen */}
                    {verlauf.length > 0 && (
                        <details className="pb-verlauf">
                            <summary>Verlauf der Betreuung ({verlauf.length})</summary>
                            <ul>
                                {verlauf.map((e) => (
                                    <li key={e.id}>
                                        {e.datum && <span className="pb-verlauf-datum">{datum(e.datum)}</span>} {e.text}
                                    </li>
                                ))}
                            </ul>
                        </details>
                    )}
                </div>
            </section>
        </>
    );
}

export default ProjektBetreuung;