package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (JobProgress).

import java.util.Map;

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class JobProgress {
    private final String jobId;
    private final boolean joined;
    private final boolean active;
    private final int level;
    private final double progress;
    private final double totalProgress;
    private final int prestige;
    private final double points;
    private final long totalActions;
    private final long levelUps;
    private final long prestiges;
    private final double totalMoney;
    private final Map<String, Double> sourceXp;
    private final Map<String, Integer> sourceActions;
    private final Map<String, Double> sourceMoney;
    private final Map<String, Integer> actionTypeActions;
    private final Map<String, Long> lastActionTimestamps;

    public JobProgress(
            String jobId,
            boolean joined,
            boolean active,
            int level,
            double progress,
            double totalProgress,
            int prestige,
            double points,
            long totalActions,
            long levelUps,
            long prestiges,
            Map<String, Double> sourceXp,
            Map<String, Integer> sourceActions,
            Map<String, Long> lastActionTimestamps
    ) {
        this(jobId, joined, active, level, progress, totalProgress, prestige, points, totalActions, levelUps, prestiges, 0.0D, sourceXp, sourceActions, Map.of(), Map.of(), lastActionTimestamps);
    }

    public JobProgress(
            String jobId,
            boolean joined,
            boolean active,
            int level,
            double progress,
            double totalProgress,
            int prestige,
            double points,
            long totalActions,
            long levelUps,
            long prestiges,
            double totalMoney,
            Map<String, Double> sourceXp,
            Map<String, Integer> sourceActions,
            Map<String, Double> sourceMoney,
            Map<String, Integer> actionTypeActions,
            Map<String, Long> lastActionTimestamps
    ) {
        this.jobId = jobId;
        this.joined = joined;
        this.active = active;
        this.level = level;
        this.progress = progress;
        this.totalProgress = totalProgress;
        this.prestige = prestige;
        this.points = points;
        this.totalActions = totalActions;
        this.levelUps = levelUps;
        this.prestiges = prestiges;
        this.totalMoney = Math.max(0.0D, totalMoney);
        this.sourceXp = Map.copyOf(sourceXp);
        this.sourceActions = Map.copyOf(sourceActions);
        this.sourceMoney = Map.copyOf(sourceMoney);
        this.actionTypeActions = Map.copyOf(actionTypeActions);
        this.lastActionTimestamps = Map.copyOf(lastActionTimestamps);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String jobId() {
        return jobId;
    }

    // Annotation YumariaJobs: Action joueur liee aux metiers ou aux menus.
    public boolean joined() {
        return joined;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean active() {
        return active;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public int level() {
        return level;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double progress() {
        return progress;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double totalProgress() {
        return totalProgress;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public int prestige() {
        return prestige;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double points() {
        return points;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public long totalActions() {
        return totalActions;
    }

    // Annotation YumariaJobs: Controle les montees de niveau et les recompenses associees.
    public long levelUps() {
        return levelUps;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public long prestiges() {
        return prestiges;
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public double totalMoney() {
        return totalMoney;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, Double> sourceXp() {
        return sourceXp;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, Integer> sourceActions() {
        return sourceActions;
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public Map<String, Double> sourceMoney() {
        return sourceMoney;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, Integer> actionTypeActions() {
        return actionTypeActions;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, Long> lastActionTimestamps() {
        return lastActionTimestamps;
    }
}
