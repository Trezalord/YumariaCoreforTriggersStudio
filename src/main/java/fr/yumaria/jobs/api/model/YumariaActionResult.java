package fr.yumaria.jobs.api.model;

import java.util.ArrayList;
import java.util.List;

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

    public static Builder builder(String addonId, String professionId, String actionType) {
        return new Builder(addonId, professionId, actionType);
    }

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

    public boolean success() {
        return success;
    }

    public YumariaActionFailureReason failureReason() {
        return failureReason;
    }

    public String addonId() {
        return addonId;
    }

    public String professionId() {
        return professionId;
    }

    public String actionType() {
        return actionType;
    }

    public double baseXp() {
        return baseXp;
    }

    public double finalXp() {
        return finalXp;
    }

    public double baseMoney() {
        return baseMoney;
    }

    public double finalMoney() {
        return finalMoney;
    }

    public int oldLevel() {
        return oldLevel;
    }

    public int newLevel() {
        return newLevel;
    }

    public boolean leveledUp() {
        return leveledUp;
    }

    public int oldPrestige() {
        return oldPrestige;
    }

    public int newPrestige() {
        return newPrestige;
    }

    public boolean prestiged() {
        return prestiged;
    }

    public List<RewardResult> rewards() {
        return rewards;
    }

    public List<String> messages() {
        return messages;
    }

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

        private Builder(String addonId, String professionId, String actionType) {
            this.addonId = addonId == null ? "" : addonId;
            this.professionId = professionId == null ? "" : professionId;
            this.actionType = actionType == null ? "" : actionType;
        }

        public Builder success(boolean success) {
            this.success = success;
            if (success) {
                this.failureReason = YumariaActionFailureReason.NONE;
            }
            return this;
        }

        public Builder failureReason(YumariaActionFailureReason failureReason) {
            this.failureReason = failureReason == null ? YumariaActionFailureReason.INTERNAL_ERROR : failureReason;
            return this;
        }

        public Builder baseXp(double baseXp) {
            this.baseXp = Math.max(0.0D, baseXp);
            return this;
        }

        public Builder finalXp(double finalXp) {
            this.finalXp = Math.max(0.0D, finalXp);
            return this;
        }

        public Builder baseMoney(double baseMoney) {
            this.baseMoney = Math.max(0.0D, baseMoney);
            return this;
        }

        public Builder finalMoney(double finalMoney) {
            this.finalMoney = Math.max(0.0D, finalMoney);
            return this;
        }

        public Builder levels(int oldLevel, int newLevel) {
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.leveledUp = newLevel > oldLevel;
            return this;
        }

        public Builder prestiges(int oldPrestige, int newPrestige) {
            this.oldPrestige = oldPrestige;
            this.newPrestige = newPrestige;
            this.prestiged = newPrestige > oldPrestige;
            return this;
        }

        public Builder rewards(List<RewardResult> rewards) {
            this.rewards.clear();
            if (rewards != null) {
                this.rewards.addAll(rewards);
            }
            return this;
        }

        public Builder message(String message) {
            if (message != null && !message.isBlank()) {
                messages.add(message);
            }
            return this;
        }

        public Builder debug(String debugMessage) {
            if (debugMessage != null && !debugMessage.isBlank()) {
                debugMessages.add(debugMessage);
            }
            return this;
        }

        public Builder debug(List<String> debugMessages) {
            if (debugMessages != null) {
                debugMessages.forEach(this::debug);
            }
            return this;
        }

        public YumariaActionResult build() {
            return new YumariaActionResult(this);
        }
    }
}
