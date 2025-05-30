package dev.ua.ikeepcalm.mythicBedwars.listener;

import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import de.marcely.bedwars.api.arena.Team;
import de.marcely.bedwars.api.event.arena.ArenaBedBreakEvent;
import de.marcely.bedwars.api.event.arena.ArenaStatusChangeEvent;
import de.marcely.bedwars.api.event.arena.ArenaUnloadEvent;
import de.marcely.bedwars.api.event.player.PlayerJoinArenaEvent;
import de.marcely.bedwars.api.event.player.PlayerKillPlayerEvent;
import de.marcely.bedwars.api.event.player.PlayerQuitArenaEvent;
import de.marcely.bedwars.api.event.player.PlayerTeamChangeEvent;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ArenaListener implements Listener {

    private final MythicBedwars plugin;

    public ArenaListener(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onArenaStatusChange(ArenaStatusChangeEvent event) {
        Arena arena = event.getArena();

        if (event.getNewStatus() == ArenaStatus.LOBBY && event.getOldStatus() != ArenaStatus.LOBBY) {
            plugin.getArenaPathwayManager().assignPathwaysToTeams(arena);

            for (Team team : arena.getAliveTeams()) {
                String pathway = plugin.getArenaPathwayManager().getTeamPathway(arena, team);
                if (pathway != null) {
                    Component message = Component.text("Team " + team.getDisplayName() + " has been assigned the ")
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(pathway).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text(" pathway!").color(NamedTextColor.GRAY));

                    for (Player player : arena.getPlayers()) {
                        player.sendMessage(message);
                    }
                }
            }
        }

        if (event.getNewStatus() == ArenaStatus.STOPPED) {
            plugin.getArenaPathwayManager().cleanupArena(arena);
        }
    }

    @EventHandler
    public void onArenaEnd(ArenaUnloadEvent event) {
        Arena arena = event.getArena();
        plugin.getArenaPathwayManager().cleanupArena(arena);
    }

    @EventHandler
    public void onPlayerJoinArena(PlayerJoinArenaEvent event) {
        Player player = event.getPlayer();
        Arena arena = event.getArena();

        if (arena.getStatus() == ArenaStatus.RUNNING) {
            Team team = arena.getPlayerTeam(player);
            if (team != null) {
                plugin.getArenaPathwayManager().initializePlayerMagic(player, arena, team);
            }
        }
    }

    @EventHandler
    public void onPlayerQuitArena(PlayerQuitArenaEvent event) {
        Player player = event.getPlayer();
        plugin.getArenaPathwayManager().cleanupPlayer(player);
    }

    @EventHandler
    public void onPlayerJoinTeam(PlayerTeamChangeEvent event) {
        Player player = event.getPlayer();
        Arena arena = event.getArena();
        Team team = event.getNewTeam();

        if (arena.getStatus() == ArenaStatus.RUNNING) {
            plugin.getArenaPathwayManager().initializePlayerMagic(player, arena, team);
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerKillPlayerEvent event) {
        Player killer = event.getKiller();
        if (killer == null) return;

        Beyonder beyonder = Beyonder.of(killer);
        if (beyonder == null) return;

        double multiplier = event.isFatalDeath() ?
                plugin.getConfigManager().getFinalKillActingMultiplier() :
                plugin.getConfigManager().getKillActingMultiplier();

        int actingAmount = (int) (100 * multiplier);

        for (var pathway : beyonder.getPathways()) {
            pathway.addActing(actingAmount);
        }
    }

    @EventHandler
    public void onBedBreak(ArenaBedBreakEvent event) {
        Player breaker = event.getPlayer();
        if (breaker == null) return;

        Beyonder beyonder = Beyonder.of(breaker);
        if (beyonder == null) return;

        double multiplier = plugin.getConfigManager().getBedBreakActingMultiplier();
        int actingAmount = (int) (100 * multiplier);

        for (var pathway : beyonder.getPathways()) {
            pathway.addActing(actingAmount);
        }
    }
}