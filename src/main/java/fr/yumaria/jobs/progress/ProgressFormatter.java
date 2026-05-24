package fr.yumaria.jobs.progress;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.util.Text;

import java.util.HashMap;
import java.util.Map;

public final class ProgressFormatter {
    private final YumariaJobsPlugin plugin;

    public ProgressFormatter(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public String formatTitle(ProgressSnapshot snapshot) {
        String title = plugin.getConfig().getString(
                "progress-bar.title",
                "&d%job_name% &8| %prestige_display%%rank% &8| &fNiv. %level% &8| &e%percent%%"
        );
        String formattedTitle = Text.placeholders(title, placeholders(snapshot));
        if (plugin.getConfig().getBoolean("progress-bar.subtitle.enabled", true) && !title.contains("%text_progress_bar%")) {
            String subtitle = plugin.getConfig().getString("progress-bar.subtitle.format", "%text_progress_bar%");
            String formattedSubtitle = Text.placeholders(subtitle, placeholders(snapshot));
            if (!formattedSubtitle.isBlank()) {
                formattedTitle = formattedTitle + " &8| " + formattedSubtitle;
            }
        }
        if (Text.stripColor(formattedTitle).isBlank()) {
            formattedTitle = "&d" + snapshot.jobName() + " &8| &fNiv. " + snapshot.level() + " &8| &e" + Text.formatNumber(snapshot.percent()) + "%";
        }
        return Text.color(formattedTitle);
    }

    public Map<String, String> placeholders(ProgressSnapshot snapshot) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%job_id%", snapshot.jobId());
        placeholders.put("%job_name%", snapshot.jobName());
        placeholders.put("%level%", Integer.toString(snapshot.level()));
        placeholders.put("%progress%", Text.formatNumber(snapshot.currentProgress()));
        placeholders.put("%required_progress%", Text.formatNumber(snapshot.requiredProgress()));
        placeholders.put("%total_progress%", Text.formatNumber(snapshot.totalProgress()));
        placeholders.put("%percent%", Text.formatNumber(snapshot.percent()));
        placeholders.put("%rank%", snapshot.rank());
        placeholders.put("%prestige%", Integer.toString(snapshot.prestige()));
        placeholders.put("%prestige_display%", prestigeDisplay(snapshot));
        placeholders.put("%text_progress_bar%", textProgressBar(snapshot));
        return placeholders;
    }

    public String prestigeDisplay(ProgressSnapshot snapshot) {
        String format = snapshot.prestige() > 0
                ? plugin.getConfig().getString("progress-bar.prestige-display", "&5P%prestige% &8| ")
                : plugin.getConfig().getString("progress-bar.no-prestige-display", "");
        return Text.placeholders(format, Map.of("%prestige%", Integer.toString(snapshot.prestige())));
    }

    public String textProgressBar(ProgressSnapshot snapshot) {
        String basePath = plugin.getConfig().isConfigurationSection("progress-bar.text-progress-bar")
                ? "progress-bar.text-progress-bar"
                : "progress-bar.progress-bar-format";
        if (!plugin.getConfig().getBoolean(basePath + ".enabled", true)) {
            return "";
        }
        int length = Math.max(1, plugin.getConfig().getInt(basePath + ".length", 14));
        int filled = (int) Math.round(snapshot.progressRatio() * length);
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
