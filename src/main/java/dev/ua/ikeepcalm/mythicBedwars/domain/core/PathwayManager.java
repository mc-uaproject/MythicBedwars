package dev.ua.ikeepcalm.mythicBedwars.domain.core;

import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.Team;
import dev.ua.ikeepcalm.coi.CircleOfImagination;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import dev.ua.ikeepcalm.mythicBedwars.domain.balancer.PathwayBalancer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PathwayManager {

    private final Map<String, Map<Team, String>> arenaPathways = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerMagicData> playerData = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> arenaPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerArenaCache = new ConcurrentHashMap<>();

    public void assignPathwaysToTeams(Arena arena) {
        PathwayBalancer balancer = MythicBedwars.getInstance().getPathwayBalancer();
        Map<Team, String> teamPathways = balancer.assignBalancedPathways(arena);
        arenaPathways.put(arena.getName(), teamPathways);
    }

    public String getBalancingInfo(Arena arena) {
        Map<Team, String> teamPathways = arenaPathways.get(arena.getName());
        if (teamPathways == null || teamPathways.isEmpty()) {
            return "No pathways assigned yet";
        }

        StringBuilder info = new StringBuilder("Pathway assignments for " + arena.getName() + ":\n");
        for (Map.Entry<Team, String> entry : teamPathways.entrySet()) {
            info.append("- ").append(entry.getKey().getDisplayName()).append(": ").append(entry.getValue()).append("\n");
        }

        boolean isBalanced = MythicBedwars.getInstance().getConfigManager().isPathwayBalancingEnabled();
        info.append("Balancing: ").append(isBalanced ? "Enabled" : "Disabled");

        return info.toString();
    }

    public String getTeamPathway(Arena arena, Team team) {
        Map<Team, String> teamPathways = arenaPathways.get(arena.getName());
        if (teamPathways != null) {
            return teamPathways.get(team);
        }
        return null;
    }

    public Set<Team> getAllParticipatingTeams(Arena arena) {
        Map<Team, String> teamPathways = arenaPathways.get(arena.getName());
        if (teamPathways != null) {
            return teamPathways.keySet();
        }
        return Collections.emptySet();
    }

    public void initializePlayerMagic(Player player, Arena arena, Team team) {
        String pathway = getTeamPathway(arena, team);
        if (pathway == null) return;

        UUID playerId = player.getUniqueId();

        Beyonder existingBeyonder = Beyonder.of(player);
        if (existingBeyonder != null) {
            existingBeyonder.destroy();
        }

        PlayerMagicData existingData = playerData.get(playerId);
        if (existingData != null && existingData.getArenaName().equals(arena.getName())) {
            Beyonder beyonder = new Beyonder(playerId, player.getName(), existingData.getPathway(), existingData.getCurrentSequence());

            if (existingData.getStoredActing() > 0) {
                beyonder.getPathways().getFirst().setActing(existingData.getStoredActing());
            }
            return;
        }

        PlayerMagicData data = new PlayerMagicData(playerId, pathway, arena.getName());
        playerData.put(playerId, data);
        playerArenaCache.put(playerId, arena.getName());

        arenaPlayers.computeIfAbsent(arena.getName(), k -> ConcurrentHashMap.newKeySet()).add(playerId);

        Beyonder beyonder = new Beyonder(playerId, player.getName(), pathway, 9);
    }

    public void markPlayerInactive(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerMagicData data = playerData.get(playerId);
        if (data != null) {
            Beyonder beyonder = Beyonder.of(player);
            if (beyonder != null && !beyonder.getPathways().isEmpty()) {
                data.setCurrentSequence(beyonder.getLowestSequenceNumber());
                data.setStoredActing(beyonder.getPathways().getFirst().getActing());
                beyonder.destroy();
            }
            data.setActive(false);
        }
    }

    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerMagicData data = playerData.remove(playerId);
        if (data != null) {
            String arenaName = data.getArenaName();
            Set<UUID> players = arenaPlayers.get(arenaName);
            if (players != null) {
                players.remove(playerId);
            }

            Beyonder beyonder = Beyonder.of(player);
            if (beyonder != null) {
                beyonder.destroy();
            }
        }
        playerArenaCache.remove(playerId);
    }

    public void cleanupArena(Arena arena) {
        String arenaName = arena.getName();
        arenaPathways.remove(arenaName);

        Set<UUID> players = arenaPlayers.remove(arenaName);
        if (players != null) {
            for (UUID playerId : players) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    Beyonder beyonder = Beyonder.of(player);
                    if (beyonder != null) {
                        beyonder.destroy();
                    }
                }
                playerData.remove(playerId);
                playerArenaCache.remove(playerId);
            }
        }
    }

    public void cleanupAll() {
        for (PlayerMagicData data : playerData.values()) {
            Player player = CircleOfImagination.getInstance().getServer().getPlayer(data.getPlayerId());
            if (player != null) {
                Beyonder beyonder = Beyonder.of(player);
                if (beyonder != null) {
                    beyonder.destroy();
                }
            }
        }
        playerData.clear();
        arenaPathways.clear();
        arenaPlayers.clear();
        playerArenaCache.clear();
    }

    public PlayerMagicData getPlayerData(Player player) {
        return playerData.get(player.getUniqueId());
    }

    public boolean hasPlayerMagic(Player player) {
        return playerData.containsKey(player.getUniqueId());
    }

    public boolean isPlayerInArena(Player player, String arenaName) {
        Set<UUID> players = arenaPlayers.get(arenaName);
        return players != null && players.contains(player.getUniqueId());
    }

    public static class PlayerMagicData {
        private final UUID playerId;
        private final String pathway;
        private final String arenaName;
        private int currentSequence;
        private final Map<Integer, Integer> potionsPurchased;
        private boolean active;
        private int storedActing;

        public PlayerMagicData(UUID playerId, String pathway, String arenaName) {
            this.playerId = playerId;
            this.pathway = pathway;
            this.arenaName = arenaName;
            this.currentSequence = 9;
            this.potionsPurchased = new HashMap<>();
            this.active = true;
            this.storedActing = 0;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getPathway() {
            return pathway;
        }

        public String getArenaName() {
            return arenaName;
        }

        public int getCurrentSequence() {
            return currentSequence;
        }

        public void setCurrentSequence(int sequence) {
            this.currentSequence = sequence;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getStoredActing() {
            return storedActing;
        }

        public void setStoredActing(int acting) {
            this.storedActing = acting;
        }

        public void incrementPotionPurchase(int sequence) {
            potionsPurchased.merge(sequence, 1, Integer::sum);
        }

        public int getPotionPurchaseCount(int sequence) {
            return potionsPurchased.getOrDefault(sequence, 0);
        }
    }
}