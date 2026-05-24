package fr.yumaria.jobs.api;

import org.bukkit.entity.Player;

import java.util.Map;

public interface YumariaJobsProvider {
    void addProgress(Player player, String jobId, double amount, String source, Map<String, Object> context);

    int getLevel(Player player, String jobId);

    double getProgress(Player player, String jobId);

    double getRequiredProgress(Player player, String jobId);

    int getPrestige(Player player, String jobId);

    boolean hasJob(Player player, String jobId);

    boolean isJobActive(Player player, String jobId);
}
