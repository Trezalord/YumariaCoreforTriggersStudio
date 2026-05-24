package fr.yumaria.jobs.api;

import java.util.Collection;
import java.util.Optional;

public interface YumariaAddonRegistry {
    void registerAddon(YumariaAddon addon);

    void unregisterAddon(String addonId);

    boolean isRegistered(String addonId);

    Optional<YumariaAddon> getAddon(String addonId);

    Collection<YumariaAddon> getAddons();
}
