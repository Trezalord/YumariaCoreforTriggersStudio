package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaProfileSaveEvent).

import fr.yumaria.jobs.api.model.PlayerProfile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaProfileSaveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerProfile profile;

    // Annotation YumariaJobs: Prepare ou execute la sauvegarde des donnees sans bloquer inutilement le serveur.
    public YumariaProfileSaveEvent(PlayerProfile profile) {
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
