package fr.yumaria.jobs.action;

import fr.yumaria.jobs.YumariaJobsPlugin;

public final class ProfessionActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    public ProfessionActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "profession";
    }

    @Override
    public ModifierResult apply(ActionModifierContext context) {
        return ModifierResult.one(id(), ActionModifierTarget.BOTH, "profession modifiers are split by XP and money");
    }

    public ModifierResult xp(ActionModifierContext context) {
        return new ModifierResult(id(), ActionModifierTarget.XP, context.profession().xpMultiplier(), "jobs.yml xp.multiplier");
    }

    public ModifierResult money(ActionModifierContext context) {
        String path = "professions." + context.profession().id() + ".money.multiplier";
        return new ModifierResult(id(), ActionModifierTarget.MONEY, ActionConfig.doubleValue(plugin.getConfig(), path, 1.0D), path);
    }
}
