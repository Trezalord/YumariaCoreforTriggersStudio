package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionConfig).

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
final class ActionConfig {
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private ActionConfig() {
    }

    static ConfigurationSection section(FileConfiguration configuration, String path) {
        ConfigurationSection direct = configuration.getConfigurationSection(path);
        if (direct != null) {
            return direct;
        }
        int index = path.lastIndexOf('.');
        if (index < 0) {
            return configuration.getConfigurationSection(path);
        }
        ConfigurationSection parent = section(configuration, path.substring(0, index));
        if (parent == null) {
            return null;
        }
        String wanted = path.substring(index + 1);
        for (String key : parent.getKeys(false)) {
            if (key.equalsIgnoreCase(wanted)) {
                return parent.getConfigurationSection(key);
            }
        }
        return null;
    }

    static boolean booleanValue(FileConfiguration configuration, String path, boolean fallback) {
        if (configuration.contains(path)) {
            return configuration.getBoolean(path, fallback);
        }
        int index = path.lastIndexOf('.');
        if (index < 0) {
            return fallback;
        }
        ConfigurationSection parent = section(configuration, path.substring(0, index));
        if (parent == null) {
            return fallback;
        }
        String wanted = path.substring(index + 1);
        for (String key : parent.getKeys(false)) {
            if (key.equalsIgnoreCase(wanted)) {
                return parent.getBoolean(key, fallback);
            }
        }
        return fallback;
    }

    static double doubleValue(FileConfiguration configuration, String path, double fallback) {
        if (configuration.contains(path)) {
            return configuration.getDouble(path, fallback);
        }
        int index = path.lastIndexOf('.');
        if (index < 0) {
            return fallback;
        }
        ConfigurationSection parent = section(configuration, path.substring(0, index));
        if (parent == null) {
            return fallback;
        }
        String wanted = path.substring(index + 1);
        for (String key : parent.getKeys(false)) {
            if (key.equalsIgnoreCase(wanted)) {
                return parent.getDouble(key, fallback);
            }
        }
        return fallback;
    }

    static long longValue(FileConfiguration configuration, String path, long fallback) {
        if (configuration.contains(path)) {
            return configuration.getLong(path, fallback);
        }
        int index = path.lastIndexOf('.');
        if (index < 0) {
            return fallback;
        }
        ConfigurationSection parent = section(configuration, path.substring(0, index));
        if (parent == null) {
            return fallback;
        }
        String wanted = path.substring(index + 1);
        for (String key : parent.getKeys(false)) {
            if (key.equalsIgnoreCase(wanted)) {
                return parent.getLong(key, fallback);
            }
        }
        return fallback;
    }
}
