package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (EventXpModifier).

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public final class EventXpModifier implements XpModifier {
    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "event";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public XpModifierResult apply(XpModifierContext context) {
        return XpModifierResult.one("Bukkit YumariaJobXpGainEvent may adjust final XP after pipeline");
    }
}
