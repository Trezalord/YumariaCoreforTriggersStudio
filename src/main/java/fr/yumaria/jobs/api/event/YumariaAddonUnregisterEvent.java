package fr.yumaria.jobs.api.event;

import fr.yumaria.jobs.api.YumariaAddon;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class YumariaAddonUnregisterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String addonId;
    private final YumariaAddon addon;

    public YumariaAddonUnregisterEvent(String addonId, YumariaAddon addon) {
        this.addonId = addonId == null ? "" : addonId;
        this.addon = addon;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public String getAddonId() {
        return addonId;
    }

    public YumariaAddon getAddon() {
        return addon;
    }
}
