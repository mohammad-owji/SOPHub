# API-Dokumentation – SOPhub

## 1. Authentifizierung

| Methode | Endpoint         | Beschreibung                        |
| ------- | ---------------- | ----------------------------------- |
| POST    | /api/auth/login  | Anmeldung über LDAP                 |
| POST    | /api/auth/logout | Abmeldung                           |
| GET     | /api/auth/me     | Aktuell angemeldeten Nutzer abrufen |

## 2. Projekte

| Methode | Endpoint           | Beschreibung                     |
| ------- | ------------------ | -------------------------------- |
| GET     | /api/projects      | Alle Projekte abrufen            |
| GET     | /api/projects/{id} | Einzelnes Projekt abrufen        |
| POST    | /api/projects      | Neues Projekt erstellen          |
| PUT     | /api/projects/{id} | Projekt bearbeiten               |
| DELETE  | /api/projects/{id} | Projekt löschen oder archivieren |

## 3. Projektideen

| Methode | Endpoint        | Beschreibung                 |
| ------- | --------------- | ---------------------------- |
| GET     | /api/ideas      | Alle Projektideen abrufen    |
| GET     | /api/ideas/{id} | Einzelne Projektidee abrufen |
| POST    | /api/ideas      | Neue Projektidee erstellen   |
| PUT     | /api/ideas/{id} | Projektidee bearbeiten       |
| DELETE  | /api/ideas/{id} | Projektidee löschen          |

## 4. Teams

| Methode | Endpoint                         | Beschreibung                          |
| ------- | -------------------------------- | ------------------------------------- |
| POST    | /api/projects/{id}/join          | Beitrittsanfrage zu Projekt senden    |
| GET     | /api/projects/{id}/team          | Teammitglieder eines Projekts abrufen |
| POST    | /api/projects/{id}/team          | Teammitglied hinzufügen               |
| DELETE  | /api/projects/{id}/team/{userId} | Teammitglied entfernen                |

## 5. Dokumente

| Methode | Endpoint                     | Beschreibung                     |
| ------- | ---------------------------- | -------------------------------- |
| POST    | /api/projects/{id}/documents | Dokument hochladen               |
| GET     | /api/projects/{id}/documents | Dokumente eines Projekts abrufen |
| GET     | /api/documents/{id}/download | Dokument herunterladen           |
| DELETE  | /api/documents/{id}          | Dokument löschen                 |

## 6. Suche und Filter

| Methode | Endpoint                     | Beschreibung                   |
| ------- | ---------------------------- | ------------------------------ |
| GET     | /api/search?query=...        | Suche nach Projekten und Ideen |
| GET     | /api/projects?tag=React      | Projekte nach Tag filtern      |
| GET     | /api/projects?status=laufend | Projekte nach Status filtern   |

## 7. KI-Funktionen

| Methode | Endpoint                | Beschreibung                  |
| ------- | ----------------------- | ----------------------------- |
| POST    | /api/ai/summary         | KI-Zusammenfassung generieren |
| POST    | /api/ai/search          | KI-gestützte Suche ausführen  |
| POST    | /api/ai/recommendations | Ähnliche Projekte empfehlen   |