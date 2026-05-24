package fr.yumaria.jobs.action;

import fr.yumaria.jobs.api.model.RewardResult;
import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.api.model.YumariaActionResult;

import java.util.List;

public final class ActionRewardPipeline {
    public List<RewardResult> collect(YumariaActionReport report, YumariaActionResult partialResult) {
        return partialResult == null ? List.of() : partialResult.rewards();
    }
}
