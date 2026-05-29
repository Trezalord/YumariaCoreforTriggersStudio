package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionModifierContext).

import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.job.JobDefinition;
import org.bukkit.entity.Player;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public record ActionModifierContext(
        Player player,
        YumariaActionReport report,
        JobDefinition profession
) {
}
