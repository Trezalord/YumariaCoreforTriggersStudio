package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (GlobalActionModifier).

import fr.yumaria.jobs.YumariaJobsPlugin;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class GlobalActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public GlobalActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "global";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public ModifierResult apply(ActionModifierContext context) {
        return ModifierResult.one(id(), ActionModifierTarget.BOTH, "global modifiers are split by XP and money");
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ModifierResult xp(ActionModifierContext context) {
        double multiplier = plugin.getConfig().contains("progression.global-xp-multiplier")
                ? plugin.getConfig().getDouble("progression.global-xp-multiplier", 1.0D)
                : plugin.getConfig().getDouble("xp.global-multiplier", 1.0D);
        return new ModifierResult(id(), ActionModifierTarget.XP, multiplier, "progression.global-xp-multiplier");
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public ModifierResult money(ActionModifierContext context) {
        double multiplier = plugin.getConfig().getDouble("progression.global-money-multiplier", 1.0D);
        return new ModifierResult(id(), ActionModifierTarget.MONEY, multiplier, "progression.global-money-multiplier");
    }
}
