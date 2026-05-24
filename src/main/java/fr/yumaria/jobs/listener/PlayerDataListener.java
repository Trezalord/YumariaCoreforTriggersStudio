package fr.yumaria.jobs.listener;

import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.progress.ProgressBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerDataListener implements Listener {
    private final PlayerDataService playerDataService;
    private final ProgressBarService progressBarService;

    public PlayerDataListener(PlayerDataService playerDataService, ProgressBarService progressBarService) {
        this.playerDataService = playerDataService;
        this.progressBarService = progressBarService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataService.getOrLoad(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        progressBarService.remove(event.getPlayer(), "player quit");
        playerDataService.unload(event.getPlayer().getUniqueId());
    }
}
