package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (YumariaJobsProvider).

import org.bukkit.entity.Player;

import java.util.Map;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface YumariaJobsProvider {
    YumariaActionService actions();

    JobXpService xp();

    PlayerProfileService profiles();

    YumariaEconomyService economy();

    PrestigeService prestiges();

    JobStatsService stats();

    RewardService rewards();

    YumariaAddonRegistry addons();

    void addProgress(Player player, String jobId, double amount, String source, Map<String, Object> context);

    int getLevel(Player player, String jobId);

    double getProgress(Player player, String jobId);

    double getRequiredProgress(Player player, String jobId);

    int getPrestige(Player player, String jobId);

    boolean hasJob(Player player, String jobId);

    boolean isJobActive(Player player, String jobId);
}
