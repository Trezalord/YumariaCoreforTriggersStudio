package fr.yumaria.jobs.config;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.job.IconDefinition;
import fr.yumaria.jobs.job.JobActionDefinition;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.RewardDefinition;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class JobRegistry {
    private final YumariaJobsPlugin plugin;
    private Map<String, JobDefinition> jobs = Map.of();

    public JobRegistry(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        saveDefaultJobs();
        Map<String, JobDefinition> loaded = new HashMap<>();
        loadFile(new File(plugin.getDataFolder(), "jobs.yml"), loaded);

        File jobsFolder = new File(plugin.getDataFolder(), "jobs");
        if (!jobsFolder.isDirectory()) {
            jobsFolder.mkdirs();
        }
        File[] files = jobsFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files != null) {
            for (File file : files) {
                loadFile(file, loaded);
            }
        }
        jobs = Map.copyOf(loaded);
        plugin.debug("Loaded " + jobs.size() + " jobs.");
    }

    public Optional<JobDefinition> get(String id) {
        String normalizedId = Text.normalizeId(id);
        JobDefinition direct = jobs.get(normalizedId);
        if (direct != null) {
            return Optional.of(direct);
        }

        String lookup = Text.normalizeLookup(id);
        for (JobDefinition job : jobs.values()) {
            if (Text.normalizeLookup(job.id()).equals(lookup) || Text.normalizeLookup(job.displayName()).equals(lookup)) {
                return Optional.of(job);
            }
        }
        return Optional.empty();
    }

    public Collection<JobDefinition> all() {
        return jobs.values();
    }

    public boolean exists(String id) {
        return jobs.containsKey(Text.normalizeId(id));
    }

    private void saveDefaultJobs() {
        File target = new File(plugin.getDataFolder(), "jobs.yml");
        if (!target.isFile()) {
            plugin.saveResource("jobs.yml", false);
        }
    }

    private void loadFile(File file, Map<String, JobDefinition> target) {
        if (!file.isFile()) {
            return;
        }
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection jobsSection = configuration.getConfigurationSection("jobs");
        if (jobsSection != null) {
            for (String id : jobsSection.getKeys(false)) {
                JobDefinition definition = parseJob(Text.normalizeId(id), jobsSection.getConfigurationSection(id));
                if (definition != null) {
                    target.put(definition.id(), definition);
                }
            }
            return;
        }

        for (String id : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(id);
            JobDefinition definition = parseJob(Text.normalizeId(id), section);
            if (definition != null) {
                target.put(definition.id(), definition);
            }
        }
    }

    private JobDefinition parseJob(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        Material material = Material.matchMaterial(section.getString("icon.material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        IconDefinition icon = new IconDefinition(material, section.getString("icon.itemsadder-id", ""));
        Map<String, JobActionDefinition> actions = parseActions(section.getConfigurationSection("actions"));
        RewardDefinition defaultReward = parseReward(section.getConfigurationSection("rewards.default"));
        Map<Integer, RewardDefinition> levelRewards = parseLevelRewards(section.getConfigurationSection("rewards.levels"));

        return new JobDefinition(
                id,
                section.getBoolean("enabled", true),
                section.getString("display-name", id),
                section.getStringList("description"),
                icon,
                section.getInt("max-level", 100),
                section.getString("required-progress", "6 * (%level% ^ 2)"),
                section.getString("points-rewarded", "1 * %level%"),
                section.getBoolean("allow-progress-when-inactive", false),
                actions,
                defaultReward,
                levelRewards
        );
    }

    private Map<String, JobActionDefinition> parseActions(ConfigurationSection section) {
        Map<String, JobActionDefinition> actions = new HashMap<>();
        if (section == null) {
            return actions;
        }
        for (String source : section.getKeys(false)) {
            ConfigurationSection actionSection = section.getConfigurationSection(source);
            if (actionSection == null) {
                continue;
            }
            actions.put(Text.normalizeId(source), new JobActionDefinition(
                    actionSection.getBoolean("enabled", true),
                    actionSection.getDouble("progress", 1.0D),
                    actionSection.getLong("cooldown-ms", 0L)
            ));
        }
        return actions;
    }

    private Map<Integer, RewardDefinition> parseLevelRewards(ConfigurationSection section) {
        Map<Integer, RewardDefinition> rewards = new TreeMap<>();
        if (section == null) {
            return rewards;
        }
        for (String key : section.getKeys(false)) {
            try {
                rewards.put(Integer.parseInt(key), parseReward(section.getConfigurationSection(key)));
            } catch (NumberFormatException exception) {
                plugin.debug("Ignoring invalid level reward key: " + key);
            }
        }
        return rewards;
    }

    private RewardDefinition parseReward(ConfigurationSection section) {
        if (section == null) {
            return RewardDefinition.empty();
        }
        String money = section.getString("money", "0");
        List<String> commands = section.getStringList("commands");
        return new RewardDefinition(money, commands);
    }
}
