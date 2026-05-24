package fr.yumaria.jobs.config;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.job.RankDefinition;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RankService {
    private final YumariaJobsPlugin plugin;
    private List<RankDefinition> ranks = List.of(new RankDefinition(1, "Apprenti"));

    public RankService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        List<RankDefinition> loaded = new ArrayList<>();
        for (Object object : plugin.getConfig().getList("ranks", List.of())) {
            if (object instanceof ConfigurationSection section) {
                loaded.add(new RankDefinition(section.getInt("min-level", 1), section.getString("name", "Apprenti")));
            } else if (object instanceof java.util.Map<?, ?> map) {
                int minLevel = parseInt(map.get("min-level"), 1);
                Object nameValue = map.containsKey("name") ? map.get("name") : "Apprenti";
                String name = String.valueOf(nameValue);
                loaded.add(new RankDefinition(minLevel, name));
            }
        }
        loaded.sort(Comparator.comparingInt(RankDefinition::minLevel).reversed());
        if (!loaded.isEmpty()) {
            ranks = List.copyOf(loaded);
        }
    }

    public String rankForLevel(int level) {
        for (RankDefinition rank : ranks) {
            if (level >= rank.minLevel()) {
                return rank.name();
            }
        }
        return ranks.get(ranks.size() - 1).name();
    }

    private int parseInt(Object object, int fallback) {
        if (object instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(object));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
