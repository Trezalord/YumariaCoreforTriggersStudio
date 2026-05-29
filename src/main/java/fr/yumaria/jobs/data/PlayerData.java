package fr.yumaria.jobs.data;

// Repere fichier YumariaJobs: donnees joueur, cache, stats et sauvegarde (PlayerData).

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Role YumariaJobs: Stocke les profils joueur, les stats et la sauvegarde disque.
public final class PlayerData {
    private final UUID uuid;
    private String name;
    private final Map<String, PlayerJobData> jobs;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerData(UUID uuid, String name) {
        this(uuid, name, new HashMap<>());
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerData(UUID uuid, String name, Map<String, PlayerJobData> jobs) {
        this.uuid = uuid;
        this.name = name;
        this.jobs = new HashMap<>(jobs);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public UUID uuid() {
        return uuid;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String name() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, PlayerJobData> jobs() {
        return jobs;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerJobData job(String jobId) {
        return jobs.computeIfAbsent(jobId, ignored -> new PlayerJobData());
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerJobData peekJob(String jobId) {
        return jobs.get(jobId);
    }

    // Annotation YumariaJobs: Produit une copie sure pour eviter d exposer les donnees internes mutables.
    public PlayerData copy() {
        Map<String, PlayerJobData> copiedJobs = new HashMap<>();
        for (Map.Entry<String, PlayerJobData> entry : jobs.entrySet()) {
            copiedJobs.put(entry.getKey(), entry.getValue().copy());
        }
        return new PlayerData(uuid, name, copiedJobs);
    }
}
