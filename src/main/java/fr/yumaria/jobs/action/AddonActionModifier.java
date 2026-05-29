package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (AddonActionModifier).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.util.Text;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class AddonActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public AddonActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "addon-action";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public ModifierResult apply(ActionModifierContext context) {
        return ModifierResult.one(id(), ActionModifierTarget.BOTH, "addon/action modifiers are split by XP and money");
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ModifierResult xp(ActionModifierContext context) {
        String path = actionPath(context) + ".base-xp-multiplier";
        return new ModifierResult(id(), ActionModifierTarget.XP, ActionConfig.doubleValue(plugin.getConfig(), path, 1.0D), path);
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public ModifierResult money(ActionModifierContext context) {
        String path = actionPath(context) + ".base-money-multiplier";
        return new ModifierResult(id(), ActionModifierTarget.MONEY, ActionConfig.doubleValue(plugin.getConfig(), path, 1.0D), path);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String actionPath(ActionModifierContext context) {
        return "addons.allowed." + context.report().addonId()
                + ".professions." + context.profession().id()
                + ".allowed-actions." + Text.normalizeId(context.report().actionType());
    }
}
