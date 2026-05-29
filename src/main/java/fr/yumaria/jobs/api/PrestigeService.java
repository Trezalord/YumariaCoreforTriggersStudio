package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (PrestigeService).

import fr.yumaria.jobs.api.model.ProgressionResult;
import org.bukkit.entity.Player;

import java.util.UUID;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface PrestigeService {
    ProgressionResult applyPrestige(Player player, String jobId);

    ProgressionResult applyPrestige(UUID playerId, String jobId);
}
