package fr.yumaria.jobs.reward;

// Repere fichier YumariaJobs: application des recompenses configurees (RewardService).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.api.event.YumariaJobRewardEvent;
import fr.yumaria.jobs.api.model.RewardResult;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.hook.EconomyService;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.JobPlaceholderService;
import fr.yumaria.jobs.job.RewardDefinition;
import fr.yumaria.jobs.progress.ProgressionService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Role YumariaJobs: Applique les recompenses configurees apres progression.
public final class RewardService implements fr.yumaria.jobs.api.RewardService {
    private final YumariaJobsPlugin plugin;
    private final EconomyService economyService;
    private final ProgressionService progressionService;
    private JobPlaceholderService placeholderService;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public RewardService(YumariaJobsPlugin plugin, EconomyService economyService, ProgressionService progressionService) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.progressionService = progressionService;
    }

    // Annotation YumariaJobs: Formate ou normalise du texte pour affichage, commandes ou recherche.
    public void setPlaceholderService(JobPlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public List<RewardResult> applyLevelRewards(Player player, JobDefinition job, PlayerJobData data) {
        java.util.ArrayList<RewardResult> results = new java.util.ArrayList<>();
        double points = progressionService.pointsRewarded(job, data);
        data.addPoints(points);
        results.add(RewardResult.success("points", "level points", points));
        results.addAll(applyReward(player, job, data, job.defaultReward(), "level-up-default"));
        RewardDefinition levelReward = job.levelRewards().get(data.getLevel());
        if (levelReward != null) {
            results.addAll(applyReward(player, job, data, levelReward, "level-up-" + data.getLevel()));
        }
        return List.copyOf(results);
    }

    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public List<RewardResult> applyPrestigeRewards(Player player, JobDefinition job, PlayerJobData data) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("prestige.rewards");
        if (section == null) {
            return List.of();
        }
        RewardDefinition reward = new RewardDefinition(section.getString("money", "0"), section.getStringList("commands"));
        return applyReward(player, job, data, reward, "prestige");
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<RewardResult> previewRewards(UUID playerId, String jobId) {
        return List.of();
    }

    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    private List<RewardResult> applyReward(Player player, JobDefinition job, PlayerJobData data, RewardDefinition reward, String trigger) {
        YumariaJobRewardEvent event = new YumariaJobRewardEvent(player, job.id(), trigger, data.getLevel(), data.getPrestige());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return List.of(RewardResult.skipped("reward", "cancelled by YumariaJobRewardEvent"));
        }
        java.util.ArrayList<RewardResult> results = new java.util.ArrayList<>();
        double money = progressionService.evaluateMoney(reward.moneyExpression(), job, data);
        if (money > 0.0D) {
            economyService.deposit(player, money);
            results.add(RewardResult.success("money", "vault deposit", money));
        }

        Map<String, String> placeholders = placeholderService.placeholders(player, job, data);
        placeholders.put("%money%", Text.formatCommandNumber(money));
        placeholders.put("%money_raw%", Text.formatCommandNumber(money));
        placeholders.put("%money_display%", Text.formatNumber(money));
        dispatchCommands(reward.commands(), placeholders);
        if (!reward.commands().isEmpty()) {
            results.add(RewardResult.success("commands", "dispatched " + reward.commands().size() + " command(s)", reward.commands().size()));
        }
        return List.copyOf(results);
    }

    // Annotation YumariaJobs: Traite une commande ou ses suggestions.
    private void dispatchCommands(List<String> commands, Map<String, String> placeholders) {
        for (String command : commands) {
            String parsed = Text.color(Text.placeholders(command, placeholders));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}
