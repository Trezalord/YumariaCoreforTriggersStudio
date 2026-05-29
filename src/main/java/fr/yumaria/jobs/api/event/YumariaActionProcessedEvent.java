package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaActionProcessedEvent).

import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.api.model.YumariaActionResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaActionProcessedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final YumariaActionReport report;
    private final YumariaActionResult result;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaActionProcessedEvent(YumariaActionReport report, YumariaActionResult result) {
        this.report = report;
        this.result = result;
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

    public YumariaActionResult getResult() {
        return result;
    }
}
