package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (JobSpecificXpModifier).

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public final class JobSpecificXpModifier implements XpModifier {
    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "job";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public XpModifierResult apply(XpModifierContext context) {
        return new XpModifierResult(context.job().xpMultiplier(), "job multiplier");
    }
}
