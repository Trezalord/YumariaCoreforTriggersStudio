package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionValidationService).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.addon.DefaultYumariaAddonRegistry;
import fr.yumaria.jobs.api.model.YumariaActionFailureReason;
import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class ActionValidationService {
    private final YumariaJobsPlugin plugin;
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final DefaultYumariaAddonRegistry addonRegistry;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ActionValidationService(YumariaJobsPlugin plugin, JobRegistry jobRegistry, PlayerDataService playerDataService, DefaultYumariaAddonRegistry addonRegistry) {
        this.plugin = plugin;
        this.jobRegistry = jobRegistry;
        this.playerDataService = playerDataService;
        this.addonRegistry = addonRegistry;
    }

    // Annotation YumariaJobs: Verifie les conditions avant de laisser continuer le pipeline.
    public ActionValidationResult validate(YumariaActionReport report) {
        // Validation stricte mais non destructive: on rejette proprement au lieu de faire crash un addon.
        List<String> debug = new ArrayList<>();
        if (report == null) {
            return rejected(YumariaActionFailureReason.INTERNAL_ERROR, debug, "action report is null");
        }
        if (!Bukkit.isPrimaryThread()) {
            return rejected(YumariaActionFailureReason.INTERNAL_ERROR, debug, "actions().report must be called on the main server thread");
        }
        Player player = report.playerId() == null ? null : Bukkit.getPlayer(report.playerId());
        if (player == null) {
            return rejected(YumariaActionFailureReason.PLAYER_NOT_FOUND, debug, "player is not online");
        }
        if (report.baseXp() < 0.0D || Double.isNaN(report.baseXp()) || Double.isInfinite(report.baseXp())) {
            return rejected(YumariaActionFailureReason.INVALID_XP_AMOUNT, debug, "invalid base XP amount");
        }
        if (report.baseMoney() < 0.0D || Double.isNaN(report.baseMoney()) || Double.isInfinite(report.baseMoney())) {
            return rejected(YumariaActionFailureReason.INVALID_MONEY_AMOUNT, debug, "invalid base money amount");
        }

        // Controle addon: inscription optionnelle, puis autorisation via config.
        if (plugin.getConfig().getBoolean("addons.require-registration", false) && !addonRegistry.isRegistered(report.addonId())) {
            return rejected(YumariaActionFailureReason.ADDON_NOT_REGISTERED, debug, "addon is not registered: " + report.addonId());
        }
        String addonPath = "addons.allowed." + report.addonId();
        if (ActionConfig.section(plugin.getConfig(), addonPath) == null && !plugin.getConfig().getBoolean("addons.accept-unconfigured", true)) {
            return rejected(YumariaActionFailureReason.ADDON_NOT_ALLOWED, debug, "addon is not configured and accept-unconfigured is false");
        }
        if (!ActionConfig.booleanValue(plugin.getConfig(), addonPath + ".enabled", true)) {
            return rejected(YumariaActionFailureReason.ADDON_NOT_ALLOWED, debug, "addon disabled by config");
        }

        Optional<JobDefinition> optionalProfession = jobRegistry.get(report.professionId());
        if (optionalProfession.isEmpty()) {
            return rejected(YumariaActionFailureReason.PROFESSION_NOT_FOUND, debug, "unknown profession/job: " + report.professionId());
        }
        JobDefinition profession = optionalProfession.get();
        if (!profession.enabled()) {
            return rejected(YumariaActionFailureReason.PROFESSION_DISABLED, debug, "profession disabled: " + profession.id());
        }
        String professionPath = addonPath + ".professions." + profession.id();
        if (!ActionConfig.booleanValue(plugin.getConfig(), professionPath + ".enabled", true)) {
            return rejected(YumariaActionFailureReason.PROFESSION_DISABLED, debug, "profession disabled for addon");
        }
        String actionPath = professionPath + ".allowed-actions." + Text.normalizeId(report.actionType());
        if (!ActionConfig.booleanValue(plugin.getConfig(), actionPath + ".enabled", true)) {
            return rejected(YumariaActionFailureReason.ACTION_TYPE_BLOCKED, debug, "action type disabled by config");
        }

        // Controle profil/metier: si le metier doit etre actif, on bloque avant le pipeline XP.
        try {
            PlayerData data = playerDataService.getOrLoad(player);
            PlayerJobData jobData = data.peekJob(profession.id());
            boolean requiresActive = ActionConfig.booleanValue(plugin.getConfig(), "professions." + profession.id() + ".requires-active-job", !profession.allowProgressWhenInactive());
            if (requiresActive) {
                if (jobData == null || !jobData.isJoined()) {
                    if (!plugin.getConfig().getBoolean("progress.auto-join-on-progress", false)) {
                        return rejected(YumariaActionFailureReason.PROFESSION_NOT_ACTIVE, debug, "player has not joined profession " + profession.id());
                    }
                } else if (!jobData.isActive()) {
                    return rejected(YumariaActionFailureReason.PROFESSION_NOT_ACTIVE, debug, "profession is inactive");
                }
            }
        } catch (RuntimeException exception) {
            return rejected(YumariaActionFailureReason.PROFILE_NOT_LOADED, debug, exception.getMessage());
        }
        debug.add("action validation accepted");
        return new ActionValidationResult(true, YumariaActionFailureReason.NONE, player, profession, debug);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private ActionValidationResult rejected(YumariaActionFailureReason reason, List<String> debug, String message) {
        debug.add(message);
        return new ActionValidationResult(false, reason, null, null, debug);
    }
}
