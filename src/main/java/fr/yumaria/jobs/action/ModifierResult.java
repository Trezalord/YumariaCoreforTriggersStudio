package fr.yumaria.jobs.action;

public record ModifierResult(String id, ActionModifierTarget target, double multiplier, String debugMessage) {
    public ModifierResult {
        id = id == null ? "unknown" : id;
        target = target == null ? ActionModifierTarget.BOTH : target;
        multiplier = Math.max(0.0D, multiplier);
        debugMessage = debugMessage == null ? "" : debugMessage;
    }

    public static ModifierResult one(String id, ActionModifierTarget target, String debugMessage) {
        return new ModifierResult(id, target, 1.0D, debugMessage);
    }
}
