-- Rollen

INSERT INTO roles (rollenname)
VALUES
('Student'),
('Lehrender'),
('Admin');

-- Benutzer
INSERT INTO users (
    vorname,
    nachname,
    hochschul_mail,
    ldap_name,
    rolle_id
)
VALUES
('Salma', 'Matlob', 'salma.matlob@hs-bochum.de', 'salma01', 1),
('Mohammad', 'Owji', 'mohammad.owji@hs-bochum.de', 'mohammad01', 1),
('Dalieh', 'Ghuzlan', 'dalieh.ghuzlan@hs-bochum.de', 'dalieh01', 1),
('Prof', 'Betreuer', 'betreuer@hs-bochum.de', 'prof01', 2);

-- Projekte
INSERT INTO projects (
    titel,
    beschreibung,
    status,
    semester,
    erstellt_von
)
VALUES
(
    'SOPhub',
    'Webportal zur Unterstützung von SOP-Projekten',
    'laufend',
    'SoSe 2026',
    1
),

(
    'Campus Connect',
    'Plattform für studentische Vernetzung',
    'abgeschlossen',
    'WiSe 2025',
    2
);


-- Projektideen
INSERT INTO ideas (
    titel,
    beschreibung,
    erstellt_von
)
VALUES
(
    'KI Lernplattform',
    'KI-gestützte Lernunterstützung für Informatik',
    1
),

(
    'Smart Campus App',
    'Mobile Anwendung für Campusdienste',
    2
);


-- Teams
INSERT INTO teams (
    projekt_id,
    user_id,
    teamrolle
)
VALUES
(1, 1, 'Projektleitung'),
(1, 2, 'Backend'),
(1, 3, 'Frontend');


-- Dokumente
INSERT INTO documents (
    projekt_id,
    dateiname,
    originalname,
    dateityp,
    dateigroesse,
    dateipfad,
    uploader_id,
    sichtbarkeit
)
VALUES
(
    1,
    'Lastenheft.pdf',
    'Lastenheft.pdf',
    'PDF',
    250000,
    '/uploads/projects/1/Lastenheft.pdf',
    1,
    'intern'
),
(
    1,
    'Pflichtenheft.pdf',
    'Pflichtenheft.pdf',
    'PDF',
    300000,
    '/uploads/projects/1/Pflichtenheft.pdf',
    2,
    'intern'
);


-- Tags
INSERT INTO tags (name)
VALUES
('React'),
('Spring Boot'),
('PostgreSQL'),
('KI'),
('SOP');
--TRUNCATE TABLE tags RESTART IDENTITY CASCADE;

-- Projekt-Tags
INSERT INTO project_tags (
    projekt_id,
    tag_id
)
VALUES
(1, 1),
(1, 2),
(1, 3),
(1, 4),
(1, 5);

