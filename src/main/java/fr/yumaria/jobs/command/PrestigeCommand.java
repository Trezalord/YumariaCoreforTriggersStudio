package fr.yumaria.jobs.command;

// Repere fichier YumariaJobs: commande joueur ou admin du plugin (PrestigeCommand).

import fr.yumaria.jobs.config.LanguageService;
import fr.yumaria.jobs.progress.JobProgressService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

// Role YumariaJobs: Traite les commandes joueur et administration du plugin.
public final class PrestigeCommand implements CommandExecutor, TabCompleter {
    private final JobProgressService progressService;
    private final LanguageService languageService;
    private final java.util.function.Supplier<List<String>> jobIds;

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public PrestigeCommand(JobProgressService progressService, LanguageService languageService, java.util.function.Supplier<List<String>> jobIds) {
        this.progressService = progressService;
        this.languageService = languageService;
        this.jobIds = jobIds;
    }

    @Override
    // Annotation YumariaJobs: Traite une commande ou ses suggestions.
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            languageService.send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission("yumariajobs.prestige")) {
            languageService.send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 1) {
            languageService.send(sender, "commands.usage.prestige");
            return true;
        }
        JobProgressService.PrestigeResult result = progressService.prestige(player, args[0]);
        switch (result) {
            case DISABLED -> languageService.send(sender, "prestige.disabled");
            case UNKNOWN_JOB -> languageService.send(sender, "commands.unknown-job");
            case NOT_JOINED -> languageService.send(sender, "jobs.not-joined");
            case REQUIRE_MAX_LEVEL -> languageService.send(sender, "prestige.require-max-level");
            case MAX_PRESTIGE -> languageService.send(sender, "prestige.max");
            case CANCELLED, SUCCESS -> {
            }
        }
        return true;
    }

    @Override
    // Annotation YumariaJobs: Traite une commande ou ses suggestions.
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String token = args[0].toLowerCase(java.util.Locale.ROOT);
            return jobIds.get().stream().filter(jobId -> jobId.startsWith(token)).toList();
        }
        return List.of();
    }
}
