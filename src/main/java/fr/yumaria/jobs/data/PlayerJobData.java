package fr.yumaria.jobs.data;

public final class PlayerJobData {
    private boolean joined;
    private boolean active;
    private int level;
    private double progress;
    private double totalProgress;
    private int prestige;
    private double points;

    public PlayerJobData() {
        this(false, false, 1, 0.0D, 0.0D, 0, 0.0D);
    }

    public PlayerJobData(boolean joined, boolean active, int level, double progress, double totalProgress, int prestige, double points) {
        this.joined = joined;
        this.active = active;
        this.level = Math.max(1, level);
        this.progress = Math.max(0.0D, progress);
        this.totalProgress = Math.max(0.0D, totalProgress);
        this.prestige = Math.max(0, prestige);
        this.points = Math.max(0.0D, points);
    }

    public PlayerJobData copy() {
        return new PlayerJobData(joined, active, level, progress, totalProgress, prestige, points);
    }

    public boolean isJoined() {
        return joined;
    }

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

    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int prestige) {
        this.prestige = Math.max(0, prestige);
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = Math.max(0.0D, points);
    }

    public void addProgress(double amount) {
        progress += amount;
        totalProgress += amount;
    }

    public void addPoints(double amount) {
        points += Math.max(0.0D, amount);
    }
}
