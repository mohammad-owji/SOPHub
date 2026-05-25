# Backend-Testprotokolle – SOPhub

## Test 1 – PostgreSQL Verbindung

### Ziel
Überprüfen, ob PostgreSQL korrekt installiert und erreichbar ist.

### Durchführung
- PostgreSQL gestartet
- pgAdmin geöffnet
- Datenbank "sophub" erstellt
- Verbindung erfolgreich hergestellt

### Erwartetes Ergebnis
Die Datenbank ist erreichbar und Tabellen können erstellt werden.

### Ergebnis
✅ Erfolgreich


---

## Test 2 – Tabellen erstellen

### Ziel
Überprüfen, ob die SQL-Tabellen korrekt erstellt werden.

### Durchführung
- init_database.sql ausgeführt
- Tabellen automatisch erstellt

### Erwartetes Ergebnis
Alle Tabellen erscheinen in pgAdmin.

### Ergebnis
✅ Erfolgreich


---

## Test 3 – Daten einfügen

### Ziel
Überprüfen, ob Daten korrekt gespeichert werden.

### Durchführung
- INSERT INTO queries ausgeführt
- Testdaten gespeichert

### Erwartetes Ergebnis
Daten erscheinen korrekt in den Tabellen.

### Ergebnis
✅ Erfolgreich


---

## Test 4 – SQL-Abfragen

### Ziel
Überprüfung von SELECT-Abfragen und JOINs.

### Durchführung
- SELECT queries ausgeführt
- JOIN zwischen Projekten und Tags getestet

### Erwartetes Ergebnis
Korrekte Daten werden zurückgegeben.

### Ergebnis
✅ Erfolgreich


---

## Test 5 – GitHub Repository

### Ziel
Überprüfung der GitHub-Synchronisation.

### Durchführung
- git add .
- git commit
- git push

### Erwartetes Ergebnis
Alle Änderungen erscheinen im GitHub Repository.

### Ergebnis
✅ Erfolgreich