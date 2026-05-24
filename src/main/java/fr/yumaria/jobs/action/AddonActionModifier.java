package fr.yumaria.jobs.action;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.util.Text;

public final class AddonActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    public AddonActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "addon-action";
    }

    @Override
    public ModifierResult apply(ActionModifierContext context) {
        return ModifierResult.one(id(), ActionModifierTarget.BOTH, "addon/action modifiers are split by XP and money");
    }

    public ModifierResult xp(ActionModifierContext context) {
        String path = actionPath(context) + ".base-xp-multiplier";
        return new ModifierResult(id(), ActionModifierTarget.XP, ActionConfig.doubleValue(plugin.getConfig(), path, 1.0D), path);
    }

    public ModifierResult money(ActionModifierContext context) {
        String path = actionPath(context) + ".base-money-multiplier";
        return new ModifierResult(id(), ActionModifierTarget.MONEY, ActionConfig.doubleValue(plugin.getConfig(), path, 1.0D), path);
    }

    private String actionPath(ActionModifierContext context) {
        return "addons.allowed." + context.report().addonId()
                + ".professions." + context.profession().id()
                + ".allowed-actions." + Text.normalizeId(context.report().actionType());
    }
}
