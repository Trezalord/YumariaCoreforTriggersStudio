package fr.yumaria.jobs.progress;

// Repere fichier YumariaJobs: progression, niveaux et feedback visuel (JobProgressService).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.anticheat.AntiAbuseResult;
import fr.yumaria.jobs.anticheat.ProgressionAntiAbuseService;
import fr.yumaria.jobs.api.JobStatsService;
import fr.yumaria.jobs.api.JobXpService;
import fr.yumaria.jobs.api.PlayerProfileService;
import fr.yumaria.jobs.api.PrestigeService;
import fr.yumaria.jobs.api.YumariaActionService;
import fr.yumaria.jobs.api.YumariaAddonRegistry;
import fr.yumaria.jobs.api.YumariaEconomyService;
import fr.yumaria.jobs.api.YumariaJobsProvider;
import fr.yumaria.jobs.api.event.YumariaJobLevelUpEvent;
import fr.yumaria.jobs.api.event.YumariaJobPrestigeEvent;
import fr.yumaria.jobs.api.event.YumariaJobProgressGainEvent;
import fr.yumaria.jobs.api.event.YumariaJobXpGainEvent;
import fr.yumaria.jobs.api.model.JobXpRequest;
import fr.yumaria.jobs.api.model.ProgressionFailureReason;
import fr.yumaria.jobs.api.model.ProgressionResult;
import fr.yumaria.jobs.api.model.RewardResult;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.config.LanguageService;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobActionDefinition;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.JobSourceDefinition;
import fr.yumaria.jobs.progression.EventXpModifier;
import fr.yumaria.jobs.progression.GlobalXpModifier;
import fr.yumaria.jobs.progression.JobSpecificXpModifier;
import fr.yumaria.jobs.progression.PermissionXpModifier;
import fr.yumaria.jobs.progression.PrestigeXpModifier;
import fr.yumaria.jobs.progression.SourceXpModifier;
import fr.yumaria.jobs.progression.XpModifierContext;
import fr.yumaria.jobs.progression.XpModifierPipeline;
import fr.yumaria.jobs.reward.RewardService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Role YumariaJobs: Gere XP, niveaux, prestige et feedback visuel de progression.
public final class JobProgressService implements YumariaJobsProvider, JobXpService, PrestigeService {
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
    private final XpModifierPipeline modifierPipeline;
    private final ProgressionAntiAbuseService antiAbuseService;
    private YumariaActionService actionService;
    private YumariaEconomyService economyApiService;
    private YumariaAddonRegistry addonRegistry;

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
        this.modifierPipeline = new XpModifierPipeline(List.of(
                new GlobalXpModifier(plugin),
                new JobSpecificXpModifier(),
                new SourceXpModifier(),
                new PrestigeXpModifier(plugin),
                new PermissionXpModifier(plugin),
                new EventXpModifier()
        ));
        this.antiAbuseService = new ProgressionAntiAbuseService(plugin);
    }

    public void setCoreServices(YumariaActionService actionService, YumariaEconomyService economyApiService, YumariaAddonRegistry addonRegistry) {
        this.actionService = actionService;
        this.economyApiService = economyApiService;
        this.addonRegistry = addonRegistry;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaActionService actions() {
        return actionService;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public JobXpService xp() {
        return this;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerProfileService profiles() {
        return playerDataService;
    }

    @Override
    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public YumariaEconomyService economy() {
        return economyApiService;
    }

    @Override
    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public PrestigeService prestiges() {
        return this;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public JobStatsService stats() {
        return playerDataService;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public fr.yumaria.jobs.api.RewardService rewards() {
        return rewardService;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaAddonRegistry addons() {
        return addonRegistry;
    }

    @Override
    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
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
        giveXp(JobXpRequest.builder()
                .player(player)
                .jobId(jobId)
                .baseAmount(amount)
                .source(source)
                .context(safeContext)
                .build());
    }

    @Override
    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public ProgressionResult giveXp(Player player, String jobId, double amount, String source) {
        return giveXp(JobXpRequest.builder()
                .player(player)
                .jobId(jobId)
                .baseAmount(amount)
                .source(source)
                .build());
    }

    @Override
    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public ProgressionResult giveXp(UUID playerId, String jobId, double amount, String source) {
        Player player = playerId == null ? null : Bukkit.getPlayer(playerId);
        return giveXp(JobXpRequest.builder()
                .player(player)
                .playerId(playerId)
                .jobId(jobId)
                .baseAmount(amount)
                .source(source)
                .build());
    }

    @Override
    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public ProgressionResult giveXp(JobXpRequest request) {
        // Point d'entree public XP: toujours rester sur le thread principal Bukkit.
        if (request == null) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, "", 0.0D, "request is null");
        }
        if (!Bukkit.isPrimaryThread()) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, request.jobId(), request.baseAmount(), "giveXp must be called on the main server thread");
        }
        return applyXp(request);
    }

    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    private ProgressionResult applyXp(JobXpRequest request) {
        // Pipeline XP metier: validation, multiplicateurs, events, anti-abuse, ajout et bossbar.
        List<String> debugMessages = new ArrayList<>();
        Map<String, Object> safeContext = sanitizeContext(request.context());
        Player player = request.playerId() == null ? null : Bukkit.getPlayer(request.playerId());
        if (player == null) {
            return failure(ProgressionFailureReason.PLAYER_NOT_FOUND, request, debugMessages, "player is not online");
        }
        if (request.baseAmount() <= 0.0D || Double.isNaN(request.baseAmount()) || Double.isInfinite(request.baseAmount())) {
            return failure(ProgressionFailureReason.INVALID_AMOUNT, request, debugMessages, "invalid XP amount");
        }
        if (!plugin.getConfig().getBoolean("xp.enabled", true)) {
            return failure(ProgressionFailureReason.XP_DISABLED, request, debugMessages, "xp.enabled is false");
        }

        Optional<JobDefinition> optionalJob = jobRegistry.get(request.jobId());
        if (optionalJob.isEmpty() || !optionalJob.get().enabled()) {
            return failure(ProgressionFailureReason.JOB_NOT_FOUND, request, debugMessages, "unknown or disabled job");
        }

        JobDefinition job = optionalJob.get();
        String normalizedSource = Text.normalizeId(request.source());
        plugin.debugProgress("giveXp start: player=" + player.getName()
                + ", job=" + job.id()
                + ", baseXp=" + request.baseAmount()
                + ", source=" + normalizedSource
                + ", context=" + safeContext);

        PlayerData data;
        try {
            data = playerDataService.getOrLoad(player);
        } catch (RuntimeException exception) {
            return failure(ProgressionFailureReason.PROFILE_NOT_LOADED, request, debugMessages, exception.getMessage());
        }

        PlayerJobData jobData = data.peekJob(job.id());
        if (jobData == null || !jobData.isJoined()) {
            // Par defaut, un joueur doit avoir rejoint le metier avant de gagner de l'XP.
            if (!plugin.getConfig().getBoolean("progress.auto-join-on-progress", false)) {
                return failure(ProgressionFailureReason.JOB_NOT_ACTIVE, request, debugMessages, "player has not joined job " + job.id());
            }
            jobData = data.job(job.id());
            jobData.setJoined(true);
            debugMessages.add("auto-joined job " + job.id());
        }

        if (!jobData.isActive() && !job.allowProgressWhenInactive()) {
            return failure(ProgressionFailureReason.JOB_NOT_ACTIVE, request, debugMessages, "job is inactive");
        }

        JobSourceDefinition sourceDefinition = job.sources().get(normalizedSource);
        if (sourceDefinition != null && !sourceDefinition.enabled()) {
            return failure(ProgressionFailureReason.SOURCE_BLOCKED, request, debugMessages, "source disabled by job config");
        }

        int oldLevel = jobData.getLevel();
        int oldPrestige = jobData.getPrestige();
        double oldProgress = jobData.getProgress();
        double oldRequired = progressionService.requiredProgress(job, jobData);
        double baseXp = request.baseAmount();
        JobActionDefinition action = job.actions().get(normalizedSource);
        if (action != null) {
            if (!action.enabled()) {
                return failure(ProgressionFailureReason.SOURCE_BLOCKED, request, debugMessages, "job action disabled");
            }
            if (isOnCooldown(player, job, normalizedSource, action.cooldownMs())) {
                return failure(ProgressionFailureReason.SOURCE_BLOCKED, request, debugMessages, "job action cooldown");
            }
            baseXp *= action.progress();
            debugMessages.add("action " + normalizedSource + " multiplier=" + action.progress());
        }

        // Multiplicateurs de progression: action configuree, source, prestige, permissions, events.
        XpModifierPipeline.XpCalculation calculation = modifierPipeline.apply(new XpModifierContext(
                player,
                job,
                jobData,
                normalizedSource,
                baseXp,
                safeContext
        ));
        debugMessages.addAll(calculation.debugMessages());
        double finalXp = calculation.finalXp();

        // Evenement moderne cancellable avant l'application de l'XP.
        YumariaJobXpGainEvent xpEvent = new YumariaJobXpGainEvent(player, job.id(), request.baseAmount(), finalXp, normalizedSource, safeContext);
        Bukkit.getPluginManager().callEvent(xpEvent);
        if (xpEvent.isCancelled() || xpEvent.getFinalXp() <= 0.0D) {
            return failure(ProgressionFailureReason.EVENT_CANCELLED, request, debugMessages, "YumariaJobXpGainEvent cancelled or zeroed");
        }
        finalXp = xpEvent.getFinalXp();

        YumariaJobProgressGainEvent legacyEvent = new YumariaJobProgressGainEvent(player, job.id(), finalXp, normalizedSource, safeContext);
        Bukkit.getPluginManager().callEvent(legacyEvent);
        if (legacyEvent.isCancelled() || legacyEvent.getAmount() <= 0.0D) {
            return failure(ProgressionFailureReason.EVENT_CANCELLED, request, debugMessages, "YumariaJobProgressGainEvent cancelled or zeroed");
        }
        finalXp = legacyEvent.getAmount();

        // Anti-abuse metier/source: cooldowns, limites par minute/heure, diminishing returns.
        AntiAbuseResult antiAbuse = antiAbuseService.validate(player.getUniqueId(), job.id(), normalizedSource, finalXp, safeContext);
        debugMessages.addAll(antiAbuse.debugMessages());
        if (!antiAbuse.accepted()) {
            return failure(ProgressionFailureReason.ANTI_ABUSE_REJECTED, request, debugMessages, antiAbuse.reason());
        }
        finalXp *= antiAbuse.multiplier();
        if (finalXp <= 0.0D) {
            return failure(ProgressionFailureReason.ANTI_ABUSE_REJECTED, request, debugMessages, "anti-abuse reduced XP to zero");
        }

        // Application reelle de l'XP dans le profil joueur.
        jobData.addProgress(finalXp);
        jobData.recordAction(normalizedSource, finalXp, System.currentTimeMillis());

        // Gestion des level-ups multiples si un seul gain donne assez d'XP.
        LevelUpOutcome levelUpOutcome = handleLevelUps(player, job, jobData);
        double required = progressionService.requiredProgress(job, jobData);
        if (jobData.getLevel() >= job.maxLevel() && jobData.getProgress() > required) {
            jobData.setProgress(required);
        }

        plugin.debugProgress("giveXp result: player=" + player.getName()
                + ", job=" + job.id()
                + ", source=" + normalizedSource
                + ", oldProgress=" + Text.formatNumber(oldProgress)
                + ", baseXp=" + Text.formatNumber(request.baseAmount())
                + ", finalXp=" + Text.formatNumber(finalXp)
                + ", newProgress=" + Text.formatNumber(jobData.getProgress())
                + ", oldLevel=" + oldLevel
                + ", newLevel=" + jobData.getLevel()
                + ", oldRequired=" + Text.formatNumber(oldRequired)
                + ", required=" + Text.formatNumber(required));

        // Feedback visuel event-driven: c'est ici que le bossbar de progression est demande.
        progressBarService.showProgress(player, job, jobData);
        playerDataService.markDirty(data);
        return ProgressionResult.builder(job.id())
                .success(true)
                .baseXp(request.baseAmount())
                .finalXp(finalXp)
                .levels(oldLevel, jobData.getLevel())
                .prestiges(oldPrestige, jobData.getPrestige())
                .rewards(levelUpOutcome.rewards())
                .debug(debugMessages)
                .build();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private ProgressionResult failure(ProgressionFailureReason reason, JobXpRequest request, List<String> debugMessages, String message) {
        debugMessages.add(message);
        plugin.debugProgress("giveXp rejected: player=" + request.playerName()
                + ", job=" + request.jobId()
                + ", amount=" + request.baseAmount()
                + ", source=" + request.source()
                + ", reason=" + reason
                + ", detail=" + message);
        return ProgressionResult.builder(request.jobId())
                .success(false)
                .failureReason(reason)
                .baseXp(request.baseAmount())
                .debug(debugMessages)
                .build();
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
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
        jobData.incrementPrestiges();
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
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public ProgressionResult applyPrestige(Player player, String jobId) {
        if (player == null) {
            return ProgressionResult.failure(ProgressionFailureReason.PLAYER_NOT_FOUND, jobId, 0.0D, "player is null");
        }
        Optional<JobDefinition> optionalJob = jobRegistry.get(jobId);
        PlayerJobData before = optionalJob.flatMap(job -> Optional.ofNullable(playerDataService.getOrLoad(player).peekJob(job.id()))).map(PlayerJobData::copy).orElse(null);
        PrestigeResult result = prestige(player, jobId);
        if (result != PrestigeResult.SUCCESS) {
            return ProgressionResult.failure(mapPrestigeFailure(result), jobId, 0.0D, result.name());
        }
        JobDefinition job = jobRegistry.get(jobId).orElseThrow();
        PlayerJobData after = playerDataService.getOrLoad(player).peekJob(job.id());
        return ProgressionResult.builder(job.id())
                .success(true)
                .levels(before == null ? 0 : before.getLevel(), after == null ? 0 : after.getLevel())
                .prestiges(before == null ? 0 : before.getPrestige(), after == null ? 0 : after.getPrestige())
                .build();
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public ProgressionResult applyPrestige(UUID playerId, String jobId) {
        Player player = playerId == null ? null : Bukkit.getPlayer(playerId);
        return applyPrestige(player, jobId);
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    private ProgressionFailureReason mapPrestigeFailure(PrestigeResult result) {
        return switch (result) {
            case DISABLED -> ProgressionFailureReason.XP_DISABLED;
            case UNKNOWN_JOB -> ProgressionFailureReason.JOB_NOT_FOUND;
            case NOT_JOINED, REQUIRE_MAX_LEVEL, MAX_PRESTIGE -> ProgressionFailureReason.JOB_NOT_ACTIVE;
            case CANCELLED -> ProgressionFailureReason.EVENT_CANCELLED;
            case SUCCESS -> ProgressionFailureReason.NONE;
        };
    }

    @Override
    public int getLevel(Player player, String jobId) {
        return withJobData(player, jobId, PlayerJobData::getLevel, 0);
    }

    @Override
    public double getProgress(Player player, String jobId) {
        return withJobData(player, jobId, PlayerJobData::getProgress, 0.0D);
    }

    @Override
    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
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
    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
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

    // Annotation YumariaJobs: Controle les montees de niveau et les recompenses associees.
    private LevelUpOutcome handleLevelUps(Player player, JobDefinition job, PlayerJobData jobData) {
        // Boucle protegee: permet plusieurs niveaux d'un coup sans risque de boucle infinie.
        int guard = 0;
        List<RewardResult> rewards = new ArrayList<>();
        while (jobData.getLevel() < job.maxLevel() && guard++ < 1000) {
            double required = progressionService.requiredProgress(job, jobData);
            if (jobData.getProgress() < required) {
                return new LevelUpOutcome(rewards);
            }
            int oldLevel = jobData.getLevel();
            jobData.setProgress(jobData.getProgress() - required);
            jobData.setLevel(oldLevel + 1);
            jobData.incrementLevelUps();
            plugin.debugProgress("level-up: player=" + player.getName()
                    + ", job=" + job.id()
                    + ", oldLevel=" + oldLevel
                    + ", newLevel=" + jobData.getLevel()
                    + ", remainingProgress=" + Text.formatNumber(jobData.getProgress()));
            Bukkit.getPluginManager().callEvent(new YumariaJobLevelUpEvent(player, job.id(), oldLevel, jobData.getLevel(), jobData.getPrestige()));
            rewards.addAll(rewardService.applyLevelRewards(player, job, jobData));
            languageService.send(player, "jobs.level-up", Map.of(
                    "%job_name%", job.displayName(),
                    "%level%", Integer.toString(jobData.getLevel())
            ));
        }
        return new LevelUpOutcome(rewards);
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

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
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

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
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

    // Annotation YumariaJobs: Controle les montees de niveau et les recompenses associees.
    private record LevelUpOutcome(List<RewardResult> rewards) {
        private LevelUpOutcome {
            rewards = List.copyOf(rewards);
        }
    }
}
