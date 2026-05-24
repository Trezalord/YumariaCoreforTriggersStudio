package fr.yumaria.jobs.anticheat;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.util.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressionAntiAbuseService {
    private final YumariaJobsPlugin plugin;
    private final Map<String, SourceWindow> windows = new ConcurrentHashMap<>();

    public ProgressionAntiAbuseService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public AntiAbuseResult validate(UUID playerId, String jobId, String source, double finalXp, Map<String, Object> context) {
        if (!plugin.getConfig().getBoolean("anti-abuse.enabled", true)) {
            return AntiAbuseResult.accepted(List.of("anti-abuse disabled"));
        }
        if (playerId == null || jobId == null || source == null) {
            return AntiAbuseResult.rejected("missing anti-abuse key", List.of());
        }

        String normalizedJob = Text.normalizeId(jobId);
        String normalizedSource = Text.normalizeId(source);
        String basePath = "anti-abuse.jobs." + normalizedJob + ".sources." + normalizedSource + ".";
        String fallbackPath = "anti-abuse.defaults.";
        long now = System.currentTimeMillis();
        long minDelayMs = longConfig(basePath + "min-delay-ms", fallbackPath + "min-delay-ms", 0L);
        double maxPerMinute = doubleConfig(basePath + "max-xp-per-minute", fallbackPath + "max-xp-per-minute", Double.MAX_VALUE);
        double maxPerHour = doubleConfig(basePath + "max-xp-per-hour", fallbackPath + "max-xp-per-hour", Double.MAX_VALUE);

        SourceWindow window = windows.computeIfAbsent(playerId + ":" + normalizedJob + ":" + normalizedSource, ignored -> new SourceWindow());
        synchronized (window) {
            window.prune(now);
            List<String> debug = new ArrayList<>();
            if (minDelayMs > 0L && window.lastAcceptedAt > 0L && now - window.lastAcceptedAt < minDelayMs) {
                debug.add("min-delay-ms " + minDelayMs + " rejected delta=" + (now - window.lastAcceptedAt));
                return AntiAbuseResult.rejected("source cooldown", debug);
            }
            if (window.sumSince(now - 60_000L) + finalXp > maxPerMinute) {
                debug.add("max-xp-per-minute " + maxPerMinute + " rejected");
                return AntiAbuseResult.rejected("minute xp limit", debug);
            }
            if (window.sumSince(now - 3_600_000L) + finalXp > maxPerHour) {
                debug.add("max-xp-per-hour " + maxPerHour + " rejected");
                return AntiAbuseResult.rejected("hour xp limit", debug);
            }

            double multiplier = diminishingMultiplier(basePath, fallbackPath, window, now, debug);
            String fingerprint = fingerprint(context);
            if (!fingerprint.isBlank() && fingerprint.equals(window.lastFingerprint)) {
                window.repeatedIdenticalActions++;
            } else {
                window.repeatedIdenticalActions = 0;
                window.lastFingerprint = fingerprint;
            }
            long burstThreshold = longConfig(basePath + "suspicious-burst-actions", fallbackPath + "suspicious-burst-actions", 0L);
            if (burstThreshold > 0L && window.repeatedIdenticalActions >= burstThreshold) {
                debug.add("suspicious repeated identical actions=" + window.repeatedIdenticalActions);
            }
            window.accept(now, finalXp);
            return AntiAbuseResult.accepted(multiplier, debug);
        }
    }

    private double diminishingMultiplier(String basePath, String fallbackPath, SourceWindow window, long now, List<String> debug) {
        boolean enabled = booleanConfig(basePath + "diminishing-returns.enabled", fallbackPath + "diminishing-returns.enabled", false);
        if (!enabled) {
            return 1.0D;
        }
        long windowMs = Math.max(1L, longConfig(basePath + "diminishing-returns.window-seconds", fallbackPath + "diminishing-returns.window-seconds", 300L)) * 1000L;
        long threshold = Math.max(0L, longConfig(basePath + "diminishing-returns.threshold-actions", fallbackPath + "diminishing-returns.threshold-actions", 120L));
        double reductionPerExtra = Math.max(0.0D, doubleConfig(basePath + "diminishing-returns.reduction-per-extra-action", fallbackPath + "diminishing-returns.reduction-per-extra-action", 0.01D));
        double maxReduction = Math.max(0.0D, Math.min(1.0D, doubleConfig(basePath + "diminishing-returns.max-reduction", fallbackPath + "diminishing-returns.max-reduction", 0.50D)));
        long actions = window.countSince(now - windowMs);
        if (actions <= threshold) {
            return 1.0D;
        }
        double reduction = Math.min(maxReduction, (actions - threshold) * reductionPerExtra);
        double multiplier = Math.max(0.0D, 1.0D - reduction);
        debug.add("diminishing returns actions=" + actions + " multiplier=" + multiplier);
        return multiplier;
    }

    private boolean booleanConfig(String path, String fallbackPath, boolean fallback) {
        return plugin.getConfig().contains(path) ? plugin.getConfig().getBoolean(path, fallback) : plugin.getConfig().getBoolean(fallbackPath, fallback);
    }

    private long longConfig(String path, String fallbackPath, long fallback) {
        return plugin.getConfig().contains(path) ? plugin.getConfig().getLong(path, fallback) : plugin.getConfig().getLong(fallbackPath, fallback);
    }

    private double doubleConfig(String path, String fallbackPath, double fallback) {
        return plugin.getConfig().contains(path) ? plugin.getConfig().getDouble(path, fallback) : plugin.getConfig().getDouble(fallbackPath, fallback);
    }

    private String fingerprint(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        Map<String, Object> reduced = new HashMap<>();
        for (String key : List.of("action_type", "material", "entity_type", "fish_species", "species", "rarity", "quality")) {
            Object value = context.get(key);
            if (value != null) {
                reduced.put(key.toLowerCase(Locale.ROOT), value);
            }
        }
        return reduced.toString();
    }

    private static final class SourceWindow {
        private final Deque<Entry> entries = new ArrayDeque<>();
        private long lastAcceptedAt;
        private String lastFingerprint = "";
        private long repeatedIdenticalActions;

        private void accept(long timestamp, double xp) {
            entries.addLast(new Entry(timestamp, xp));
            lastAcceptedAt = timestamp;
        }

        private void prune(long now) {
            long cutoff = now - 3_600_000L;
            while (!entries.isEmpty() && entries.peekFirst().timestamp < cutoff) {
                entries.removeFirst();
            }
        }

        private double sumSince(long cutoff) {
            double sum = 0.0D;
            for (Entry entry : entries) {
                if (entry.timestamp >= cutoff) {
                    sum += entry.xp;
                }
            }
            return sum;
        }

        private long countSince(long cutoff) {
            long count = 0L;
            Iterator<Entry> iterator = entries.descendingIterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                if (entry.timestamp < cutoff) {
                    break;
                }
                count++;
            }
            return count;
        }
    }

    private record Entry(long timestamp, double xp) {
    }
}
