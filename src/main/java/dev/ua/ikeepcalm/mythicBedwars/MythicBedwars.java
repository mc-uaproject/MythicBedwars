package dev.ua.ikeepcalm.mythicBedwars;

import de.marcely.bedwars.api.BedwarsAPI;
import dev.ua.ikeepcalm.mythicBedwars.listener.ArenaListener;
import dev.ua.ikeepcalm.mythicBedwars.listener.PlayerListener;
import dev.ua.ikeepcalm.mythicBedwars.manager.PathwayManager;
import dev.ua.ikeepcalm.mythicBedwars.manager.ConfigManager;
import dev.ua.ikeepcalm.mythicBedwars.manager.ShopManager;
import dev.ua.ikeepcalm.mythicBedwars.runnable.ActingProgressionTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MythicBedwars extends JavaPlugin {

    private static MythicBedwars instance;
    private ConfigManager configManager;
    private PathwayManager pathwayManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;

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
            } catch(Exception e) {
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

        pathwayManager = new PathwayManager();
        shopManager = new ShopManager(this);

        registerEvents();
        registerShopItems();

        new ActingProgressionTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("BedwarsMagicAddon enabled!");
    }

    @Override
    public void onDisable() {
        pathwayManager.cleanupAll();
        getLogger().info("BedwarsMagicAddon disabled!");
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new ArenaListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void registerShopItems() {
        BedwarsAPI.onReady(() -> {
            shopManager.registerPotionItems();
        });
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
}
