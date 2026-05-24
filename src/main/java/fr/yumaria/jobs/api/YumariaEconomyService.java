package fr.yumaria.jobs.api;

import fr.yumaria.jobs.api.model.EconomyRewardRequest;
import fr.yumaria.jobs.api.model.EconomyTransactionResult;

public interface YumariaEconomyService {
    EconomyTransactionResult give(EconomyRewardRequest request);

    boolean isAvailable();
}
