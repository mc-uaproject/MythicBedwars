package dev.ua.ikeepcalm.mythicBedwars.manager;

import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.Team;
import dev.ua.ikeepcalm.coi.CircleOfImagination;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.Beyonder;
import dev.ua.ikeepcalm.coi.pathways.Pathways;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PathwayManager {

    private final Map<String, Map<Team, String>> arenaPathways = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerMagicData> playerData = new ConcurrentHashMap<>();

    public void assignPathwaysToTeams(Arena arena) {
        Map<Team, String> teamPathways = new HashMap<>();
        List<String> availablePathways = new ArrayList<>(Pathways.allPathways.keySet());
        Collections.shuffle(availablePathways);

        int index = 0;
        for (Team team : arena.getAliveTeams()) {
            if (index >= availablePathways.size()) {
                Collections.shuffle(availablePathways);
                index = 0;
            }
            teamPathways.put(team, availablePathways.get(index));
            index++;
        }

        arenaPathways.put(arena.getName(), teamPathways);
    }

    public String getTeamPathway(Arena arena, Team team) {
        Map<Team, String> teamPathways = arenaPathways.get(arena.getName());
        if (teamPathways != null) {
            return teamPathways.get(team);
        }
        return null;
    }

    public void initializePlayerMagic(Player player, Arena arena, Team team) {
        String pathway = getTeamPathway(arena, team);
        if (pathway == null) return;

        PlayerMagicData data = new PlayerMagicData(player.getUniqueId(), pathway, arena.getName());
        playerData.put(player.getUniqueId(), data);

        Beyonder beyonder = new Beyonder(player.getUniqueId(), player.getName(), pathway, 9);
    }

    public void cleanupPlayer(Player player) {
        PlayerMagicData data = playerData.remove(player.getUniqueId());
        if (data != null) {
            Beyonder beyonder = Beyonder.of(player);
            if (beyonder != null) {
                beyonder.destroy();
            }
        }
    }

    public void cleanupArena(Arena arena) {
        arenaPathways.remove(arena.getName());

        playerData.entrySet().removeIf(entry -> {
            if (entry.getValue().getArenaName().equals(arena.getName())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    Beyonder beyonder = Beyonder.of(player);
                    if (beyonder != null) {
                        beyonder.destroy();
                    }
                }
                return true;
            }
            return false;
        });
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
    }

    public PlayerMagicData getPlayerData(Player player) {
        return playerData.get(player.getUniqueId());
    }

    public boolean hasPlayerMagic(Player player) {
        return playerData.containsKey(player.getUniqueId());
    }

    public static class PlayerMagicData {
        private final UUID playerId;
        private final String pathway;
        private final String arenaName;
        private int currentSequence;
        private final Map<Integer, Integer> potionsPurchased;

        public PlayerMagicData(UUID playerId, String pathway, String arenaName) {
            this.playerId = playerId;
            this.pathway = pathway;
            this.arenaName = arenaName;
            this.currentSequence = 9;
            this.potionsPurchased = new HashMap<>();
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

        public void incrementPotionPurchase(int sequence) {
            potionsPurchased.merge(sequence, 1, Integer::sum);
        }

        public int getPotionPurchaseCount(int sequence) {
            return potionsPurchased.getOrDefault(sequence, 0);
        }
    }
}