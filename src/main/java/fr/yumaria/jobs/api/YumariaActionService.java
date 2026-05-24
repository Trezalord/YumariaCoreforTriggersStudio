package fr.yumaria.jobs.api;

import fr.yumaria.jobs.api.model.YumariaActionReport;
import fr.yumaria.jobs.api.model.YumariaActionResult;

public interface YumariaActionService {
    YumariaActionResult report(YumariaActionReport report);
}
