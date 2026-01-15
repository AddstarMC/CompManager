package au.com.addstar.comp.commands;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.services.PlotResetService;
import au.com.addstar.comp.services.PlotResetService.ResetProgressCallback;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import org.jetbrains.annotations.NotNull;

/**
 * Command for resetting competition plots (backup → transfer → clear).
 */
public class ResetCommand implements ICommand {
	private final CompManager manager;
	private final PlotResetService resetService;
	private final Messages messages;
	
	public ResetCommand(CompManager manager, PlotResetService resetService, Messages messages) {
		this.manager = manager;
		this.resetService = resetService;
		this.messages = messages;
	}
	
	@Override
	public String getName() {
		return "reset";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.reset";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label;
	}

	@Override
	public String getDescription() {
		return "Resets all plots for the current competition (backs up, transfers players, then clears)";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		// Check if reset is already in progress
		if (resetService.isResetInProgress()) {
			sender.sendMessage(messages.get("reset.in-progress"));
			return true;
		}
		
		// Check if competition exists
		au.com.addstar.comp.Competition comp = manager.getCurrentComp();
		if (comp == null) {
			sender.sendMessage(messages.get("reset.no-competition"));
			return true;
		}
		
		// Create progress callback
		ResetProgressCallback progressCallback = new ResetProgressCallback() {
			@Override
			public void onPhaseStart(String phase) {
				String message;
				switch (phase) {
					case "backup":
						message = messages.get("reset.backup-phase");
						break;
					case "transfer":
						message = messages.get("reset.transfer-phase");
						break;
					case "clearing":
						message = messages.get("reset.clearing-phase");
						break;
					default:
						message = messages.get("reset.starting").replace("{theme}", comp.getTheme());
				}
				sender.sendMessage(message);
			}
		};
		
		// Start reset
		sender.sendMessage(messages.get("reset.starting").replace("{theme}", comp.getTheme()));
		
		ListenableFuture<Void> future = resetService.resetPlots(comp, progressCallback, null);
		
		Futures.addCallback(future, new FutureCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				sender.sendMessage(messages.get("reset.success"));
			}
			
			@Override
			public void onFailure(@NotNull Throwable error) {
				String errorMessage;
				if (error instanceof IllegalStateException || error instanceof IllegalArgumentException) {
					errorMessage = messages.get("reset.failed").replace("{error}", error.getMessage());
				} else {
					String errorText = error.getMessage() != null ? error.getMessage() : "Unexpected error";
					errorMessage = messages.get("reset.failed").replace("{error}", errorText);
					CompPlugin.instance.getLogger().log(java.util.logging.Level.SEVERE, "Reset failed", error);
				}
				sender.sendMessage(errorMessage);
			}
		}, org.bukkit.Bukkit.getScheduler().getMainThreadExecutor(CompPlugin.instance));
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
