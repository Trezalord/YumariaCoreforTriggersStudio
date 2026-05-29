package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (PrestigeXpModifier).

import fr.yumaria.jobs.YumariaJobsPlugin;

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public final class PrestigeXpModifier implements XpModifier {
    private final YumariaJobsPlugin plugin;

    // Annotation YumariaJobs: Gere la logique de prestige et ses conditions.
    public PrestigeXpModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "prestige";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
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
