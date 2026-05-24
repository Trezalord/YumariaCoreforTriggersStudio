package fr.yumaria.jobs.data;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, String playerName, int level, int prestige, double totalProgress) {
}
