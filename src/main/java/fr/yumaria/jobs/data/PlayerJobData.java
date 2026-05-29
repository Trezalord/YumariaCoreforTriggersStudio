package fr.yumaria.jobs.data;

// Repere fichier YumariaJobs: donnees joueur, cache, stats et sauvegarde (PlayerJobData).

import java.util.HashMap;
import java.util.Map;

// Role YumariaJobs: Stocke les profils joueur, les stats et la sauvegarde disque.
public final class PlayerJobData {
    private boolean joined;
    private boolean active;
    private int level;
    private double progress;
    private double totalProgress;
    private int prestige;
    private double points;
    private long totalActions;
    private long levelUps;
    private long prestiges;
    private double totalMoney;
    private final Map<String, Double> sourceProgress;
    private final Map<String, Integer> sourceActions;
    private final Map<String, Double> sourceMoney;
    private final Map<String, Integer> actionTypeActions;
    private final Map<String, Long> lastActionTimestamps;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerJobData() {
        this(false, false, 1, 0.0D, 0.0D, 0, 0.0D);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerJobData(boolean joined, boolean active, int level, double progress, double totalProgress, int prestige, double points) {
        this(joined, active, level, progress, totalProgress, prestige, points, 0L, 0L, 0L, Map.of(), Map.of(), Map.of());
    }

    public PlayerJobData(
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
            Map<String, Double> sourceProgress,
            Map<String, Integer> sourceActions,
            Map<String, Long> lastActionTimestamps
    ) {
        this(joined, active, level, progress, totalProgress, prestige, points, totalActions, levelUps, prestiges, 0.0D, sourceProgress, sourceActions, Map.of(), Map.of(), lastActionTimestamps);
    }

    public PlayerJobData(
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
            Map<String, Double> sourceProgress,
            Map<String, Integer> sourceActions,
            Map<String, Double> sourceMoney,
            Map<String, Integer> actionTypeActions,
            Map<String, Long> lastActionTimestamps
    ) {
        this.joined = joined;
        this.active = active;
        this.level = Math.max(1, level);
        this.progress = Math.max(0.0D, progress);
        this.totalProgress = Math.max(0.0D, totalProgress);
        this.prestige = Math.max(0, prestige);
        this.points = Math.max(0.0D, points);
        this.totalActions = Math.max(0L, totalActions);
        this.levelUps = Math.max(0L, levelUps);
        this.prestiges = Math.max(0L, prestiges);
        this.totalMoney = Math.max(0.0D, totalMoney);
        this.sourceProgress = new HashMap<>(sourceProgress);
        this.sourceActions = new HashMap<>(sourceActions);
        this.sourceMoney = new HashMap<>(sourceMoney);
        this.actionTypeActions = new HashMap<>(actionTypeActions);
        this.lastActionTimestamps = new HashMap<>(lastActionTimestamps);
    }

    // Annotation YumariaJobs: Produit une copie sure pour eviter d exposer les donnees internes mutables.
    public PlayerJobData copy() {
        return new PlayerJobData(joined, active, level, progress, totalProgress, prestige, points, totalActions, levelUps, prestiges, totalMoney, sourceProgress, sourceActions, sourceMoney, actionTypeActions, lastActionTimestamps);
    }

    // Annotation YumariaJobs: Action joueur liee aux metiers ou aux menus.
    public boolean isJoined() {
        return joined;
    }

    // Annotation YumariaJobs: Action joueur liee aux metiers ou aux menus.
    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.max(0.0D, progress);
    }

    public double getTotalProgress() {
        return totalProgress;
    }

    public void setTotalProgress(double totalProgress) {
        this.totalProgress = Math.max(0.0D, totalProgress);
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public int getPrestige() {
        return prestige;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public void setPrestige(int prestige) {
        this.prestige = Math.max(0, prestige);
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = Math.max(0.0D, points);
    }

    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public void addProgress(double amount) {
        progress += amount;
        totalProgress += amount;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void addPoints(double amount) {
        points += Math.max(0.0D, amount);
    }

    public long getTotalActions() {
        return totalActions;
    }

    // Annotation YumariaJobs: Controle les montees de niveau et les recompenses associees.
    public long getLevelUps() {
        return levelUps;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public long getPrestiges() {
        return prestiges;
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public double getTotalMoney() {
        return totalMoney;
    }

    public Map<String, Double> getSourceProgress() {
        return Map.copyOf(sourceProgress);
    }

    public Map<String, Integer> getSourceActions() {
        return Map.copyOf(sourceActions);
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public Map<String, Double> getSourceMoney() {
        return Map.copyOf(sourceMoney);
    }

    public Map<String, Integer> getActionTypeActions() {
        return Map.copyOf(actionTypeActions);
    }

    public Map<String, Long> getLastActionTimestamps() {
        return Map.copyOf(lastActionTimestamps);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void recordAction(String source, double amount, long timestamp) {
        String safeSource = source == null || source.isBlank() ? "unknown" : source;
        totalActions++;
        sourceProgress.merge(safeSource, Math.max(0.0D, amount), Double::sum);
        sourceActions.merge(safeSource, 1, Integer::sum);
        lastActionTimestamps.put(safeSource, timestamp);
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public void recordMoney(String source, double amount) {
        String safeSource = source == null || source.isBlank() ? "unknown" : source;
        double safeAmount = Math.max(0.0D, amount);
        totalMoney += safeAmount;
        sourceMoney.merge(safeSource, safeAmount, Double::sum);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void recordActionType(String actionType) {
        String safeActionType = actionType == null || actionType.isBlank() ? "unknown" : actionType;
        actionTypeActions.merge(safeActionType, 1, Integer::sum);
    }

    // Annotation YumariaJobs: Controle les montees de niveau et les recompenses associees.
    public void incrementLevelUps() {
        levelUps++;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public void incrementPrestiges() {
        prestiges++;
    }
}
