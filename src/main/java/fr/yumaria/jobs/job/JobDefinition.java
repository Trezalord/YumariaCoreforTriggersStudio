package fr.yumaria.jobs.job;

// Repere fichier YumariaJobs: definition et logique metier configurable (JobDefinition).

import java.util.List;
import java.util.Map;

// Role YumariaJobs: Represente les metiers, actions, rangs et placeholders associes.
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

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return id;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean enabled() {
        return enabled;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String displayName() {
        return displayName;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<String> description() {
        return description;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public IconDefinition icon() {
        return icon;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public int maxLevel() {
        return maxLevel;
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    public String requiredProgressExpression() {
        return requiredProgressExpression;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String pointsRewardedExpression() {
        return pointsRewardedExpression;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean allowProgressWhenInactive() {
        return allowProgressWhenInactive;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double xpMultiplier() {
        return xpMultiplier;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, JobActionDefinition> actions() {
        return actions;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, JobSourceDefinition> sources() {
        return sources;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public RewardDefinition defaultReward() {
        return defaultReward;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<Integer, RewardDefinition> levelRewards() {
        return levelRewards;
    }
}
