Create Table roles(
	id serial Primary key,
	rollenname Varchar(50) not null unique,
	beschreibung text
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    vorname VARCHAR(100) NOT NULL,
    nachname VARCHAR(100) NOT NULL,
    hochschul_mail VARCHAR(255) NOT NULL UNIQUE,
    ldap_name VARCHAR(100) NOT NULL UNIQUE,
    matrikelnummer VARCHAR(50),
    fachbereich VARCHAR(100),
    rolle_id INTEGER REFERENCES roles(id),
    profilbild TEXT,
    konto_status VARCHAR(50) DEFAULT 'aktiv',
    letzter_login TIMESTAMP,
    erstellt_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id SERIAL PRIMARY KEY,
    titel VARCHAR(255) NOT NULL,
    kurzbeschreibung TEXT,
    beschreibung TEXT,
    status VARCHAR(50) DEFAULT 'offen',
    semester VARCHAR(50),
    startdatum DATE,
    enddatum DATE,
    betreuer_id INTEGER REFERENCES users(id),
    erstellt_von INTEGER REFERENCES users(id),
    github_link TEXT,
    demo_link TEXT,
    erstellt_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ideas (
    id SERIAL PRIMARY KEY,
    titel VARCHAR(255) NOT NULL,
    beschreibung TEXT NOT NULL,
    kategorie VARCHAR(100),
    prioritaet VARCHAR(50),
    status VARCHAR(50) DEFAULT 'offen',
    erstellt_von INTEGER REFERENCES users(id),
    erstellt_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teams (
    id SERIAL PRIMARY KEY,
    projekt_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    teamrolle VARCHAR(100),
    beigetreten_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'aktiv'
);

CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    projekt_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
    dateiname VARCHAR(255) NOT NULL,
    originalname VARCHAR(255),
    dateityp VARCHAR(50),
    dateigroesse BIGINT,
    dateipfad TEXT NOT NULL,
    version INTEGER DEFAULT 1,
    uploader_id INTEGER REFERENCES users(id),
    sichtbarkeit VARCHAR(50) DEFAULT 'intern',
    hochgeladen_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    beschreibung TEXT,
    erstellt_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_tags (
    id SERIAL PRIMARY KEY,
    projekt_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
    tag_id INTEGER REFERENCES tags(id) ON DELETE CASCADE,
    erstellt_am TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);