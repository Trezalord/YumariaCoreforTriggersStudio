package fr.yumaria.jobs.progression;

public interface XpModifier {
    String id();

    XpModifierResult apply(XpModifierContext context);
}
