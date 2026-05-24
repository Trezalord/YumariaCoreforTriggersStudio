package fr.yumaria.jobs.progression;

public final class JobSpecificXpModifier implements XpModifier {
    @Override
    public String id() {
        return "job";
    }

    @Override
    public XpModifierResult apply(XpModifierContext context) {
        return new XpModifierResult(context.job().xpMultiplier(), "job multiplier");
    }
}
