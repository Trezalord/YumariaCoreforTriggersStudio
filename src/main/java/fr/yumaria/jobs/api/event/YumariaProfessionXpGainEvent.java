package fr.yumaria.jobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

public final class YumariaProfessionXpGainEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String addonId;
    private final String professionId;
    private final String actionType;
    private final double baseXp;
    private final Map<String, Object> context;
    private boolean cancelled;
    private double finalXp;

    public YumariaProfessionXpGainEvent(Player player, String addonId, String professionId, String actionType, double baseXp, double finalXp, Map<String, Object> context) {
        this.player = player;
        this.addonId = addonId == null ? "" : addonId;
        this.professionId = professionId == null ? "" : professionId;
        this.actionType = actionType == null ? "" : actionType;
        this.baseXp = Math.max(0.0D, baseXp);
        this.finalXp = Math.max(0.0D, finalXp);
        this.context = Map.copyOf(context);
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

    public double getBaseXp() {
        return baseXp;
    }

    public double getFinalXp() {
        return finalXp;
    }

    public void setFinalXp(double finalXp) {
        this.finalXp = Math.max(0.0D, finalXp);
    }

    public Map<String, Object> getContext() {
        return context;
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
