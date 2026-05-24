package fr.yumaria.jobs.api.model;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EconomyRewardRequest {
    private final UUID playerId;
    private final String playerName;
    private final String source;
    private final String professionId;
    private final double amount;
    private final Map<String, Object> context;

    private EconomyRewardRequest(Builder builder) {
        this.playerId = builder.playerId;
        this.playerName = builder.playerName == null ? "" : builder.playerName;
        this.source = builder.source == null || builder.source.isBlank() ? "unknown" : builder.source;
        this.professionId = builder.professionId == null ? "" : builder.professionId;
        this.amount = builder.amount;
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

    public String source() {
        return source;
    }

    public String professionId() {
        return professionId;
    }

    public double amount() {
        return amount;
    }

    public Map<String, Object> context() {
        return context;
    }

    public static final class Builder {
        private UUID playerId;
        private String playerName;
        private String source = "unknown";
        private String professionId;
        private double amount;
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

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder professionId(String professionId) {
            this.professionId = professionId;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
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

        public EconomyRewardRequest build() {
            return new EconomyRewardRequest(this);
        }
    }
}
