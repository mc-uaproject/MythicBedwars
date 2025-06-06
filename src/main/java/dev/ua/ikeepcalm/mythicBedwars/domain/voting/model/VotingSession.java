package dev.ua.ikeepcalm.mythicBedwars.domain.voting.model;

import de.marcely.bedwars.api.arena.Arena;
import dev.ua.ikeepcalm.mythicBedwars.MythicBedwars;
import dev.ua.ikeepcalm.mythicBedwars.domain.runnable.VotingReminderTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VotingSession {

    private final Arena arena;
    private final MythicBedwars plugin;
    private final Map<UUID, Boolean> votes = new ConcurrentHashMap<>();
    private boolean active = false;
    private boolean magicEnabled = true;
    private VotingReminderTask reminderTask;

    public VotingSession(Arena arena, MythicBedwars plugin) {
        this.arena = arena;
        this.plugin = plugin;
        this.reminderTask = new VotingReminderTask(plugin, arena, this);
    }

    public void start() {
        active = true;
        reminderTask.runTaskTimerAsynchronously(plugin, 20L, 200L);
        broadcastMessage("magic.voting.started", NamedTextColor.YELLOW);
        broadcastMessage("magic.voting.instructions", NamedTextColor.GRAY);
    }

    public void castVote(UUID playerId, boolean voteYes) {
        if (!active) return;

        votes.put(playerId, voteYes);
        updateVoteStatus();
    }

    private void updateVoteStatus() {
        int totalPlayers = arena.getPlayers().size();
        int yesVotes = (int) votes.values().stream().filter(vote -> vote).count();
        int noVotes = votes.size() - yesVotes;

        String statusMessage = plugin.getLocaleManager().formatMessage("magic.voting.status",
                "yes", yesVotes, "no", noVotes, "total", totalPlayers);

        for (Player player : arena.getPlayers()) {
            player.sendMessage(Component.text(statusMessage, NamedTextColor.AQUA));
        }
    }

    public void end() {
        if (!active) return;

        active = false;

        this.reminderTask.cancel();

        int totalPlayers = arena.getPlayers().size();
        int yesVotes = (int) votes.values().stream().filter(vote -> vote).count();
        int noVotes = votes.size() - yesVotes;
        int totalVotes = votes.size();

        if (totalVotes == 0) {
            magicEnabled = true;
            broadcastMessage("magic.voting.no_votes_at_all", NamedTextColor.YELLOW);
            return;
        }

        double yesPercentage = (double) yesVotes / totalVotes;
        double noPercentage = (double) noVotes / totalVotes;

        if (yesPercentage > 0.5) {
            magicEnabled = true;
        } else if (noPercentage > 0.5) {
            magicEnabled = false;
        } else {
            magicEnabled = Math.random() < 0.5;
            broadcastMessage("magic.voting.tie_breaker", NamedTextColor.YELLOW);
        }

        if (magicEnabled) {
            broadcastMessage("magic.voting.magic_enabled", NamedTextColor.GREEN);
        } else {
            broadcastMessage("magic.voting.magic_disabled", NamedTextColor.RED);
        }

        String resultMessage = plugin.getLocaleManager().formatMessage("magic.voting.final_result",
                "yes", yesVotes, "no", noVotes);

        for (Player player : arena.getPlayers()) {
            player.sendMessage(Component.text(resultMessage, NamedTextColor.GOLD));
        }

        String participationMessage = plugin.getLocaleManager().formatMessage("magic.voting.participation",
                "voted", totalVotes, "total", totalPlayers);
        for (Player player : arena.getPlayers()) {
            player.sendMessage(Component.text(participationMessage, NamedTextColor.AQUA));
        }
    }

    private void broadcastMessage(String key, NamedTextColor color) {
        String message = plugin.getLocaleManager().getMessage(key);
        for (Player player : arena.getPlayers()) {
            player.sendMessage(Component.text(message, color));
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isMagicEnabled() {
        return magicEnabled;
    }

    public int getYesVotes() {
        return (int) votes.values().stream().filter(vote -> vote).count();
    }

    public int getNoVotes() {
        return votes.size() - getYesVotes();
    }

    public boolean hasVoted(UUID playerId) {
        return votes.containsKey(playerId);
    }

    public Boolean getVote(UUID playerId) {
        return votes.get(playerId);
    }
}