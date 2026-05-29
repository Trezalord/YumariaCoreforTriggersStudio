package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaJobXpGainEvent).

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaJobXpGainEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String jobId;
    private final String source;
    private final Map<String, Object> context;
    private final double baseXp;
    private boolean cancelled;
    private double finalXp;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaJobXpGainEvent(Player player, String jobId, double baseXp, double finalXp, String source, Map<String, Object> context) {
        this.player = player;
        this.jobId = jobId;
        this.baseXp = Math.max(0.0D, baseXp);
        this.finalXp = Math.max(0.0D, finalXp);
        this.source = source == null ? "" : source;
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

    public String getSource() {
        return source;
    }

    public Map<String, Object> getContext() {
        return context;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
