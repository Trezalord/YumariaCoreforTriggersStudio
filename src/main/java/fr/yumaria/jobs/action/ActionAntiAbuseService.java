package fr.yumaria.jobs.action;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.anticheat.AntiAbuseResult;
import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.util.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionAntiAbuseService {
    private final YumariaJobsPlugin plugin;
    private final Map<String, ActionWindow> windows = new ConcurrentHashMap<>();

    public ActionAntiAbuseService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public AntiAbuseResult validate(UUID playerId, YumariaActionReport report, double xp, double money) {
        if (!plugin.getConfig().getBoolean("anti-abuse.enabled", true)) {
            return AntiAbuseResult.accepted(List.of("anti-abuse disabled"));
        }
        if (playerId == null || report == null) {
            return AntiAbuseResult.rejected("missing action anti-abuse key", List.of());
        }
        String addon = Text.normalizeId(report.addonId());
        String action = Text.normalizeId(report.actionType());
        String basePath = "anti-abuse.actions." + report.addonId() + "." + action + ".";
        String normalizedPath = "anti-abuse.actions." + addon + "." + action + ".";
        String defaults = "anti-abuse.defaults.";
        long now = System.currentTimeMillis();
        long minDelayMs = longConfig(basePath + "min-delay-ms", normalizedPath + "min-delay-ms", defaults + "min-delay-ms", 0L);
        double maxXpPerMinute = doubleConfig(basePath + "max-xp-per-minute", normalizedPath + "max-xp-per-minute", defaults + "max-xp-per-minute", Double.MAX_VALUE);
        double maxMoneyPerMinute = doubleConfig(basePath + "max-money-per-minute", normalizedPath + "max-money-per-minute", defaults + "max-money-per-minute", Double.MAX_VALUE);
        long maxActionsPerMinute = longConfig(basePath + "max-actions-per-minute", normalizedPath + "max-actions-per-minute", defaults + "max-actions-per-minute", Long.MAX_VALUE);

        ActionWindow window = windows.computeIfAbsent(playerId + ":" + addon + ":" + action, ignored -> new ActionWindow());
        synchronized (window) {
            window.prune(now);
            List<String> debug = new ArrayList<>();
            if (minDelayMs > 0L && window.lastAcceptedAt > 0L && now - window.lastAcceptedAt < minDelayMs) {
                debug.add("action min-delay-ms " + minDelayMs + " rejected delta=" + (now - window.lastAcceptedAt));
                return AntiAbuseResult.rejected("action cooldown", debug);
            }
            if (window.countSince(now - 60_000L) + 1L > maxActionsPerMinute) {
                debug.add("max-actions-per-minute " + maxActionsPerMinute + " rejected");
                return AntiAbuseResult.rejected("minute action limit", debug);
            }
            if (window.xpSince(now - 60_000L) + xp > maxXpPerMinute) {
                debug.add("max-xp-per-minute " + maxXpPerMinute + " rejected");
                return AntiAbuseResult.rejected("minute action xp limit", debug);
            }
            if (window.moneySince(now - 60_000L) + money > maxMoneyPerMinute) {
                debug.add("max-money-per-minute " + maxMoneyPerMinute + " rejected");
                return AntiAbuseResult.rejected("minute action money limit", debug);
            }
            double multiplier = diminishingMultiplier(basePath, normalizedPath, defaults, window, now, debug);
            window.accept(now, xp, money);
            return AntiAbuseResult.accepted(multiplier, debug);
        }
    }

    private double diminishingMultiplier(String basePath, String normalizedPath, String defaults, ActionWindow window, long now, List<String> debug) {
        boolean enabled = booleanConfig(basePath + "diminishing-returns.enabled", normalizedPath + "diminishing-returns.enabled", defaults + "diminishing-returns.enabled", false);
        if (!enabled) {
            return 1.0D;
        }
        long windowMs = Math.max(1L, longConfig(basePath + "diminishing-returns.window-seconds", normalizedPath + "diminishing-returns.window-seconds", defaults + "diminishing-returns.window-seconds", 300L)) * 1000L;
        long threshold = Math.max(0L, longConfig(basePath + "diminishing-returns.threshold-actions", normalizedPath + "diminishing-returns.threshold-actions", defaults + "diminishing-returns.threshold-actions", 120L));
        double reductionPerExtra = Math.max(0.0D, doubleConfig(basePath + "diminishing-returns.reduction-per-extra-action", normalizedPath + "diminishing-returns.reduction-per-extra-action", defaults + "diminishing-returns.reduction-per-extra-action", 0.01D));
        double maxReduction = Math.max(0.0D, Math.min(1.0D, doubleConfig(basePath + "diminishing-returns.max-reduction", normalizedPath + "diminishing-returns.max-reduction", defaults + "diminishing-returns.max-reduction", 0.50D)));
        long actions = window.countSince(now - windowMs);
        if (actions <= threshold) {
            return 1.0D;
        }
        double reduction = Math.min(maxReduction, (actions - threshold) * reductionPerExtra);
        double multiplier = Math.max(0.0D, 1.0D - reduction);
        debug.add("action diminishing returns actions=" + actions + " multiplier=" + multiplier);
        return multiplier;
    }

    private boolean booleanConfig(String primary, String secondary, String fallbackPath, boolean fallback) {
        if (plugin.getConfig().contains(primary)) {
            return plugin.getConfig().getBoolean(primary, fallback);
        }
        if (plugin.getConfig().contains(secondary)) {
            return plugin.getConfig().getBoolean(secondary, fallback);
        }
        return plugin.getConfig().getBoolean(fallbackPath, fallback);
    }

    private long longConfig(String primary, String secondary, String fallbackPath, long fallback) {
        if (plugin.getConfig().contains(primary)) {
            return plugin.getConfig().getLong(primary, fallback);
        }
        if (plugin.getConfig().contains(secondary)) {
            return plugin.getConfig().getLong(secondary, fallback);
        }
        return plugin.getConfig().getLong(fallbackPath, fallback);
    }

    private double doubleConfig(String primary, String secondary, String fallbackPath, double fallback) {
        if (plugin.getConfig().contains(primary)) {
            return plugin.getConfig().getDouble(primary, fallback);
        }
        if (plugin.getConfig().contains(secondary)) {
            return plugin.getConfig().getDouble(secondary, fallback);
        }
        return plugin.getConfig().getDouble(fallbackPath, fallback);
    }

    private static final class ActionWindow {
        private final Deque<Entry> entries = new ArrayDeque<>();
        private long lastAcceptedAt;

        private void accept(long timestamp, double xp, double money) {
            entries.addLast(new Entry(timestamp, xp, money));
            lastAcceptedAt = timestamp;
        }

        private void prune(long now) {
            long cutoff = now - 3_600_000L;
            while (!entries.isEmpty() && entries.peekFirst().timestamp < cutoff) {
                entries.removeFirst();
            }
        }

        private long countSince(long cutoff) {
            return entries.stream().filter(entry -> entry.timestamp >= cutoff).count();
        }

        private double xpSince(long cutoff) {
            return entries.stream().filter(entry -> entry.timestamp >= cutoff).mapToDouble(Entry::xp).sum();
        }

        private double moneySince(long cutoff) {
            return entries.stream().filter(entry -> entry.timestamp >= cutoff).mapToDouble(Entry::money).sum();
        }
    }

    private record Entry(long timestamp, double xp, double money) {
    }
}
