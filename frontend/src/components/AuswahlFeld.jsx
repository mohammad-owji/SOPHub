import { useState } from "react";

/**
 * Auswahlfeld mit Suche und freier Eingabe (sog. "Combobox").
 *
 * - Sieht aus wie ein normales Bootstrap-Auswahlfeld (mit Pfeil rechts).
 * - Klick ins Feld: alle Vorschlaege werden angezeigt.
 * - Tippen: die Liste wird nach dem eingegebenen Text gefiltert.
 * - Steht etwas nicht in der Liste, kann man es einfach selbst eintragen.
 * - Tastatur: Pfeil hoch/runter zum Markieren, Enter zum Uebernehmen, Esc zum Schliessen.
 *
 * Props:
 *   value        aktueller Text im Feld
 *   onChange     wird mit dem neuen Text aufgerufen
 *   optionen     Liste der Vorschlaege (Array von Strings)
 *   placeholder  grauer Hinweistext im leeren Feld
 */
function AuswahlFeld({ value, onChange, optionen, placeholder }) {

    const [offen, setOffen] = useState(false);
    const [filtern, setFiltern] = useState(false);
    const [markiert, setMarkiert] = useState(-1);

    // Beim Tippen filtern, beim reinen Oeffnen alle Vorschlaege zeigen
    const sichtbareOptionen = filtern && value
        ? optionen.filter((option) =>
            option.toLowerCase().includes(value.toLowerCase())
        )
        : optionen;

    const oeffnen = () => {
        setOffen(true);
        setFiltern(false);
        setMarkiert(-1);
    };

    const auswaehlen = (option) => {
        onChange(option);
        setOffen(false);
        setMarkiert(-1);
    };

    const handleTippen = (event) => {
        onChange(event.target.value);
        setOffen(true);
        setFiltern(true);
        setMarkiert(-1);
    };

    const handleTaste = (event) => {
        if (event.key === "ArrowDown") {
            event.preventDefault();
            setOffen(true);
            setMarkiert((alt) => Math.min(alt + 1, sichtbareOptionen.length - 1));
        } else if (event.key === "ArrowUp") {
            event.preventDefault();
            setMarkiert((alt) => Math.max(alt - 1, 0));
        } else if (event.key === "Enter" && offen && markiert >= 0) {
            // Enter uebernimmt den markierten Vorschlag (und schickt das Formular NICHT ab)
            event.preventDefault();
            auswaehlen(sichtbareOptionen[markiert]);
        } else if (event.key === "Escape") {
            setOffen(false);
        }
    };

    return (
        <div className="position-relative">
            <input
                type="text"
                className="form-select"
                placeholder={placeholder}
                value={value}
                onChange={handleTippen}
                onFocus={oeffnen}
                onClick={oeffnen}
                onBlur={() => setOffen(false)}
                onKeyDown={handleTaste}
                autoComplete="off"
            />

            {offen && sichtbareOptionen.length > 0 && (
                <ul
                    className="list-group position-absolute w-100 shadow-sm"
                    style={{ zIndex: 1000, maxHeight: "240px", overflowY: "auto" }}
                >
                    {sichtbareOptionen.map((option, index) => (
                        <li
                            key={option}
                            className={`list-group-item list-group-item-action ${index === markiert ? "active" : ""}`}
                            style={{ cursor: "pointer" }}
                            // onMouseDown statt onClick: sonst schliesst onBlur die Liste,
                            // bevor der Klick ankommt
                            onMouseDown={(event) => {
                                event.preventDefault();
                                auswaehlen(option);
                            }}
                        >
                            {option}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

export default AuswahlFeld;