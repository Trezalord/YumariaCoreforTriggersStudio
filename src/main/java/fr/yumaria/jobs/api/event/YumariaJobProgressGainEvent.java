package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaJobProgressGainEvent).

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaJobProgressGainEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String jobId;
    private final String source;
    private final Map<String, Object> context;
    private boolean cancelled;
    private double amount;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaJobProgressGainEvent(Player player, String jobId, double amount, String source, Map<String, Object> context) {
        this.player = player;
        this.jobId = jobId;
        this.amount = amount;
        this.source = source;
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

    public String getJobId() {
        return jobId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0.0D, amount);
    }

    public String getSource() {
        return source;
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
