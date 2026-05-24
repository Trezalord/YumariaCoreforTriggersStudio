package fr.yumaria.jobs.api;

import fr.yumaria.jobs.api.model.JobStats;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JobStatsService {
    Optional<JobStats> jobStats(UUID playerId, String jobId);

    Map<String, JobStats> allJobStats(UUID playerId);
}
