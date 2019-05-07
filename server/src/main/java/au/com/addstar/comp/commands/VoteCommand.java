package au.com.addstar.comp.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.voting.Vote;
import au.com.addstar.comp.voting.VoteStorage;

import com.github.intellectualsites.plotsquared.plot.object.Plot;

public class VoteCommand implements TabExecutor {
	private final CompManager manager;
	private final P2Bridge bridge;
	private final Messages messages;
	
	public VoteCommand(CompManager manager, P2Bridge bridge, Messages messages) {
		this.manager = manager;
		this.bridge = bridge;
		this.messages = messages;
	}
	
	public void registerAs(PluginCommand command) {
		command.setExecutor(this);
		command.setTabCompleter(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(messages.get("command.require.player"));
			return true;
		}
		
		Player player = (Player)sender;
		
		if (manager.getState() != CompState.Voting) {
			sender.sendMessage(messages.get("vote.denied.state"));
			return true;
		}
		Plot plot = bridge.getPlotAt(player.getLocation());
		if (plot == null) {
			sender.sendMessage(messages.get("vote.denied.no-plot"));
			return true;
		}

		if (!plot.hasOwner()) {
			sender.sendMessage(messages.get("vote.denied.no-owner"));
			return true;
		}

		try {
			if (plot.getOwners().isEmpty()) {
				sender.sendMessage(messages.get("vote.denied.no-owner"));
				return true;
			}
		} catch (Exception e) {
			sender.sendMessage("Debug: plot.getOwners() access exception: " + e.getMessage());

			UUID ownerDeprecated = plot.owner;

			if (ownerDeprecated == null) {
				sender.sendMessage("Debug: ownerDeprecated == null");
				sender.sendMessage(messages.get("vote.denied.no-owner"));
				return true;
			}
		}

		VoteStorage<Vote> storage = (VoteStorage<Vote>)manager.getVoteStorage();
		Vote vote;
		try {
			UUID owner = plot.getOwners().iterator().next();
			vote = storage.getProvider().onVoteCommand(player, plot.getId(), owner, args);
		} catch (IllegalArgumentException e) {
			sender.sendMessage(e.getMessage());
			return true;
		}
		
		// It will be handled elsewhere
		if (vote == null) {
			return true;
		}
		
		// We will need to handle it
		try {
			storage.recordVote(player, vote);
			sender.sendMessage(messages.get("vote.done"));
		} catch (IllegalArgumentException e) {
			sender.sendMessage(messages.get("vote.denied.revote"));
		}
	
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return null;
		}
		
		if (manager.getState() != CompState.Voting) {
			return null;
		}
		
		Iterable<String> results = manager.getVoteStorage().getProvider().onVoteTabComplete(args);
		if (results == null) {
			return null;
		} else {
			return Lists.newArrayList(results);
		}
	}
}
