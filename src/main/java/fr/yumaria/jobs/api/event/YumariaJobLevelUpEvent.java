package fr.yumaria.jobs.api.event;

// Repere fichier YumariaJobs: evenement public expose aux autres plugins (YumariaJobLevelUpEvent).

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Role YumariaJobs: Expose un evenement Bukkit public pour les plugins externes.
public final class YumariaJobLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String jobId;
    private final int oldLevel;
    private final int newLevel;
    private final int prestige;

    // Annotation YumariaJobs: Controle les montees de niveau et les recompenses associees.
    public YumariaJobLevelUpEvent(Player player, String jobId, int oldLevel, int newLevel, int prestige) {
        this.player = player;
        this.jobId = jobId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.prestige = prestige;
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

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public int getPrestige() {
        return prestige;
    }
}
