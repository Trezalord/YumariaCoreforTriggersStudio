package fr.yumaria.jobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class YumariaJobLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String jobId;
    private final int oldLevel;
    private final int newLevel;
    private final int prestige;

    public YumariaJobLevelUpEvent(Player player, String jobId, int oldLevel, int newLevel, int prestige) {
        this.player = player;
        this.jobId = jobId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
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

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public int getPrestige() {
        return prestige;
    }
}
