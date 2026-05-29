package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (ProgressionResult).

import java.util.ArrayList;
import java.util.List;

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class ProgressionResult {
    private final boolean success;
    private final ProgressionFailureReason failureReason;
    private final String jobId;
    private final double baseXp;
    private final double finalXp;
    private final int oldLevel;
    private final int newLevel;
    private final boolean leveledUp;
    private final int oldPrestige;
    private final int newPrestige;
    private final List<RewardResult> rewards;
    private final List<String> debugMessages;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private ProgressionResult(Builder builder) {
        this.success = builder.success;
        this.failureReason = builder.failureReason;
        this.jobId = builder.jobId;
        this.baseXp = builder.baseXp;
        this.finalXp = builder.finalXp;
        this.oldLevel = builder.oldLevel;
        this.newLevel = builder.newLevel;
        this.leveledUp = builder.leveledUp;
        this.oldPrestige = builder.oldPrestige;
        this.newPrestige = builder.newPrestige;
        this.rewards = List.copyOf(builder.rewards);
        this.debugMessages = List.copyOf(builder.debugMessages);
    }

    // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
    public static Builder builder(String jobId) {
        return new Builder(jobId);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static ProgressionResult failure(ProgressionFailureReason reason, String jobId, double baseXp, String debugMessage) {
        Builder builder = builder(jobId).success(false).failureReason(reason).baseXp(baseXp);
        if (debugMessage != null && !debugMessage.isBlank()) {
            builder.debug(debugMessage);
        }
        return builder.build();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean success() {
        return success;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ProgressionFailureReason failureReason() {
        return failureReason;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String jobId() {
        return jobId;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double baseXp() {
        return baseXp;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double finalXp() {
        return finalXp;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public int oldLevel() {
        return oldLevel;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public int newLevel() {
        return newLevel;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean leveledUp() {
        return leveledUp;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public int oldPrestige() {
        return oldPrestige;
    }

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public int newPrestige() {
        return newPrestige;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<RewardResult> rewards() {
        return rewards;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<String> debugMessages() {
        return debugMessages;
    }

    public static final class Builder {
        private final String jobId;
        private boolean success;
        private ProgressionFailureReason failureReason = ProgressionFailureReason.NONE;
        private double baseXp;
        private double finalXp;
        private int oldLevel;
        private int newLevel;
        private boolean leveledUp;
        private int oldPrestige;
        private int newPrestige;
        private final List<RewardResult> rewards = new ArrayList<>();
        private final List<String> debugMessages = new ArrayList<>();

        // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
        private Builder(String jobId) {
            this.jobId = jobId == null ? "" : jobId;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder success(boolean success) {
            this.success = success;
            if (success) {
                this.failureReason = ProgressionFailureReason.NONE;
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder failureReason(ProgressionFailureReason failureReason) {
            this.failureReason = failureReason == null ? ProgressionFailureReason.INTERNAL_ERROR : failureReason;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder baseXp(double baseXp) {
            this.baseXp = Math.max(0.0D, baseXp);
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder finalXp(double finalXp) {
            this.finalXp = Math.max(0.0D, finalXp);
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder levels(int oldLevel, int newLevel) {
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.leveledUp = newLevel > oldLevel;
            return this;
        }

        // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
        public Builder prestiges(int oldPrestige, int newPrestige) {
            this.oldPrestige = oldPrestige;
            this.newPrestige = newPrestige;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder rewards(List<RewardResult> rewards) {
            this.rewards.clear();
            if (rewards != null) {
                this.rewards.addAll(rewards);
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder reward(RewardResult reward) {
            if (reward != null) {
                this.rewards.add(reward);
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder debug(String message) {
            if (message != null && !message.isBlank()) {
                this.debugMessages.add(message);
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder debug(List<String> messages) {
            if (messages != null) {
                messages.forEach(this::debug);
            }
            return this;
        }

        // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
        public ProgressionResult build() {
            return new ProgressionResult(this);
        }
    }
}
