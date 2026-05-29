package fr.yumaria.jobs;

// Repere fichier YumariaJobs: classe principale du plugin (YumariaJobsPlugin).

import fr.yumaria.jobs.api.YumariaJobsAPI;
import fr.yumaria.jobs.action.ActionAntiAbuseService;
import fr.yumaria.jobs.action.ActionEconomyService;
import fr.yumaria.jobs.action.ActionRewardPipeline;
import fr.yumaria.jobs.action.ActionStatsService;
import fr.yumaria.jobs.action.ActionValidationService;
import fr.yumaria.jobs.action.DefaultActionModifierPipelineFactory;
import fr.yumaria.jobs.action.DefaultYumariaActionService;
import fr.yumaria.jobs.addon.DefaultYumariaAddonRegistry;
import fr.yumaria.jobs.command.JobsCommand;
import fr.yumaria.jobs.command.PrestigeCommand;
import fr.yumaria.jobs.command.YJobsCommand;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.config.LanguageService;
import fr.yumaria.jobs.config.RankService;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.economy.DefaultYumariaEconomyService;
import fr.yumaria.jobs.gui.JobGuiService;
import fr.yumaria.jobs.hook.EconomyService;
import fr.yumaria.jobs.hook.ItemsAdderIconService;
import fr.yumaria.jobs.hook.YumariaFishingHook;
import fr.yumaria.jobs.hook.YumariaJobsPlaceholderExpansion;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.JobPlaceholderService;
import fr.yumaria.jobs.job.PlayerJobService;
import fr.yumaria.jobs.listener.NativeJobListener;
import fr.yumaria.jobs.listener.PlayerDataListener;
import fr.yumaria.jobs.progress.JobProgressService;
import fr.yumaria.jobs.progress.BossBarManager;
import fr.yumaria.jobs.progress.ProgressBarService;
import fr.yumaria.jobs.progress.ProgressFormatter;
import fr.yumaria.jobs.progress.ProgressionService;
import fr.yumaria.jobs.reward.RewardService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

// Role YumariaJobs: Point central du plugin Paper YumariaJobs.
public final class YumariaJobsPlugin extends JavaPlugin {
    private JobRegistry jobRegistry;
    private LanguageService languageService;
    private RankService rankService;
    private PlayerDataService playerDataService;
    private PlayerJobService playerJobService;
    private ProgressionService progressionService;
    private JobPlaceholderService placeholderService;
    private EconomyService economyService;
    private ItemsAdderIconService itemsAdderIconService;
    private YumariaFishingHook yumariaFishingHook;
    private RewardService rewardService;
    private DefaultYumariaAddonRegistry addonRegistry;
    private DefaultYumariaEconomyService economyApiService;
    private DefaultYumariaActionService actionService;
    private ProgressFormatter progressFormatter;
    private BossBarManager bossBarManager;
    private ProgressBarService progressBarService;
    private JobProgressService jobProgressService;
    private JobGuiService guiService;
    private int autosaveTaskId = -1;
    private final List<String> registeredCommands = new ArrayList<>();
    private final List<String> registeredListeners = new ArrayList<>();

    @Override
    // Annotation YumariaJobs: Activation Paper: initialise services, configs, commandes, listeners et API.
    public void onEnable() {
        saveDefaultConfig();

        // Initialise tous les services coeur de YumariaJobs avant d'exposer l'API publique.
        jobRegistry = new JobRegistry(this);
        languageService = new LanguageService(this);
        rankService = new RankService(this);
        playerDataService = new PlayerDataService(this);
        playerDataService.start();
        progressionService = new ProgressionService(this);
        economyService = new EconomyService(this);
        economyApiService = new DefaultYumariaEconomyService(this, economyService);
        addonRegistry = new DefaultYumariaAddonRegistry(this);
        itemsAdderIconService = new ItemsAdderIconService(this);
        rewardService = new RewardService(this, economyService, progressionService);
        placeholderService = new JobPlaceholderService(this, progressionService, rankService);
        rewardService.setPlaceholderService(placeholderService);
        progressFormatter = new ProgressFormatter(this);
        bossBarManager = new BossBarManager(this);
        progressBarService = new ProgressBarService(this, progressionService, rankService, progressFormatter, bossBarManager);
        playerJobService = new PlayerJobService(this, playerDataService);

        // Moteur XP historique: tout gain de progression doit finir ici pour garder niveaux, rewards et bossbar.
        jobProgressService = new JobProgressService(this, jobRegistry, playerDataService, progressionService, rewardService, progressBarService, languageService);

        // Nouveau coeur MMORPG: les addons reportent leurs actions ici, puis YumariaJobs decide XP, argent et stats.
        actionService = new DefaultYumariaActionService(
                this,
                new ActionValidationService(this, jobRegistry, playerDataService, addonRegistry),
                DefaultActionModifierPipelineFactory.create(this),
                new ActionAntiAbuseService(this),
                new ActionEconomyService(economyApiService),
                new ActionRewardPipeline(),
                new ActionStatsService(playerDataService),
                jobProgressService
        );
        jobProgressService.setCoreServices(actionService, economyApiService, addonRegistry);

        // Hook legacy de compatibilite. L'architecture cible est que YumariaFishing appelle directement l'API actions().
        yumariaFishingHook = new YumariaFishingHook(this, jobRegistry, playerDataService, jobProgressService);
        guiService = new JobGuiService(this, jobRegistry, playerDataService, playerJobService, placeholderService, itemsAdderIconService, languageService);

        // Charge les configs, commandes, listeners, bossbar et publie YumariaJobsAPI pour les autres plugins.
        reloadPlugin(false);
        registerCommands();
        registerListeners();
        progressBarService.start();
        YumariaJobsAPI.setProvider(jobProgressService);
        registerPlaceholderApi();
        logStartupDiagnostics();

        getLogger().info("YumariaJobs enabled.");
    }

