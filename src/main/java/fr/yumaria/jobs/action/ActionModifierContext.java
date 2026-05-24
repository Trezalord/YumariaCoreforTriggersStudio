package fr.yumaria.jobs.action;

import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.job.JobDefinition;
import org.bukkit.entity.Player;

public record ActionModifierContext(
        Player player,
        YumariaActionReport report,
        JobDefinition profession
) {
}
