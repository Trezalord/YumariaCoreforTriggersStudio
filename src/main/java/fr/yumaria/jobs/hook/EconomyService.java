package fr.yumaria.jobs.hook;

import fr.yumaria.jobs.YumariaJobsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyService {
    private final YumariaJobsPlugin plugin;
    private Economy economy;

    public EconomyService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        economy = null;
        if (!plugin.getConfig().getBoolean("hooks.vault.enabled", true)) {
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.debug("Vault not found. Money rewards will use commands only.");
            return;
        }
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            economy = provider.getProvider();
            plugin.debug("Vault economy hooked: " + economy.getName());
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public void deposit(Player player, double amount) {
        if (economy == null || amount <= 0.0D) {
            return;
        }
        economy.depositPlayer(player, amount);
    }
}
