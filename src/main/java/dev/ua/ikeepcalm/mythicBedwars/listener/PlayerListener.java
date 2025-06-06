package dev.ua.ikeepcalm.mythicBedwars.listener;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import dev.ua.ikeepcalm.coi.api.events.AbilityUsageEvent;
import dev.ua.ikeepcalm.coi.api.events.MagicBlockEvent;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.coi.domain.pathway.types.FlexiblePathway;
import dev.ua.ikeepcalm.coi.domain.potion.model.SequencePotion;
import dev.ua.ikeepcalm.coi.pathways.darkness.abilities.nightmare.Nightmare;
import dev.ua.ikeepcalm.coi.pathways.demoness.abilities.ThreadHands;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class PlayerListener implements Listener {

    private final MythicBedwars plugin;

    public PlayerListener(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpecificAbilityUsage(AbilityUsageEvent event) {
        Arena arena = BedwarsAPI.getGameAPI().getArenaByPlayer(event.getPlayer());
        if (arena == null || !plugin.getVotingManager().isMagicEnabled(arena.getName())) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.getArenaPathwayManager().hasPlayerMagic(event.getPlayer())) {
            return;
        }

        List<Class<?>> blockedAbilities = List.of(ThreadHands.class, Nightmare.class);
        if (blockedAbilities.stream().anyMatch(clazz -> clazz.isInstance(event.getAbility()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onNonArenaAbilityUsage(AbilityUsageEvent event) {
        Arena arena = BedwarsAPI.getGameAPI().getArenaByPlayer(event.getPlayer());
        if (arena == null) {
            event.setCancelled(true);
        } else {
            if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(MagicBlockEvent event) {
        @Nullable Collection<Arena> arenas = BedwarsAPI.getGameAPI().getArenaByLocation(event.getLocation());
        if (arenas != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!plugin.getArenaPathwayManager().hasPlayerMagic(player)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (meta.getPersistentDataContainer().has(SequencePotion.getPotionKey(), PersistentDataType.INTEGER)) {
            Beyonder beyonder = Beyonder.of(player);
            if (beyonder != null && !beyonder.getPathways().isEmpty()) {
                FlexiblePathway currentPathway = beyonder.getPathways().getFirst();

                if (!currentPathway.getName().equals(plugin.getArenaPathwayManager().getPlayerData(player).getPathway())) {
                    event.setCancelled(true);
                }
            }
        }
    }
}