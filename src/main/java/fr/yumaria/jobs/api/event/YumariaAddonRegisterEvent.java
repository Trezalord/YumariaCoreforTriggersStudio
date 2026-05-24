package fr.yumaria.jobs.api.event;

import fr.yumaria.jobs.api.YumariaAddon;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class YumariaAddonRegisterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final YumariaAddon addon;

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
