package fr.yumaria.jobs.job;

// Repere fichier YumariaJobs: definition et logique metier configurable (RewardDefinition).

import java.util.List;

// Role YumariaJobs: Represente les metiers, actions, rangs et placeholders associes.
// Annotation YumariaJobs: Repere methode: logique locale de cette classe.
public record RewardDefinition(String moneyExpression, List<String> commands) {
    public static RewardDefinition empty() {
        return new RewardDefinition("0", List.of());
    }
}
