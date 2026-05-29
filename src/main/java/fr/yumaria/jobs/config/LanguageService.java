package fr.yumaria.jobs.config;

// Repere fichier YumariaJobs: chargement et lecture des fichiers de configuration (LanguageService).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

// Role YumariaJobs: Charge les fichiers YAML et resout les definitions configurables.
public final class LanguageService {
    private final YumariaJobsPlugin plugin;
    private FileConfiguration language;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public LanguageService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    // Annotation YumariaJobs: Recharge la configuration sans effacer les donnees joueur en memoire.
    public void reload() {
        String languageCode = plugin.getConfig().getString("language", "fr");
        saveIfMissing("lang/fr.yml");
        saveIfMissing("lang/en.yml");
        File file = new File(plugin.getDataFolder(), "lang/" + languageCode + ".yml");
        if (!file.isFile()) {
            file = new File(plugin.getDataFolder(), "lang/fr.yml");
        }
        language = YamlConfiguration.loadConfiguration(file);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String raw(String path) {
        if (language == null) {
            return path;
        }
        return language.getString(path, path);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String prefixed(String path, Map<String, String> placeholders) {
        return Text.color(Text.placeholders(raw("prefix") + raw(path), placeholders));
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(prefixed(path, placeholders));
    }

    // Annotation YumariaJobs: Prepare ou execute la sauvegarde des donnees sans bloquer inutilement le serveur.
    private void saveIfMissing(String resource) {
        File target = new File(plugin.getDataFolder(), resource);
        if (!target.isFile()) {
            plugin.saveResource(resource, false);
        }
    }
}
