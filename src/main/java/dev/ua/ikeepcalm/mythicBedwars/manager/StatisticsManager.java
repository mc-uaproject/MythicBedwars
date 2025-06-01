package dev.ua.ikeepcalm.mythicBedwars.manager;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.StringProvider;
import com.djrapitops.plan.extension.annotation.TableProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.Team;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@PluginInfo(name = "MythicBedwars", iconName = "magic", iconFamily = Family.SOLID, color = Color.PURPLE)
public class StatisticsManager implements DataExtension {

    private final Map<String, PathwayStats> pathwayStatistics = new ConcurrentHashMap<>();
    private final MythicBedwars plugin;

    public StatisticsManager(MythicBedwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.SERVER_PERIODICAL,
                CallEvents.SERVER_EXTENSION_REGISTER
        };
    }

    public void recordGameEnd(Arena arena, Team winningTeam) {
        String winningPathway = plugin.getArenaPathwayManager().getTeamPathway(arena, winningTeam);
        if (winningPathway != null) {
            PathwayStats stats = pathwayStatistics.computeIfAbsent(winningPathway, k -> new PathwayStats());
            stats.wins++;
            stats.totalGames++;
        }

        for (Team team : arena.getAliveTeams()) {
            if (team != winningTeam) {
                String pathway = plugin.getArenaPathwayManager().getTeamPathway(arena, team);
                if (pathway != null) {
                    PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());
                    stats.losses++;
                    stats.totalGames++;
                }
            }
        }
    }

    public void recordSequenceReached(String pathway, int sequence) {
        PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());
        stats.sequencesReached.add(sequence);
    }

    public void recordGameDuration(Arena arena, long durationMillis) {
        for (Team team : arena.getAliveTeams()) {
            String pathway = plugin.getArenaPathwayManager().getTeamPathway(arena, team);
            if (pathway != null) {
                PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());
                stats.gameDurations.add(durationMillis);
            }
        }
    }

    public void recordDamageDealt(String pathway, double damage) {
        PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());
        stats.totalDamageDealt += damage;
    }

    public void recordAbilityUse(String pathway, String abilityName) {
        PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());
        stats.abilityUsage.merge(abilityName, 1, Integer::sum);
    }

    @TableProvider(tableColor = Color.PURPLE)
    public Table pathwayPerformance() {
        Table.Factory table = Table.builder()
                .columnOne("Pathway", Icon.called("magic").build())
                .columnTwo("Wins", Icon.called("trophy").build())
                .columnThree("Losses", Icon.called("times").build())
                .columnFour("Win Rate", Icon.called("percentage").build())
                .columnFive("Avg Sequence", Icon.called("chart-line").build());

        for (Map.Entry<String, PathwayStats> entry : pathwayStatistics.entrySet()) {
            String pathway = entry.getKey();
            PathwayStats stats = entry.getValue();

            double winRate = stats.totalGames > 0 ? (double) stats.wins / stats.totalGames * 100 : 0;
            double avgSequence = stats.getAverageSequence();

            table.addRow(
                    pathway,
                    stats.wins,
                    stats.losses,
                    String.format("%.1f%%", winRate),
                    String.format("%.1f", avgSequence)
            );
        }

        return table.build();
    }

    @TableProvider(tableColor = Color.AMBER)
    public Table pathwayDamageStats() {
        Table.Factory table = Table.builder()
                .columnOne("Pathway", Icon.called("magic").build())
                .columnTwo("Total Damage", Icon.called("fire").build())
                .columnThree("Avg Damage/Game", Icon.called("chart-bar").build())
                .columnFour("Most Used Ability", Icon.called("star").build());

        for (Map.Entry<String, PathwayStats> entry : pathwayStatistics.entrySet()) {
            String pathway = entry.getKey();
            PathwayStats stats = entry.getValue();

            double avgDamage = stats.totalGames > 0 ? stats.totalDamageDealt / stats.totalGames : 0;
            String mostUsedAbility = stats.getMostUsedAbility();

            table.addRow(
                    pathway,
                    String.format("%.0f", stats.totalDamageDealt),
                    String.format("%.0f", avgDamage),
                    mostUsedAbility
            );
        }

        return table.build();
    }

    @NumberProvider(
            text = "Total Games Played",
            description = "Total number of MythicBedwars games played",
            priority = 100,
            iconName = "gamepad",
            iconColor = Color.PURPLE
    )
    public long totalGames() {
        return pathwayStatistics.values().stream()
                .mapToLong(stats -> stats.totalGames)
                .sum() / Math.max(1, pathwayStatistics.size());
    }

    @StringProvider(
            text = "Most Winning Pathway",
            description = "Pathway with the highest win rate",
            priority = 90,
            iconName = "crown",
            iconColor = Color.INDIGO
    )
    public String bestPathway() {
        if (pathwayStatistics.isEmpty()) {
            return "No pathways played";
        } else {
            return pathwayStatistics.entrySet().stream()
                    .filter(e -> e.getValue().totalGames > 0)
                    .max(Comparator.comparingDouble(e -> (double) e.getValue().wins / e.getValue().totalGames))
                    .map(Map.Entry::getKey)
                    .orElse("None");
        }
    }

    @NumberProvider(
            text = "Highest Win Rate",
            description = "The highest win rate percentage among all pathways",
            priority = 85,
            iconName = "percentage",
            iconColor = Color.GREEN,
            format = FormatType.NONE
    )
    public long highestWinRate() {
        double maxWinRate = pathwayStatistics.entrySet().stream()
                .filter(e -> e.getValue().totalGames > 0)
                .mapToDouble(e -> (double) e.getValue().wins / e.getValue().totalGames * 100)
                .max()
                .orElse(0.0);
        return Math.round(maxWinRate);
    }

    @NumberProvider(
            text = "Average Game Duration",
            description = "Average duration of MythicBedwars games in minutes",
            priority = 80,
            iconName = "clock",
            iconColor = Color.LIGHT_BLUE,
            format = FormatType.NONE
    )
    public long averageGameDuration() {
        double avgDuration = pathwayStatistics.values().stream()
                .flatMap(stats -> stats.gameDurations.stream())
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1000 / 60;
        return Math.round(avgDuration);
    }

    @StringProvider(
            text = "Most Powerful Pathway",
            description = "Pathway that deals the most damage on average",
            priority = 75,
            iconName = "fire",
            iconColor = Color.RED
    )
    public String mostPowerfulPathway() {
        return pathwayStatistics.entrySet().stream()
                .filter(e -> e.getValue().totalGames > 0)
                .max(Comparator.comparingDouble(e ->
                        e.getValue().totalDamageDealt / e.getValue().totalGames))
                .map(Map.Entry::getKey)
                .orElse("None");
    }

    public static class PathwayStats implements Serializable, ConfigurationSerializable {
        int wins = 0;
        int losses = 0;
        int totalGames = 0;
        double totalDamageDealt = 0;
        List<Integer> sequencesReached = new ArrayList<>();
        List<Long> gameDurations = new ArrayList<>();
        Map<String, Integer> abilityUsage = new HashMap<>();

        public PathwayStats() {}

        @SuppressWarnings("unchecked")
        public PathwayStats(Map<String, Object> map) {
            this.wins = (int) map.getOrDefault("wins", 0);
            this.losses = (int) map.getOrDefault("losses", 0);
            this.totalGames = (int) map.getOrDefault("totalGames", 0);
            this.totalDamageDealt = ((Number) map.getOrDefault("totalDamageDealt", 0.0)).doubleValue();
            this.sequencesReached = (List<Integer>) map.getOrDefault("sequencesReached", new ArrayList<>());
            this.gameDurations = (List<Long>) map.getOrDefault("gameDurations", new ArrayList<>());
            this.abilityUsage = (Map<String, Integer>) map.getOrDefault("abilityUsage", new HashMap<>());
        }

        double getAverageSequence() {
            if (sequencesReached.isEmpty()) return 9.0;
            return 9.0 - sequencesReached.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
        }

        double getAverageGameDuration() {
            if (gameDurations.isEmpty()) return 0;
            return gameDurations.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0) / 1000 / 60;
        }

        String getMostUsedAbility() {
            return abilityUsage.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("None");
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("wins", wins);
            map.put("losses", losses);
            map.put("totalGames", totalGames);
            map.put("totalDamageDealt", totalDamageDealt);
            map.put("sequencesReached", sequencesReached);
            map.put("gameDurations", gameDurations);
            map.put("abilityUsage", abilityUsage);
            return map;
        }
    }

    public Map<String, PathwayStats> getPathwayStatistics() {
        return this.pathwayStatistics;
    }

    public void setPathwayStatistics(Map<String, PathwayStats> pathwayStatistics) {
        this.pathwayStatistics.clear();
        if (pathwayStatistics != null) {
            this.pathwayStatistics.putAll(pathwayStatistics);
        }
    }
}