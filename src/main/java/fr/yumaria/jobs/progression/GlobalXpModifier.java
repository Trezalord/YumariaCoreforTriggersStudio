package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (GlobalXpModifier).

import fr.yumaria.jobs.YumariaJobsPlugin;

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public final class GlobalXpModifier implements XpModifier {
    private final YumariaJobsPlugin plugin;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public GlobalXpModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "global";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public XpModifierResult apply(XpModifierContext context) {
        double multiplier = plugin.getConfig().getDouble("xp.global-multiplier", 1.0D);
        return new XpModifierResult(multiplier, "config xp.global-multiplier");
    }
}
