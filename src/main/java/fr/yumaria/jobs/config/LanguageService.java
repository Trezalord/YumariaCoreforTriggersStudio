package fr.yumaria.jobs.config;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public final class LanguageService {
    private final YumariaJobsPlugin plugin;
    private FileConfiguration language;

    public LanguageService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

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

    public String raw(String path) {
        if (language == null) {
            return path;
        }
        return language.getString(path, path);
    }

    public String prefixed(String path, Map<String, String> placeholders) {
        return Text.color(Text.placeholders(raw("prefix") + raw(path), placeholders));
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(prefixed(path, placeholders));
    }

    private void saveIfMissing(String resource) {
        File target = new File(plugin.getDataFolder(), resource);
        if (!target.isFile()) {
            plugin.saveResource(resource, false);
        }
    }
}
