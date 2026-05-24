package fr.yumaria.jobs.progression;

import fr.yumaria.jobs.YumariaJobsPlugin;
import org.bukkit.configuration.ConfigurationSection;

public final class PermissionXpModifier implements XpModifier {
    private final YumariaJobsPlugin plugin;

    public PermissionXpModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "permission";
    }

    @Override
    public XpModifierResult apply(XpModifierContext context) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("xp.permission-multipliers");
        if (section == null) {
            return XpModifierResult.one("no permission multipliers");
        }
        double multiplier = 1.0D;
        for (String permission : section.getKeys(false)) {
            if (context.player().hasPermission(permission)) {
                multiplier *= Math.max(0.0D, section.getDouble(permission, 1.0D));
            }
        }
        return new XpModifierResult(multiplier, "permission aggregate");
    }
}
