package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionValidationResult).

import fr.yumaria.jobs.api.model.YumariaActionFailureReason;
import fr.yumaria.jobs.job.JobDefinition;
import org.bukkit.entity.Player;

import java.util.List;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public record ActionValidationResult(
        boolean accepted,
        YumariaActionFailureReason failureReason,
        Player player,
        JobDefinition profession,
        List<String> debugMessages
) {
    public ActionValidationResult {
        failureReason = failureReason == null ? YumariaActionFailureReason.INTERNAL_ERROR : failureReason;
        debugMessages = List.copyOf(debugMessages == null ? List.of() : debugMessages);
    }
}
