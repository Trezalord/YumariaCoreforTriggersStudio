package fr.yumaria.jobs.job;

// Repere fichier YumariaJobs: definition et logique metier configurable (JobActionDefinition).

// Role YumariaJobs: Represente les metiers, actions, rangs et placeholders associes.
// Annotation YumariaJobs: Repere methode: logique locale de cette classe.
public record JobActionDefinition(boolean enabled, double progress, long cooldownMs) {
}
