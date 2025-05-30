package dev.ua.ikeepcalm.bedwarsmagic.manager;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.game.shop.price.ItemShopPrice;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import dev.ua.ikeepcalm.mythicBedwars.manager.ConfigManager;
import dev.ua.ikeepcalm.mythicBedwars.model.PotionShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopManager {

    private final MythicBedwars plugin;
    private final List<PotionShopItem> registeredItems = new ArrayList<>();

    public ShopManager(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    public void registerPotionItems() {
        for (int sequence = 9; sequence >= 0; sequence--) {
            PotionShopItem item = createPotionShopItem(sequence);
            if (item != null) {
                registeredItems.add(item);
                BedwarsAPI.getGameAPI().registerShopItem(item);
            }
        }
    }

    private PotionShopItem createPotionShopItem(int sequence) {
        ConfigManager.PotionPrice price = plugin.getConfigManager().getPotionPrice(sequence);
        if (price == null) return null;

        ItemStack displayItem = new ItemStack(Material.POTION);
        ItemMeta meta = displayItem.getItemMeta();
        meta.displayName(Component.text("Sequence " + sequence + " Potion", NamedTextColor.LIGHT_PURPLE));
        displayItem.setItemMeta(meta);

        PotionShopItem item = new PotionShopItem("magic_potion_" + sequence, displayItem, sequence);

        if (price.iron() > 0) {
            item.addPrice(new ItemShopPrice(Material.IRON_INGOT, price.getIron()));
        }
        if (price.gold() > 0) {
            item.addPrice(new ItemShopPrice(Material.GOLD_INGOT, price.getGold()));
        }
        if (price.diamond() > 0) {
            item.addPrice(new ItemShopPrice(Material.DIAMOND, price.diamond()));
        }
        if (price.emerald() > 0) {
            item.addPrice(new ItemShopPrice(Material.EMERALD, price.emerald()));
        }

        return item;
    }

}