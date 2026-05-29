package fr.yumaria.jobs.listener;

// Repere fichier YumariaJobs: ecouteur Bukkit/Paper pour actions vanilla (PlayerDataListener).

import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.progress.ProgressBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// Role YumariaJobs: Ecoute les evenements vanilla Paper et les convertit en progression.
public final class PlayerDataListener implements Listener {
    private final PlayerDataService playerDataService;
    private final ProgressBarService progressBarService;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PlayerDataListener(PlayerDataService playerDataService, ProgressBarService progressBarService) {
        this.playerDataService = playerDataService;
        this.progressBarService = progressBarService;
    }

    @EventHandler
    // Annotation YumariaJobs: Point d entree Bukkit appele par un evenement serveur.
    public void onJoin(PlayerJoinEvent event) {
        playerDataService.getOrLoad(event.getPlayer());
    }

    @EventHandler
    // Annotation YumariaJobs: Point d entree Bukkit appele par un evenement serveur.
    public void onQuit(PlayerQuitEvent event) {
        progressBarService.remove(event.getPlayer(), "player quit");
        playerDataService.unload(event.getPlayer().getUniqueId());
    }
}
