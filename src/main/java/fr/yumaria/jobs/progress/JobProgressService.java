package fr.yumaria.jobs.progress;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.api.YumariaJobsProvider;
import fr.yumaria.jobs.api.event.YumariaJobLevelUpEvent;
import fr.yumaria.jobs.api.event.YumariaJobPrestigeEvent;
import fr.yumaria.jobs.api.event.YumariaJobProgressGainEvent;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.config.LanguageService;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobActionDefinition;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.reward.RewardService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class JobProgressService implements YumariaJobsProvider {
    public enum PrestigeResult {
        SUCCESS,
        DISABLED,
        UNKNOWN_JOB,
        NOT_JOINED,
        REQUIRE_MAX_LEVEL,
        MAX_PRESTIGE,
        CANCELLED
    }

    private final YumariaJobsPlugin plugin;
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final ProgressionService progressionService;
    private final RewardService rewardService;
    private final ProgressBarService progressBarService;
    private final LanguageService languageService;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public JobProgressService(
            YumariaJobsPlugin plugin,
            JobRegistry jobRegistry,
            PlayerDataService playerDataService,
            ProgressionService progressionService,
            RewardService rewardService,
            ProgressBarService progressBarService,
            LanguageService languageService
    ) {
        this.plugin = plugin;
        this.jobRegistry = jobRegistry;
        this.playerDataService = playerDataService;
        this.progressionService = progressionService;
        this.rewardService = rewardService;
        this.progressBarService = progressBarService;
        this.languageService = languageService;
    }

    @Override
    public void addProgress(Player player, String jobId, double amount, String source, Map<String, Object> context) {
        Map<String, Object> safeContext = sanitizeContext(context);
        if (!Bukkit.isPrimaryThread()) {
            double requestedAmount = amount;
            plugin.debugProgress("addProgress requested off main thread; rescheduling: player=" + (player == null ? "-" : player.getName())
                    + ", job=" + jobId
                    + ", amount=" + requestedAmount
                    + ", source=" + source);
            Bukkit.getScheduler().runTask(plugin, () -> addProgress(player, jobId, requestedAmount, source, safeContext));
            return;
        }
        if (player == null || amount <= 0.0D) {
            plugin.debugProgress("addProgress ignored: invalid player/amount player=" + (player == null ? "-" : player.getName())
                    + ", job=" + jobId
                    + ", amount=" + amount
                    + ", source=" + source);
            return;
        }
        String normalizedJobId = Text.normalizeId(jobId);
        Optional<JobDefinition> optionalJob = jobRegistry.get(normalizedJobId);
        if (optionalJob.isEmpty() || !optionalJob.get().enabled()) {
            plugin.debugProgress("addProgress ignored: unknown or disabled job input=" + jobId + ", normalized=" + normalizedJobId + ", player=" + player.getName());
            return;
        }

        JobDefinition job = optionalJob.get();
        plugin.debugProgress("addProgress start: player=" + player.getName()
                + ", job=" + job.id()
                + ", requestedAmount=" + amount
                + ", source=" + Text.normalizeId(source)
                + ", context=" + safeContext);
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(job.id());
        if (jobData == null || !jobData.isJoined()) {
            if (!plugin.getConfig().getBoolean("progress.auto-join-on-progress", false)) {
                plugin.debugProgress("addProgress ignored: player has not joined job. player=" + player.getName()
                        + ", job=" + job.id()
                        + ", autoJoin=false");
                return;
            }
            jobData = data.job(job.id());
            jobData.setJoined(true);
            plugin.debugProgress("addProgress auto-joined player=" + player.getName() + ", job=" + job.id());
        }

        if (!jobData.isActive() && !job.allowProgressWhenInactive()) {
            plugin.debugProgress("addProgress ignored: job inactive. player=" + player.getName()
                    + ", job=" + job.id()
                    + ", allowProgressWhenInactive=false");
            return;
        }

        int oldLevel = jobData.getLevel();
        double oldProgress = jobData.getProgress();
        double oldRequired = progressionService.requiredProgress(job, jobData);
        String normalizedSource = Text.normalizeId(source);
        JobActionDefinition action = job.actions().get(normalizedSource);
        if (action != null) {
            if (!action.enabled()) {
                plugin.debugProgress("addProgress ignored: action disabled. player=" + player.getName()
                        + ", job=" + job.id()
                        + ", source=" + normalizedSource);
                return;
            }
            if (isOnCooldown(player, job, normalizedSource, action.cooldownMs())) {
                plugin.debugProgress("addProgress ignored: action cooldown. player=" + player.getName()
                        + ", job=" + job.id()
                        + ", source=" + normalizedSource
                        + ", cooldownMs=" + action.cooldownMs());
                return;
            }
            amount *= action.progress();
            plugin.debugProgress("addProgress action matched: job=" + job.id()
                    + ", source=" + normalizedSource
                    + ", multiplier/progress=" + action.progress()
                    + ", calculatedAmount=" + amount);
        } else {
            plugin.debugProgress("addProgress source has no configured action; using supplied amount. job=" + job.id()
                    + ", source=" + normalizedSource
                    + ", amount=" + amount);
        }

        YumariaJobProgressGainEvent event = new YumariaJobProgressGainEvent(player, job.id(), amount, normalizedSource, safeContext);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getAmount() <= 0.0D) {
            plugin.debugProgress("addProgress ignored: YumariaJobProgressGainEvent cancelled or non-positive. player=" + player.getName()
                    + ", job=" + job.id()
                    + ", cancelled=" + event.isCancelled()
                    + ", amount=" + event.getAmount());
            return;
        }

        jobData.addProgress(event.getAmount());
        handleLevelUps(player, job, jobData);
        double required = progressionService.requiredProgress(job, jobData);
        if (jobData.getLevel() >= job.maxLevel() && jobData.getProgress() > required) {
            jobData.setProgress(required);
        }
        plugin.debugProgress("addProgress result: player=" + player.getName()
                + ", job=" + job.id()
                + ", source=" + normalizedSource
                + ", oldProgress=" + Text.formatNumber(oldProgress)
                + ", addedProgress=" + Text.formatNumber(event.getAmount())
                + ", newProgress=" + Text.formatNumber(jobData.getProgress())
                + ", oldLevel=" + oldLevel
                + ", newLevel=" + jobData.getLevel()
                + ", oldRequired=" + Text.formatNumber(oldRequired)
                + ", required=" + Text.formatNumber(required)
                + ", active=" + jobData.isActive()
                + ", joined=" + jobData.isJoined());
        progressBarService.showProgress(player, job, jobData);
        playerDataService.markDirty(data);
    }

    public PrestigeResult prestige(Player player, String jobId) {
        Optional<JobDefinition> optionalJob = jobRegistry.get(jobId);
        if (optionalJob.isEmpty()) {
            return PrestigeResult.UNKNOWN_JOB;
        }
        if (!plugin.getConfig().getBoolean("prestige.enabled", true)) {
            return PrestigeResult.DISABLED;
        }
        JobDefinition job = optionalJob.get();
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(job.id());
        if (jobData == null || !jobData.isJoined()) {
            return PrestigeResult.NOT_JOINED;
        }
        if (plugin.getConfig().getBoolean("prestige.require-max-level", true) && jobData.getLevel() < job.maxLevel()) {
            return PrestigeResult.REQUIRE_MAX_LEVEL;
        }
        int maxPrestige = plugin.getConfig().getInt("prestige.max-prestige", 10);
        if (jobData.getPrestige() >= maxPrestige) {
            return PrestigeResult.MAX_PRESTIGE;
        }

        int oldPrestige = jobData.getPrestige();
        int newPrestige = oldPrestige + 1;
        YumariaJobPrestigeEvent event = new YumariaJobPrestigeEvent(player, job.id(), oldPrestige, newPrestige);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return PrestigeResult.CANCELLED;
        }

        jobData.setPrestige(newPrestige);
        jobData.setLevel(plugin.getConfig().getInt("prestige.reset-level-to", 1));
        if (plugin.getConfig().getBoolean("prestige.reset-progress", true)) {
            jobData.setProgress(0.0D);
        }
        rewardService.applyPrestigeRewards(player, job, jobData);
        playerDataService.markDirty(data);
        languageService.send(player, "prestige.success", Map.of(
                "%job_name%", job.displayName(),
                "%prestige%", Integer.toString(newPrestige)
        ));
        return PrestigeResult.SUCCESS;
    }

    @Override
    public int getLevel(Player player, String jobId) {
        return withJobData(player, jobId, data -> data.getLevel(), 0);
    }

    @Override
    public double getProgress(Player player, String jobId) {
        return withJobData(player, jobId, PlayerJobData::getProgress, 0.0D);
    }

    @Override
    public double getRequiredProgress(Player player, String jobId) {
        Optional<JobDefinition> optionalJob = jobRegistry.get(jobId);
        if (optionalJob.isEmpty()) {
            plugin.debug("Required progress requested for unknown job: " + jobId);
            return 0.0D;
        }
        PlayerJobData data = playerDataService.getOrLoad(player).peekJob(optionalJob.get().id());
        if (data == null) {
            return 0.0D;
        }
        return progressionService.requiredProgress(optionalJob.get(), data);
    }

    @Override
    public int getPrestige(Player player, String jobId) {
        return withJobData(player, jobId, PlayerJobData::getPrestige, 0);
    }

    @Override
    public boolean hasJob(Player player, String jobId) {
        return withJobData(player, jobId, PlayerJobData::isJoined, false);
    }

    @Override
    public boolean isJobActive(Player player, String jobId) {
        return withJobData(player, jobId, PlayerJobData::isActive, false);
    }

    private void handleLevelUps(Player player, JobDefinition job, PlayerJobData jobData) {
        int guard = 0;
        while (jobData.getLevel() < job.maxLevel() && guard++ < 1000) {
            double required = progressionService.requiredProgress(job, jobData);
            if (jobData.getProgress() < required) {
                return;
            }
            int oldLevel = jobData.getLevel();
            jobData.setProgress(jobData.getProgress() - required);
            jobData.setLevel(oldLevel + 1);
            plugin.debugProgress("level-up: player=" + player.getName()
                    + ", job=" + job.id()
                    + ", oldLevel=" + oldLevel
                    + ", newLevel=" + jobData.getLevel()
                    + ", remainingProgress=" + Text.formatNumber(jobData.getProgress()));
            Bukkit.getPluginManager().callEvent(new YumariaJobLevelUpEvent(player, job.id(), oldLevel, jobData.getLevel(), jobData.getPrestige()));
            rewardService.applyLevelRewards(player, job, jobData);
            languageService.send(player, "jobs.level-up", Map.of(
                    "%job_name%", job.displayName(),
                    "%level%", Integer.toString(jobData.getLevel())
            ));
        }
    }

    private boolean isOnCooldown(Player player, JobDefinition job, String source, long cooldownMs) {
        if (cooldownMs <= 0L) {
            return false;
        }
        String key = player.getUniqueId() + ":" + job.id() + ":" + source;
        long now = System.currentTimeMillis();
        Long previous = cooldowns.get(key);
        if (previous != null && previous + cooldownMs > now) {
            return true;
        }
        cooldowns.put(key, now);
        return false;
    }

    private Map<String, Object> sanitizeContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(sanitized);
    }

    private <T> T withJobData(Player player, String jobId, java.util.function.Function<PlayerJobData, T> function, T fallback) {
        Optional<JobDefinition> optionalJob = jobRegistry.get(jobId);
        if (optionalJob.isEmpty()) {
            plugin.debug("API requested unknown job: " + jobId);
            return fallback;
        }
        PlayerJobData data = playerDataService.getOrLoad(player).peekJob(optionalJob.get().id());
        if (data == null) {
            return fallback;
        }
        return function.apply(data);
    }
}
