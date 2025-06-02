package dev.ua.ikeepcalm.mythicBedwars.manager;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.game.specialitem.SpecialItem;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import dev.ua.ikeepcalm.mythicBedwars.model.item.PotionShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopManager {

    private final MythicBedwars plugin;

    public ShopManager(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    public void registerPotionItems() {
        for (int sequence = 9; sequence >= 0; sequence--) {
            ItemStack specialItem = createPotionSpecialItem(sequence);
            if (specialItem != null) {
                String id = "magic_potion_" + sequence;
                String itemName = plugin.getLocaleManager().formatMessage("magic.shop.potion.name", "sequence", String.valueOf(sequence));
                SpecialItem createdItem = BedwarsAPI.getGameAPI().registerSpecialItem(
                        id,
                        MythicBedwars.getInstance(),
                        itemName,
                        specialItem
                );

                if (createdItem != null) {
                    createdItem.setHandler(new PotionShopItem(id, specialItem, sequence));
                }
            }
        }
    }

    private ItemStack createPotionSpecialItem(int sequence) {
        ItemStack displayItem = new ItemStack(Material.POTION);
        ItemMeta meta = displayItem.getItemMeta();

        String itemName = plugin.getLocaleManager().formatMessage("magic.shop.potion.name", "sequence", String.valueOf(sequence));
        meta.displayName(Component.text(itemName, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        String loreLine1 = plugin.getLocaleManager().formatMessage("magic.shop.potion.lore.0", "sequence", String.valueOf(sequence));
        String loreLine2 = plugin.getLocaleManager().formatMessage("magic.shop.potion.lore.1");

        lore.add(Component.text(loreLine1, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(loreLine2, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(plugin.getLocaleManager().getMessage("magic.shop.potion.lore.2"), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        displayItem.setItemMeta(meta);

        return displayItem;
    }
}
