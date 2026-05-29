package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaProfileLoadEvent).

import fr.yumaria.jobs.api.model.PlayerProfile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaProfileLoadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerProfile profile;

    // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
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
