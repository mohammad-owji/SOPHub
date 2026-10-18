// Gemeinsame Auswahllisten fuer Projektdaten (Projekt erstellen / Projekt bearbeiten).
// Man kann auswaehlen, danach suchen oder einen eigenen Wert eintragen (AuswahlFeld).

export const SEMESTER = [
    "SoSe 2027",
    "WiSe 2026/27",
    "SoSe 2026",
    "WiSe 2025/26",
    "SoSe 2025",
];

// Aktuelles Semester als Vorauswahl beim Erstellen
export const STANDARD_SEMESTER = "WiSe 2026/27";

/**
 * Fachbereiche -> Studiengaenge -> Projektarten der Hochschule Bochum.
 * Die Projektarten stammen aus den aktuellen Modulhandbuechern der Studiengaenge
 * (Stand Oktober 2026, Quelle: hochschule-bochum.de). "Bachelorarbeit"/"Masterarbeit"
 * schliessen das Kolloquium ein. Fehlt etwas, kann man es weiterhin selbst eintippen.
 */
export const STUDIENANGEBOT = {
    "Architektur": {
        "Architektur (B.Sc.)": ["Entwurfsprojekt", "Städtebauliches Projekt", "Konstruktives Projekt", "Seminar", "Exkursion", "Büropraktikum", "Bachelorarbeit"],
        "Architektur Media Management (M.Sc.)": ["Projektarbeit", "Masterprojekt", "Exkursion", "Masterarbeit"],
        "Architektur: ProjektEntwicklung (M.Sc.)": ["Projekt", "Masterarbeit"],
    },
    "Bau- und Umweltingenieurwesen": {
        "Bauingenieurwesen (B.Sc.)": ["Projektseminar", "BIM-Projekt", "Praxisphase", "Bachelorarbeit"],
        "Umweltingenieurwesen (B.Sc.)": ["Projektseminar", "BIM-Projekt", "Praxisphase", "Bachelorarbeit"],
        "Bauingenieurwesen (M.Sc.)": ["Projekt", "BIM-Seminar", "Masterarbeit"],
        "Umweltingenieurwesen (M.Sc.)": ["Projektarbeit", "BIM-Seminar", "Masterarbeit"],
    },
    "Elektrotechnik und Informatik": {
        "Elektrotechnik (B.Sc.)": ["Studienprojekt", "Praxisphase", "Bachelorarbeit"],
        "Informatik (B.Sc.)": ["Softwarepraktikum", "Praxisphase", "Bachelorarbeit"],
        "Wirtschaftsinformatik (B.Sc.)": ["Seminar", "Praxisphase", "Bachelorarbeit"],
        "Nachhaltige Entwicklung (B.Sc.)": ["Projektstudien", "Praxisphase", "Bachelorarbeit"],
        "Elektrotechnik (M.Sc.)": ["Projektarbeit", "Masterarbeit"],
        "Informatik (M.Sc.)": ["Entwicklungsprojekt", "Projektbasierte Vertiefung", "Seminar", "Masterarbeit"],
        "Nachhaltige Entwicklung (M.Sc.)": ["Projektstudien", "Masterarbeit"],
        "Angewandte Nachhaltigkeit (M.Sc.)": ["Projektstudien", "Seminar", "Masterarbeit"],
    },
    "Geodäsie": {
        "Vermessung (B.Eng.)": ["Vermessungsprojekt", "Seminar", "Praxisphase", "Bachelorarbeit"],
        "Umweltinformatik (B.Sc.)": ["Projekt", "Praxisphase", "Bachelorarbeit"],
        "Geodäsie (M.Eng.)": ["Projektarbeit", "BIM-Seminar", "Summer School", "Masterarbeit"],
    },
    "Gesundheitswissenschaften": {
        "Gesundheitsökonomie (B.Sc.)": ["Projekt", "Praxissemester", "Bachelorarbeit"],
        "Gesundheitsbezogene Soziale Arbeit (B.Sc.)": ["Forschungsprojekt", "Praxisphase", "Seminar", "Bachelorarbeit"],
        "Gesundheitswissenschaften (B.Sc.)": ["Forschungsprojekt", "Seminar", "Bachelorarbeit"],
        "Management nachhaltiger Innovationen im Gesundheitswesen (B.Sc.)": ["Projekt", "Praxissemester", "Bachelorarbeit"],
        "Nachhaltiges Management in der Gesundheitswirtschaft (B.Sc.)": ["Projekt", "Praxissemester", "Bachelorarbeit"],
        "Angewandte Gesundheitswissenschaften (M.Sc.)": ["Forschungsprojekt", "Seminar", "Masterarbeit"],
        "Gesundheit und Diversity in der Arbeit (M.A.)": ["Forschungsprojekt", "Masterarbeit"],
        "Management für Pflege- und Gesundheitsberufe (M.A.)": ["Forschungsprojekt", "Masterarbeit"],
    },
    "Mechatronik und Maschinenbau": {
        "Maschinenbau (B.Eng.)": ["Projektfach", "Entwicklungsprojekt", "Erweitertes Entwicklungsprojekt", "Praxisprojekt", "Praxisphase", "Praxisauslandssemester", "Bachelorarbeit"],
        "Mechatronik (B.Eng.)": ["Entwicklungsprojekt", "Erweitertes Entwicklungsprojekt", "Praxisprojekt", "Praxisphase", "Bachelorarbeit"],
        "Maschinenbau (M.Sc.)": ["Projektarbeit", "Erweiterte Projektarbeit", "Seminar", "Masterarbeit"],
        "Mechatronik (M.Sc.)": ["Projektarbeit", "Erweiterte Projektarbeit", "Seminar", "Masterarbeit"],
        "Mechanical Engineering International (M.Sc.)": ["Project Work", "Extended Project Work", "Master's Thesis"],
    },
    "Pflege-, Hebammen- und Therapiewissenschaften": {
        "Clinical Research Management (B.Sc.)": ["Forschungsprojekt", "Praxismodul", "Praxisphase", "Bachelorarbeit"],
        "Ergotherapie (B.Sc.)": ["Interprofessionelles Projekt", "Praxisphase", "Bachelorarbeit"],
        "Ergotherapie – weiterqualifizierend (B.Sc.)": ["Interprofessionelles Projekt", "Bachelorarbeit"],
        "Gesundheitspsychologie (B.Sc.)": ["Projekt", "Forschungsprojekt", "Praxisphase", "Seminar", "Bachelorarbeit"],
        "Hebammenwissenschaft (B.Sc.)": ["Interprofessionelles Projekt", "Praxisphase", "Bachelorarbeit"],
        "Logopädie (B.Sc.)": ["Interprofessionelles Projekt", "Praxisphase", "Bachelorarbeit"],
        "Logopädie – weiterqualifizierend (B.Sc.)": ["Interprofessionelles Projekt", "Praxisphase", "Bachelorarbeit"],
        "Physiotherapie (B.Sc.)": ["Interprofessionelles Projekt", "Praxisphase", "Bachelorarbeit"],
        "Physiotherapie – weiterqualifizierend (B.Sc.)": ["Interprofessionelles Projekt", "Bachelorarbeit"],
        "Pflegewissenschaft (B.Sc.)": ["Forschungsprojekt", "Praxismodul", "Bachelorarbeit"],
        "Pflege (B.Sc.)": ["Praxismodul", "Bachelorarbeit"],
        "Advanced Nursing Practice (M.Sc.)": ["Praxismodul", "Masterarbeit"],
        "Hebammenwissenschaft (M.Sc.)": ["Seminar", "Masterarbeit"],
        "Pflege- und Gesundheitspädagogik (M.A.)": ["Praxisphase", "Masterarbeit"],
        "Therapiewissenschaften (M.Sc.)": ["Projekt", "Masterarbeit"],
    },
    "Wirtschaft": {
        "Betriebswirtschaftslehre (B.A.)": ["Unternehmensplanspiel", "Strategisches Unternehmensplanspiel", "Business Case Studies", "Entwicklungsprojekt", "Praxisphase", "Bachelorarbeit"],
        "Betriebswirtschaftslehre – Verbund (B.A.)": ["Unternehmenssimulation", "Internationales Projekt", "Seminar", "Bachelorarbeit"],
        "International Business and Management (B.A.)": ["Unternehmensplanspiel", "Strategisches Unternehmensplanspiel", "Business Case Studies", "Entwicklungsprojekt", "Auslandssemester", "Auslandspraktikum", "Bachelorarbeit"],
        "Wirtschaftsingenieurwesen Maschinenbau (B.Sc.)": ["Seminar", "Praxisphase", "Bachelorarbeit"],
        "Wirtschaftsingenieurwesen – Verbund (B.Sc.)": ["Unternehmenssimulation", "Seminar", "Bachelorarbeit"],
        "International Management (M.Sc.)": ["Business Case Studies", "Projekt", "Auslandssemester", "Auslandspraktikum", "Masterarbeit"],
        "Accounting and Taxation (M.A.)": ["Praktikum", "Masterarbeit"],
        "Accounting, Auditing and Taxation (M.A.)": ["Praktikum", "Masterarbeit"],
    },
};

