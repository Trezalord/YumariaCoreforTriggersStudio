package fr.yumaria.jobs.api;

// Repere fichier YumariaJobs: contrat public utilise par les addons Yumaria (YumariaAddonRegistry).

import java.util.Collection;
import java.util.Optional;

// Role YumariaJobs: Definit le contrat public que les addons Yumaria doivent utiliser.
public interface YumariaAddonRegistry {
    void registerAddon(YumariaAddon addon);

    void unregisterAddon(String addonId);

    boolean isRegistered(String addonId);

    Optional<YumariaAddon> getAddon(String addonId);

    Collection<YumariaAddon> getAddons();
}
