package fr.yumaria.jobs.api;

import fr.yumaria.jobs.api.model.ProgressionResult;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface PrestigeService {
    ProgressionResult applyPrestige(Player player, String jobId);

    ProgressionResult applyPrestige(UUID playerId, String jobId);
}
