# Datenbankstruktur – SOPhub

## Verwendete Datenbank
PostgreSQL

---

# Tabellenübersicht

## 1. users

Speichert alle Benutzer des Systems.

| Feld        | Typ       | Beschreibung         |
| ----------- | --------- | -------------------- |
| id          | SERIAL    | Primärschlüssel      |
| vorname     | VARCHAR   | Vorname des Nutzers  |
| nachname    | VARCHAR   | Nachname des Nutzers |
| email       | VARCHAR   | Hochschul-E-Mail     |
| rolle_id    | INTEGER   | Verweis auf Rollen   |
| erstellt_am | TIMESTAMP | Erstellungsdatum     |

---

## 2. roles

Speichert die Benutzerrollen.

| Feld       | Typ     | Beschreibung    |
| ---------- | ------- | --------------- |
| id         | SERIAL  | Primärschlüssel |
| rollenname | VARCHAR | Name der Rolle  |

Beispiele:
- Student
- Lehrender
- Admin

---

## 3. projects

Speichert SOP-Projekte.

| Feld         | Typ     | Beschreibung           |
| ------------ | ------- | ---------------------- |
| id           | SERIAL  | Primärschlüssel        |
| titel        | VARCHAR | Projekttitel           |
| beschreibung | TEXT    | Projektbeschreibung    |
| status       | VARCHAR | Projektstatus          |
| semester     | VARCHAR | Zugehöriges Semester   |
| erstellt_von | INTEGER | Ersteller des Projekts |

---

## 4. ideas

Speichert Projektideen.

| Feld         | Typ     | Beschreibung    |
| ------------ | ------- | --------------- |
| id           | SERIAL  | Primärschlüssel |
| titel        | VARCHAR | Titel der Idee  |
| beschreibung | TEXT    | Beschreibung    |
| erstellt_von | INTEGER | Ersteller       |

---

## 5. teams

Verknüpft Nutzer mit Projekten.

| Feld       | Typ     | Beschreibung    |
| ---------- | ------- | --------------- |
| id         | SERIAL  | Primärschlüssel |
| projekt_id | INTEGER | Projekt-ID      |
| user_id    | INTEGER | Nutzer-ID       |
| teamrolle  | VARCHAR | Rolle im Team   |

---

## 6. documents

Speichert Dokument-Metadaten.

| Feld        | Typ     | Beschreibung          |
| ----------- | ------- | --------------------- |
| id          | SERIAL  | Primärschlüssel       |
| projekt_id  | INTEGER | Zugehöriges Projekt   |
| dateiname   | VARCHAR | Dateiname             |
| dateityp    | VARCHAR | PDF/DOCX/TXT          |
| uploader_id | INTEGER | Hochladender Benutzer |

---

## 7. tags

Speichert Schlagwörter.

| Feld | Typ     | Beschreibung    |
| ---- | ------- | --------------- |
| id   | SERIAL  | Primärschlüssel |
| name | VARCHAR | Tagname         |

---

## 8. project_tags

Verknüpfung zwischen Projekten und Tags.

| Feld       | Typ     | Beschreibung    |
| ---------- | ------- | --------------- |
| id         | SERIAL  | Primärschlüssel |
| projekt_id | INTEGER | Projekt-ID      |
| tag_id     | INTEGER | Tag-ID          |

---

# Beziehungen

## users ↔ roles
Viele Benutzer besitzen genau eine Rolle.

## users ↔ projects
Ein Benutzer kann mehrere Projekte erstellen.

## users ↔ teams
Ein Benutzer kann Mitglied mehrerer Teams sein.

## projects ↔ teams
Ein Projekt besitzt mehrere Teammitglieder.

## projects ↔ documents
Ein Projekt kann mehrere Dokumente besitzen.

## projects ↔ tags
Ein Projekt kann mehrere Tags besitzen.

---

# Ziel des Datenmodells

Das Datenmodell dient dazu:

- Projekte strukturiert zu verwalten
- Teamarbeit abzubilden
- Dokumente zentral zu speichern
- KI-Funktionen auf Projektdaten anzuwenden
- spätere Erweiterungen zu ermöglichen