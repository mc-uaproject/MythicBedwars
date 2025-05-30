package dev.ua.ikeepcalm.mythicBedwars.model;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.Team;
import de.marcely.bedwars.api.game.shop.ShopItem;
import de.marcely.bedwars.api.game.shop.product.ItemShopProduct;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.coi.domain.pathway.types.FlexiblePathway;
import dev.ua.ikeepcalm.coi.domain.potion.model.SequencePotion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PotionShopItem implements ShopItem {
        private final int sequence;

        public PotionShopItem(String id, ItemStack displayItem, int sequence) {
            this.sequence = sequence;
        }

        @Override
        public ItemShopProduct createProduct() {
            return new ItemShopProduct(this) {
                @Override
                public void handlePurchase(Player player) {
                    Arena arena = BedwarsAPI.getGameAPI().getArenaByPlayer(player);
                    if (arena == null) return;

                    Team team = arena.getPlayerTeam(player);
                    if (team == null) return;

                    ArenaPathwayManager manager = plugin.getArenaPathwayManager();
                    PlayerMagicData data = manager.getPlayerData(player);

                    if (data == null) {
                        manager.initializePlayerMagic(player, arena, team);
                        data = manager.getPlayerData(player);
                    }

                    Beyonder beyonder = Beyonder.of(player);
                    if (beyonder == null) return;

                    FlexiblePathway pathway = beyonder.getPathways().get(0);
                    if (pathway == null) return;

                    if (sequence > pathway.getLowestSequence() - 1) {
                        player.sendMessage(Component.text("You must advance sequences in order!", NamedTextColor.RED));
                        return;
                    }

                    Sequence targetSequence = null;
                    for (Sequence seq : pathway.getSequences()) {
                        if (seq.getLevel() == sequence) {
                            targetSequence = seq;
                            break;
                        }
                    }

                    if (targetSequence == null) return;

                    SequencePotion potion = targetSequence.getPotion();
                    if (potion == null) return;

                    ItemStack potionItem = potion.getFinishedPotion();
                    player.getInventory().addItem(potionItem);

                    data.incrementPotionPurchase(sequence);

                    player.sendMessage(Component.text("Purchased Sequence " + sequence + " potion!", NamedTextColor.GREEN));
                }
            };
        }
    }