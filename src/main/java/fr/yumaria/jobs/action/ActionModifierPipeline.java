package fr.yumaria.jobs.action;

import java.util.ArrayList;
import java.util.List;

public final class ActionModifierPipeline {
    private final List<ActionModifier> modifiers;

    public ActionModifierPipeline(List<ActionModifier> modifiers) {
        this.modifiers = List.copyOf(modifiers);
    }

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
