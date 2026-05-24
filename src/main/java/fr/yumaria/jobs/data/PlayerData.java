package fr.yumaria.jobs.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    private final UUID uuid;
    private String name;
    private final Map<String, PlayerJobData> jobs;

    public PlayerData(UUID uuid, String name) {
        this(uuid, name, new HashMap<>());
    }

    public PlayerData(UUID uuid, String name, Map<String, PlayerJobData> jobs) {
        this.uuid = uuid;
        this.name = name;
        this.jobs = new HashMap<>(jobs);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public Map<String, PlayerJobData> jobs() {
        return jobs;
    }

    public PlayerJobData job(String jobId) {
        return jobs.computeIfAbsent(jobId, ignored -> new PlayerJobData());
    }

    public PlayerJobData peekJob(String jobId) {
        return jobs.get(jobId);
    }

    public PlayerData copy() {
        Map<String, PlayerJobData> copiedJobs = new HashMap<>();
        for (Map.Entry<String, PlayerJobData> entry : jobs.entrySet()) {
            copiedJobs.put(entry.getKey(), entry.getValue().copy());
        }
        return new PlayerData(uuid, name, copiedJobs);
    }
}
