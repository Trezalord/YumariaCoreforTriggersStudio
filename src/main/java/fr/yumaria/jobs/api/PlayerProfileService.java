package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (PlayerProfileService).

import fr.yumaria.jobs.api.model.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface PlayerProfileService {
    Optional<PlayerProfile> profile(UUID playerId);

    Optional<PlayerProfile> profile(Player player);

    PlayerProfile getOrLoadProfile(Player player);

    boolean isLoaded(UUID playerId);
}
