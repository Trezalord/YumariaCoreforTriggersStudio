package fr.yumaria.jobs.listener;

// Repere fichier YumariaJobs: ecouteur Bukkit/Paper pour actions vanilla (NativeJobListener).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.progress.JobProgressService;
import fr.yumaria.jobs.util.MaterialMatcher;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Map;

// Role YumariaJobs: Ecoute les evenements vanilla Paper et les convertit en progression.
public final class NativeJobListener implements Listener {
    private final YumariaJobsPlugin plugin;
    private final JobProgressService progressService;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public NativeJobListener(YumariaJobsPlugin plugin, JobProgressService progressService) {
        this.plugin = plugin;
        this.progressService = progressService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // Annotation YumariaJobs: Point d entree Bukkit appele par un evenement serveur.
    public void onBlockBreak(BlockBreakEvent event) {
        // Detection vanilla uniquement: les addons custom doivent appeler l'API actions().
        if (!enabled()) {
            plugin.debugListeners("BlockBreakEvent ignored: native listeners disabled");
            return;
        }
        plugin.debugListeners("BlockBreakEvent: player=" + event.getPlayer().getName()
                + ", material=" + event.getBlock().getType().name());
        if (plugin.getConfig().getBoolean("hooks.native-listeners.lumberjack-log-break", true)
                && MaterialMatcher.isLog(event.getBlock().getType())) {
            plugin.debugListeners("BlockBreakEvent matched job=lumberjack action=vanilla_log_break amount=1.0 material=" + event.getBlock().getType().name());
            progressService.addProgress(event.getPlayer(), "lumberjack", 1.0D, "vanilla_log_break", Map.of("material", event.getBlock().getType().name()));
            return;
        }
        if (plugin.getConfig().getBoolean("hooks.native-listeners.farmer-crop-harvest", true)
                && MaterialMatcher.isMatureCrop(event.getBlock())) {
            plugin.debugListeners("BlockBreakEvent matched job=farmer action=vanilla_crop_harvest amount=1.0 material=" + event.getBlock().getType().name());
            progressService.addProgress(event.getPlayer(), "farmer", 1.0D, "vanilla_crop_harvest", Map.of("material", event.getBlock().getType().name()));
            return;
        }
        if (plugin.getConfig().getBoolean("hooks.native-listeners.miner-block-break", true)
                && MaterialMatcher.isMinerBlock(event.getBlock().getType())) {
            plugin.debugListeners("BlockBreakEvent matched job=miner action=vanilla_block_break amount=1.0 material=" + event.getBlock().getType().name());
            progressService.addProgress(event.getPlayer(), "miner", 1.0D, "vanilla_block_break", Map.of("material", event.getBlock().getType().name()));
            return;
        }
        plugin.debugListeners("BlockBreakEvent no job match: material=" + event.getBlock().getType().name()
                + ", minerEnabled=" + plugin.getConfig().getBoolean("hooks.native-listeners.miner-block-break", true)
                + ", lumberjackEnabled=" + plugin.getConfig().getBoolean("hooks.native-listeners.lumberjack-log-break", true)
                + ", farmerEnabled=" + plugin.getConfig().getBoolean("hooks.native-listeners.farmer-crop-harvest", true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // Annotation YumariaJobs: Point d entree Bukkit appele par un evenement serveur.
    public void onFish(PlayerFishEvent event) {
        // Ne donne de l'XP vanilla qu'au moment CAUGHT_FISH, jamais au lancer ou a la morsure.
        if (!enabled() || !plugin.getConfig().getBoolean("hooks.native-listeners.fisherman-fish-catch", true)) {
            plugin.debugListeners("PlayerFishEvent ignored: nativeEnabled=" + enabled()
                    + ", fishermanEnabled=" + plugin.getConfig().getBoolean("hooks.native-listeners.fisherman-fish-catch", true)
                    + ", state=" + event.getState());
            return;
        }
        plugin.debugListeners("PlayerFishEvent: player=" + event.getPlayer().getName()
                + ", state=" + event.getState()
                + ", caught=" + (event.getCaught() == null ? "-" : event.getCaught().getType().name()));
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            plugin.debugListeners("PlayerFishEvent ignored: state=FISHING reason=rod_cast_no_confirmed_catch");
            return;
        }
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.debugListeners("PlayerFishEvent matched job=fisherman action=vanilla_fish_catch amount=1.0");
            progressService.addProgress(event.getPlayer(), "fisherman", 1.0D, "vanilla_fish_catch", Map.of());
        } else {
            plugin.debugListeners("PlayerFishEvent no job match: state is not CAUGHT_FISH");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    // Annotation YumariaJobs: Point d entree Bukkit appele par un evenement serveur.
    public void onEntityDeath(EntityDeathEvent event) {
        // Progression chasseur vanilla: uniquement quand un joueur tue reellement l'entite.
        if (!enabled() || !plugin.getConfig().getBoolean("hooks.native-listeners.hunter-mob-kill", true)) {
            plugin.debugListeners("EntityDeathEvent ignored: nativeEnabled=" + enabled()
                    + ", hunterEnabled=" + plugin.getConfig().getBoolean("hooks.native-listeners.hunter-mob-kill", true)
                    + ", entity=" + event.getEntityType().name());
            return;
        }
        Player killer = event.getEntity().getKiller();
        plugin.debugListeners("EntityDeathEvent: entity=" + event.getEntityType().name()
                + ", killer=" + (killer == null ? "-" : killer.getName()));
        if (killer == null || event.getEntity() instanceof Player || event.getEntity() instanceof ArmorStand || event.getEntity().hasMetadata("NPC")) {
            plugin.debugListeners("EntityDeathEvent no job match: invalid killer/player/armorstand/npc");
            return;
        }
        if (event.getEntity() instanceof Monster) {
            plugin.debugListeners("EntityDeathEvent matched job=hunter action=vanilla_mob_kill amount=1.0 entity=" + event.getEntityType().name());
            progressService.addProgress(killer, "hunter", 1.0D, "vanilla_mob_kill", Map.of("entity_type", event.getEntityType().name()));
        } else {
            plugin.debugListeners("EntityDeathEvent no job match: entity is not Monster");
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private boolean enabled() {
        return plugin.getConfig().getBoolean("hooks.native-listeners.enabled", true);
    }
}
