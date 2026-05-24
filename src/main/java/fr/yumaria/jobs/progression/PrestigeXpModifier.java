package fr.yumaria.jobs.progression;

import fr.yumaria.jobs.YumariaJobsPlugin;

public final class PrestigeXpModifier implements XpModifier {
    private final YumariaJobsPlugin plugin;

    public PrestigeXpModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "prestige";
    }

    @Override
    public XpModifierResult apply(XpModifierContext context) {
        double perPrestige = plugin.getConfig().getDouble("xp.prestige-multiplier-per-prestige", 1.0D);
        int prestige = Math.max(0, context.jobData().getPrestige());
        if (prestige <= 0 || perPrestige == 1.0D) {
            return XpModifierResult.one("no prestige bonus");
        }
        double multiplier = Math.pow(Math.max(0.0D, perPrestige), prestige);
        return new XpModifierResult(multiplier, "prestige=" + prestige);
    }
}
