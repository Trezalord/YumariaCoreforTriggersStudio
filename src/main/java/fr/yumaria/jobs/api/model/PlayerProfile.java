package fr.yumaria.jobs.api.model;

import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uuid;
    private final String name;
    private final Map<String, JobProgress> jobs;
    private final Map<String, Object> metadata;

    public PlayerProfile(UUID uuid, String name, Map<String, JobProgress> jobs, Map<String, Object> metadata) {
        this.uuid = uuid;
        this.name = name == null ? "" : name;
        this.jobs = Map.copyOf(jobs);
        this.metadata = Map.copyOf(metadata);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Map<String, JobProgress> jobs() {
        return jobs;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}
