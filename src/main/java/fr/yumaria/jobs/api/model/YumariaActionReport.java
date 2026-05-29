package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (YumariaActionReport).

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
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

    // Annotation YumariaJobs: Reçoit une action gameplay et la fait passer dans le coeur MMORPG.
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

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public double baseMoney() {
        return baseMoney;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public Map<String, Object> context() {
        return context;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
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
        public Builder addonId(String addonId) {
            this.addonId = addonId;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder professionId(String professionId) {
            this.professionId = professionId;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder baseXp(double baseXp) {
            this.baseXp = baseXp;
            return this;
        }

        // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
        public Builder baseMoney(double baseMoney) {
            this.baseMoney = baseMoney;
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

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        // Annotation YumariaJobs: Construit un objet immutable a partir du builder courant.
        public YumariaActionReport build() {
            return new YumariaActionReport(this);
        }
    }
}
