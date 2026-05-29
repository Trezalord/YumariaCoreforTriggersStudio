package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (RewardService).

import fr.yumaria.jobs.api.model.RewardResult;

import java.util.List;
import java.util.UUID;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface RewardService {
    List<RewardResult> previewRewards(UUID playerId, String jobId);
}
