# ERD-Erklärung – SOPhub

## Ziel des ERD

Das Entity-Relationship-Diagramm (ERD) beschreibt die Struktur der Datenbank des SOPHub-Systems. Es zeigt die wichtigsten Tabellen, ihre Attribute sowie die Beziehungen zwischen den einzelnen Entitäten.

Das Datenmodell dient dazu, Projekte, Nutzer, Teams, Dokumente und KI-gestützte Funktionen strukturiert und effizient verwalten zu können.

---

# Zentrale Entitäten

## users

Die Tabelle `users` speichert alle Benutzer des Systems.

Dazu gehören:
- Studierende
- Lehrende
- Administratoren

Jeder Benutzer besitzt:
- persönliche Informationen
- eine Hochschul-Mailadresse
- eine Rolle
- Login-Informationen

---

## roles

Die Tabelle `roles` definiert die Benutzerrollen.

Beispiele:
- Student
- Lehrender
- Administrator

Jeder Benutzer besitzt genau eine Rolle.

---

## projects

Die Tabelle `projects` speichert alle SOP-Projekte.

Zu jedem Projekt werden unter anderem gespeichert:
- Titel
- Beschreibung
- Status
- Semester
- Teammitglieder
- betreuende Lehrpersonen

---

## ideas

Die Tabelle `ideas` speichert Projektideen.

Dadurch entsteht eine zentrale Ideenbörse für SOP-Themen.

---

## teams

Die Tabelle `teams` verbindet Benutzer mit Projekten.

Dadurch kann:
- ein Nutzer mehreren Projekten angehören
- ein Projekt mehrere Teammitglieder besitzen

Diese Tabelle bildet die Teamstruktur des Systems ab.

---

## documents

Die Tabelle `documents` verwaltet hochgeladene Dateien.

Gespeichert werden:
- Dateiname
- Dateityp
- Upload-Datum
- Projektzuordnung
- hochladender Benutzer

---

## tags

Die Tabelle `tags` speichert Schlagwörter.

Tags dienen zur:
- Filterung
- Suche
- Kategorisierung von Projekten

---

## project_tags

Diese Tabelle verbindet Projekte mit Tags.

Dadurch kann:
- ein Projekt mehrere Tags besitzen
- ein Tag mehreren Projekten zugeordnet werden

---

# Beziehungen im Datenmodell

## users → roles
Viele Benutzer können dieselbe Rolle besitzen.

Beziehung:
- N:1

---

## users → projects
Ein Benutzer kann mehrere Projekte erstellen.

Beziehung:
- 1:N

---

## users ↔ projects über teams
Benutzer und Projekte besitzen eine Viele-zu-Viele-Beziehung.

Diese wird über die Tabelle `teams` umgesetzt.

Beziehung:
- N:M

---

## projects → documents
Ein Projekt kann mehrere Dokumente besitzen.

Beziehung:
- 1:N

---

## projects ↔ tags über project_tags
Projekte und Tags besitzen eine Viele-zu-Viele-Beziehung.

Diese wird über `project_tags` umgesetzt.

Beziehung:
- N:M

---

# Vorteile des Datenmodells

Das Datenmodell bietet folgende Vorteile:

- klare Strukturierung aller Projektdaten
- einfache Erweiterbarkeit
- gute Wartbarkeit
- effiziente Such- und Filtermöglichkeiten
- Unterstützung der KI-Funktionen
- saubere Trennung der Systembereiche

---

# Bedeutung für das System

Das ERD bildet die Grundlage für:
- das Backend
- die REST-API
- die Suchfunktionen
- die KI-Analyse
- die Teamverwaltung
- das Dokumentenmanagement

Ohne ein sauberes Datenmodell wäre eine stabile und skalierbare Umsetzung des Systems nicht möglich.