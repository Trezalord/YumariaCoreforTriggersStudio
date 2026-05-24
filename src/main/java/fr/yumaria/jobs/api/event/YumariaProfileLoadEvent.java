package fr.yumaria.jobs.api.event;

import fr.yumaria.jobs.api.model.PlayerProfile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class YumariaProfileLoadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerProfile profile;

    public YumariaProfileLoadEvent(PlayerProfile profile) {
        this.profile = profile;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public PlayerProfile getProfile() {
        return profile;
    }
}
