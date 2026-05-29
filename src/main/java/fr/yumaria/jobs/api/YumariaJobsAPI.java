package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (YumariaJobsAPI).

import fr.yumaria.jobs.api.model.JobXpRequest;
import fr.yumaria.jobs.api.model.ProgressionFailureReason;
import fr.yumaria.jobs.api.model.ProgressionResult;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public final class YumariaJobsAPI {
    private static final YumariaJobsAPI INSTANCE = new YumariaJobsAPI();
    private static final AtomicReference<YumariaJobsProvider> PROVIDER = new AtomicReference<>();
    private static final NoopServices NOOP = new NoopServices();

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private YumariaJobsAPI() {
    }

    public static YumariaJobsAPI get() {
        return INSTANCE;
    }

    public static void setProvider(YumariaJobsProvider provider) {
        PROVIDER.set(provider);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static void clearProvider(YumariaJobsProvider provider) {
        PROVIDER.compareAndSet(provider, null);
    }

    // Acces direct a l'ancien service XP, conserve pour compatibilite avec les integrations deja faites.
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public JobXpService xp() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.xp();
    }

    // Point d'entree recommande pour les addons: report d'action gameplay vers le coeur MMORPG.
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaActionService actions() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.actions();
    }

    // Profils joueur en lecture controlee, sans exposer les maps internes mutables.
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerProfileService profiles() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.profiles();
    }

    // Wrapper economie YumariaJobs autour de Vault, utilise pour les recompenses d'action.
    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public YumariaEconomyService economy() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.economy();
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public PrestigeService prestiges() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.prestiges();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public JobStatsService stats() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.stats();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public RewardService rewards() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.rewards();
    }

    // Registre souple des addons Yumaria: YumariaFishing, YumariaFarming, futurs modules.
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaAddonRegistry addons() {
        YumariaJobsProvider provider = PROVIDER.get();
        return provider == null ? NOOP : provider.addons();
    }

    // API legacy: ajoute de la progression en routant vers giveXp().
    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public static void addProgress(Player player, String jobId, double amount, String source) {
        addProgress(player, jobId, amount, source, Map.of());
    }

    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public static void addProgress(Player player, String jobId, double amount, String source, Map<String, Object> context) {
        giveXp(player, jobId, amount, source, context);
    }

    // API legacy plus expressive: retourne un resultat structure au lieu d'echouer silencieusement.
    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public static ProgressionResult giveXp(Player player, String jobId, double amount, String source) {
        return giveXp(player, jobId, amount, source, Map.of());
    }

    // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
    public static ProgressionResult giveXp(Player player, String jobId, double amount, String source, Map<String, Object> context) {
        if (player == null) {
            return ProgressionResult.failure(ProgressionFailureReason.PLAYER_NOT_FOUND, jobId, amount, "player is null");
        }
        try {
            return get().xp().giveXp(JobXpRequest.builder()
                    .player(player)
                    .jobId(jobId)
                    .baseAmount(amount)
                    .source(source)
                    .context(context == null ? Map.of() : context)
                    .build());
        } catch (RuntimeException exception) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, jobId, amount, exception.getMessage());
        }
    }

    public static int getLevel(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0;
        }
        try {
            return provider.getLevel(player, jobId);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    public static double getProgress(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0.0D;
        }
        try {
            return provider.getProgress(player, jobId);
        } catch (RuntimeException exception) {
            return 0.0D;
        }
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    public static double getRequiredProgress(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0.0D;
        }
        try {
            return provider.getRequiredProgress(player, jobId);
        } catch (RuntimeException exception) {
            return 0.0D;
        }
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public static int getPrestige(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0;
        }
        try {
            return provider.getPrestige(player, jobId);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    public static boolean hasJob(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return false;
        }
        try {
            return provider.hasJob(player, jobId);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean isJobActive(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return false;
        }
        try {
            return provider.isJobActive(player, jobId);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static final class NoopServices implements JobXpService, YumariaActionService, PlayerProfileService, YumariaEconomyService, PrestigeService, JobStatsService, RewardService, YumariaAddonRegistry {
        @Override
        // Annotation YumariaJobs: Reçoit une action gameplay et la fait passer dans le coeur MMORPG.
        public fr.yumaria.jobs.api.model.YumariaActionResult report(fr.yumaria.jobs.api.model.YumariaActionReport report) {
            return fr.yumaria.jobs.api.model.YumariaActionResult.failure(fr.yumaria.jobs.api.model.YumariaActionFailureReason.INTERNAL_ERROR, report, "YumariaJobs provider is not available");
        }

        @Override
        // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
        public ProgressionResult giveXp(Player player, String jobId, double amount, String source) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, jobId, amount, "YumariaJobs provider is not available");
        }

        @Override
        // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
        public ProgressionResult giveXp(UUID playerId, String jobId, double amount, String source) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, jobId, amount, "YumariaJobs provider is not available");
        }

        @Override
        // Annotation YumariaJobs: Ajoute de la progression via le chemin XP officiel du plugin.
        public ProgressionResult giveXp(JobXpRequest request) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, request == null ? "" : request.jobId(), request == null ? 0.0D : request.baseAmount(), "YumariaJobs provider is not available");
        }

        @Override
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public java.util.Optional<fr.yumaria.jobs.api.model.PlayerProfile> profile(UUID playerId) {
            return java.util.Optional.empty();
        }

        @Override
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public java.util.Optional<fr.yumaria.jobs.api.model.PlayerProfile> profile(Player player) {
            return java.util.Optional.empty();
        }

        @Override
        // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
        public fr.yumaria.jobs.api.model.PlayerProfile getOrLoadProfile(Player player) {
            UUID uuid = player == null ? new UUID(0L, 0L) : player.getUniqueId();
            String name = player == null ? "" : player.getName();
            return new fr.yumaria.jobs.api.model.PlayerProfile(uuid, name, java.util.Map.of(), java.util.Map.of());
        }

        @Override
        // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
        public boolean isLoaded(UUID playerId) {
            return false;
        }

        @Override
        // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
        public fr.yumaria.jobs.api.model.EconomyTransactionResult give(fr.yumaria.jobs.api.model.EconomyRewardRequest request) {
            double amount = request == null ? 0.0D : request.amount();
            return fr.yumaria.jobs.api.model.EconomyTransactionResult.failure(fr.yumaria.jobs.api.model.YumariaActionFailureReason.INTERNAL_ERROR, amount, "YumariaJobs provider is not available");
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
        public ProgressionResult applyPrestige(Player player, String jobId) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, jobId, 0.0D, "YumariaJobs provider is not available");
        }

        @Override
        // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
        public ProgressionResult applyPrestige(UUID playerId, String jobId) {
            return ProgressionResult.failure(ProgressionFailureReason.INTERNAL_ERROR, jobId, 0.0D, "YumariaJobs provider is not available");
        }

        @Override
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public java.util.Optional<fr.yumaria.jobs.api.model.JobStats> jobStats(UUID playerId, String jobId) {
            return java.util.Optional.empty();
        }

        @Override
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public java.util.Map<String, fr.yumaria.jobs.api.model.JobStats> allJobStats(UUID playerId) {
            return java.util.Map.of();
        }

        @Override
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public java.util.List<fr.yumaria.jobs.api.model.RewardResult> previewRewards(UUID playerId, String jobId) {
            return java.util.List.of();
        }

        @Override
        // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
        public void registerAddon(YumariaAddon addon) {
        }

        @Override
        // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
        public void unregisterAddon(String addonId) {
        }

        @Override
        // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
        public boolean isRegistered(String addonId) {
            return false;
        }

        @Override
        public java.util.Optional<YumariaAddon> getAddon(String addonId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Collection<YumariaAddon> getAddons() {
            return java.util.List.of();
        }
    }
}
