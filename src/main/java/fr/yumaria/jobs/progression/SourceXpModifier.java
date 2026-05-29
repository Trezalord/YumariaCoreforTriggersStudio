package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (SourceXpModifier).

import fr.yumaria.jobs.job.JobSourceDefinition;
import fr.yumaria.jobs.util.Text;

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public final class SourceXpModifier implements XpModifier {
    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "source";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public XpModifierResult apply(XpModifierContext context) {
        JobSourceDefinition source = context.job().sources().get(Text.normalizeId(context.source()));
        if (source == null) {
            return XpModifierResult.one("source not configured");
        }
        return new XpModifierResult(source.multiplier(), "source " + context.source());
    }
}
