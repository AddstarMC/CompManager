package au.com.addstar.comp;

import au.com.addstar.comp.prizes.BasePrize;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Represents a result from a comp.
 */
public class EntrantResult implements Comparable<EntrantResult> {
	private final UUID playerId;
	private String name;
	private final String plotId;

	private final Optional<Integer> rank;
	private final Optional<BasePrize> prize;

	private boolean claimed;

	public EntrantResult(UUID playerId, String name, String plotId, Optional<Integer> rank, Optional<BasePrize> prize, boolean claimed) {
		this.playerId = playerId;
		this.name = name;
		this.plotId = plotId;
		this.rank = rank;
		this.prize = prize;
		this.claimed = claimed;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	public String getPlayerName() {
		return name;
	}

	public void setPlayerName(String name) {
		this.name = name;
	}

	public OfflinePlayer getPlayer() {
		return Bukkit.getOfflinePlayer(playerId);
	}

	public String getPlotId() {
		return plotId;
	}

	public Optional<Integer> getRank() {
		return rank;
	}

	public Optional<BasePrize> getPrize() {
		return prize;
	}

	public boolean isPrizeClaimed() {
		return claimed;
	}

	public void setPrizeClaimed(boolean isClaimed) {
		claimed  = isClaimed;
	}

	@Override
	public int compareTo(EntrantResult o) {
		return rank.map(integer1 -> o.rank.map(integer1::compareTo).orElse(-1)).orElse(1);
	}
}
