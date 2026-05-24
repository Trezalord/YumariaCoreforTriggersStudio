package fr.yumaria.jobs.job;

import java.util.List;
import java.util.Map;

public final class JobDefinition {
    private final String id;
    private final boolean enabled;
    private final String displayName;
    private final List<String> description;
    private final IconDefinition icon;
    private final int maxLevel;
    private final String requiredProgressExpression;
    private final String pointsRewardedExpression;
    private final boolean allowProgressWhenInactive;
    private final double xpMultiplier;
    private final Map<String, JobActionDefinition> actions;
    private final Map<String, JobSourceDefinition> sources;
    private final RewardDefinition defaultReward;
    private final Map<Integer, RewardDefinition> levelRewards;

    public JobDefinition(
            String id,
            boolean enabled,
            String displayName,
            List<String> description,
            IconDefinition icon,
            int maxLevel,
            String requiredProgressExpression,
            String pointsRewardedExpression,
            boolean allowProgressWhenInactive,
            double xpMultiplier,
            Map<String, JobActionDefinition> actions,
            Map<String, JobSourceDefinition> sources,
            RewardDefinition defaultReward,
            Map<Integer, RewardDefinition> levelRewards
    ) {
        this.id = id;
        this.enabled = enabled;
        this.displayName = displayName;
        this.description = List.copyOf(description);
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.requiredProgressExpression = requiredProgressExpression;
        this.pointsRewardedExpression = pointsRewardedExpression;
        this.allowProgressWhenInactive = allowProgressWhenInactive;
        this.xpMultiplier = Math.max(0.0D, xpMultiplier);
        this.actions = Map.copyOf(actions);
        this.sources = Map.copyOf(sources);
        this.defaultReward = defaultReward;
        this.levelRewards = Map.copyOf(levelRewards);
    }

    public String id() {
        return id;
    }

    public boolean enabled() {
        return enabled;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> description() {
        return description;
    }

    public IconDefinition icon() {
        return icon;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public String requiredProgressExpression() {
        return requiredProgressExpression;
    }

    public String pointsRewardedExpression() {
        return pointsRewardedExpression;
    }

    public boolean allowProgressWhenInactive() {
        return allowProgressWhenInactive;
    }

    public double xpMultiplier() {
        return xpMultiplier;
    }

    public Map<String, JobActionDefinition> actions() {
        return actions;
    }

    public Map<String, JobSourceDefinition> sources() {
        return sources;
    }

    public RewardDefinition defaultReward() {
        return defaultReward;
    }

    public Map<Integer, RewardDefinition> levelRewards() {
        return levelRewards;
    }
}
