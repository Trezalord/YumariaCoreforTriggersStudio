package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (YumariaAddon).

import java.util.List;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface YumariaAddon {
    String id();

    String displayName();

    String version();

    List<String> providedProfessions();
}
