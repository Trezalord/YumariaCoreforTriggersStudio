package fr.yumaria.jobs.action;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.anticheat.AntiAbuseResult;
import fr.yumaria.jobs.api.YumariaActionService;
import fr.yumaria.jobs.api.event.YumariaActionProcessedEvent;
import fr.yumaria.jobs.api.event.YumariaActionRejectedEvent;
import fr.yumaria.jobs.api.event.YumariaActionReportEvent;
import fr.yumaria.jobs.api.event.YumariaProfessionLevelUpEvent;
import fr.yumaria.jobs.api.event.YumariaProfessionXpGainEvent;
import fr.yumaria.jobs.api.model.EconomyTransactionResult;
import fr.yumaria.jobs.api.model.JobXpRequest;
import fr.yumaria.jobs.api.model.ProgressionFailureReason;
import fr.yumaria.jobs.api.model.ProgressionResult;
import fr.yumaria.jobs.api.model.YumariaActionFailureReason;
import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.api.model.YumariaActionResult;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.progress.JobProgressService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultYumariaActionService implements YumariaActionService {
    private final YumariaJobsPlugin plugin;
    private final ActionValidationService validationService;
    private final ActionModifierPipeline modifierPipeline;
    private final ActionAntiAbuseService antiAbuseService;
    private final ActionEconomyService economyService;
    private final ActionRewardPipeline rewardPipeline;
    private final ActionStatsService statsService;
    private final JobProgressService jobProgressService;

    public DefaultYumariaActionService(
            YumariaJobsPlugin plugin,
            ActionValidationService validationService,
            ActionModifierPipeline modifierPipeline,
            ActionAntiAbuseService antiAbuseService,
            ActionEconomyService economyService,
            ActionRewardPipeline rewardPipeline,
            ActionStatsService statsService,
            JobProgressService jobProgressService
    ) {
        this.plugin = plugin;
        this.validationService = validationService;
        this.modifierPipeline = modifierPipeline;
        this.antiAbuseService = antiAbuseService;
        this.economyService = economyService;
        this.rewardPipeline = rewardPipeline;
        this.statsService = statsService;
        this.jobProgressService = jobProgressService;
    }

    @Override
    public YumariaActionResult report(YumariaActionReport report) {
        if (report == null) {
            return YumariaActionResult.failure(YumariaActionFailureReason.INTERNAL_ERROR, null, "action report is null");
        }
        if (!Bukkit.isPrimaryThread()) {
            YumariaActionResult result = YumariaActionResult.failure(YumariaActionFailureReason.INTERNAL_ERROR, report, "actions().report must be called on the main server thread");
            fireRejected(report, result);
            return result;
        }

        List<String> debug = new ArrayList<>();
        plugin.debugProgress("action report received: player=" + report.playerName()
                + ", addon=" + report.addonId()
                + ", profession=" + report.professionId()
                + ", action=" + report.actionType()
                + ", baseXp=" + Text.formatNumber(report.baseXp())
                + ", baseMoney=" + Text.formatNumber(report.baseMoney()));

        ActionValidationResult validation = validationService.validate(report);
        debug.addAll(validation.debugMessages());
        if (!validation.accepted()) {
            YumariaActionResult result = rejected(report, validation.failureReason(), debug);
            fireRejected(report, result);
            return result;
        }

        YumariaActionReportEvent reportEvent = new YumariaActionReportEvent(report);
        Bukkit.getPluginManager().callEvent(reportEvent);
        if (reportEvent.isCancelled()) {
            debug.add("YumariaActionReportEvent cancelled");
            YumariaActionResult result = rejected(report, YumariaActionFailureReason.EVENT_CANCELLED, debug);
            fireRejected(report, result);
            return result;
        }

        Player player = validation.player();
        JobDefinition profession = validation.profession();
        ActionModifierPipeline.ActionModifierCalculation calculation = modifierPipeline.apply(new ActionModifierContext(player, report, profession));
        debug.addAll(calculation.debugMessages());

        AntiAbuseResult antiAbuse = antiAbuseService.validate(player.getUniqueId(), report, calculation.finalXp(), calculation.finalMoney());
        debug.addAll(antiAbuse.debugMessages());
        if (!antiAbuse.accepted()) {
            debug.add(antiAbuse.reason());
            YumariaActionResult result = rejected(report, YumariaActionFailureReason.ANTI_ABUSE_REJECTED, debug);
            fireRejected(report, result);
            return result;
        }

        double actionXp = calculation.finalXp() * antiAbuse.multiplier();
        double actionMoney = calculation.finalMoney() * antiAbuse.multiplier();
        Map<String, Object> context = actionContext(report, actionXp, actionMoney);

        ProgressionResult progression = applyProgression(player, report, profession, actionXp, context, debug);
        if (!progression.success() && actionXp > 0.0D) {
            debug.addAll(progression.debugMessages());
            YumariaActionResult result = rejected(report, mapProgressionFailure(progression.failureReason()), debug);
            fireRejected(report, result);
            return result;
        }

        double finalMoney = applyMoney(player, report, profession.id(), actionMoney, context, progression.success(), debug);
        YumariaActionResult result = YumariaActionResult.builder(report.addonId(), profession.id(), report.actionType())
                .success(true)
                .baseXp(report.baseXp())
                .finalXp(progression.success() ? progression.finalXp() : 0.0D)
                .baseMoney(report.baseMoney())
                .finalMoney(finalMoney)
                .levels(progression.oldLevel(), progression.newLevel())
                .prestiges(progression.oldPrestige(), progression.newPrestige())
                .rewards(rewardPipeline.collect(report, YumariaActionResult.builder(report.addonId(), profession.id(), report.actionType())
                        .success(true)
                        .rewards(progression.rewards())
                        .build()))
                .debug(debug)
                .debug(progression.debugMessages())
                .build();

        if (result.leveledUp()) {
            Bukkit.getPluginManager().callEvent(new YumariaProfessionLevelUpEvent(player, report.addonId(), profession.id(), report.actionType(), result.oldLevel(), result.newLevel(), result.newPrestige()));
        }
        statsService.recordActionType(player, profession.id(), Text.normalizeId(report.actionType()));
        Bukkit.getPluginManager().callEvent(new YumariaActionProcessedEvent(report, result));
        plugin.debugProgress("action processed: player=" + player.getName()
                + ", addon=" + report.addonId()
                + ", profession=" + profession.id()
                + ", action=" + report.actionType()
                + ", finalXp=" + Text.formatNumber(result.finalXp())
                + ", finalMoney=" + Text.formatNumber(result.finalMoney()));
        return result;
    }

    private ProgressionResult applyProgression(Player player, YumariaActionReport report, JobDefinition profession, double actionXp, Map<String, Object> context, List<String> debug) {
        if (actionXp <= 0.0D) {
            return ProgressionResult.builder(profession.id()).success(true).build();
        }
        YumariaProfessionXpGainEvent xpEvent = new YumariaProfessionXpGainEvent(player, report.addonId(), profession.id(), report.actionType(), report.baseXp(), actionXp, context);
        Bukkit.getPluginManager().callEvent(xpEvent);
        if (xpEvent.isCancelled() || xpEvent.getFinalXp() <= 0.0D) {
            return ProgressionResult.failure(ProgressionFailureReason.EVENT_CANCELLED, profession.id(), actionXp, "YumariaProfessionXpGainEvent cancelled or zeroed");
        }
        debug.add("calling JobProgressService.giveXp from action pipeline");
        return jobProgressService.giveXp(JobXpRequest.builder()
                .player(player)
                .jobId(profession.id())
                .baseAmount(xpEvent.getFinalXp())
                .source(report.addonId())
                .context(context)
                .build());
    }

    private double applyMoney(Player player, YumariaActionReport report, String professionId, double actionMoney, Map<String, Object> context, boolean progressionSucceeded, List<String> debug) {
        if (actionMoney <= 0.0D) {
            return 0.0D;
        }
        EconomyTransactionResult economy = economyService.give(player, report, professionId, actionMoney, context);
        if (!economy.success()) {
            debug.add("money reward skipped: " + economy.failureReason() + " " + economy.message());
            return 0.0D;
        }
        statsService.recordMoney(player, professionId, report.addonId(), economy.amount());
        if (!progressionSucceeded) {
            debug.add("money granted without XP because action had no XP component");
        }
        return economy.amount();
    }

    private Map<String, Object> actionContext(YumariaActionReport report, double actionXp, double actionMoney) {
        Map<String, Object> context = new HashMap<>(report.context());
        context.put("addon_id", report.addonId());
        context.put("plugin_source", report.addonId());
        context.put("action_type", Text.normalizeId(report.actionType()));
        context.put("base_xp", report.baseXp());
        context.put("base_money", report.baseMoney());
        context.put("action_xp_before_progression", actionXp);
        context.put("action_money", actionMoney);
        context.put("timestamp", report.timestamp());
        return Map.copyOf(context);
    }

    private YumariaActionResult rejected(YumariaActionReport report, YumariaActionFailureReason reason, List<String> debug) {
        return YumariaActionResult.builder(report.addonId(), report.professionId(), report.actionType())
                .success(false)
                .failureReason(reason)
                .baseXp(report.baseXp())
                .baseMoney(report.baseMoney())
                .debug(debug)
                .build();
    }

    private void fireRejected(YumariaActionReport report, YumariaActionResult result) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(new YumariaActionRejectedEvent(report, result));
        }
        plugin.debugProgress("action rejected: addon=" + (report == null ? "-" : report.addonId())
                + ", profession=" + (report == null ? "-" : report.professionId())
                + ", action=" + (report == null ? "-" : report.actionType())
                + ", reason=" + result.failureReason()
                + ", debug=" + result.debugMessages());
    }

    private YumariaActionFailureReason mapProgressionFailure(ProgressionFailureReason reason) {
        return switch (reason) {
            case PLAYER_NOT_FOUND -> YumariaActionFailureReason.PLAYER_NOT_FOUND;
            case JOB_NOT_FOUND -> YumariaActionFailureReason.PROFESSION_NOT_FOUND;
            case JOB_NOT_ACTIVE -> YumariaActionFailureReason.PROFESSION_NOT_ACTIVE;
            case INVALID_AMOUNT -> YumariaActionFailureReason.INVALID_XP_AMOUNT;
            case XP_DISABLED -> YumariaActionFailureReason.PROFESSION_DISABLED;
            case SOURCE_BLOCKED -> YumariaActionFailureReason.ACTION_TYPE_BLOCKED;
            case ANTI_ABUSE_REJECTED -> YumariaActionFailureReason.ANTI_ABUSE_REJECTED;
            case PROFILE_NOT_LOADED -> YumariaActionFailureReason.PROFILE_NOT_LOADED;
            case EVENT_CANCELLED -> YumariaActionFailureReason.EVENT_CANCELLED;
            case INTERNAL_ERROR, NONE -> YumariaActionFailureReason.INTERNAL_ERROR;
        };
    }
}
