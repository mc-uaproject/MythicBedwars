package dev.ua.ikeepcalm.mythicBedwars.model;

import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import dev.ua.ikeepcalm.mythicBedwars.manager.StatisticsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class StatisticsDatabase {

    private final MythicBedwars plugin;
    private final File statisticsFile;

    public StatisticsDatabase(MythicBedwars plugin) {
        this.plugin = plugin;
        this.statisticsFile = new File(plugin.getDataFolder(), "statistics.yml");
    }

    public void saveStatistics(Map<String, StatisticsManager.PathwayStats> pathwayStatistics) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(statisticsFile);
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        if (pathwayStatistics != null) {
            for (Map.Entry<String, StatisticsManager.PathwayStats> entry : pathwayStatistics.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(statisticsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save statistics to " + statisticsFile.getName(), e);
        }
    }

    public Map<String, StatisticsManager.PathwayStats> loadStatistics() {
        if (!statisticsFile.exists()) {
            return new HashMap<>();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(statisticsFile);
        Map<String, StatisticsManager.PathwayStats> loadedStats = new HashMap<>();

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section != null) {
                Object rawStat = config.get(key);
                if (rawStat instanceof StatisticsManager.PathwayStats) {
                     loadedStats.put(key, (StatisticsManager.PathwayStats) rawStat);
                } else if (rawStat instanceof ConfigurationSection) {
                    loadedStats.put(key, new StatisticsManager.PathwayStats(((ConfigurationSection) rawStat).getValues(false)));
                } else if (rawStat instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) rawStat;
                    loadedStats.put(key, new StatisticsManager.PathwayStats(map));
                }
            }
        }
        return loadedStats;
    }
}