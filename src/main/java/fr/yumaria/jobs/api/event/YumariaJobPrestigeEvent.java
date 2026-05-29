package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaJobPrestigeEvent).

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaJobPrestigeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String jobId;
    private final int oldPrestige;
    private final int newPrestige;
    private boolean cancelled;

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public YumariaJobPrestigeEvent(Player player, String jobId, int oldPrestige, int newPrestige) {
        this.player = player;
        this.jobId = jobId;
        this.oldPrestige = oldPrestige;
        this.newPrestige = newPrestige;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    public String getJobId() {
        return jobId;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public int getOldPrestige() {
        return oldPrestige;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public int getNewPrestige() {
        return newPrestige;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
