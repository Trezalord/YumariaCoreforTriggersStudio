package fr.yumaria.jobs.api;

import fr.yumaria.jobs.api.model.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileService {
    Optional<PlayerProfile> profile(UUID playerId);

    Optional<PlayerProfile> profile(Player player);

    PlayerProfile getOrLoadProfile(Player player);

    boolean isLoaded(UUID playerId);
}
