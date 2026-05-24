package fr.yumaria.jobs.progression;

import fr.yumaria.jobs.YumariaJobsPlugin;

public final class GlobalXpModifier implements XpModifier {
    private final YumariaJobsPlugin plugin;

    public GlobalXpModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "global";
    }

    @Override
    public XpModifierResult apply(XpModifierContext context) {
        double multiplier = plugin.getConfig().getDouble("xp.global-multiplier", 1.0D);
        return new XpModifierResult(multiplier, "config xp.global-multiplier");
    }
}
