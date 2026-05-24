package fr.yumaria.jobs.progression;

import java.util.ArrayList;
import java.util.List;

public final class XpModifierPipeline {
    private final List<XpModifier> modifiers;

    public XpModifierPipeline(List<XpModifier> modifiers) {
        this.modifiers = List.copyOf(modifiers);
    }

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

    public record XpCalculation(double finalXp, double multiplier, List<String> debugMessages) {
        public XpCalculation {
            debugMessages = List.copyOf(debugMessages);
        }
    }
}
