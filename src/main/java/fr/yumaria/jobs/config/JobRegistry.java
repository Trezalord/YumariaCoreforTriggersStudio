package fr.yumaria.jobs.config;

// Repere fichier YumariaJobs: chargement et lecture des fichiers de configuration (JobRegistry).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.job.IconDefinition;
import fr.yumaria.jobs.job.JobActionDefinition;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.JobSourceDefinition;
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

// Role YumariaJobs: Charge les fichiers YAML et resout les definitions configurables.
public final class JobRegistry {
    private final YumariaJobsPlugin plugin;
    private Map<String, JobDefinition> jobs = Map.of();

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public JobRegistry(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    // Annotation YumariaJobs: Recharge la configuration sans effacer les donnees joueur en memoire.
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

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Collection<JobDefinition> all() {
        return jobs.values();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean exists(String id) {
        return jobs.containsKey(Text.normalizeId(id));
    }

    // Annotation YumariaJobs: Prepare ou execute la sauvegarde des donnees sans bloquer inutilement le serveur.
    private void saveDefaultJobs() {
        File target = new File(plugin.getDataFolder(), "jobs.yml");
        if (!target.isFile()) {
            plugin.saveResource("jobs.yml", false);
        }
    }

    // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
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

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
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
        Map<String, JobSourceDefinition> sources = parseSources(section.getConfigurationSection("sources"));
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
                section.getDouble("xp.multiplier", section.getDouble("xp-multiplier", 1.0D)),
                actions,
                sources,
                defaultReward,
                levelRewards
        );
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
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

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    private Map<String, JobSourceDefinition> parseSources(ConfigurationSection section) {
        Map<String, JobSourceDefinition> sources = new HashMap<>();
        if (section == null) {
            return sources;
        }
        for (String source : section.getKeys(false)) {
            ConfigurationSection sourceSection = section.getConfigurationSection(source);
            if (sourceSection == null) {
                continue;
            }
            sources.put(Text.normalizeId(source), new JobSourceDefinition(
                    sourceSection.getBoolean("enabled", true),
                    sourceSection.getDouble("multiplier", 1.0D)
            ));
        }
        return sources;
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
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

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    private RewardDefinition parseReward(ConfigurationSection section) {
        if (section == null) {
            return RewardDefinition.empty();
        }
        String money = section.getString("money", "0");
        List<String> commands = section.getStringList("commands");
        return new RewardDefinition(money, commands);
    }
}