    @Override
    // Annotation YumariaJobs: Extinction Paper: annule les taches et sauvegarde proprement les profils.
    public void onDisable() {
        if (autosaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autosaveTaskId);
            autosaveTaskId = -1;
        }
        if (progressBarService != null) {
            progressBarService.stop();
        }
        if (jobProgressService != null) {
            YumariaJobsAPI.clearProvider(jobProgressService);
        }
        if (yumariaFishingHook != null) {
            yumariaFishingHook.shutdown();
        }
        if (playerDataService != null) {
            playerDataService.saveAllBlocking();
        }
    }

    // Annotation YumariaJobs: Recharge la configuration sans effacer les donnees joueur en memoire.
    public void reloadPlugin() {
        reloadPlugin(true);
    }

    // Annotation YumariaJobs: Recharge la configuration sans effacer les donnees joueur en memoire.
    public void reloadPlugin(boolean refreshGuis) {
        // Recharge uniquement les configs/services, jamais les donnees joueur deja en memoire.
        reloadConfig();
        languageService.reload();
        rankService.reload();
        jobRegistry.reload();
        economyService.reload();
        guiService.reload();
        yumariaFishingHook.reload();
        restartAutosave();
        if (refreshGuis) {
            guiService.refreshOpenMenus();
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void debug(String message) {
        debug("jobs", message);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void debug(String category, String message) {
        if (debugEnabled(category)) {
            getLogger().info("[debug] " + message);
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void debugProgress(String message) {
        debug("progress", message);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void debugListeners(String message) {
        debug("listeners", message);
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void debugBossbar(String message) {
        debug("bossbar", message);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void debugJobs(String message) {
        debug("jobs", message);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean debugEnabled(String category) {
        if (getConfig().isBoolean("debug")) {
            return getConfig().getBoolean("debug", false);
        }
        return getConfig().getBoolean("debug.enabled", false)
                && (getConfig().getBoolean("debug." + category, false) || getConfig().getBoolean("debug.all", false));
    }

    // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
    private void registerCommands() {
        // Commandes joueur/admin principales: /jobs, /prestige et /yjobs.
        registeredCommands.clear();
        JobsCommand jobsCommand = new JobsCommand(jobRegistry, playerDataService, playerJobService, progressionService, rankService, guiService, languageService);
        registerCommand("jobs", jobsCommand, jobsCommand);

        PrestigeCommand prestigeCommand = new PrestigeCommand(
                jobProgressService,
                languageService,
                () -> jobRegistry.all().stream().map(JobDefinition::id).toList()
        );
        registerCommand("prestige", prestigeCommand, prestigeCommand);

        YJobsCommand yJobsCommand = new YJobsCommand(this, jobRegistry, playerDataService, progressionService, rankService, jobProgressService, playerJobService, languageService, yumariaFishingHook);
        registerCommand("yjobs", yJobsCommand, yJobsCommand);
    }

    // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
    private void registerListeners() {
        // Listeners internes vanilla et GUI. Les addons externes doivent plutot passer par YumariaJobsAPI.
        registeredListeners.clear();
        Bukkit.getPluginManager().registerEvents(new PlayerDataListener(playerDataService, progressBarService), this);
        registeredListeners.add("PlayerDataListener");
        Bukkit.getPluginManager().registerEvents(new NativeJobListener(this, jobProgressService), this);
        registeredListeners.add("NativeJobListener");
        Bukkit.getPluginManager().registerEvents(guiService, this);
        registeredListeners.add("JobGuiService");
    }

    // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
        registeredCommands.add(name);
    }

    // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
    private void registerPlaceholderApi() {
        if (!getConfig().getBoolean("hooks.placeholderapi.enabled", true)) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            debug("PlaceholderAPI not found.");
            return;
        }
        new YumariaJobsPlaceholderExpansion(getDescription().getVersion(), jobRegistry, playerDataService, progressionService, rankService).register();
        debug("PlaceholderAPI expansion registered.");
    }

    // Annotation YumariaJobs: Prepare ou execute la sauvegarde des donnees sans bloquer inutilement le serveur.
    private void restartAutosave() {
        if (autosaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autosaveTaskId);
        }
        long intervalSeconds = Math.max(30L, getConfig().getLong("save-interval-seconds", 300L));
        autosaveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, playerDataService::saveDirtyAsync, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    private void logStartupDiagnostics() {
        // Logs de demarrage disponibles quand debug.jobs / debug.bossbar sont actifs.
        debugJobs("Startup services: jobProgressService=" + (jobProgressService != null)
                + ", progressBarService=" + (progressBarService != null)
                + ", bossBarManager=" + (bossBarManager != null)
                + ", configLoaded=" + (getConfig() != null));
        debugJobs("Loaded jobs count=" + jobRegistry.all().size() + " ids=" + jobRegistry.all().stream().map(JobDefinition::id).toList());
        debugJobs("Registered listeners=" + registeredListeners);
        debugJobs("Registered commands=" + registeredCommands);
        debugJobs("YumariaFishing hook=" + yumariaFishingHook.status());
        debugJobs("Addon registry loaded addons=" + addonRegistry.getAddons().size());
        debugBossbar("Progress-bar enabled=" + getConfig().getBoolean("progress-bar.enabled", true)
                + ", durationSeconds=" + getConfig().getLong("progress-bar.display-duration-seconds", 8L)
                + ", color=" + getConfig().getString("progress-bar.color", "PURPLE")
                + ", style=" + getConfig().getString("progress-bar.style", "SEGMENTED_20"));
    }
}
