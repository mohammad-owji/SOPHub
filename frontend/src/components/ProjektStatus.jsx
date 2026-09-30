/**
 * Einheitliche Anzeige des Projektstatus in ganz SOPhub.
 *
 * Im Backend gibt es diese Status (siehe ProjektService):
 *   ENTWURF       -> noch kein Betreuer ausgewaehlt
 *   OFFEN         -> Anfrage an Betreuer gesendet, Antwort steht aus
 *   ANGENOMMEN    -> Betreuer hat zugesagt
 *   ABGELEHNT     -> Betreuer hat abgesagt
 *   abgeschlossen -> Projekt ist fertig (z.B. Beispielprojekte)
 *
 * Gross-/Kleinschreibung spielt keine Rolle.
 */
export const PROJEKT_STATUS = {
    ENTWURF: { text: "Entwurf", farbe: "bg-secondary" },
    OFFEN: { text: "Wartet auf Betreuer", farbe: "bg-warning" },
    ANGENOMMEN: { text: "Angenommen", farbe: "bg-success" },
    ABGELEHNT: { text: "Abgelehnt", farbe: "bg-danger" },
    ABGESCHLOSSEN: { text: "Abgeschlossen", farbe: "bg-primary" },
};

export function statusInfo(status) {
    const schluessel = (status || "").toUpperCase();
    return PROJEKT_STATUS[schluessel] || { text: status || "Unbekannt", farbe: "bg-secondary" };
}

// Abzeichen, z.B. <ProjektStatusBadge status={projekt.status} />
function ProjektStatusBadge({ status, className = "" }) {
    const info = statusInfo(status);
    return <span className={`badge ${info.farbe} ${className}`}>{info.text}</span>;
}

export default ProjektStatusBadge;