package dev.ua.ikeepcalm.mythicBedwars.model.database;

import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import dev.ua.ikeepcalm.mythicBedwars.manager.StatisticsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseMigration {
    
    private final MythicBedwars plugin;
    private final SQLiteDatabase database;
    
    public DatabaseMigration(MythicBedwars plugin, SQLiteDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    public CompletableFuture<Boolean> migrateFromYaml() {
        File yamlFile = new File(plugin.getDataFolder(), "statistics.yml");
        if (!yamlFile.exists()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(yamlFile);
                Map<String, PathwayStats> stats = new HashMap<>();
                
                for (String key : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section != null) {
                        Object rawStat = config.get(key);
                        if (rawStat instanceof PathwayStats) {
                            stats.put(key, (PathwayStats) rawStat);
                        } else if (rawStat instanceof ConfigurationSection) {
                            stats.put(key, new PathwayStats(((ConfigurationSection) rawStat).getValues(false)));
                        } else if (rawStat instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) rawStat;
                            stats.put(key, new PathwayStats(map));
                        }
                    }
                }
                
                if (!stats.isEmpty()) {
                    database.saveStatistics(stats).get();
                    
                    File backupFile = new File(plugin.getDataFolder(), "statistics.yml.backup");
                    if (yamlFile.renameTo(backupFile)) {
                        plugin.getLogger().info("Migration successful! Old statistics.yml backed up as statistics.yml.backup");
                    }
                    return true;
                }
                
                return false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to migrate from YAML", e);
                return false;
            }
        });
    }
}