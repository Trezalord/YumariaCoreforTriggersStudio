package fr.yumaria.jobs.anticheat;

import java.util.List;

public record AntiAbuseResult(boolean accepted, double multiplier, String reason, List<String> debugMessages) {
    public static AntiAbuseResult accepted(List<String> debugMessages) {
        return new AntiAbuseResult(true, 1.0D, "", debugMessages);
    }

    public static AntiAbuseResult accepted(double multiplier, List<String> debugMessages) {
        return new AntiAbuseResult(true, Math.max(0.0D, multiplier), "", debugMessages);
    }

    public static AntiAbuseResult rejected(String reason, List<String> debugMessages) {
        return new AntiAbuseResult(false, 0.0D, reason, debugMessages);
    }

    public AntiAbuseResult {
        reason = reason == null ? "" : reason;
        debugMessages = List.copyOf(debugMessages == null ? List.of() : debugMessages);
    }
}
