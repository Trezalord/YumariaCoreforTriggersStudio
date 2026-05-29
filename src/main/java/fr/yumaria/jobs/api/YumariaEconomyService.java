package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (YumariaEconomyService).

import fr.yumaria.jobs.api.model.EconomyRewardRequest;
import fr.yumaria.jobs.api.model.EconomyTransactionResult;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface YumariaEconomyService {
    EconomyTransactionResult give(EconomyRewardRequest request);

    boolean isAvailable();
}
