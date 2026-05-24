package fr.yumaria.jobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class YumariaProfessionLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String addonId;
    private final String professionId;
    private final String actionType;
    private final int oldLevel;
    private final int newLevel;
    private final int prestige;

    public YumariaProfessionLevelUpEvent(Player player, String addonId, String professionId, String actionType, int oldLevel, int newLevel, int prestige) {
        this.player = player;
        this.addonId = addonId == null ? "" : addonId;
        this.professionId = professionId == null ? "" : professionId;
        this.actionType = actionType == null ? "" : actionType;
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

    public String getAddonId() {
        return addonId;
    }

    public String getProfessionId() {
        return professionId;
    }

    public String getActionType() {
        return actionType;
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
