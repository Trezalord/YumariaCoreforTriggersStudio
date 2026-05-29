package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (ActionEconomyService).

import fr.yumaria.jobs.api.YumariaEconomyService;
import fr.yumaria.jobs.api.model.EconomyRewardRequest;
import fr.yumaria.jobs.api.model.EconomyTransactionResult;
import fr.yumaria.jobs.api.model.YumariaActionReport;
import org.bukkit.entity.Player;

import java.util.Map;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class ActionEconomyService {
    private final YumariaEconomyService economyService;

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public ActionEconomyService(YumariaEconomyService economyService) {
        this.economyService = economyService;
    }

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    public EconomyTransactionResult give(Player player, YumariaActionReport report, String professionId, double amount, Map<String, Object> context) {
        return economyService.give(EconomyRewardRequest.builder()
                .player(player)
                .source(report.addonId())
                .professionId(professionId)
                .amount(amount)
                .context(context)
                .build());
    }
}
