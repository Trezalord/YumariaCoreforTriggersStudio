package fr.yumaria.jobs.job;

// Repere fichier YumariaJobs: definition et logique metier configurable (JobSourceDefinition).

// Role YumariaJobs: Represente les metiers, actions, rangs et placeholders associes.
// Annotation YumariaJobs: Repere methode: logique locale de cette classe.
public record JobSourceDefinition(boolean enabled, double multiplier) {
}
