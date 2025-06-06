package dev.ua.ikeepcalm.mythicBedwars.listener;

import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import de.marcely.bedwars.api.arena.KickReason;
import de.marcely.bedwars.api.arena.Team;
import de.marcely.bedwars.api.event.arena.ArenaBedBreakEvent;
import de.marcely.bedwars.api.event.arena.ArenaStatusChangeEvent;
import de.marcely.bedwars.api.event.arena.ArenaUnloadEvent;
import de.marcely.bedwars.api.event.arena.RoundEndEvent;
import de.marcely.bedwars.api.event.player.PlayerJoinArenaEvent;
import de.marcely.bedwars.api.event.player.PlayerKillPlayerEvent;
import de.marcely.bedwars.api.event.player.PlayerQuitArenaEvent;
import de.marcely.bedwars.api.event.player.PlayerTeamChangeEvent;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ArenaListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(ArenaListener.class);
    private final MythicBedwars plugin;
    private final Map<String, Long> arenaStartTimes = new HashMap<>();

    public ArenaListener(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onArenaStatusChange(ArenaStatusChangeEvent event) {
        Arena arena = event.getArena();

        log.info("Arena status change: {} -> {}", event.getOldStatus(), event.getNewStatus());

        if (event.getNewStatus() == ArenaStatus.LOBBY && event.getOldStatus() != ArenaStatus.LOBBY) {
            if (plugin.getConfigManager().isGloballyEnabled() && plugin.getConfigManager().isArenaEnabled(arena.getName())) {
                log.info("Starting voting for arena: {}", arena.getName());
                plugin.getVotingManager().startVoting(arena);
            }
        }

        if (event.getNewStatus() == ArenaStatus.RUNNING) {
            arenaStartTimes.put(arena.getName(), System.currentTimeMillis());
            plugin.getVotingManager().endVoting(arena);

            if (plugin.getVotingManager().isMagicEnabled(arena.getName())) {
                log.info("Magic is enabled for arena: {}, assigning pathways", arena.getName());
                plugin.getArenaPathwayManager().assignPathwaysToTeams(arena);
            } else {
                log.info("Magic is disabled for arena: {}, skipping pathway assignment", arena.getName());
            }
        }

        if (event.getNewStatus() == ArenaStatus.STOPPED) {
            Long startTime = arenaStartTimes.remove(arena.getName());
            if (startTime != null && plugin.getStatisticsManager() != null) {
                long duration = System.currentTimeMillis() - startTime;
                plugin.getStatisticsManager().recordGameDuration(arena, duration);
            }
            plugin.getArenaPathwayManager().cleanupArena(arena);
            plugin.getVotingManager().cleanupArena(arena.getName());
        }
    }

    @EventHandler
    public void onArenaEnd(ArenaUnloadEvent event) {
        Arena arena = event.getArena();
        plugin.getArenaPathwayManager().cleanupArena(arena);
        plugin.getVotingManager().cleanupArena(arena.getName());
        arenaStartTimes.remove(arena.getName());
    }

    @EventHandler
    public void onGameEnd(RoundEndEvent event) {
        Arena arena = event.getArena();
        Team winner = event.getWinnerTeam();

        if (plugin.getStatisticsManager() != null && plugin.getVotingManager().isMagicEnabled(arena.getName())) {
            plugin.getStatisticsManager().recordGameEnd(arena, winner);
        }
    }

    @EventHandler
    public void onPlayerJoinArena(PlayerJoinArenaEvent event) {
        Player player = event.getPlayer();
        Arena arena = event.getArena();

        log.info("Player {} joined arena {} with status: {}", player.getName(), arena.getName(), arena.getStatus());

        if (arena.getStatus() == ArenaStatus.LOBBY) {
            if (plugin.getVotingManager().hasActiveVoting(arena.getName())) {
                log.info("Giving voting items to late-joining player: {}", player.getName());
                plugin.getVotingManager().giveVotingItems(player, arena);
            } else if (plugin.getConfigManager().isGloballyEnabled() && plugin.getConfigManager().isArenaEnabled(arena.getName())) {
                if (!plugin.getVotingManager().hasActiveVoting(arena.getName())) {
                    log.info("First player joined, starting voting for arena: {}", arena.getName());
                    plugin.getVotingManager().startVoting(arena);
                    plugin.getVotingManager().giveVotingItems(player, arena);
                }
            }
        }

        if (arena.getStatus() == ArenaStatus.RUNNING && plugin.getVotingManager().isMagicEnabled(arena.getName())) {
            Team team = arena.getPlayerTeam(player);
            if (team != null) {
                if (plugin.getArenaPathwayManager().isPlayerInArena(player, arena.getName())) {
                    var data = plugin.getArenaPathwayManager().getPlayerData(player);
                    if (data != null) {
                        data.setActive(true);
                    }
                }
                plugin.getArenaPathwayManager().initializePlayerMagic(player, arena, team);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitArenaEvent event) {
        Player player = event.getPlayer();
        Arena arena = event.getArena();

        if (arena.getStatus() == ArenaStatus.LOBBY) {
            plugin.getVotingManager().removeVotingItems(player);
        }

        boolean isGameEnding = event.getReason() == KickReason.GAME_LOSE
                || event.getReason() == KickReason.GAME_END
                || event.getReason() == KickReason.ARENA_STOP;

        boolean isVoluntaryLeave = event.getReason() == KickReason.LEAVE
                || event.getReason() == KickReason.PLUGIN_STOP;

        if (isGameEnding || isVoluntaryLeave || arena.getStatus() != ArenaStatus.RUNNING) {
            plugin.getArenaPathwayManager().cleanupPlayer(player);
        } else {
            plugin.getArenaPathwayManager().markPlayerInactive(player);
        }
    }

    @EventHandler
    public void onPlayerJoinTeam(PlayerTeamChangeEvent event) {
        Player player = event.getPlayer();
        Arena arena = event.getArena();
        Team team = event.getNewTeam();

        if (arena.getStatus() == ArenaStatus.RUNNING && team != null && plugin.getVotingManager().isMagicEnabled(arena.getName())) {
            plugin.getArenaPathwayManager().initializePlayerMagic(player, arena, team);
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerKillPlayerEvent event) {
        Player killer = event.getKiller();
        if (killer == null) return;

        Arena arena = event.getArena();
        if (!plugin.getVotingManager().isMagicEnabled(arena.getName())) return;

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

        Arena arena = event.getArena();
        if (!plugin.getVotingManager().isMagicEnabled(arena.getName())) return;

        Beyonder beyonder = Beyonder.of(breaker);
        if (beyonder == null) return;

        double multiplier = plugin.getConfigManager().getBedBreakActingMultiplier();
        int actingAmount = (int) (100 * multiplier);

        for (var pathway : beyonder.getPathways()) {
            pathway.addActing(actingAmount);
        }
    }
}