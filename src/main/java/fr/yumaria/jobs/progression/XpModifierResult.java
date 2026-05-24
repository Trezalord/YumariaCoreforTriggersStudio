package fr.yumaria.jobs.progression;

public record XpModifierResult(double multiplier, String debugMessage) {
    public static XpModifierResult one(String debugMessage) {
        return new XpModifierResult(1.0D, debugMessage);
    }

    public XpModifierResult {
        multiplier = Math.max(0.0D, multiplier);
        debugMessage = debugMessage == null ? "" : debugMessage;
    }
}
