package fr.yumaria.jobs.job;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import org.bukkit.entity.Player;

public final class PlayerJobService {
    public enum ToggleResult {
        ACTIVE,
        INACTIVE,
        NOT_JOINED,
        ACTIVE_LIMIT
    }

    private final YumariaJobsPlugin plugin;
    private final PlayerDataService playerDataService;

    public PlayerJobService(YumariaJobsPlugin plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
    }

    public boolean join(Player player, JobDefinition job) {
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.job(job.id());
        if (jobData.isJoined()) {
            return false;
        }
        jobData.setJoined(true);
        jobData.setLevel(Math.max(1, jobData.getLevel()));
        if (canActivate(data)) {
            jobData.setActive(true);
        }
        playerDataService.markDirty(data);
        return true;
    }

    public boolean leave(Player player, JobDefinition job) {
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(job.id());
        if (jobData == null || !jobData.isJoined()) {
            return false;
        }
        data.jobs().remove(job.id());
        playerDataService.markDirty(data);
        return true;
    }

    public ToggleResult toggle(Player player, JobDefinition job) {
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(job.id());
        if (jobData == null || !jobData.isJoined()) {
            return ToggleResult.NOT_JOINED;
        }
        if (jobData.isActive()) {
            jobData.setActive(false);
            playerDataService.markDirty(data);
            return ToggleResult.INACTIVE;
        }
        if (!canActivate(data)) {
            return ToggleResult.ACTIVE_LIMIT;
        }
        jobData.setActive(true);
        playerDataService.markDirty(data);
        return ToggleResult.ACTIVE;
    }

    public void setLevel(Player player, JobDefinition job, int level) {
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.job(job.id());
        jobData.setJoined(true);
        jobData.setLevel(Math.min(job.maxLevel(), Math.max(1, level)));
        jobData.setProgress(0.0D);
        if (!jobData.isActive() && canActivate(data)) {
            jobData.setActive(true);
        }
        playerDataService.markDirty(data);
    }

    public void reset(Player player, JobDefinition job) {
        PlayerData data = playerDataService.getOrLoad(player);
        data.jobs().remove(job.id());
        playerDataService.markDirty(data);
    }

    public boolean canActivate(PlayerData data) {
        if (plugin.getConfig().getBoolean("allow-multiple-active-jobs", true)) {
            return activeCount(data) < plugin.getConfig().getInt("max-active-jobs", 2);
        }
        return activeCount(data) == 0;
    }

    public int activeCount(PlayerData data) {
        int count = 0;
        for (PlayerJobData jobData : data.jobs().values()) {
            if (jobData.isJoined() && jobData.isActive()) {
                count++;
            }
        }
        return count;
    }
}
