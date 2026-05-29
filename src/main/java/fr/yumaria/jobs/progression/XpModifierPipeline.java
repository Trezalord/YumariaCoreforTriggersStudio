package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (XpModifierPipeline).

import java.util.ArrayList;
import java.util.List;

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public final class XpModifierPipeline {
    private final List<XpModifier> modifiers;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public XpModifierPipeline(List<XpModifier> modifiers) {
        this.modifiers = List.copyOf(modifiers);
    }

    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public XpCalculation apply(XpModifierContext context) {
        double multiplier = 1.0D;
        List<String> debugMessages = new ArrayList<>();
        for (XpModifier modifier : modifiers) {
            XpModifierResult result = modifier.apply(context);
            multiplier *= result.multiplier();
            if (!result.debugMessage().isBlank()) {
                debugMessages.add(modifier.id() + "=" + result.multiplier() + " (" + result.debugMessage() + ")");
            }
        }
        return new XpCalculation(Math.max(0.0D, context.baseXp() * multiplier), multiplier, debugMessages);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public record XpCalculation(double finalXp, double multiplier, List<String> debugMessages) {
        public XpCalculation {
            debugMessages = List.copyOf(debugMessages);
        }
    }
}
