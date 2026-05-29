package fr.yumaria.jobs.progress;

// Repere fichier YumariaJobs: progression, niveaux et feedback visuel (ProgressBarService).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.config.RankService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// Role YumariaJobs: Gere XP, niveaux, prestige et feedback visuel de progression.
public final class ProgressBarService {
    private final YumariaJobsPlugin plugin;
    private final ProgressionService progressionService;
    private final RankService rankService;
    private final ProgressFormatter formatter;
    private final BossBarManager bossBarManager;

    public ProgressBarService(
            YumariaJobsPlugin plugin,
            ProgressionService progressionService,
            RankService rankService,
            ProgressFormatter formatter,
            BossBarManager bossBarManager
    ) {
        this.plugin = plugin;
        this.progressionService = progressionService;
        this.rankService = rankService;
        this.formatter = formatter;
        this.bossBarManager = bossBarManager;
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void start() {
        // Event-driven only. Bossbar hiding is handled by each BossBarSession.
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void stop() {
        bossBarManager.removeAll();
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void showProgress(Player player, JobDefinition job, PlayerJobData data) {
        if (!Bukkit.isPrimaryThread()) {
            plugin.debugBossbar("showProgress requested off main thread; rescheduling player=" + (player == null ? "-" : player.getName())
                    + ", job=" + (job == null ? "-" : job.id()));
            Bukkit.getScheduler().runTask(plugin, () -> showProgress(player, job, data));
            return;
        }
        boolean enabled = plugin.getConfig().getBoolean("progress-bar.enabled", true);
        if (!enabled) {
            plugin.debugBossbar("showProgress skipped: progress-bar.enabled=false player=" + player.getName()
                    + ", job=" + job.id());
            return;
        }

        double required = progressionService.requiredProgress(job, data);
        String rank = rankService.rankForLevel(data.getLevel());
        ProgressSnapshot snapshot = ProgressSnapshot.of(job, data, required, rank);
        String title = formatter.formatTitle(snapshot);
        plugin.debugBossbar("showProgress: player=" + player.getName()
                + ", job=" + job.id()
                + ", level=" + snapshot.level()
                + ", progress=" + snapshot.currentProgress()
                + ", required=" + snapshot.requiredProgress()
                + ", percent=" + snapshot.percent()
                + ", enabled=" + enabled
                + ", actionbarFallback=" + plugin.getConfig().getBoolean("progress-bar.use-actionbar-fallback", false));

        if (plugin.getConfig().getBoolean("progress-bar.use-actionbar-fallback", false)) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(title));
            plugin.debugBossbar("showProgress sent actionbar fallback: player=" + player.getName()
                    + ", title=" + title);
            return;
        }

        bossBarManager.showProgress(player, snapshot, title);
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void remove(Player player) {
        remove(player, "player removal");
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void remove(Player player, String reason) {
        if (player != null) {
            bossBarManager.remove(player.getUniqueId(), reason);
        }
    }
}
