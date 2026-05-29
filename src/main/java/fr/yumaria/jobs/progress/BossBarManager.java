package fr.yumaria.jobs.progress;

// Repere fichier YumariaJobs: progression, niveaux et feedback visuel (BossBarManager).

import fr.yumaria.jobs.YumariaJobsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Role YumariaJobs: Gere XP, niveaux, prestige et feedback visuel de progression.
public final class BossBarManager {
    private final YumariaJobsPlugin plugin;
    private final Map<UUID, BossBarSession> sessions = new ConcurrentHashMap<>();
    private final Set<String> warnedValues = new HashSet<>();

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public BossBarManager(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void showProgress(Player player, ProgressSnapshot snapshot, String title) {
        if (!Bukkit.isPrimaryThread()) {
            plugin.debugBossbar("BossBarManager.showProgress off main thread; rescheduling player=" + (player == null ? "-" : player.getName())
                    + ", job=" + snapshot.jobId());
            Bukkit.getScheduler().runTask(plugin, () -> showProgress(player, snapshot, title));
            return;
        }
        if (player == null || !player.isOnline()) {
            plugin.debugBossbar("BossBarManager.showProgress ignored: player offline/null job=" + snapshot.jobId());
            return;
        }

        UUID uuid = player.getUniqueId();
        BossBarSession session = sessions.get(uuid);
        if (session == null) {
            BossBar bossBar = Bukkit.createBossBar(title, color(), style());
            bossBar.addPlayer(player);
            session = new BossBarSession(snapshot.jobId(), bossBar);
            sessions.put(uuid, session);
            plugin.debugBossbar("Created bossbar: player=" + player.getName()
                    + ", job=" + snapshot.jobId()
                    + ", title=" + title
                    + ", progressRatio=" + snapshot.progressRatio());
        } else {
            session.jobId = snapshot.jobId();
            if (!session.bossBar.getPlayers().contains(player)) {
                session.bossBar.addPlayer(player);
            }
            plugin.debugBossbar("Updated bossbar: player=" + player.getName()
                    + ", job=" + snapshot.jobId()
                    + ", title=" + title
                    + ", progressRatio=" + snapshot.progressRatio());
        }

        session.bossBar.setTitle(title);
        session.bossBar.setColor(color());
        session.bossBar.setStyle(style());
        session.bossBar.setProgress(snapshot.progressRatio());
        refreshHideTask(uuid, session);
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void remove(Player player) {
        if (player != null) {
            remove(player.getUniqueId(), "player removal");
        }
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void remove(UUID uuid) {
        remove(uuid, "hide timer");
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void remove(UUID uuid, String reason) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> remove(uuid, reason));
            return;
        }
        BossBarSession session = sessions.remove(uuid);
        if (session == null) {
            plugin.debugBossbar("Remove bossbar skipped: no active session uuid=" + uuid + ", reason=" + reason);
            return;
        }
        if (session.hideTask != null) {
            session.hideTask.cancel();
        }
        session.bossBar.removeAll();
        plugin.debugBossbar("Removed bossbar: uuid=" + uuid
                + ", job=" + session.jobId
                + ", reason=" + reason);
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    public void removeAll() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::removeAll);
            return;
        }
        plugin.debugBossbar("Removing all bossbars: count=" + sessions.size());
        for (BossBarSession session : sessions.values()) {
            if (session.hideTask != null) {
                session.hideTask.cancel();
            }
            session.bossBar.removeAll();
        }
        sessions.clear();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void refreshHideTask(UUID uuid, BossBarSession session) {
        if (session.hideTask != null) {
            session.hideTask.cancel();
        }
        long durationSeconds = Math.max(1L, plugin.getConfig().getLong("progress-bar.display-duration-seconds", 8L));
        session.hideTask = Bukkit.getScheduler().runTaskLater(plugin, () -> remove(uuid, "hide timer expired"), durationSeconds * 20L);
        plugin.debugBossbar("Scheduled bossbar hide: uuid=" + uuid
                + ", job=" + session.jobId
                + ", seconds=" + durationSeconds);
    }

    // Annotation YumariaJobs: Formate ou normalise du texte pour affichage, commandes ou recherche.
    private BarColor color() {
        String configured = plugin.getConfig().getString("progress-bar.color", "PURPLE");
        try {
            return BarColor.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            warnOnce("color:" + configured, "Invalid progress-bar.color '" + configured + "'. Falling back to PURPLE.");
            return BarColor.PURPLE;
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private BarStyle style() {
        String configured = plugin.getConfig().getString("progress-bar.style", "SEGMENTED_20");
        try {
            return BarStyle.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            warnOnce("style:" + configured, "Invalid progress-bar.style '" + configured + "'. Falling back to SEGMENTED_20/SOLID.");
            return fallbackStyle();
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private BarStyle fallbackStyle() {
        try {
            return BarStyle.valueOf("SEGMENTED_20");
        } catch (IllegalArgumentException exception) {
            warnOnce("style:SEGMENTED_20", "SEGMENTED_20 is unavailable in this server API. Falling back to SOLID.");
            return BarStyle.SOLID;
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void warnOnce(String key, String message) {
        if (warnedValues.add(key)) {
            plugin.getLogger().warning(message);
        }
    }

    private static final class BossBarSession {
        private String jobId;
        private final BossBar bossBar;
        private BukkitTask hideTask;

        // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
        private BossBarSession(String jobId, BossBar bossBar) {
            this.jobId = jobId;
            this.bossBar = bossBar;
        }
    }
}
