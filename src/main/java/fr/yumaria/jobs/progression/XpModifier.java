package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (XpModifier).

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public interface XpModifier {
    String id();

    XpModifierResult apply(XpModifierContext context);
}
