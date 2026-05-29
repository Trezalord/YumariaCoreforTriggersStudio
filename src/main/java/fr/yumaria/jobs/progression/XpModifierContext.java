package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (XpModifierContext).

import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import org.bukkit.entity.Player;

import java.util.Map;

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public record XpModifierContext(
        Player player,
        JobDefinition job,
        PlayerJobData jobData,
        String source,
        double baseXp,
        Map<String, Object> requestContext
) {
}
