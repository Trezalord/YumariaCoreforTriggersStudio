package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaAddonRegisterEvent).

import fr.yumaria.jobs.api.YumariaAddon;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaAddonRegisterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final YumariaAddon addon;

    // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
    public YumariaAddonRegisterEvent(YumariaAddon addon) {
        this.addon = addon;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public YumariaAddon getAddon() {
        return addon;
    }
}
