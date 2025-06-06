package dev.ua.ikeepcalm.mythicBedwars.model.feature.vote;

import de.marcely.bedwars.api.arena.Arena;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VotingManager {

    private static final Logger log = LoggerFactory.getLogger(VotingManager.class);
    private final MythicBedwars plugin;
    private final Map<String, VotingSession> arenaSessions = new ConcurrentHashMap<>();

    public VotingManager(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    public void startVoting(Arena arena) {
        if (!plugin.getConfigManager().isGloballyEnabled() || !plugin.getConfigManager().isArenaEnabled(arena.getName())) {
            log.info("Arena " + arena.getName() + " is not enabled");
            return;
        }

        log.info("Starting voting");

        VotingSession session = new VotingSession(arena, plugin);
        arenaSessions.put(arena.getName(), session);
        session.start();

        log.info("Broadcasted msg ");

        for (Player player : arena.getPlayers()) {
            log.info("Player " + player.getName() + " voted to arena " + arena.getName());
            giveVotingItems(player, arena);
        }
    }

    public void giveVotingItems(Player player, Arena arena) {
        if (arena == null || !hasActiveVoting(arena.getName())) {
            return;
        }

        ItemStack yesItem = createVotingItem(Material.LIME_DYE,
                plugin.getLocaleManager().getMessage("magic.voting.yes_item"),
                plugin.getLocaleManager().getMessage("magic.voting.yes_description"));

        ItemStack noItem = createVotingItem(Material.RED_DYE,
                plugin.getLocaleManager().getMessage("magic.voting.no_item"),
                plugin.getLocaleManager().getMessage("magic.voting.no_description"));

        new BukkitRunnable() {
            @Override
            public void run() {
                log.info("Voting arena " + arena.getName() + " started for " + player.getName());
                player.getInventory().setItem(3, yesItem);
                player.getInventory().setItem(5, noItem);
            }
        }.runTaskLater(plugin, 60);
    }

    private ItemStack createVotingItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    public void removeVotingItems(Player player) {
        player.getInventory().setItem(3, null);
        player.getInventory().setItem(5, null);
    }

    public void handleVoteClick(Player player, Material material) {
        Arena arena = de.marcely.bedwars.api.BedwarsAPI.getGameAPI().getArenaByPlayer(player);
        if (arena == null) return;

        VotingSession session = arenaSessions.get(arena.getName());
        if (session == null || !session.isActive()) return;

        boolean vote = material == Material.LIME_DYE;
        session.castVote(player.getUniqueId(), vote);

        String messageKey = vote ? "magic.voting.voted_yes" : "magic.voting.voted_no";
        player.sendMessage(Component.text(plugin.getLocaleManager().getMessage(messageKey), NamedTextColor.GREEN));
    }

    public boolean hasActiveVoting(String arenaName) {
        VotingSession session = arenaSessions.get(arenaName);
        return session != null && session.isActive();
    }

    public boolean isMagicEnabled(String arenaName) {
        VotingSession session = arenaSessions.get(arenaName);
        if (session == null) return true; // Default enabled
        return session.isMagicEnabled();
    }

    public void endVoting(Arena arena) {
        VotingSession session = arenaSessions.remove(arena.getName());
        if (session != null) {
            session.end();

            for (Player player : arena.getPlayers()) {
                removeVotingItems(player);
            }
        }
    }

    public VotingSession getVotingSession(String arenaName) {
        return arenaSessions.get(arenaName);
    }

    public void cleanupArena(String arenaName) {
        arenaSessions.remove(arenaName);
    }

    public VotingGUI getVotingGUI() {
        return new VotingGUI(plugin);
    }
}