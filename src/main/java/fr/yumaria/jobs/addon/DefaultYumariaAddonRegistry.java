package fr.yumaria.jobs.addon;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.api.YumariaAddon;
import fr.yumaria.jobs.api.YumariaAddonRegistry;
import fr.yumaria.jobs.api.event.YumariaAddonRegisterEvent;
import fr.yumaria.jobs.api.event.YumariaAddonUnregisterEvent;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultYumariaAddonRegistry implements YumariaAddonRegistry {
    private final YumariaJobsPlugin plugin;
    private final Map<String, YumariaAddon> addons = new ConcurrentHashMap<>();

    public DefaultYumariaAddonRegistry(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerAddon(YumariaAddon addon) {
        if (addon == null || addon.id() == null || addon.id().isBlank()) {
            return;
        }
        String key = Text.normalizeId(addon.id());
        addons.put(key, addon);
        plugin.debugJobs("Addon registered: id=" + addon.id()
                + ", version=" + addon.version()
                + ", professions=" + addon.providedProfessions());
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(new YumariaAddonRegisterEvent(addon));
        }
    }

    @Override
    public void unregisterAddon(String addonId) {
        String key = Text.normalizeId(addonId);
        YumariaAddon removed = addons.remove(key);
        plugin.debugJobs("Addon unregistered: id=" + addonId + ", existed=" + (removed != null));
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(new YumariaAddonUnregisterEvent(addonId, removed));
        }
    }

    @Override
    public boolean isRegistered(String addonId) {
        return addons.containsKey(Text.normalizeId(addonId));
    }

    @Override
    public Optional<YumariaAddon> getAddon(String addonId) {
        return Optional.ofNullable(addons.get(Text.normalizeId(addonId)));
    }

    @Override
    public Collection<YumariaAddon> getAddons() {
        return List.copyOf(addons.values());
    }
}
