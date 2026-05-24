package fr.yumaria.jobs.command;

import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.config.LanguageService;
import fr.yumaria.jobs.config.RankService;
import fr.yumaria.jobs.data.LeaderboardEntry;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.gui.JobGuiService;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.PlayerJobService;
import fr.yumaria.jobs.progress.ProgressionService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class JobsCommand implements CommandExecutor, TabCompleter {
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final PlayerJobService playerJobService;
    private final ProgressionService progressionService;
    private final RankService rankService;
    private final JobGuiService guiService;
    private final LanguageService languageService;

    public JobsCommand(
            JobRegistry jobRegistry,
            PlayerDataService playerDataService,
            PlayerJobService playerJobService,
            ProgressionService progressionService,
            RankService rankService,
            JobGuiService guiService,
            LanguageService languageService
    ) {
        this.jobRegistry = jobRegistry;
        this.playerDataService = playerDataService;
        this.playerJobService = playerJobService;
        this.progressionService = progressionService;
        this.rankService = rankService;
        this.guiService = guiService;
        this.languageService = languageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                languageService.send(sender, "commands.player-only");
                return true;
            }
            if (!sender.hasPermission("yumariajobs.use")) {
                languageService.send(sender, "commands.no-permission");
                return true;
            }
            guiService.openMain(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "join" -> join(sender, args);
            case "leave" -> leave(sender, args);
            case "toggle" -> toggle(sender, args);
            case "stats" -> stats(sender, args);
            case "top" -> top(sender, args);
            default -> {
                languageService.send(sender, "commands.usage.jobs");
                yield true;
            }
        };
    }

    private boolean join(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            languageService.send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission("yumariajobs.join")) {
            languageService.send(sender, "commands.no-permission");
            return true;
        }
        Optional<JobDefinition> job = jobArg(sender, args);
        if (job.isEmpty()) {
            return true;
        }
        if (playerJobService.join(player, job.get())) {
            languageService.send(sender, "jobs.joined", Map.of("%job_name%", job.get().displayName()));
        } else {
            languageService.send(sender, "jobs.already-joined");
        }
        return true;
    }

    private boolean leave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            languageService.send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission("yumariajobs.leave")) {
            languageService.send(sender, "commands.no-permission");
            return true;
        }
        Optional<JobDefinition> job = jobArg(sender, args);
        if (job.isEmpty()) {
            return true;
        }
        if (playerJobService.leave(player, job.get())) {
            languageService.send(sender, "jobs.left", Map.of("%job_name%", job.get().displayName()));
        } else {
            languageService.send(sender, "jobs.not-joined");
        }
        return true;
    }

    private boolean toggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            languageService.send(sender, "commands.player-only");
            return true;
        }
        Optional<JobDefinition> job = jobArg(sender, args);
        if (job.isEmpty()) {
            return true;
        }
        PlayerJobService.ToggleResult result = playerJobService.toggle(player, job.get());
        switch (result) {
            case ACTIVE -> languageService.send(sender, "jobs.active", Map.of("%job_name%", job.get().displayName()));
            case INACTIVE -> languageService.send(sender, "jobs.inactive", Map.of("%job_name%", job.get().displayName()));
            case ACTIVE_LIMIT -> languageService.send(sender, "jobs.active-limit");
            case NOT_JOINED -> languageService.send(sender, "jobs.not-joined");
        }
        return true;
    }

    private boolean stats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("yumariajobs.stats")) {
            languageService.send(sender, "commands.no-permission");
            return true;
        }
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            languageService.send(sender, "commands.player-only");
            return true;
        }

        PlayerData data = playerDataService.getOrLoad(target);
        sender.sendMessage(Text.color(Text.placeholders(languageService.raw("prefix") + languageService.raw("jobs.stats-header"), Map.of("%player%", data.name()))));
        for (JobDefinition job : jobRegistry.all()) {
            PlayerJobData jobData = data.peekJob(job.id());
            if (jobData == null || !jobData.isJoined()) {
                continue;
            }
            double required = progressionService.requiredProgress(job, jobData);
            sender.sendMessage(Text.color(Text.placeholders(languageService.raw("jobs.stats-line"), Map.of(
                    "%job_name%", job.displayName(),
                    "%level%", Integer.toString(jobData.getLevel()),
                    "%prestige%", Integer.toString(jobData.getPrestige()),
                    "%rank%", rankService.rankForLevel(jobData.getLevel()),
                    "%percent%", Text.formatPercent(jobData.getProgress(), required)
            ))));
        }
        return true;
    }

    private boolean top(CommandSender sender, String[] args) {
        if (!sender.hasPermission("yumariajobs.top")) {
            languageService.send(sender, "commands.no-permission");
            return true;
        }
        Optional<JobDefinition> job = jobArg(sender, args);
        if (job.isEmpty()) {
            return true;
        }
        sender.sendMessage(Text.color(Text.placeholders(languageService.raw("prefix") + languageService.raw("jobs.top-header"), Map.of("%job_name%", job.get().displayName()))));
        List<LeaderboardEntry> entries = playerDataService.leaderboard(job.get().id(), 10);
        for (int index = 0; index < entries.size(); index++) {
            LeaderboardEntry entry = entries.get(index);
            sender.sendMessage(Text.color(Text.placeholders(languageService.raw("jobs.top-line"), Map.of(
                    "%position%", Integer.toString(index + 1),
                    "%player%", entry.playerName(),
                    "%prestige%", Integer.toString(entry.prestige()),
                    "%level%", Integer.toString(entry.level()),
                    "%total_progress%", Text.formatNumber(entry.totalProgress())
            ))));
        }
        return true;
    }

    private Optional<JobDefinition> jobArg(CommandSender sender, String[] args) {
        if (args.length < 2) {
            languageService.send(sender, "commands.usage.jobs");
            return Optional.empty();
        }
        Optional<JobDefinition> job = jobRegistry.get(args[1]);
        if (job.isEmpty()) {
            languageService.send(sender, "commands.unknown-job");
        }
        return job;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return startsWith(args[0], List.of("join", "leave", "stats", "top", "toggle"));
        }
        if (args.length == 2 && List.of("join", "leave", "top", "toggle").contains(args[0].toLowerCase(Locale.ROOT))) {
            return startsWith(args[1], jobIds());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            return startsWith(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }

    private List<String> jobIds() {
        return jobRegistry.all().stream().map(JobDefinition::id).toList();
    }

    private List<String> startsWith(String token, List<String> values) {
        String normalized = token.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                results.add(value);
            }
        }
        return results;
    }
}
