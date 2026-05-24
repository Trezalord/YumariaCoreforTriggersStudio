package fr.yumaria.jobs.action;

public interface ActionModifier {
    String id();

    ModifierResult apply(ActionModifierContext context);
}
