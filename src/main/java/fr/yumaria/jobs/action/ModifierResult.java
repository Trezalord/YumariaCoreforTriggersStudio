package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ModifierResult).

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
// Annotation YumariaJobs: Repere methode: logique locale de cette classe.
public record ModifierResult(String id, ActionModifierTarget target, double multiplier, String debugMessage) {
    public ModifierResult {
        id = id == null ? "unknown" : id;
        target = target == null ? ActionModifierTarget.BOTH : target;
        multiplier = Math.max(0.0D, multiplier);
        debugMessage = debugMessage == null ? "" : debugMessage;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static ModifierResult one(String id, ActionModifierTarget target, String debugMessage) {
        return new ModifierResult(id, target, 1.0D, debugMessage);
    }
}
