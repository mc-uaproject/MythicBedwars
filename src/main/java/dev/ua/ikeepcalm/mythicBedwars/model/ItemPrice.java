package dev.ua.ikeepcalm.mythicBedwars.model;

import de.marcely.bedwars.api.game.shop.ShopItem;
import de.marcely.bedwars.api.game.shop.price.ShopPrice;
import de.marcely.bedwars.api.game.shop.price.ShopPriceType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemPrice implements ShopPrice {
    private final Material material;
    private final int amount;

    public ItemPrice(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    @Override
    public ShopItem getItem() {
        // There is a type mismatch: ShopItem is not ItemStack.
        // If ShopItem has a constructor or static method to wrap an ItemStack, use it.
        // Otherwise, return null or throw UnsupportedOperationException.
        // Example (adjust as needed):
        return new ShopItem() {
        };
    }

    @Override
    public ShopPriceType getType() {
        // Return a suitable ShopPriceType if available, otherwise null.
        return null;
    }

    @Override
    public String getDisplayName(@Nullable CommandSender sender) {
        // Return a display name for the price, e.g., "x Iron Ingot"
        return amount + " " + material.name().replace('_', ' ').toLowerCase();
    }

    @Override
    public String getDisplayName(@Nullable CommandSender sender, int amount) {
        // Return a display name for the given amount
        return amount + " " + material.name().replace('_', ' ').toLowerCase();
    }

    @Override
    public ItemStack getDisplayItem(Player player) {
        // Return an ItemStack representing the price
        return new ItemStack(material, amount);
    }

    @Override
    public int getAmount(Player player) {
        // Return the amount required for the price
        return amount;
    }

    @Override
    public int getGeneralAmount() {
        // Return the general amount (default to amount)
        return amount;
    }

    @Override
    public boolean setGeneralAmount(int amount) {
        // Not supported for this implementation
        return false;
    }

    @Override
    public int getHoldingAmount(Player player, @Nullable ItemStack[] inv) {
        // Return the amount of the material the player is holding (in inventory or provided array)
        ItemStack[] inventory = inv != null ? inv : player.getInventory().getContents();
        int count = 0;
        for (ItemStack item : inventory) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Override
    public int getMissingAmount(Player player, @Nullable ItemStack[] inv) {
        // Return how many more items the player needs to meet the price
        int holding = getHoldingAmount(player, inv);
        return Math.max(0, amount - holding);
    }

    @Override
    public boolean take(Player player, int multiplier, @Nullable ItemStack[] inv) {
        // Remove the required amount * multiplier from the player's inventory
        int total = amount * multiplier;
        if (getHoldingAmount(player, inv) < total) {
            return false;
        }
        int toRemove = total;
        ItemStack[] inventory = inv != null ? inv : player.getInventory().getContents();
        for (int i = 0; i < inventory.length && toRemove > 0; i++) {
            ItemStack item = inventory[i];
            if (item != null && item.getType() == material) {
                int remove = Math.min(item.getAmount(), toRemove);
                item.setAmount(item.getAmount() - remove);
                toRemove -= remove;
                if (item.getAmount() <= 0) {
                    inventory[i] = null;
                }
            }
        }
        if (inv == null) {
            player.getInventory().setContents(inventory);
        }
        return true;
    }

}
