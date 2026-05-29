package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionModifierPipeline).

import java.util.ArrayList;
import java.util.List;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class ActionModifierPipeline {
    private final List<ActionModifier> modifiers;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public ActionModifierPipeline(List<ActionModifier> modifiers) {
        this.modifiers = List.copyOf(modifiers);
    }

    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
    public ActionModifierCalculation apply(ActionModifierContext context) {
        double xpMultiplier = 1.0D;
        double moneyMultiplier = 1.0D;
        List<String> debug = new ArrayList<>();
        for (ActionModifier modifier : modifiers) {
            ModifierResult result = modifier.apply(context);
            if (result.target() == ActionModifierTarget.XP || result.target() == ActionModifierTarget.BOTH) {
                xpMultiplier *= result.multiplier();
            }
            if (result.target() == ActionModifierTarget.MONEY || result.target() == ActionModifierTarget.BOTH) {
                moneyMultiplier *= result.multiplier();
            }
            if (!result.debugMessage().isBlank()) {
                debug.add(result.id() + "=" + result.multiplier() + " (" + result.debugMessage() + ")");
            }
        }
        return new ActionModifierCalculation(
                Math.max(0.0D, context.report().baseXp() * xpMultiplier),
                Math.max(0.0D, context.report().baseMoney() * moneyMultiplier),
                xpMultiplier,
                moneyMultiplier,
                debug
        );
    }

    public record ActionModifierCalculation(
            double finalXp,
            double finalMoney,
            double xpMultiplier,
            double moneyMultiplier,
            List<String> debugMessages
    ) {
        public ActionModifierCalculation {
            debugMessages = List.copyOf(debugMessages);
        }
    }
}
