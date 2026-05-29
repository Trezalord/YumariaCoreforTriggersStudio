package fr.yumaria.jobs.progression;

// Repere fichier YumariaJobs: multiplicateurs et calculs XP (PermissionXpModifier).

import fr.yumaria.jobs.YumariaJobsPlugin;
import org.bukkit.configuration.ConfigurationSection;

// Role YumariaJobs: Calcule les multiplicateurs et le resultat final de l XP.
public final class PermissionXpModifier implements XpModifier {
    private final YumariaJobsPlugin plugin;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public PermissionXpModifier(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String id() {
        return "permission";
    }

    @Override
    // Annotation YumariaJobs: Applique un calcul, une recompense ou une etape du pipeline.
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
