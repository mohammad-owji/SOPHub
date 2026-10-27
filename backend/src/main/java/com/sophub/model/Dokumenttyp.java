package com.sophub.model;

/**
 * Kanonische Dokumenttypen eines Projekts. Wird im bestehenden String-Feld
 * {@link Dokument#getTyp()} als Enum-Name gespeichert (keine zusätzliche Spalte).
 * <p>
 * Unbekannte oder alte Freitext-Werte werden über {@link #vonEingabe(String)}
 * auf {@link #SONSTIGES} abgebildet; {@link #ausDateiname(String)} liefert eine
 * einfache Heuristik für Altdaten/fehlende Angaben.
 */
public enum Dokumenttyp {
    LASTENHEFT,
    PFLICHTENHEFT,
    ABSCHLUSSBERICHT,
    PRAESENTATION,
    SONSTIGES;

    /** Standardtyp, wenn nichts erkannt wird. */
    public static final Dokumenttyp STANDARD = SONSTIGES;

    /** Mappt eine (freie) Eingabe auf einen bekannten Typ; unbekannt -> SONSTIGES. */
    public static Dokumenttyp vonEingabe(String wert) {
        if (wert == null) {
            return STANDARD;
        }
        String w = wert.trim().toUpperCase();
        for (Dokumenttyp typ : values()) {
            if (typ.name().equals(w)) {
                return typ;
            }
        }
        return STANDARD;
    }

    /** Einfache Heuristik über den Dateinamen (für Altdaten / fehlende Angabe). */
    public static Dokumenttyp ausDateiname(String dateiname) {
        if (dateiname == null) {
            return STANDARD;
        }
        String n = dateiname.toLowerCase();
        if (n.contains("lastenheft")) {
            return LASTENHEFT;
        }
        if (n.contains("pflichtenheft")) {
            return PFLICHTENHEFT;
        }
        if (n.contains("abschluss") || n.contains("abschlussbericht") || n.contains("endbericht")) {
            return ABSCHLUSSBERICHT;
        }
        if (n.contains("praesent") || n.contains("präsent") || n.contains("presentation")
                || n.contains("folien") || n.contains("slides")) {
            return PRAESENTATION;
        }
        return STANDARD;
    }

    /**
     * Bestimmt den Typ: bevorzugt die ausdrückliche Eingabe; ist diese unbekannt
     * bzw. SONSTIGES, wird über den Dateinamen verfeinert.
     */
    public static Dokumenttyp bestimme(String eingabe, String dateiname) {
        Dokumenttyp ausEingabe = vonEingabe(eingabe);
        return ausEingabe != SONSTIGES ? ausEingabe : ausDateiname(dateiname);
    }
}
