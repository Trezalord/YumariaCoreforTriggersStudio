package fr.yumaria.jobs.api;

import fr.yumaria.jobs.api.model.RewardResult;

import java.util.List;
import java.util.UUID;

public interface RewardService {
    List<RewardResult> previewRewards(UUID playerId, String jobId);
}
