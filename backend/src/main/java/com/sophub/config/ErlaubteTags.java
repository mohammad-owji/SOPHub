package com.sophub.config;

import java.util.List;

/**
 * Feste, erlaubte Tag-Liste für das Auto-Tagging.
 * Wird sowohl im KI-Prompt als auch bei der Validierung der KI-Antwort genutzt.
 */
public class ErlaubteTags {

    private ErlaubteTags() {}

    public static final List<String> ALLE = List.of(
            "Frontend", "Backend",
            "Vue", "React", "Angular",
            "TypeScript", "JavaScript", "HTML", "CSS",
            "Java", "Spring", "Spring Boot", "Python",
            "PostgreSQL", "MySQL", "MongoDB",
            "Docker", "Sicherheit", "REST", "API", "KI",
            "Testing", "DevOps", "Datenbank"
    );
}
