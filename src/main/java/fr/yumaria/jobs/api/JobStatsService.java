package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (JobStatsService).

import fr.yumaria.jobs.api.model.JobStats;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface JobStatsService {
    Optional<JobStats> jobStats(UUID playerId, String jobId);

    Map<String, JobStats> allJobStats(UUID playerId);
}
