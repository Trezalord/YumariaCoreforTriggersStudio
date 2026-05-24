package fr.yumaria.jobs.job;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.config.RankService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.progress.ProgressionService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class JobPlaceholderService {
    private final YumariaJobsPlugin plugin;
    private final ProgressionService progressionService;
    private final RankService rankService;

    public JobPlaceholderService(YumariaJobsPlugin plugin, ProgressionService progressionService, RankService rankService) {
        this.plugin = plugin;
        this.progressionService = progressionService;
        this.rankService = rankService;
    }

    public Map<String, String> placeholders(Player player, JobDefinition job, PlayerJobData data) {
        double required = progressionService.requiredProgress(job, data);
        double percentValue = required <= 0.0D ? 0.0D : Math.max(0.0D, Math.min(100.0D, (data.getProgress() / required) * 100.0D));
        String prestigeDisplay = data.getPrestige() > 0
                ? plugin.getConfig().getString("progress-bar.prestige-display", "&5P%prestige% &8| ")
                : plugin.getConfig().getString("progress-bar.no-prestige-display", "");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%uuid%", player.getUniqueId().toString());
        placeholders.put("%job_id%", job.id());
        placeholders.put("%job_name%", job.displayName());
        placeholders.put("%level%", Integer.toString(data.getLevel()));
        placeholders.put("%max_level%", Integer.toString(job.maxLevel()));
        placeholders.put("%progress%", Text.formatNumber(data.getProgress()));
        placeholders.put("%required_progress%", Text.formatNumber(required));
        placeholders.put("%total_progress%", Text.formatNumber(data.getTotalProgress()));
        placeholders.put("%percent%", Text.formatNumber(percentValue));
        placeholders.put("%rank%", rankService.rankForLevel(data.getLevel()));
        placeholders.put("%prestige%", Integer.toString(data.getPrestige()));
        placeholders.put("%prestige_display%", Text.placeholders(prestigeDisplay, Map.of("%prestige%", Integer.toString(data.getPrestige()))));
        placeholders.put("%text_progress_bar%", textProgressBar(data.getProgress(), required));
        placeholders.put("%points%", Text.formatNumber(data.getPoints()));
        placeholders.put("%next_money%", Text.formatNumber(progressionService.evaluateMoney(job.defaultReward().moneyExpression(), job, data)));
        return placeholders;
    }

    public String textProgressBar(double progress, double required) {
        String basePath = plugin.getConfig().isConfigurationSection("progress-bar.text-progress-bar")
                ? "progress-bar.text-progress-bar"
                : "progress-bar.progress-bar-format";
        if (!plugin.getConfig().getBoolean(basePath + ".enabled", true)) {
            return "";
        }
        int length = Math.max(1, plugin.getConfig().getInt(basePath + ".length", 14));
        double ratio = required <= 0.0D ? 0.0D : Math.max(0.0D, Math.min(1.0D, progress / required));
        int filled = (int) Math.round(ratio * length);
        String filledSymbol = plugin.getConfig().getString(basePath + ".filled-symbol", "■");
        String emptySymbol = plugin.getConfig().getString(basePath + ".empty-symbol", "■");
        String filledColor = plugin.getConfig().getString(basePath + ".filled-color", "&d");
        String emptyColor = plugin.getConfig().getString(basePath + ".empty-color", "&8");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < length; index++) {
            if (index < filled) {
                builder.append(filledColor).append(filledSymbol);
            } else {
                builder.append(emptyColor).append(emptySymbol);
            }
        }
        return builder.toString();
    }
}
