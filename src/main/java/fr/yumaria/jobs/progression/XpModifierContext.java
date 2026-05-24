package fr.yumaria.jobs.progression;

import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import org.bukkit.entity.Player;

import java.util.Map;

public record XpModifierContext(
        Player player,
        JobDefinition job,
        PlayerJobData jobData,
        String source,
        double baseXp,
        Map<String, Object> requestContext
) {
}
