package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ProfessionActionModifier).

import fr.yumaria.jobs.YumariaJobsPlugin;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class ProfessionActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ProfessionActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "profession";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public ModifierResult apply(ActionModifierContext context) {
        return ModifierResult.one(id(), ActionModifierTarget.BOTH, "profession modifiers are split by XP and money");
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ModifierResult xp(ActionModifierContext context) {
        return new ModifierResult(id(), ActionModifierTarget.XP, context.profession().xpMultiplier(), "jobs.yml xp.multiplier");
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public ModifierResult money(ActionModifierContext context) {
        String path = "professions." + context.profession().id() + ".money.multiplier";
        return new ModifierResult(id(), ActionModifierTarget.MONEY, ActionConfig.doubleValue(plugin.getConfig(), path, 1.0D), path);
    }
}
