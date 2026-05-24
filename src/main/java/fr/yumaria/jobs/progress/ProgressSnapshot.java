package fr.yumaria.jobs.progress;

import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;

public record ProgressSnapshot(
        String jobId,
        String jobName,
        int level,
        int prestige,
        double currentProgress,
        double requiredProgress,
        double totalProgress,
        double progressRatio,
        double percent,
        String rank
) {
    public static ProgressSnapshot of(JobDefinition job, PlayerJobData data, double requiredProgress, String rank) {
        double safeRequired = Math.max(1.0D, requiredProgress);
        double ratio = clamp(data.getProgress() / safeRequired);
        return new ProgressSnapshot(
                job.id(),
                job.displayName(),
                data.getLevel(),
                data.getPrestige(),
                data.getProgress(),
                safeRequired,
                data.getTotalProgress(),
                ratio,
                ratio * 100.0D,
                rank
        );
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
