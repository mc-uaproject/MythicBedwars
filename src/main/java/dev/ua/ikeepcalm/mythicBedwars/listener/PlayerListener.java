package dev.ua.ikeepcalm.mythicBedwars.listener;

import dev.ua.ikeepcalm.coi.api.events.PotionConsumptionEvent;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.coi.domain.pathway.types.FlexiblePathway;
import dev.ua.ikeepcalm.coi.domain.potion.model.SequencePotion;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    @EventHandler(priority = EventPriority.LOW)
    public void onPotionConsume(PotionConsumptionEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getArenaPathwayManager().hasPlayerMagic(player)) {
            return;
        }

        if (plugin.getConfigManager().isSkipRitualsEnabled()) {
            Beyonder beyonder = Beyonder.of(player);
            if (beyonder != null && !beyonder.getPathways().isEmpty()) {
                FlexiblePathway pathway = beyonder.getPathways().getFirst();
                int targetSequence = event.getSequence();

                if (targetSequence == pathway.getLowestSequenceLevel() - 1) {
                    event.setSkipRitual(true);
                }
            }
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
                    player.sendMessage(Component.text("You can only consume potions from your team's pathway!", NamedTextColor.RED));
                }
            }
        }
    }
}