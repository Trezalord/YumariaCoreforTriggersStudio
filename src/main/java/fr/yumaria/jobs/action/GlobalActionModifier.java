package fr.yumaria.jobs.action;

import fr.yumaria.jobs.YumariaJobsPlugin;

public final class GlobalActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    public GlobalActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "global";
    }

    @Override
    public ModifierResult apply(ActionModifierContext context) {
        return ModifierResult.one(id(), ActionModifierTarget.BOTH, "global modifiers are split by XP and money");
    }

    public ModifierResult xp(ActionModifierContext context) {
        double multiplier = plugin.getConfig().contains("progression.global-xp-multiplier")
                ? plugin.getConfig().getDouble("progression.global-xp-multiplier", 1.0D)
                : plugin.getConfig().getDouble("xp.global-multiplier", 1.0D);
        return new ModifierResult(id(), ActionModifierTarget.XP, multiplier, "progression.global-xp-multiplier");
    }

    public ModifierResult money(ActionModifierContext context) {
        double multiplier = plugin.getConfig().getDouble("progression.global-money-multiplier", 1.0D);
        return new ModifierResult(id(), ActionModifierTarget.MONEY, multiplier, "progression.global-money-multiplier");
    }
}
