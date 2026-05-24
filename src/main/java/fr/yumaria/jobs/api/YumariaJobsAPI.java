package fr.yumaria.jobs.api;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class YumariaJobsAPI {
    private static final AtomicReference<YumariaJobsProvider> PROVIDER = new AtomicReference<>();

    private YumariaJobsAPI() {
    }

    public static void setProvider(YumariaJobsProvider provider) {
        PROVIDER.set(provider);
    }

    public static void clearProvider(YumariaJobsProvider provider) {
        PROVIDER.compareAndSet(provider, null);
    }

    public static void addProgress(Player player, String jobId, double amount, String source) {
        addProgress(player, jobId, amount, source, Map.of());
    }

    public static void addProgress(Player player, String jobId, double amount, String source, Map<String, Object> context) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return;
        }
        try {
            provider.addProgress(player, jobId, amount, source, context == null ? Map.of() : context);
        } catch (RuntimeException exception) {
            // API calls must never crash custom Yumaria plugins.
        }
    }

    public static int getLevel(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0;
        }
        try {
            return provider.getLevel(player, jobId);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    public static double getProgress(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0.0D;
        }
        try {
            return provider.getProgress(player, jobId);
        } catch (RuntimeException exception) {
            return 0.0D;
        }
    }

    public static double getRequiredProgress(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0.0D;
        }
        try {
            return provider.getRequiredProgress(player, jobId);
        } catch (RuntimeException exception) {
            return 0.0D;
        }
    }

    public static int getPrestige(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return 0;
        }
        try {
            return provider.getPrestige(player, jobId);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    public static boolean hasJob(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return false;
        }
        try {
            return provider.hasJob(player, jobId);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean isJobActive(Player player, String jobId) {
        YumariaJobsProvider provider = PROVIDER.get();
        if (provider == null || player == null) {
            return false;
        }
        try {
            return provider.isJobActive(player, jobId);
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
