package fr.yumaria.jobs.api;

import fr.yumaria.jobs.api.model.JobXpRequest;
import fr.yumaria.jobs.api.model.ProgressionResult;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface JobXpService {
    ProgressionResult giveXp(Player player, String jobId, double amount, String source);

    ProgressionResult giveXp(UUID playerId, String jobId, double amount, String source);

    ProgressionResult giveXp(JobXpRequest request);
}
