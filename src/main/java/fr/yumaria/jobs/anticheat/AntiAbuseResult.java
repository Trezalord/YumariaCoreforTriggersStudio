package fr.yumaria.jobs.anticheat;

// Repere fichier YumariaJobs: garde-fous anti-abuse de progression (AntiAbuseResult).

import java.util.List;

// Role YumariaJobs: Applique les limites anti-abuse sans punir agressivement les joueurs.
// Annotation YumariaJobs: Repere methode: logique locale de cette classe.
public record AntiAbuseResult(boolean accepted, double multiplier, String reason, List<String> debugMessages) {
    public static AntiAbuseResult accepted(List<String> debugMessages) {
        return new AntiAbuseResult(true, 1.0D, "", debugMessages);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static AntiAbuseResult accepted(double multiplier, List<String> debugMessages) {
        return new AntiAbuseResult(true, Math.max(0.0D, multiplier), "", debugMessages);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static AntiAbuseResult rejected(String reason, List<String> debugMessages) {
        return new AntiAbuseResult(false, 0.0D, reason, debugMessages);
    }

    public AntiAbuseResult {
        reason = reason == null ? "" : reason;
        debugMessages = List.copyOf(debugMessages == null ? List.of() : debugMessages);
    }
}
