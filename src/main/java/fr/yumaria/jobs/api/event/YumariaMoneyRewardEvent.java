package fr.yumaria.jobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

public final class YumariaMoneyRewardEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String professionId;
    private final String source;
    private final Map<String, Object> context;
    private boolean cancelled;
    private double amount;

    public YumariaMoneyRewardEvent(Player player, String professionId, String source, double amount, Map<String, Object> context) {
        this.player = player;
        this.professionId = professionId == null ? "" : professionId;
        this.source = source == null ? "" : source;
        this.amount = Math.max(0.0D, amount);
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

    public String getProfessionId() {
        return professionId;
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0.0D, amount);
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