const sortiert = (liste) => [...new Set(liste)].sort((a, b) => a.localeCompare(b, "de"));

export const FACHBEREICHE = Object.keys(STUDIENANGEBOT);

// Alle Studiengaenge bzw. Projektarten (ohne Doppelte), z.B. solange nichts gewaehlt ist
export const STUDIENGAENGE = sortiert(Object.values(STUDIENANGEBOT).flatMap((sg) => Object.keys(sg)));
export const PROJEKTARTEN = sortiert(
    Object.values(STUDIENANGEBOT).flatMap((sg) => Object.values(sg).flat())
);

// Fruehere Fachbereich-Namen (aeltere Projekte) dem richtigen Fachbereich zuordnen
const ALTE_FACHBEREICHE = {
    "informatik": "Elektrotechnik und Informatik",
    "wirtschaftsinformatik": "Elektrotechnik und Informatik",
    "medieninformatik": "Elektrotechnik und Informatik",
    "elektrotechnik": "Elektrotechnik und Informatik",
    "mechatronik": "Mechatronik und Maschinenbau",
    "maschinenbau": "Mechatronik und Maschinenbau",
    "bauingenieurwesen": "Bau- und Umweltingenieurwesen",
    "umweltingenieurwesen": "Bau- und Umweltingenieurwesen",
};

