package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (EconomyRewardRequest).

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class EconomyRewardRequest {
    private final UUID playerId;
    private final String playerName;
    private final String source;
    private final String professionId;
    private final double amount;
    private final Map<String, Object> context;

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    private EconomyRewardRequest(Builder builder) {
        this.playerId = builder.playerId;
        this.playerName = builder.playerName == null ? "" : builder.playerName;
        this.source = builder.source == null || builder.source.isBlank() ? "unknown" : builder.source;
        this.professionId = builder.professionId == null ? "" : builder.professionId;
        this.amount = builder.amount;
        this.context = Map.copyOf(builder.context);
    }

    // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
    public static Builder builder() {
        return new Builder();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public UUID playerId() {
        return playerId;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String playerName() {
        return playerName;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String source() {
        return source;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String professionId() {
        return professionId;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double amount() {
        return amount;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
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

        // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
        private Builder() {
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder player(Player player) {
            if (player != null) {
                this.playerId = player.getUniqueId();
                this.playerName = player.getName();
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder playerId(UUID playerId) {
            this.playerId = playerId;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder professionId(String professionId) {
            this.professionId = professionId;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder context(String key, Object value) {
            if (key != null && !key.isBlank() && value != null) {
                context.put(key, value);
            }
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder context(Map<String, Object> values) {
            if (values != null) {
                values.forEach(this::context);
            }
            return this;
        }

        // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
        public EconomyRewardRequest build() {
            return new EconomyRewardRequest(this);
        }
    }
}
