package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionStatsService).

import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import org.bukkit.entity.Player;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class ActionStatsService {
    private final PlayerDataService playerDataService;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ActionStatsService(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
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

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
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
