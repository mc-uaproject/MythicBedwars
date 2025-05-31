package dev.ua.ikeepcalm.mythicBedwars.listener;

import dev.ua.ikeepcalm.coi.api.events.AbilityUsageEvent;
import dev.ua.ikeepcalm.coi.api.events.PotionConsumptionEvent;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.coi.domain.pathway.types.FlexiblePathway;
import dev.ua.ikeepcalm.coi.domain.potion.model.SequencePotion;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {

    private final MythicBedwars plugin;

    public PlayerListener(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAbilityUsage(AbilityUsageEvent event) {
        if (!plugin.getArenaPathwayManager().hasPlayerMagic(event.getPlayer())) {
            return;
        }

        if (hasBedNearby(event.getPlayer().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean hasBedNearby(Location location) {
        int radius = 25;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location checkLoc = location.clone().add(x, y, z);
                    if (Tag.BEDS.isTagged(checkLoc.getBlock().getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
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
                    player.sendMessage(Component.text("Атятя! Лише магію свого шляху!", NamedTextColor.RED));
                }
            }
        }
    }
}