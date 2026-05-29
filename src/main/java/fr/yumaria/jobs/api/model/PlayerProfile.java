package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (PlayerProfile).

import java.util.Map;
import java.util.UUID;

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class PlayerProfile {
    private final UUID uuid;
    private final String name;
    private final Map<String, JobProgress> jobs;
    private final Map<String, Object> metadata;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerProfile(UUID uuid, String name, Map<String, JobProgress> jobs, Map<String, Object> metadata) {
        this.uuid = uuid;
        this.name = name == null ? "" : name;
        this.jobs = Map.copyOf(jobs);
        this.metadata = Map.copyOf(metadata);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public UUID uuid() {
        return uuid;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String name() {
        return name;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, JobProgress> jobs() {
        return jobs;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, Object> metadata() {
        return metadata;
    }
}
