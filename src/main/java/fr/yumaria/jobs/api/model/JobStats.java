package fr.yumaria.jobs.api.model;

import java.util.Map;

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

    public String jobId() {
        return jobId;
    }

    public double totalXp() {
        return totalXp;
    }

    public long totalActions() {
        return totalActions;
    }

    public long levelUps() {
        return levelUps;
    }

    public long prestiges() {
        return prestiges;
    }

    public double totalMoney() {
        return totalMoney;
    }

    public Map<String, Double> sourceXp() {
        return sourceXp;
    }

    public Map<String, Integer> sourceActions() {
        return sourceActions;
    }

    public Map<String, Double> sourceMoney() {
        return sourceMoney;
    }

    public Map<String, Integer> actionTypeActions() {
        return actionTypeActions;
    }
}
