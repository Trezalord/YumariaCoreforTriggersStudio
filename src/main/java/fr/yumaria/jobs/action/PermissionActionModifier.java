package fr.yumaria.jobs.action;

// Repere fichier YumariaJobs: pipeline central des actions reportees par les addons (PermissionActionModifier).

import fr.yumaria.jobs.YumariaJobsPlugin;
import org.bukkit.configuration.ConfigurationSection;

// Role YumariaJobs: Reçoit les actions des addons et les transforme en progression YumariaJobs.
public final class PermissionActionModifier implements ActionModifier {
    private final YumariaJobsPlugin plugin;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PermissionActionModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "permission";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
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
