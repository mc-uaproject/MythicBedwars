package dev.ua.ikeepcalm.mythicBedwars;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.ExtensionService;
import de.marcely.bedwars.api.BedwarsAPI;
import dev.ua.ikeepcalm.mythicBedwars.listener.ArenaListener;
import dev.ua.ikeepcalm.mythicBedwars.listener.DamageListener;
import dev.ua.ikeepcalm.mythicBedwars.listener.PlayerListener;
import dev.ua.ikeepcalm.mythicBedwars.manager.*;
import dev.ua.ikeepcalm.mythicBedwars.model.StatisticsDatabase;
import dev.ua.ikeepcalm.mythicBedwars.runnable.ActingProgressionTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;

public final class MythicBedwars extends JavaPlugin {

    private static MythicBedwars instance;
    private ConfigManager configManager;
    private LocaleManager localeManager;
    private PathwayManager pathwayManager;
    private ShopManager shopManager;
    private StatisticsManager statisticsManager;
    private StatisticsDatabase statisticsDatabase;

    @Override
    public void onEnable() {
        instance = this;

        ConfigurationSerialization.registerClass(StatisticsManager.PathwayStats.class);

        if (!Bukkit.getPluginManager().isPluginEnabled("MBedwars")) {
            getLogger().severe("MBedwars not found! Disabling addon...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
            final int supportedAPIVersion = 203;
            final String supportedVersionName = "5.5.3";

            try {
                Class<?> apiClass = Class.forName("de.marcely.bedwars.api.BedwarsAPI");
                int apiVersion = (int) apiClass.getMethod("getAPIVersion").invoke(null);

                if (apiVersion < supportedAPIVersion)
                    throw new IllegalStateException();
            } catch (Exception e) {
                getLogger().warning("Sorry, your installed version of MBedwars is not supported. Please install at least v" + supportedVersionName);
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("CircleOfImagination")) {
            getLogger().severe("CircleOfImagination not found! Disabling addon...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        localeManager = new LocaleManager(this, LocaleManager.Locale.UK);
        localeManager.loadLocales();

        pathwayManager = new PathwayManager();
        shopManager = new ShopManager(this);

        statisticsDatabase = new StatisticsDatabase(this);

        statisticsManager = new StatisticsManager(this);
        Map<String, StatisticsManager.PathwayStats> loadedStats = statisticsDatabase.loadStatistics();
        if (loadedStats != null && !loadedStats.isEmpty()) {
            statisticsManager.setPathwayStatistics(loadedStats);
            getLogger().info("Loaded " + loadedStats.size() + " pathway statistics entries.");
        } else {
            getLogger().info("No statistics data found or loaded.");
        }

        registerEvents();
        registerShopItems();
        registerPlanStatistics();

        new ActingProgressionTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("BedwarsMagicAddon enabled!");
    }

    @Override
    public void onDisable() {
        if (statisticsManager != null && statisticsDatabase != null) {
            statisticsDatabase.saveStatistics(statisticsManager.getPathwayStatistics());
            getLogger().info("Statistics saved.");
        }

        if (pathwayManager != null) {
            pathwayManager.cleanupAll();
        }

        getLogger().info("BedwarsMagicAddon disabled!");
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new ArenaListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DamageListener(this), this);
    }

    private void registerShopItems() {
        BedwarsAPI.onReady(() -> {
            shopManager.registerPotionItems();
        });
    }

    private void registerPlanStatistics() {
        if (Bukkit.getPluginManager().isPluginEnabled("Plan")) {

            if (statisticsManager == null) {
                statisticsManager = new StatisticsManager(this);
            }

            try {
                if (CapabilityService.getInstance().hasCapability("DATA_EXTENSION_VALUES")) {
                    Optional<ExtensionService> service = Optional.ofNullable(ExtensionService.getInstance());
                    if (service.isPresent()) {
                        service.get().register(statisticsManager);
                        getLogger().info("Successfully registered Plan statistics!");


                        CapabilityService.getInstance().registerEnableListener(
                                isPlanEnabled -> {
                                    if (isPlanEnabled) registerPlanStatistics();
                                }
                        );
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Failed to register Plan statistics: " + e.getMessage());
                e.printStackTrace(); // For more detailed error logging during development
            }
        }
    }

    public static MythicBedwars getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PathwayManager getArenaPathwayManager() {
        return pathwayManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }
}