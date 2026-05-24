package fr.yumaria.jobs.api.model;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JobXpRequest {
    private final UUID playerId;
    private final String playerName;
    private final String jobId;
    private final double baseAmount;
    private final String source;
    private final Map<String, Object> context;

    private JobXpRequest(Builder builder) {
        this.playerId = builder.playerId;
        this.playerName = builder.playerName == null ? "" : builder.playerName;
        this.jobId = builder.jobId == null ? "" : builder.jobId;
        this.baseAmount = builder.baseAmount;
        this.source = builder.source == null || builder.source.isBlank() ? "unknown" : builder.source;
        this.context = Map.copyOf(builder.context);
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

    public String jobId() {
        return jobId;
    }

    public double baseAmount() {
        return baseAmount;
    }

    public String source() {
        return source;
    }

    public Map<String, Object> context() {
        return context;
    }

    public static final class Builder {
        private UUID playerId;
        private String playerName;
        private String jobId;
        private double baseAmount;
        private String source = "unknown";
        private final Map<String, Object> context = new HashMap<>();

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

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder baseAmount(double baseAmount) {
            this.baseAmount = baseAmount;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
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
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    context(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public JobXpRequest build() {
            return new JobXpRequest(this);
        }
    }
}
