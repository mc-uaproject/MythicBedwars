package dev.ua.ikeepcalm.mythicBedwars;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.ExtensionService;
import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import dev.ua.ikeepcalm.coi.pathways.Pathways;
import dev.ua.ikeepcalm.mythicBedwars.cmd.CommandManager;
import dev.ua.ikeepcalm.mythicBedwars.cmd.impls.SpectatorCommand;
import dev.ua.ikeepcalm.mythicBedwars.config.ConfigLoader;
import dev.ua.ikeepcalm.mythicBedwars.config.LocaleLoader;
import dev.ua.ikeepcalm.mythicBedwars.domain.balancer.PathwayBalancer;
import dev.ua.ikeepcalm.mythicBedwars.domain.core.PathwayManager;
import dev.ua.ikeepcalm.mythicBedwars.domain.core.ShopManager;
import dev.ua.ikeepcalm.mythicBedwars.domain.core.StatisticsManager;
import dev.ua.ikeepcalm.mythicBedwars.domain.runnable.ActingProgressionTask;
import dev.ua.ikeepcalm.mythicBedwars.domain.runnable.PathwayVerificationTask;
import dev.ua.ikeepcalm.mythicBedwars.domain.spectator.SpectatorManager;
import dev.ua.ikeepcalm.mythicBedwars.domain.stats.db.DatabaseMigration;
import dev.ua.ikeepcalm.mythicBedwars.domain.stats.db.PathwayStats;
import dev.ua.ikeepcalm.mythicBedwars.domain.stats.db.SQLiteDatabase;
import dev.ua.ikeepcalm.mythicBedwars.domain.voting.service.VotingManager;
import dev.ua.ikeepcalm.mythicBedwars.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class MythicBedwars extends JavaPlugin {

    private static MythicBedwars instance;
    private ConfigLoader configLoader;
    private LocaleLoader localeLoader;
    private PathwayManager pathwayManager;
    private ShopManager shopManager;
    private StatisticsManager statisticsManager;
    private CommandManager commandManager;
    private PathwayBalancer pathwayBalancer;
    private SpectatorManager spectatorManager;
    private VotingManager votingManager;

    private SQLiteDatabase database;
    private BukkitTask periodicSaveTask;
    private int saveIntervalSeconds;

    @Override
    public void onEnable() {
        instance = this;

        ConfigurationSerialization.registerClass(PathwayStats.class);

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

                if (apiVersion < supportedAPIVersion) throw new IllegalStateException();
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

        configLoader = new ConfigLoader(this);
        configLoader.loadConfig();

        this.saveIntervalSeconds = configLoader.getAutoSaveInterval();

        localeLoader = new LocaleLoader(this, LocaleLoader.Locale.UK);
        localeLoader.loadLocales();

        pathwayManager = new PathwayManager();
        shopManager = new ShopManager(this);

        database = new SQLiteDatabase(this);
        database.initialize();

        pathwayBalancer = new PathwayBalancer(this);
        commandManager = new CommandManager(this);
        spectatorManager = new SpectatorManager(this);
        votingManager = new VotingManager(this);

        Objects.requireNonNull(getCommand("mythicbedwars")).setExecutor(commandManager);
        Objects.requireNonNull(getCommand("mythicbedwars")).setTabCompleter(commandManager);

        Objects.requireNonNull(getCommand("mb")).setExecutor(commandManager);
        Objects.requireNonNull(getCommand("mb")).setTabCompleter(commandManager);

        SpectatorCommand spectatorCommand = new SpectatorCommand(this);
        Objects.requireNonNull(getCommand("mbspec")).setExecutor(spectatorCommand);
        Objects.requireNonNull(getCommand("mbspec")).setTabCompleter(spectatorCommand);

        statisticsManager = new StatisticsManager(this);

        CompletableFuture<Void> loadingFuture = loadStatistics().thenRun(() -> {
            registerEvents();
            registerShopItems();

            Bukkit.getScheduler().runTask(this, this::registerPlanStatistics);

            new ActingProgressionTask(this).runTaskTimer(this, 20L, 20L);
            new PathwayVerificationTask(this).runTaskTimer(this, 200L, 400L);

            if (this.saveIntervalSeconds > 0 && database != null && statisticsManager != null) {
                long saveIntervalTicks = this.saveIntervalSeconds * 20L;
                this.periodicSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                    if (database.isConnected() && statisticsManager.getPathwayStatistics() != null && !statisticsManager.getPathwayStatistics().isEmpty()) {
                        getLogger().info("Periodically saving statistics...");
                        database.saveStatistics(statisticsManager.getPathwayStatistics()).thenRun(() -> getLogger().info("Periodic statistics save complete.")).exceptionally(ex -> {
                            getLogger().log(Level.WARNING, "Periodic statistics save failed.", ex);
                            return null;
                        });
                    } else if (!database.isConnected()) {
                        getLogger().warning("Cannot perform periodic statistics save: Database not connected.");
                    }
                }, saveIntervalTicks, saveIntervalTicks);
                getLogger().info("Scheduled periodic statistics save every " + this.saveIntervalSeconds + " seconds.");
            } else if (this.saveIntervalSeconds <= 0) {
                getLogger().info("Periodic statistics saving is disabled (save-interval-seconds <= 0).");
            }

            getLogger().info("BedwarsMagicAddon enabled!");
        });
    }

    @Override
    public void onDisable() {
        if (periodicSaveTask != null && !periodicSaveTask.isCancelled()) {
            periodicSaveTask.cancel();
            getLogger().info("Cancelled periodic statistics save task.");
        }

        if (spectatorManager != null) {
            spectatorManager.shutdown();
            getLogger().info("Spectator manager shut down.");
        }

        if (statisticsManager != null && database != null && database.isConnected()) {
            getLogger().info("Saving final statistics synchronously on disable...");
            database.saveStatistics(statisticsManager.getPathwayStatistics(), true).thenRun(() -> {
                getLogger().info("Final statistics saved.");
            }).exceptionally(ex -> {
                getLogger().severe("An unexpected issue occurred with the final save's CompletableFuture: " + ex.getMessage());
                return null;
            }).thenRun(() -> {
                database.close();
                getLogger().info("Database connection closed.");
            });
        } else {
            if (database != null && !database.isConnected()) {
                getLogger().warning("Could not save final statistics: Database not connected.");
            } else if (database != null) {
                database.close();
                getLogger().info("Database connection closed (statistics or manager was null).");
            }
        }

        if (pathwayManager != null) {
            pathwayManager.cleanupAll();
        }

        getLogger().info("BedwarsMagicAddon disabled!");
    }

    private CompletableFuture<Void> loadStatistics() {
        DatabaseMigration migration = new DatabaseMigration(this, database);

        return migration.migrateFromYaml().thenCompose(migrated -> {
            if (migrated) {
                getLogger().info("Successfully migrated statistics from YAML to SQLite!");
            }

            return database.loadStatistics().thenAccept(loadedStats -> {
                if (loadedStats != null && !loadedStats.isEmpty()) {
                    statisticsManager.setPathwayStatistics(loadedStats);
                    getLogger().info("Loaded " + loadedStats.size() + " pathway statistics entries from database.");
                } else {
                    getLogger().info("No statistics data found in database.");
                }
            });
        });
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new ArenaListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DamageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ServerShutdownListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SpectatorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VotingListener(this), this);
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

                        CapabilityService.getInstance().registerEnableListener(isPlanEnabled -> {
                            if (isPlanEnabled) registerPlanStatistics();
                        });
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Failed to register Plan statistics: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static MythicBedwars getInstance() {
        return instance;
    }

    public PathwayBalancer getPathwayBalancer() {
        return pathwayBalancer;
    }

    public ConfigLoader getConfigManager() {
        return configLoader;
    }

    public PathwayManager getArenaPathwayManager() {
        return pathwayManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public LocaleLoader getLocaleManager() {
        return localeLoader;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public VotingManager getVotingManager() {
        return votingManager;
    }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public List<String> getArenaNames() {
        return BedwarsAPI.getGameAPI().getArenas().stream()
                .map(Arena::getName)
                .collect(Collectors.toList());
    }

    public List<String> getAvailablePathways() {
        return new ArrayList<>(Pathways.allPathways.keySet());
    }
}