package dev.ua.ikeepcalm.mythicBedwars.model.feature.vote;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class VotingListener implements Listener {
    
    private final MythicBedwars plugin;
    
    public VotingListener(MythicBedwars plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta()) return;
        
        Material material = item.getType();
        if (material != Material.LIME_DYE && material != Material.RED_DYE) return;
        
        Arena arena = BedwarsAPI.getGameAPI().getArenaByPlayer(player);
        if (arena == null) return;
        
        if (!plugin.getVotingManager().hasActiveVoting(arena.getName())) return;
        
        event.setCancelled(true);
        plugin.getVotingManager().getVotingGUI().openVotingGUI(player);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = plugin.getLocaleManager().getMessage("magic.voting.gui_title");
        if (!event.getView().getTitle().equals(title)) return;
        
        event.setCancelled(true);
        
        Arena arena = BedwarsAPI.getGameAPI().getArenaByPlayer(player);
        if (arena == null) return;
        
        VotingSession session = plugin.getVotingManager().getVotingSession(arena.getName());
        if (session == null || !session.isActive()) {
            player.closeInventory();
            player.sendMessage(Component.text(plugin.getLocaleManager().getMessage("magic.voting.not_active"), NamedTextColor.RED));
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        Material material = clicked.getType();
        
        if (material == Material.LIME_WOOL) {
            session.castVote(player.getUniqueId(), true);
            player.sendMessage(Component.text(plugin.getLocaleManager().getMessage("magic.voting.voted_yes"), NamedTextColor.GREEN));
            player.closeInventory();
            
            plugin.getVotingManager().getVotingGUI().openVotingGUI(player);
            
        } else if (material == Material.RED_WOOL) {
            session.castVote(player.getUniqueId(), false);
            player.sendMessage(Component.text(plugin.getLocaleManager().getMessage("magic.voting.voted_no"), NamedTextColor.RED));
            player.closeInventory();
            
            plugin.getVotingManager().getVotingGUI().openVotingGUI(player);
        }
    }
}