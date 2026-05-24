package fr.yumaria.jobs.action;

import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import org.bukkit.entity.Player;

public final class ActionStatsService {
    private final PlayerDataService playerDataService;

    public ActionStatsService(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    public void recordMoney(Player player, String professionId, String source, double money) {
        if (player == null || professionId == null || money <= 0.0D) {
            return;
        }
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(professionId);
        if (jobData == null) {
            return;
        }
        jobData.recordMoney(source, money);
        playerDataService.markDirty(data);
    }

    public void recordActionType(Player player, String professionId, String actionType) {
        if (player == null || professionId == null || actionType == null) {
            return;
        }
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(professionId);
        if (jobData == null) {
            return;
        }
        jobData.recordActionType(actionType);
        playerDataService.markDirty(data);
    }
}
