package fr.yumaria.jobs.action;

import fr.yumaria.jobs.YumariaJobsPlugin;
import org.bukkit.configuration.ConfigurationSection;

public final class PermissionActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    public PermissionActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "permission";
    }

    @Override
    public ModifierResult apply(ActionModifierContext context) {
        double multiplier = 1.0D;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("progression.permission-multipliers");
        if (section == null) {
            section = plugin.getConfig().getConfigurationSection("xp.permission-multipliers");
        }
        if (section != null) {
            for (String permission : section.getKeys(false)) {
                if (context.player().hasPermission(permission)) {
                    multiplier = Math.max(multiplier, section.getDouble(permission, 1.0D));
                }
            }
        }
        return new ModifierResult(id(), ActionModifierTarget.XP, multiplier, "best permission XP multiplier");
    }
}
