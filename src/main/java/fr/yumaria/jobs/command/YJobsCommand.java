package fr.yumaria.jobs.command;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.api.model.JobXpRequest;
import fr.yumaria.jobs.api.model.ProgressionResult;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.config.LanguageService;
import fr.yumaria.jobs.config.RankService;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.hook.YumariaFishingHook;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.PlayerJobService;
import fr.yumaria.jobs.progress.JobProgressService;
import fr.yumaria.jobs.progress.ProgressionService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class YJobsCommand implements CommandExecutor, TabCompleter {
    private final YumariaJobsPlugin plugin;
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final ProgressionService progressionService;
    private final RankService rankService;
    private final JobProgressService progressService;
    private final PlayerJobService playerJobService;
    private final LanguageService languageService;
    private final YumariaFishingHook yumariaFishingHook;

    public YJobsCommand(
            YumariaJobsPlugin plugin,
            JobRegistry jobRegistry,
            PlayerDataService playerDataService,
            ProgressionService progressionService,
            RankService rankService,
            JobProgressService progressService,
            PlayerJobService playerJobService,
            LanguageService languageService,
            YumariaFishingHook yumariaFishingHook
    ) {
        this.plugin = plugin;
        this.jobRegistry = jobRegistry;
        this.playerDataService = playerDataService;
        this.progressionService = progressionService;
        this.rankService = rankService;
        this.progressService = progressService;
        this.playerJobService = playerJobService;
        this.languageService = languageService;
        this.yumariaFishingHook = yumariaFishingHook;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("yumariajobs.admin")) {
            languageService.send(sender, "commands.no-permission");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            languageService.send(sender, "commands.reload");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
            return debug(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("profile")) {
            return profile(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("xp")) {
            return xp(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("level")) {
            return level(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            return admin(sender, args);
        }
        languageService.send(sender, "commands.usage.yjobs");
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            languageService.send(sender, "commands.usage.yjobs-debug");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "progress" -> debugProgress(sender, args);
            case "info" -> debugInfo(sender, args);
            case "item" -> debugItem(sender);
            default -> {
                languageService.send(sender, "commands.usage.yjobs-debug");
                yield true;
            }
        };
    }

    private boolean debugItem(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            languageService.send(sender, "jobs.debug-item-player-only");
            return true;
        }
        if (yumariaFishingHook == null) {
            sender.sendMessage(Text.color("&cYumariaFishing hook unavailable."));
            return true;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        for (String line : yumariaFishingHook.describeItem(itemStack)) {
            sender.sendMessage(Text.color(line));
        }
        return true;
    }

    private boolean profile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            languageService.send(sender, "commands.usage.yjobs");
            return true;
        }
        return debugInfo(sender, new String[] {"debug", "info", args[1]});
    }

    private boolean xp(CommandSender sender, String[] args) {
        if (args.length < 5 || !args[1].equalsIgnoreCase("add")) {
            languageService.send(sender, "commands.usage.yjobs");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            languageService.send(sender, "commands.unknown-player");
            return true;
        }
        Optional<JobDefinition> job = jobRegistry.get(args[3]);
        if (job.isEmpty()) {
            languageService.send(sender, "commands.unknown-job");
            return true;
        }
        double amount = parseDouble(sender, args[4]);
        if (Double.isNaN(amount)) {
            return true;
        }
        String source = args.length >= 6 ? args[5] : "admin";
        ProgressionResult result = progressService.giveXp(JobXpRequest.builder()
                .player(target)
                .jobId(job.get().id())
                .baseAmount(amount)
                .source(source)
                .context("admin", sender.getName())
                .build());
        if (result.success()) {
            languageService.send(sender, "jobs.progress-added", Map.of("%player%", target.getName()));
        } else {
            languageService.send(sender, "jobs.debug-progress-blocked", Map.of("%reason%", result.failureReason().name()));
        }
        return true;
    }

    private boolean level(CommandSender sender, String[] args) {
        if (args.length < 5 || !args[1].equalsIgnoreCase("set")) {
            languageService.send(sender, "commands.usage.yjobs");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            languageService.send(sender, "commands.unknown-player");
            return true;
        }
        Optional<JobDefinition> job = jobRegistry.get(args[3]);
        if (job.isEmpty()) {
            languageService.send(sender, "commands.unknown-job");
            return true;
        }
        int level = parseInt(sender, args[4]);
        if (level < 0) {
            return true;
        }
        playerJobService.setLevel(target, job.get(), level);
        languageService.send(sender, "jobs.set-level", Map.of("%player%", target.getName()));
        return true;
    }

    private boolean debugProgress(CommandSender sender, String[] args) {
        if (args.length < 5) {
            languageService.send(sender, "commands.usage.yjobs-debug");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            languageService.send(sender, "commands.unknown-player");
            return true;
        }
        Optional<JobDefinition> job = jobRegistry.get(args[3]);
        if (job.isEmpty()) {
            languageService.send(sender, "commands.unknown-job");
            plugin.debugProgress("Debug progress blocked: unknown job input=" + args[3]);
            return true;
        }
        double amount = parseDouble(sender, args[4]);
        if (Double.isNaN(amount)) {
            return true;
        }

        String blockReason = ensureDiagnosticEligibility(target, job.get());
        if (blockReason != null) {
            languageService.send(sender, "jobs.debug-progress-blocked", Map.of("%reason%", blockReason));
            plugin.debugProgress("Debug progress blocked before addProgress: player=" + target.getName()
                    + ", job=" + job.get().id()
                    + ", amount=" + amount
                    + ", reason=" + blockReason);
            return true;
        }

        plugin.debugProgress("Debug progress command: sender=" + sender.getName()
                + ", player=" + target.getName()
                + ", job=" + job.get().id()
                + ", amount=" + amount);
        progressService.addProgress(target, job.get().id(), amount, "debug_progress", Map.of("debug-command", sender.getName()));
        languageService.send(sender, "jobs.debug-progress-requested", Map.of("%player%", target.getName()));
        return true;
    }

    private boolean debugInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            languageService.send(sender, "commands.usage.yjobs-debug");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            languageService.send(sender, "commands.unknown-player");
            return true;
        }
        PlayerData data = playerDataService.getOrLoad(target);
        sender.sendMessage(Text.color(Text.placeholders(languageService.raw("prefix") + languageService.raw("jobs.debug-info-header"), Map.of("%player%", target.getName()))));
        String activeJobs = data.jobs().entrySet().stream()
                .filter(entry -> entry.getValue().isJoined() && entry.getValue().isActive())
                .map(entry -> entry.getKey())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        sender.sendMessage(Text.color(Text.placeholders(languageService.raw("jobs.debug-info-active"), Map.of("%active_jobs%", activeJobs))));
        sender.sendMessage(Text.color(Text.placeholders(languageService.raw("jobs.debug-info-yumaria-fishing"), Map.of(
                "%status%", yumariaFishingHook == null ? "unavailable" : yumariaFishingHook.status()
        ))));
        if (data.jobs().isEmpty()) {
            sender.sendMessage(Text.color(languageService.raw("jobs.debug-info-none")));
            return true;
        }
        for (Map.Entry<String, PlayerJobData> entry : data.jobs().entrySet()) {
            Optional<JobDefinition> job = jobRegistry.get(entry.getKey());
            PlayerJobData jobData = entry.getValue();
            double required = job.map(definition -> progressionService.requiredProgress(definition, jobData)).orElse(0.0D);
            sender.sendMessage(Text.color(Text.placeholders(languageService.raw("jobs.debug-info-line"), Map.of(
                    "%job_id%", entry.getKey(),
                    "%joined%", Boolean.toString(jobData.isJoined()),
                    "%active%", Boolean.toString(jobData.isActive()),
                    "%level%", Integer.toString(jobData.getLevel()),
                    "%progress%", Text.formatNumber(jobData.getProgress()),
                    "%required_progress%", Text.formatNumber(required),
                    "%prestige%", Integer.toString(jobData.getPrestige()),
                    "%rank%", rankService.rankForLevel(jobData.getLevel())
            ))));
        }
        return true;
    }

    private String ensureDiagnosticEligibility(Player target, JobDefinition job) {
        if (!plugin.getConfig().getBoolean("progress-bar.enabled", true)) {
            return "progress-bar.enabled is false";
        }
        PlayerData data = playerDataService.getOrLoad(target);
        PlayerJobData jobData = data.peekJob(job.id());
        if (jobData == null || !jobData.isJoined()) {
            boolean joined = playerJobService.join(target, job);
            plugin.debugProgress("Debug progress auto-join attempt: player=" + target.getName()
                    + ", job=" + job.id()
                    + ", joined=" + joined);
            data = playerDataService.getOrLoad(target);
            jobData = data.peekJob(job.id());
        }
        if (jobData == null) {
            return "job data could not be created for " + job.id();
        }
        if (!jobData.isActive() && !job.allowProgressWhenInactive()) {
            PlayerJobService.ToggleResult toggleResult = playerJobService.toggle(target, job);
            plugin.debugProgress("Debug progress auto-activate attempt: player=" + target.getName()
                    + ", job=" + job.id()
                    + ", result=" + toggleResult);
            if (toggleResult != PlayerJobService.ToggleResult.ACTIVE) {
                return "job " + job.id() + " is not active and could not be activated: " + toggleResult;
            }
        }
        return null;
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (args.length < 4) {
            languageService.send(sender, "commands.usage.yjobs");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            languageService.send(sender, "commands.unknown-player");
            return true;
        }
        Optional<JobDefinition> job = jobRegistry.get(args[3]);
        if (job.isEmpty()) {
            languageService.send(sender, "commands.unknown-job");
            return true;
        }

        switch (action) {
            case "addprogress" -> {
                if (args.length < 5) {
                    languageService.send(sender, "commands.usage.yjobs");
                    return true;
                }
                double amount = parseDouble(sender, args[4]);
                if (Double.isNaN(amount)) {
                    return true;
                }
                String source = args.length >= 6 ? args[5] : "admin";
                progressService.addProgress(target, job.get().id(), amount, source, Map.of("admin", sender.getName()));
                languageService.send(sender, "jobs.progress-added", Map.of("%player%", target.getName()));
            }
            case "setlevel" -> {
                if (args.length < 5) {
                    languageService.send(sender, "commands.usage.yjobs");
                    return true;
                }
                int level = parseInt(sender, args[4]);
                if (level < 0) {
                    return true;
                }
                playerJobService.setLevel(target, job.get(), level);
                languageService.send(sender, "jobs.set-level", Map.of("%player%", target.getName()));
            }
            case "reset" -> {
                playerJobService.reset(target, job.get());
                languageService.send(sender, "jobs.reset", Map.of("%player%", target.getName()));
            }
            default -> languageService.send(sender, "commands.usage.yjobs");
        }
        return true;
    }

    private double parseDouble(CommandSender sender, String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            languageService.send(sender, "commands.invalid-number");
            return Double.NaN;
        }
    }

    private int parseInt(CommandSender sender, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            languageService.send(sender, "commands.invalid-number");
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return startsWith(args[0], List.of("reload", "profile", "xp", "level", "admin", "debug"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("xp")) {
            return startsWith(args[1], List.of("add"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("level")) {
            return startsWith(args[1], List.of("set"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return startsWith(args[1], List.of("addprogress", "setlevel", "reset"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return startsWith(args[1], List.of("progress", "info", "item"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            return startsWith(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug")) {
            return startsWith(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("profile")) {
            return startsWith(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("xp") || args[0].equalsIgnoreCase("level"))) {
            return startsWith(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            return startsWith(args[3], jobRegistry.all().stream().map(JobDefinition::id).toList());
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("xp") || args[0].equalsIgnoreCase("level"))) {
            return startsWith(args[3], jobRegistry.all().stream().map(JobDefinition::id).toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("progress")) {
            return startsWith(args[3], jobRegistry.all().stream().map(JobDefinition::id).toList());
        }
        return List.of();
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
