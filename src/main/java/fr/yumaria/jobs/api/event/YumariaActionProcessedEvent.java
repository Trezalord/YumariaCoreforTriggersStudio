package fr.yumaria.jobs.api.event;

import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.api.model.YumariaActionResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class YumariaActionProcessedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final YumariaActionReport report;
    private final YumariaActionResult result;

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

    public YumariaActionReport getReport() {
        return report;
    }

    public YumariaActionResult getResult() {
        return result;
    }
}
