package fr.yumaria.jobs.action;

import fr.yumaria.jobs.api.model.YumariaActionFailureReason;
import fr.yumaria.jobs.job.JobDefinition;
import org.bukkit.entity.Player;

import java.util.List;

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
