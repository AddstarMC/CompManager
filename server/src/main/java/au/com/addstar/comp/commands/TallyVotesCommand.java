package au.com.addstar.comp.commands;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.confirmations.Confirmable;
import au.com.addstar.comp.confirmations.Confirmation;
import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

public class TallyVotesCommand implements ICommand {
	private final CompManager manager;
	private final ConfirmationManager confirmations;
	
	public TallyVotesCommand(CompManager manager, ConfirmationManager confirmations) {
		this.manager = manager;
		this.confirmations = confirmations;
	}
	
	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public String getDescription() {
		return "Tallies up all votes and declares the winners";
	}

	@Override
	public String getName() {
		return "tallyvotes";
	}

	@Override
	public String getPermission() {
		return "comp.admin.tallyvotes";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label;
	}

	@Override
	public boolean onCommand(final CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if (manager.getState() != CompState.Voting) {
			sender.sendMessage(ChatColor.RED + "Comp needs to be in voting state to tally votes");
			return true;
		}
		
		Confirmation<? extends Confirmable> confirmation = Confirmation.builder(new Confirmable() {
			@Override
			public void confirm() {
				try {
					manager.finishCompetition();
				} catch (IllegalStateException e) {
					sender.sendMessage(ChatColor.RED + "Sorry, comp cannot be completed right now");
				}
			}
			
			@Override
			public void abort() {
			}
		}).expiresIn(20, TimeUnit.SECONDS).build();
		
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&c&lWARNING&6] &Running this will tally the votes, close the comp, and award winners"));
		sender.sendMessage(ChatColor.GOLD + "Do you really want to do this? Run " + ChatColor.RED + "/agree" + ChatColor.GOLD + " to continue");
		
		confirmations.addConfirmation(sender, confirmation);
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}

}
