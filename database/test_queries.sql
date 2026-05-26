-- =========================================
-- TEST QUERIES – SOPHub
-- =========================================


-- =========================================
-- 1. Rollen anzeigen
-- =========================================

SELECT * FROM roles;


-- =========================================
-- 2. Benutzer anzeigen
-- =========================================

SELECT * FROM users;


-- =========================================
-- 3. Projekte anzeigen
-- =========================================

SELECT * FROM projects;


-- =========================================
-- 4. Projektideen anzeigen
-- =========================================

SELECT * FROM ideas;


-- =========================================
-- 5. Teams anzeigen
-- =========================================

SELECT * FROM teams;


-- =========================================
-- 6. Dokumente anzeigen
-- =========================================

SELECT * FROM documents;


-- =========================================
-- 7. Tags anzeigen
-- =========================================

SELECT * FROM tags;


-- =========================================
-- 8. Projekt-Tags anzeigen
-- =========================================

SELECT * FROM project_tags;


-- =========================================
-- 9. Projekte + Ersteller anzeigen
-- =========================================

SELECT
    projects.id,
    projects.titel,
    users.vorname,
    users.nachname
FROM projects
JOIN users
ON projects.erstellt_von = users.id;


-- =========================================
-- 10. Projekte + Teammitglieder
-- =========================================

SELECT
    projects.titel,
    users.vorname,
    users.nachname,
    teams.teamrolle
FROM teams
JOIN projects
ON teams.projekt_id = projects.id
JOIN users
ON teams.user_id = users.id;


-- =========================================
-- 11. Projekte + Tags
-- =========================================

SELECT
    projects.titel,
    tags.name AS tag
FROM project_tags
JOIN projects
ON project_tags.projekt_id = projects.id
JOIN tags
ON project_tags.tag_id = tags.id;


-- =========================================
-- 12. Dokumente + Projekte
-- =========================================

SELECT
    documents.dateiname,
    documents.dateityp,
    projects.titel
FROM documents
JOIN projects
ON documents.projekt_id = projects.id;


-- =========================================
-- 13. Anzahl der Projekte
-- =========================================

SELECT COUNT(*) AS projektanzahl
FROM projects;


-- =========================================
-- 14. Anzahl der Benutzer
-- =========================================

SELECT COUNT(*) AS benutzeranzahl
FROM users;


-- =========================================
-- 15. Alle Studenten anzeigen
-- =========================================

SELECT
    users.vorname,
    users.nachname,
    roles.rollenname
FROM users
JOIN roles
ON users.rolle_id = roles.id
WHERE roles.rollenname = 'Student';


-- =========================================
-- 16. Laufende Projekte anzeigen
-- =========================================

SELECT *
FROM projects
WHERE status = 'laufend';


-- =========================================
-- 17. Projekte mit React anzeigen
-- =========================================

SELECT
    projects.titel,
    tags.name
FROM project_tags
JOIN projects
ON project_tags.projekt_id = projects.id
JOIN tags
ON project_tags.tag_id = tags.id
WHERE tags.name = 'React';


-- =========================================
-- 18. Dokumente eines Projekts anzeigen
-- =========================================

SELECT
    projects.titel,
    documents.dateiname
FROM documents
JOIN projects
ON documents.projekt_id = projects.id
WHERE projects.id = 1;


-- =========================================
-- 19. Teammitglieder eines Projekts
-- =========================================

SELECT
    projects.titel,
    users.vorname,
    users.nachname,
    teams.teamrolle
FROM teams
JOIN users
ON teams.user_id = users.id
JOIN projects
ON teams.projekt_id = projects.id
WHERE projects.id = 1;


-- =========================================
-- 20. Vollständige Projektübersicht
-- =========================================

SELECT
    projects.titel,
    projects.status,
    users.vorname AS erstellt_von,
    tags.name AS technologie
FROM project_tags
JOIN projects
ON project_tags.projekt_id = projects.id
JOIN tags
ON project_tags.tag_id = tags.id
JOIN users
ON projects.erstellt_von = users.id;