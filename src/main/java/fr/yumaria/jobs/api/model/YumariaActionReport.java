package fr.yumaria.jobs.api.model;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class YumariaActionReport {
    private final UUID playerId;
    private final String playerName;
    private final String addonId;
    private final String professionId;
    private final String actionType;
    private final double baseXp;
    private final double baseMoney;
    private final Map<String, Object> context;
    private final long timestamp;

    private YumariaActionReport(Builder builder) {
        this.playerId = builder.playerId;
        this.playerName = builder.playerName == null ? "" : builder.playerName;
        this.addonId = builder.addonId == null || builder.addonId.isBlank() ? "unknown" : builder.addonId;
        this.professionId = builder.professionId == null ? "" : builder.professionId;
        this.actionType = builder.actionType == null || builder.actionType.isBlank() ? "unknown" : builder.actionType;
        this.baseXp = builder.baseXp;
        this.baseMoney = builder.baseMoney;
        this.context = Map.copyOf(builder.context);
        this.timestamp = builder.timestamp <= 0L ? System.currentTimeMillis() : builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
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

    public double baseMoney() {
        return baseMoney;
    }

    public Map<String, Object> context() {
        return context;
    }

    public long timestamp() {
        return timestamp;
    }

    public static final class Builder {
        private UUID playerId;
        private String playerName;
        private String addonId = "unknown";
        private String professionId;
        private String actionType = "unknown";
        private double baseXp;
        private double baseMoney;
        private final Map<String, Object> context = new HashMap<>();
        private long timestamp;

        private Builder() {
        }

        public Builder player(Player player) {
            if (player != null) {
                this.playerId = player.getUniqueId();
                this.playerName = player.getName();
            }
            return this;
        }

        public Builder playerId(UUID playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public Builder addonId(String addonId) {
            this.addonId = addonId;
            return this;
        }

        public Builder professionId(String professionId) {
            this.professionId = professionId;
            return this;
        }

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder baseXp(double baseXp) {
            this.baseXp = baseXp;
            return this;
        }

        public Builder baseMoney(double baseMoney) {
            this.baseMoney = baseMoney;
            return this;
        }

        public Builder context(String key, Object value) {
            if (key != null && !key.isBlank() && value != null) {
                context.put(key, value);
            }
            return this;
        }

        public Builder context(Map<String, Object> values) {
            if (values != null) {
                values.forEach(this::context);
            }
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public YumariaActionReport build() {
            return new YumariaActionReport(this);
        }
    }
}
