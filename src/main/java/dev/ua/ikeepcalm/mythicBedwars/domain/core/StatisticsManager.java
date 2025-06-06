package dev.ua.ikeepcalm.mythicBedwars.domain.core;

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
import dev.ua.ikeepcalm.mythicBedwars.domain.stats.db.PathwayStats;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@PluginInfo(name = "MythicBedwars", iconName = "magic", iconFamily = Family.SOLID, color = Color.PURPLE)
public class StatisticsManager implements DataExtension {

    private final Map<String, PathwayStats> pathwayStatistics = new ConcurrentHashMap<>();
    private final MythicBedwars plugin;
    private final AtomicLong totalUniqueGames = new AtomicLong(0);

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
        Set<Team> allParticipatingTeams = plugin.getArenaPathwayManager().getAllParticipatingTeams(arena);

        if (allParticipatingTeams.isEmpty()) {
            return;
        }

        totalUniqueGames.incrementAndGet();

        for (Team team : allParticipatingTeams) {
            String pathway = plugin.getArenaPathwayManager().getTeamPathway(arena, team);
            if (pathway != null) {
                PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());

                if (team.equals(winningTeam)) {
                    stats.wins++;
                } else {
                    stats.losses++;
                }
                stats.totalGames++;
            }
        }
    }

    public void recordSequenceReached(String pathway, int sequence) {
        PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());
        stats.sequencesReached.add(sequence);
    }

    public void recordGameDuration(Arena arena, long durationMillis) {
        Set<Team> allParticipatingTeams = plugin.getArenaPathwayManager().getAllParticipatingTeams(arena);

        if (!allParticipatingTeams.isEmpty()) {
            for (Team team : allParticipatingTeams) {
                String pathway = plugin.getArenaPathwayManager().getTeamPathway(arena, team);
                if (pathway != null) {
                    PathwayStats stats = pathwayStatistics.computeIfAbsent(pathway, k -> new PathwayStats());
                    stats.gameDurations.add(durationMillis);
                    break;
                }
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
            description = "Total number of unique MythicBedwars games played",
            priority = 100,
            iconName = "gamepad",
            iconColor = Color.PURPLE
    )
    public long totalGames() {
        return totalUniqueGames.get();
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
        double maxWinRate = pathwayStatistics.values().stream()
                .filter(pathwayStats -> pathwayStats.totalGames > 0)
                .mapToDouble(pathwayStats -> (double) pathwayStats.wins / pathwayStats.totalGames * 100)
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

    public Map<String, PathwayStats> getPathwayStatistics() {
        return this.pathwayStatistics;
    }

    public void setPathwayStatistics(Map<String, PathwayStats> pathwayStatistics) {
        this.pathwayStatistics.clear();
        if (pathwayStatistics != null) {
            this.pathwayStatistics.putAll(pathwayStatistics);
        }

        long maxGames = pathwayStatistics != null ?
                pathwayStatistics.values().stream()
                        .mapToLong(stats -> stats.gameDurations.size())
                        .max()
                        .orElse(0) : 0;
        totalUniqueGames.set(maxGames);
    }
}