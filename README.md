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
