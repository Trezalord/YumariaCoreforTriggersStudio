package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (YumariaActionResult).

import java.util.ArrayList;
import java.util.List;

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class YumariaActionResult {
    private final boolean success;
    private final YumariaActionFailureReason failureReason;
    private final String addonId;
    private final String professionId;
    private final String actionType;
    private final double baseXp;
    private final double finalXp;
    private final double baseMoney;
    private final double finalMoney;
    private final int oldLevel;
    private final int newLevel;
    private final boolean leveledUp;
    private final int oldPrestige;
    private final int newPrestige;
    private final boolean prestiged;
    private final List<RewardResult> rewards;
    private final List<String> messages;
    private final List<String> debugMessages;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private YumariaActionResult(Builder builder) {
        this.success = builder.success;
        this.failureReason = builder.failureReason;
        this.addonId = builder.addonId;
        this.professionId = builder.professionId;
        this.actionType = builder.actionType;
        this.baseXp = builder.baseXp;
        this.finalXp = builder.finalXp;
        this.baseMoney = builder.baseMoney;
        this.finalMoney = builder.finalMoney;
        this.oldLevel = builder.oldLevel;
        this.newLevel = builder.newLevel;
        this.leveledUp = builder.leveledUp;
        this.oldPrestige = builder.oldPrestige;
        this.newPrestige = builder.newPrestige;
        this.prestiged = builder.prestiged;
        this.rewards = List.copyOf(builder.rewards);
        this.messages = List.copyOf(builder.messages);
        this.debugMessages = List.copyOf(builder.debugMessages);
    }

    // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
    public static Builder builder(String addonId, String professionId, String actionType) {
        return new Builder(addonId, professionId, actionType);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static YumariaActionResult failure(YumariaActionFailureReason reason, YumariaActionReport report, String debugMessage) {
        Builder builder = builder(
                report == null ? "" : report.addonId(),
                report == null ? "" : report.professionId(),
                report == null ? "" : report.actionType()
        ).success(false).failureReason(reason);
        if (report != null) {
            builder.baseXp(report.baseXp()).baseMoney(report.baseMoney());
        }
        builder.debug(debugMessage);
        return builder.build();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean success() {
        return success;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaActionFailureReason failureReason() {
        return failureReason;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String addonId() {
        return addonId;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String professionId() {
        return professionId;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String actionType() {
        return actionType;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double baseXp() {
        return baseXp;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double finalXp() {
        return finalXp;
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public double baseMoney() {
        return baseMoney;
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public double finalMoney() {
        return finalMoney;
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

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public boolean prestiged() {
        return prestiged;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<RewardResult> rewards() {
        return rewards;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<String> messages() {
        return messages;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<String> debugMessages() {
        return debugMessages;
    }

    public static final class Builder {
        private final String addonId;
        private final String professionId;
        private final String actionType;
        private boolean success;
        private YumariaActionFailureReason failureReason = YumariaActionFailureReason.NONE;
        private double baseXp;
        private double finalXp;
        private double baseMoney;
        private double finalMoney;
        private int oldLevel;
        private int newLevel;
        private boolean leveledUp;
        private int oldPrestige;
        private int newPrestige;
        private boolean prestiged;
        private final List<RewardResult> rewards = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private final List<String> debugMessages = new ArrayList<>();

        // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
        private Builder(String addonId, String professionId, String actionType) {
            this.addonId = addonId == null ? "" : addonId;
            this.professionId = professionId == null ? "" : professionId;
            this.actionType = actionType == null ? "" : actionType;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder success(boolean success) {
            this.success = success;
            if (success) {
                this.failureReason = YumariaActionFailureReason.NONE;
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder failureReason(YumariaActionFailureReason failureReason) {
            this.failureReason = failureReason == null ? YumariaActionFailureReason.INTERNAL_ERROR : failureReason;
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

        // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
        public Builder baseMoney(double baseMoney) {
            this.baseMoney = Math.max(0.0D, baseMoney);
            return this;
        }

        // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
        public Builder finalMoney(double finalMoney) {
            this.finalMoney = Math.max(0.0D, finalMoney);
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
            this.prestiged = newPrestige > oldPrestige;
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
        public Builder message(String message) {
            if (message != null && !message.isBlank()) {
                messages.add(message);
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder debug(String debugMessage) {
            if (debugMessage != null && !debugMessage.isBlank()) {
                debugMessages.add(debugMessage);
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder debug(List<String> debugMessages) {
            if (debugMessages != null) {
                debugMessages.forEach(this::debug);
            }
            return this;
        }

        // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
        public YumariaActionResult build() {
            return new YumariaActionResult(this);
        }
    }
}
