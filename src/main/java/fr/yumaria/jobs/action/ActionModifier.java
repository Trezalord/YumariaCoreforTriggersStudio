package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionModifier).

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public interface ActionModifier {
    String id();

    ModifierResult apply(ActionModifierContext context);
}
