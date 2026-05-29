package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaActionReportEvent).

import fr.yumaria.jobs.api.model.YumariaActionReport;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaActionReportEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final YumariaActionReport report;
    private boolean cancelled;

    // Annotation YumariaJobs: Reçoit une action gameplay et la fait passer dans le coeur MMORPG.
    public YumariaActionReportEvent(YumariaActionReport report) {
        this.report = report;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    // Annotation YumariaJobs: Reçoit une action gameplay et la fait passer dans le coeur MMORPG.
    public YumariaActionReport getReport() {
        return report;
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
