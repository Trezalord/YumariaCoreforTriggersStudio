package fr.yumaria.jobs.action;

import fr.yumaria.jobs.api.YumariaEconomyService;
import fr.yumaria.jobs.api.model.EconomyRewardRequest;
import fr.yumaria.jobs.api.model.EconomyTransactionResult;
import fr.yumaria.jobs.api.model.YumariaActionReport;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ActionEconomyService {
    private final YumariaEconomyService economyService;

    public ActionEconomyService(YumariaEconomyService economyService) {
        this.economyService = economyService;
    }

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
