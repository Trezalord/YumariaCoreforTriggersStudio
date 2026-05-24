package fr.yumaria.jobs.data;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class PlayerDataService {
    private final YumariaJobsPlugin plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> pendingSaveTasks = new ConcurrentHashMap<>();
    private final ExecutorService saveExecutor;
    private final File playersFolder;

    public PlayerDataService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "data/players");
        this.saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "YumariaJobs-PlayerData");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        playersFolder.mkdirs();
    }

    public PlayerData getOrLoad(Player player) {
        PlayerData data = getOrLoad(player.getUniqueId(), player.getName());
        data.setName(player.getName());
        return data;
    }

    public PlayerData getOrLoad(OfflinePlayer player) {
        String name = player.getName() == null ? player.getUniqueId().toString() : player.getName();
        return getOrLoad(player.getUniqueId(), name);
    }

    public PlayerData getOrLoad(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, ignored -> loadFromDisk(uuid, name));
    }

    public void markDirty(PlayerData data) {
        dirty.add(data.uuid());
        scheduleSave(data.uuid());
    }

    public void saveDirtyAsync() {
        for (UUID uuid : List.copyOf(dirty)) {
            PlayerData data = cache.get(uuid);
            if (data != null) {
                dirty.remove(uuid);
                submitSave(data.copy());
            }
        }
    }

    public void saveAsync(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            return;
        }
        dirty.remove(uuid);
        submitSave(data.copy());
    }

    public void unload(UUID uuid) {
        saveAsync(uuid);
        cache.remove(uuid);
        Integer taskId = pendingSaveTasks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void saveAllBlocking() {
        for (Integer taskId : pendingSaveTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingSaveTasks.clear();

        List<Future<?>> futures = new ArrayList<>();
        for (PlayerData data : cache.values()) {
            futures.add(saveExecutor.submit(() -> writeSnapshot(data.copy())));
        }
        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not finish saving player data: " + exception.getMessage());
            }
        }
        dirty.clear();
        saveExecutor.shutdown();
        try {
            saveExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public List<LeaderboardEntry> leaderboard(String jobId, int limit) {
        Map<UUID, LeaderboardEntry> entries = new HashMap<>();
        File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                LeaderboardEntry entry = readLeaderboardEntry(file, jobId);
                if (entry != null) {
                    entries.put(entry.uuid(), entry);
                }
            }
        }
        for (PlayerData data : cache.values()) {
            PlayerJobData jobData = data.peekJob(jobId);
            if (jobData != null && jobData.isJoined()) {
                entries.put(data.uuid(), new LeaderboardEntry(data.uuid(), data.name(), jobData.getLevel(), jobData.getPrestige(), jobData.getTotalProgress()));
            }
        }
        return entries.values().stream()
                .sorted(Comparator.comparingInt(LeaderboardEntry::prestige).reversed()
                        .thenComparing(Comparator.comparingInt(LeaderboardEntry::level).reversed())
                        .thenComparing(Comparator.comparingDouble(LeaderboardEntry::totalProgress).reversed()))
                .limit(limit)
                .toList();
    }

    private void scheduleSave(UUID uuid) {
        if (pendingSaveTasks.containsKey(uuid)) {
            return;
        }
        long delay = Math.max(1L, plugin.getConfig().getLong("progress.save-delay-ticks", 40L));
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            pendingSaveTasks.remove(uuid);
            if (dirty.remove(uuid)) {
                PlayerData data = cache.get(uuid);
                if (data != null) {
                    submitSave(data.copy());
                }
            }
        }, delay);
        pendingSaveTasks.put(uuid, taskId);
    }

    private PlayerData loadFromDisk(UUID uuid, String fallbackName) {
        File file = fileFor(uuid);
        if (!file.isFile()) {
            return new PlayerData(uuid, fallbackName);
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        String name = configuration.getString("name", fallbackName);
        Map<String, PlayerJobData> jobs = new HashMap<>();
        ConfigurationSection jobsSection = configuration.getConfigurationSection("jobs");
        if (jobsSection != null) {
            for (String jobId : jobsSection.getKeys(false)) {
                ConfigurationSection section = jobsSection.getConfigurationSection(jobId);
                if (section == null) {
                    continue;
                }
                jobs.put(Text.normalizeId(jobId), new PlayerJobData(
                        section.getBoolean("joined", false),
                        section.getBoolean("active", false),
                        section.getInt("level", 1),
                        section.getDouble("progress", 0.0D),
                        section.getDouble("total-progress", 0.0D),
                        section.getInt("prestige", 0),
                        section.getDouble("points", 0.0D)
                ));
            }
        }
        return new PlayerData(uuid, name, jobs);
    }

    private LeaderboardEntry readLeaderboardEntry(File file, String jobId) {
        try {
            UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = configuration.getConfigurationSection("jobs." + jobId);
            if (section == null || !section.getBoolean("joined", false)) {
                return null;
            }
            String name = configuration.getString("name", uuid.toString());
            return new LeaderboardEntry(
                    uuid,
                    name,
                    section.getInt("level", 1),
                    section.getInt("prestige", 0),
                    section.getDouble("total-progress", 0.0D)
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void submitSave(PlayerData snapshot) {
        saveExecutor.submit(() -> writeSnapshot(snapshot));
    }

    private void writeSnapshot(PlayerData snapshot) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("name", snapshot.name());
        for (Map.Entry<String, PlayerJobData> entry : snapshot.jobs().entrySet()) {
            String path = "jobs." + entry.getKey() + ".";
            PlayerJobData jobData = entry.getValue();
            configuration.set(path + "joined", jobData.isJoined());
            configuration.set(path + "active", jobData.isActive());
            configuration.set(path + "level", jobData.getLevel());
            configuration.set(path + "progress", jobData.getProgress());
            configuration.set(path + "total-progress", jobData.getTotalProgress());
            configuration.set(path + "prestige", jobData.getPrestige());
            configuration.set(path + "points", jobData.getPoints());
        }
        try {
            playersFolder.mkdirs();
            configuration.save(fileFor(snapshot.uuid()));
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save data for " + snapshot.uuid() + ": " + exception.getMessage());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(playersFolder, uuid + ".yml");
    }
}
