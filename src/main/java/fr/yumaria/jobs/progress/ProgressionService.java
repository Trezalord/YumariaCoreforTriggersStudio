package fr.yumaria.jobs.progress;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.util.ExpressionEvaluator;

import java.util.HashMap;
import java.util.Map;

public final class ProgressionService {
    private final YumariaJobsPlugin plugin;
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

    public ProgressionService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public double requiredProgress(JobDefinition job, PlayerJobData data) {
        Map<String, Double> variables = variables(job, data, 0.0D);
        double base = evaluator.evaluate(job.requiredProgressExpression(), variables, 1.0D);
        double multiplier = plugin.getConfig().getDouble("prestige.progress-multiplier-per-prestige", 1.0D);
        if (plugin.getConfig().getBoolean("prestige.enabled", true) && multiplier > 0.0D && data.getPrestige() > 0) {
            base *= Math.pow(multiplier, data.getPrestige());
        }
        return Math.max(1.0D, base);
    }

    public double pointsRewarded(JobDefinition job, PlayerJobData data) {
        return Math.max(0.0D, evaluator.evaluate(job.pointsRewardedExpression(), variables(job, data, requiredProgress(job, data)), 0.0D));
    }

    public double evaluateMoney(String expression, JobDefinition job, PlayerJobData data) {
        return Math.max(0.0D, evaluator.evaluate(expression, variables(job, data, requiredProgress(job, data)), 0.0D));
    }

    public Map<String, Double> variables(JobDefinition job, PlayerJobData data, double requiredProgress) {
        Map<String, Double> variables = new HashMap<>();
        variables.put("level", (double) data.getLevel());
        variables.put("prestige", (double) data.getPrestige());
        variables.put("progress", data.getProgress());
        variables.put("required_progress", requiredProgress);
        variables.put("total_progress", data.getTotalProgress());
        variables.put("max_level", (double) job.maxLevel());
        return variables;
    }
}
