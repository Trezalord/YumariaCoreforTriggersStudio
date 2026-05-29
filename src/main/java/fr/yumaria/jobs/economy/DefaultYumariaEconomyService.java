package fr.yumaria.jobs.economy;

// Repere fichier YumariaJobs: couche economie centrale de YumariaJobs (DefaultYumariaEconomyService).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.api.YumariaEconomyService;
import fr.yumaria.jobs.api.event.YumariaMoneyRewardEvent;
import fr.yumaria.jobs.api.model.EconomyRewardRequest;
import fr.yumaria.jobs.api.model.EconomyTransactionResult;
import fr.yumaria.jobs.api.model.YumariaActionFailureReason;
import fr.yumaria.jobs.hook.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// Role YumariaJobs: Centralise les decisions d argent avant de passer par Vault.
public final class DefaultYumariaEconomyService implements YumariaEconomyService {
    private final YumariaJobsPlugin plugin;
    private final EconomyService economyService;

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public DefaultYumariaEconomyService(YumariaJobsPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }

    @Override
    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public EconomyTransactionResult give(EconomyRewardRequest request) {
        // Couche economie centrale: YumariaJobs valide avant de deposer via Vault.
        if (request == null) {
            return EconomyTransactionResult.failure(YumariaActionFailureReason.INTERNAL_ERROR, 0.0D, "economy request is null");
        }
        if (request.amount() <= 0.0D) {
            return EconomyTransactionResult.success(0.0D);
        }
        if (!Bukkit.isPrimaryThread()) {
            return EconomyTransactionResult.failure(YumariaActionFailureReason.INTERNAL_ERROR, request.amount(), "economy give must run on the main server thread");
        }
        if (!plugin.getConfig().getBoolean("economy.enabled", true)) {
            return EconomyTransactionResult.failure(YumariaActionFailureReason.ECONOMY_DISABLED, request.amount(), "economy.enabled is false");
        }
        if (!economyService.isAvailable()) {
            return EconomyTransactionResult.failure(YumariaActionFailureReason.ECONOMY_DISABLED, request.amount(), "Vault economy is not available");
        }
        Player player = request.playerId() == null ? null : Bukkit.getPlayer(request.playerId());
        if (player == null) {
            return EconomyTransactionResult.failure(YumariaActionFailureReason.PLAYER_NOT_FOUND, request.amount(), "player is not online");
        }

        // Derniere chance pour les autres plugins de modifier ou annuler l'argent donne.
        YumariaMoneyRewardEvent event = new YumariaMoneyRewardEvent(player, request.professionId(), request.source(), request.amount(), request.context());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getAmount() <= 0.0D) {
            return EconomyTransactionResult.failure(YumariaActionFailureReason.EVENT_CANCELLED, request.amount(), "YumariaMoneyRewardEvent cancelled or zeroed");
        }

        // Depot Vault uniquement: YumariaJobs ne retire jamais d'argent dans ce service.
        economyService.deposit(player, event.getAmount());
        return EconomyTransactionResult.success(event.getAmount());
    }

    @Override
    public boolean isAvailable() {
        return economyService.isAvailable();
    }
}
