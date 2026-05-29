package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (JobXpService).

import fr.yumaria.jobs.api.model.JobXpRequest;
import fr.yumaria.jobs.api.model.ProgressionResult;
import org.bukkit.entity.Player;

import java.util.UUID;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface JobXpService {
    ProgressionResult giveXp(Player player, String jobId, double amount, String source);

    ProgressionResult giveXp(UUID playerId, String jobId, double amount, String source);

    ProgressionResult giveXp(JobXpRequest request);
}
