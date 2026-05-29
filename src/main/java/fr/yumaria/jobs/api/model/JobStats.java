package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (JobStats).

import java.util.Map;

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class JobStats {
    private final String jobId;
    private final double totalXp;
    private final long totalActions;
    private final long levelUps;
    private final long prestiges;
    private final double totalMoney;
    private final Map<String, Double> sourceXp;
    private final Map<String, Integer> sourceActions;
    private final Map<String, Double> sourceMoney;
    private final Map<String, Integer> actionTypeActions;

    public JobStats(
            String jobId,
            double totalXp,
            long totalActions,
            long levelUps,
            long prestiges,
            Map<String, Double> sourceXp,
            Map<String, Integer> sourceActions
    ) {
        this(jobId, totalXp, totalActions, levelUps, prestiges, 0.0D, sourceXp, sourceActions, Map.of(), Map.of());
    }

    public JobStats(
            String jobId,
            double totalXp,
            long totalActions,
            long levelUps,
            long prestiges,
            double totalMoney,
            Map<String, Double> sourceXp,
            Map<String, Integer> sourceActions,
            Map<String, Double> sourceMoney,
            Map<String, Integer> actionTypeActions
    ) {
        this.jobId = jobId;
        this.totalXp = totalXp;
        this.totalActions = totalActions;
        this.levelUps = levelUps;
        this.prestiges = prestiges;
        this.totalMoney = Math.max(0.0D, totalMoney);
        this.sourceXp = Map.copyOf(sourceXp);
        this.sourceActions = Map.copyOf(sourceActions);
        this.sourceMoney = Map.copyOf(sourceMoney);
        this.actionTypeActions = Map.copyOf(actionTypeActions);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String jobId() {
        return jobId;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double totalXp() {
        return totalXp;
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
}
