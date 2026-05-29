package fr.yumaria.jobs.hook;

// Repere fichier YumariaJobs: integration optionnelle avec plugins externes (YumariaJobsPlaceholderExpansion).

import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.config.RankService;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.progress.ProgressionService;
import fr.yumaria.jobs.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Role YumariaJobs: Branche les integrations optionnelles sans dependance obligatoire.
public final class YumariaJobsPlaceholderExpansion extends PlaceholderExpansion {
    private static final List<String> SUFFIXES = List.of(
            "percentage_progress",
            "required_progress",
            "total_progress",
            "prestige",
            "progress",
            "active",
            "level",
            "rank"
    );

    private final String version;
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final ProgressionService progressionService;
    private final RankService rankService;

    public YumariaJobsPlaceholderExpansion(
            String version,
            JobRegistry jobRegistry,
            PlayerDataService playerDataService,
            ProgressionService progressionService,
            RankService rankService
    ) {
        this.version = version;
        this.jobRegistry = jobRegistry;
        this.playerDataService = playerDataService;
        this.progressionService = progressionService;
        this.rankService = rankService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "yumariajobs";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Yumaria";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean persist() {
        return true;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        PlayerData data = playerDataService.getOrLoad(player);
        String normalized = params.toLowerCase(Locale.ROOT);
        if (normalized.equals("active")) {
            return activeJobs(data);
        }

        PlaceholderRequest request = parse(normalized);
        if (request == null) {
            return "";
        }
        Optional<JobDefinition> optionalJob = jobRegistry.get(request.jobId());
        if (optionalJob.isEmpty()) {
            return "";
        }
        JobDefinition job = optionalJob.get();
        PlayerJobData jobData = data.peekJob(job.id());
        if (jobData == null) {
            return "";
        }
        double required = progressionService.requiredProgress(job, jobData);
        return switch (request.field()) {
            case "level" -> Integer.toString(jobData.getLevel());
            case "progress" -> Text.formatNumber(jobData.getProgress());
            case "required_progress" -> Text.formatNumber(required);
            case "total_progress" -> Text.formatNumber(jobData.getTotalProgress());
            case "percentage_progress" -> Text.formatPercent(jobData.getProgress(), required);
            case "prestige" -> Integer.toString(jobData.getPrestige());
            case "rank" -> rankService.rankForLevel(jobData.getLevel());
            case "active" -> Boolean.toString(jobData.isActive());
            default -> "";
        };
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String activeJobs(PlayerData data) {
        return data.jobs().entrySet().stream()
                .filter(entry -> entry.getValue().isJoined() && entry.getValue().isActive())
                .sorted(Comparator.comparing(entry -> entry.getKey()))
                .map(entry -> jobRegistry.get(entry.getKey()).map(JobDefinition::displayName).orElse(entry.getKey()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    private PlaceholderRequest parse(String params) {
        for (String suffix : SUFFIXES) {
            String token = "_" + suffix;
            if (params.endsWith(token)) {
                String jobId = params.substring(0, params.length() - token.length());
                if (!jobId.isBlank()) {
                    return new PlaceholderRequest(jobId, suffix);
                }
            }
        }
        return null;
    }

    // Annotation YumariaJobs: Formate ou normalise du texte pour affichage, commandes ou recherche.
    private record PlaceholderRequest(String jobId, String field) {
    }
}
