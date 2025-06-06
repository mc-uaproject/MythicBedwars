package dev.ua.ikeepcalm.mythicBedwars.domain.runnable;

import de.marcely.bedwars.api.BedwarsAPI;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import dev.ua.ikeepcalm.mythicBedwars.domain.core.PathwayManager;
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

            if (!plugin.getVotingManager().isMagicEnabled(arena.getName())) continue;

            for (Player player : arena.getPlayers()) {
                PathwayManager.PlayerMagicData data = plugin.getArenaPathwayManager().getPlayerData(player);
                if (data == null || !data.isActive()) continue;

                Beyonder beyonder = Beyonder.of(player);
                if (beyonder == null) continue;

                double multiplier = plugin.getConfigManager().getPassiveActingMultiplier();
                int baseAmount = plugin.getConfigManager().getPassiveActingAmount();

                int sequence = beyonder.getLowestSequenceNumber();
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
            case 9 -> 3.5;
            case 8 -> 3.0;
            case 7 -> 2.5;
            case 6 -> 1.3;
            case 5 -> 1.1;
            case 4 -> 0.4;
            case 3 -> 0.3;
            case 2 -> 0.2;
            case 1 -> 0.1;
            default -> 0.05;
        };
    }
}