const klein = (text) => (text || "").trim().toLowerCase();

/** Offizieller Fachbereich zu einem (auch alten) Namen, sonst null. */
export function findeFachbereich(name) {
    const text = klein(name);
    if (!text) return null;
    return FACHBEREICHE.find((fb) => fb.toLowerCase() === text) || ALTE_FACHBEREICHE[text] || null;
}

/** Fachbereich, zu dem ein Studiengang gehoert, sonst null. */
export function fachbereichVonStudiengang(studiengang) {
    const text = klein(studiengang);
    if (!text) return null;
    return FACHBEREICHE.find((fb) =>
        Object.keys(STUDIENANGEBOT[fb]).some((sg) => sg.toLowerCase() === text)
    ) || null;
}

/** Projektarten eines Studiengangs, sonst null. */
function projektartenVonStudiengang(studiengang) {
    const fb = fachbereichVonStudiengang(studiengang);
    if (!fb) return null;
    const name = Object.keys(STUDIENANGEBOT[fb]).find((sg) => sg.toLowerCase() === klein(studiengang));
    return STUDIENANGEBOT[fb][name];
}

/** Studiengaenge des Fachbereichs; ohne bekannten Fachbereich alle. */
export function studiengaengeFuer(fachbereich) {
    const fb = findeFachbereich(fachbereich);
    return fb ? Object.keys(STUDIENANGEBOT[fb]) : STUDIENGAENGE;
}

/** Projektarten: zuerst nach Studiengang, sonst nach Fachbereich, sonst alle. */
export function projektartenFuer(fachbereich, studiengang) {
    const nachStudiengang = projektartenVonStudiengang(studiengang);
    if (nachStudiengang) return nachStudiengang;
    const fb = findeFachbereich(fachbereich);
    return fb ? sortiert(Object.values(STUDIENANGEBOT[fb]).flat()) : PROJEKTARTEN;
}

// Projektart leeren, wenn sie zu einer bekannten Auswahl nicht passt
function pruefeProjektart(daten) {
    const bekannt = fachbereichVonStudiengang(daten.studiengang) || findeFachbereich(daten.fachbereich);
    if (!daten.projektart || !bekannt) return daten;
    const passt = projektartenFuer(daten.fachbereich, daten.studiengang).includes(daten.projektart);
    return passt ? daten : { ...daten, projektart: "" };
}

/**
 * Neuer Fachbereich: Studiengang und Projektart werden geleert, wenn sie nicht dazu passen.
 * daten = { fachbereich, studiengang, projektart }
 */
export function nachFachbereichWechsel(daten, neuerFachbereich) {
    let neu = { ...daten, fachbereich: neuerFachbereich };
    const fb = findeFachbereich(neuerFachbereich);
    const fbDesStudiengangs = fachbereichVonStudiengang(neu.studiengang);
    if (fb && fbDesStudiengangs && fbDesStudiengangs !== fb) {
        neu = { ...neu, studiengang: "" };
    }
    return pruefeProjektart(neu);
}

/**
 * Neuer Studiengang: der passende Fachbereich wird automatisch gesetzt,
 * eine nicht passende Projektart wird geleert.
 */
export function nachStudiengangWechsel(daten, neuerStudiengang) {
    let neu = { ...daten, studiengang: neuerStudiengang };
    const fb = fachbereichVonStudiengang(neuerStudiengang);
    if (fb) {
        neu = { ...neu, fachbereich: fb };
    }
    return pruefeProjektart(neu);
}

// Moegliche Gruppengroessen (das Backend erlaubt 1 bis 10)
export const GRUPPENGROESSEN = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];