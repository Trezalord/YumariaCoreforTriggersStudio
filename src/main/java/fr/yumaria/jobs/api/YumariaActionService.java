package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (YumariaActionService).

import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.api.model.YumariaActionResult;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface YumariaActionService {
    YumariaActionResult report(YumariaActionReport report);
}
