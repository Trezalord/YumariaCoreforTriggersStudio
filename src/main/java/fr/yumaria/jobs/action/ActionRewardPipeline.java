package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionRewardPipeline).

import fr.yumaria.jobs.api.model.RewardResult;
import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.api.model.YumariaActionResult;

import java.util.List;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class ActionRewardPipeline {
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<RewardResult> collect(YumariaActionReport report, YumariaActionResult partialResult) {
        return partialResult == null ? List.of() : partialResult.rewards();
    }
}
