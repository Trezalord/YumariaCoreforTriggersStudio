package fr.yumaria.jobs.reward;

import fr.yumaria.jobs.YumariaJobsPlugin;
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

public final class RewardService {
    private final YumariaJobsPlugin plugin;
    private final EconomyService economyService;
    private final ProgressionService progressionService;
    private JobPlaceholderService placeholderService;

    public RewardService(YumariaJobsPlugin plugin, EconomyService economyService, ProgressionService progressionService) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.progressionService = progressionService;
    }

    public void setPlaceholderService(JobPlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    public void applyLevelRewards(Player player, JobDefinition job, PlayerJobData data) {
        double points = progressionService.pointsRewarded(job, data);
        data.addPoints(points);
        applyReward(player, job, data, job.defaultReward());
        RewardDefinition levelReward = job.levelRewards().get(data.getLevel());
        if (levelReward != null) {
            applyReward(player, job, data, levelReward);
        }
    }

    public void applyPrestigeRewards(Player player, JobDefinition job, PlayerJobData data) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("prestige.rewards");
        if (section == null) {
            return;
        }
        RewardDefinition reward = new RewardDefinition(section.getString("money", "0"), section.getStringList("commands"));
        applyReward(player, job, data, reward);
    }

    private void applyReward(Player player, JobDefinition job, PlayerJobData data, RewardDefinition reward) {
        double money = progressionService.evaluateMoney(reward.moneyExpression(), job, data);
        if (money > 0.0D) {
            economyService.deposit(player, money);
        }

        Map<String, String> placeholders = placeholderService.placeholders(player, job, data);
        placeholders.put("%money%", Text.formatCommandNumber(money));
        placeholders.put("%money_raw%", Text.formatCommandNumber(money));
        placeholders.put("%money_display%", Text.formatNumber(money));
        dispatchCommands(reward.commands(), placeholders);
    }

    private void dispatchCommands(List<String> commands, Map<String, String> placeholders) {
        for (String command : commands) {
            String parsed = Text.color(Text.placeholders(command, placeholders));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}
