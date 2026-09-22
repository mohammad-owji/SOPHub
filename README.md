# SOPHub

Eine webbasierte Plattform zur Unterstützung des Softwareprojekts (SOP) im Informatikstudiengang.

## Technologien

| Bereich | Technologie |
|---|---|
| Frontend | React |
| Backend | Java 21 + Spring Boot 4 |
| Datenbank | PostgreSQL 17 |
| Authentifizierung | LDAP |

---

## Voraussetzungen

- [Java 21](https://adoptium.net)
- [PostgreSQL 17](https://www.postgresql.org/download/windows/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) (empfohlen)
- [Node.js](https://nodejs.org/) (für Frontend)
- Git

---

## Installation

### 1. Repository clonen

```bash
git clone <repo-url>
cd SOPHub
```

### 2. PostgreSQL einrichten

1. PostgreSQL 17 installieren
2. pgAdmin öffnen
3. Rechtsklick auf **Databases** → **Create** → **Database**
4. Name: `sophub` → **Save**

### 3. Lokale Konfiguration erstellen

Diese Datei ist **nicht in Git** und muss von jedem selbst erstellt werden.

Datei erstellen unter:
```
backend/src/main/resources/application.properties
```

Inhalt:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sophub
spring.datasource.username=postgres
spring.datasource.password=DeinPostgreSQLPasswort
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 4. Backend starten

In IntelliJ:
1. Projekt öffnen
2. `backend/src/main/java/com/sophub/SophubApplication.java` öffnen
3. ▶ Run klicken

Oder im Terminal:
```bash
cd backend
.\mvnw.cmd spring-boot:run
```

Backend läuft dann auf: `http://localhost:8080`

### 5. Frontend starten

```bash
cd frontend
npm install
npm start
```

Frontend läuft dann auf: `http://localhost:3000`

---

## KI-Setup

SOPHub nutzt die kostenlose Google-Gemini-API für automatische Projekt-/Dokument-Zusammenfassungen und Auto-Tagging.

### 1. Kostenlosen Gemini-API-Key erstellen

1. [aistudio.google.com](https://aistudio.google.com) öffnen und mit einem Google-Konto einloggen
2. Auf **"Get API key"** bzw. **"Create API key"** klicken
3. Ein Projekt auswählen (oder neu erstellen) und den generierten Key kopieren

### 2. API-Key als Umgebungsvariable setzen

Der Key wird **nicht** in `application.properties` eingetragen, sondern als Umgebungsvariable gesetzt (`gemini.api.key` liest ihn automatisch aus `GEMINI_API_KEY`):

**Windows (PowerShell):**
```powershell
setx GEMINI_API_KEY "dein-key"
```
Danach Terminal bzw. IntelliJ neu starten, damit die Variable geladen wird.

**macOS/Linux (bash/zsh):**
```bash
export GEMINI_API_KEY="dein-key"
```

Optional kann auch das verwendete Modell überschrieben werden (Standard: `gemini-3.6-flash`):
```bash
export GEMINI_MODEL="gemini-3.6-flash"
```

### 3. Testen, dass die KI-Funktion läuft

Direkter Test der Gemini-API per curl (unabhängig vom Backend):
```bash
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"contents":[{"parts":[{"text":"Sag Hallo in einem Satz."}]}]}'
```

Test über das Backend (Backend muss laufen, gültiger JWT-Token z. B. aus dem Login-Response nötig):
```bash
curl -X POST http://localhost:8080/sop/api/ai/generieren \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DEIN_JWT_TOKEN>" \
  -d '{"text":"Sag Hallo in einem Satz."}'
```

Kommt eine KI-Antwort zurück, ist die Konfiguration korrekt.

---

## Projektstruktur

```
SOPHub/
├── backend/                        # Java Spring Boot Backend
│   └── src/main/java/com/sophub/
│       ├── controller/             # API Endpunkte
│       ├── service/                # Geschäftslogik
│       ├── repository/             # Datenbankzugriff
│       ├── model/                  # Datenbankmodelle
│       └── config/                 # Konfigurationen
├── frontend/                       # React Frontend
│   └── src/
│       ├── pages/                  # Seiten (Login, Dashboard)
│       ├── components/             # UI Komponenten
│       ├── services/               # API Aufrufe
│       └── context/                # Globaler State
├── database/                       # SQL Skripte
└── docs/                           # Dokumentation
```

---

## Team

| Name | Bereich |
|---|---|
| Mohammad Owji | Backend / Systemlogik / Schnittstellen |
| Dalieh Ghuzlan | Frontend / UI / Nutzerführung |
| Salma Matlob | Datenmodell / Dokumentation / Tests |

---

## Hinweise

- Die Datei `application.properties` niemals in Git hochladen
- Passwörter und Secrets gehören nicht in den Code
