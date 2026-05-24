package fr.yumaria.jobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class YumariaJobRewardEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String jobId;
    private final String trigger;
    private final int level;
    private final int prestige;
    private boolean cancelled;

    public YumariaJobRewardEvent(Player player, String jobId, String trigger, int level, int prestige) {
        this.player = player;
        this.jobId = jobId;
        this.trigger = trigger == null ? "" : trigger;
        this.level = level;
        this.prestige = prestige;
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

    public String getTrigger() {
        return trigger;
    }

    public int getLevel() {
        return level;
    }

    public int getPrestige() {
        return prestige;
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
