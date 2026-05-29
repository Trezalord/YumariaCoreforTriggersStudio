package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (XpModifierResult).

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
// Annotation YumariaJobs: Repere methode: logique locale de cette classe.
public record XpModifierResult(double multiplier, String debugMessage) {
    public static XpModifierResult one(String debugMessage) {
        return new XpModifierResult(1.0D, debugMessage);
    }

    public XpModifierResult {
        multiplier = Math.max(0.0D, multiplier);
        debugMessage = debugMessage == null ? "" : debugMessage;
    }
}
