package dev.ua.ikeepcalm.mythicBedwars.runnable;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActingProgressionTask extends BukkitRunnable {

    private final MythicBedwars plugin;

    public ActingProgressionTask(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Arena arena : BedwarsAPI.getGameAPI().getArenas()) {
            if (arena.getStatus() != ArenaStatus.RUNNING) continue;

            for (Player player : arena.getPlayers()) {
                if (!plugin.getArenaPathwayManager().hasPlayerMagic(player)) continue;

                Beyonder beyonder = Beyonder.of(player);
                if (beyonder == null) continue;

                double multiplier = plugin.getConfigManager().getPassiveActingMultiplier();
                int baseAmount = plugin.getConfigManager().getPassiveActingAmount();

                int sequence = beyonder.getLowestSequence();
                double sequenceMultiplier = getSequenceMultiplier(sequence);

                int actingAmount = (int) (baseAmount * multiplier * sequenceMultiplier);

                for (var pathway : beyonder.getPathways()) {
                    pathway.addActing(actingAmount);
                }
            }
        }
    }

    private double getSequenceMultiplier(int sequence) {
        return switch (sequence) {
            case 9 -> 4.0;
            case 8 -> 2.0;
            case 7 -> 1.0;
            case 6 -> 0.8;
            case 5 -> 0.6;
            case 4 -> 0.4;
            case 3 -> 0.3;
            case 2 -> 0.2;
            case 1 -> 0.1;
            default -> 0.05;
        };
    }
}