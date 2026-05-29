package fr.yumaria.jobs.data;

// Repere fichier YumariaJobs: donnees joueur, cache, stats et sauvegarde (LeaderboardEntry).

import java.util.UUID;

// Role YumariaJobs: Stocke les profils joueur, les stats et la sauvegarde disque.
// Annotation YumariaJobs: Repere methode: logique locale de cette classe.
public record LeaderboardEntry(UUID uuid, String playerName, int level, int prestige, double totalProgress) {
}
