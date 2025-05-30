package dev.ua.ikeepcalm.mythicBedwars.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final Map<Integer, PotionPrice> potionPrices = new HashMap<>();
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        loadPotionPrices();
    }
    
    private void loadPotionPrices() {
        potionPrices.clear();
        
        for (String key : config.getConfigurationSection("potion-prices").getKeys(false)) {
            int sequence = Integer.parseInt(key);
            String path = "potion-prices." + key;
            
            PotionPrice price = new PotionPrice(
                config.getInt(path + ".iron", 0),
                config.getInt(path + ".gold", 0),
                config.getInt(path + ".diamond", 0),
                config.getInt(path + ".emerald", 0)
            );
            
            potionPrices.put(sequence, price);
        }
    }
    
    public PotionPrice getPotionPrice(int sequence) {
        return potionPrices.get(sequence);
    }
    
    public double getPassiveActingMultiplier() {
        return config.getDouble("acting.passive-multiplier", 1.0);
    }
    
    public double getKillActingMultiplier() {
        return config.getDouble("acting.kill-multiplier", 5.0);
    }
    
    public double getBedBreakActingMultiplier() {
        return config.getDouble("acting.bed-break-multiplier", 10.0);
    }
    
    public double getFinalKillActingMultiplier() {
        return config.getDouble("acting.final-kill-multiplier", 7.0);
    }
    
    public int getPassiveActingAmount() {
        return config.getInt("acting.passive-amount", 10);
    }
    
    public boolean isSkipRitualsEnabled() {
        return config.getBoolean("skip-rituals", true);
    }

    public record PotionPrice(int iron, int gold, int diamond, int emerald) {}
}