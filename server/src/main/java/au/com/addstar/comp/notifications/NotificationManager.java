package au.com.addstar.comp.notifications;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import au.com.addstar.monolith.chat.ChatMessage;
import au.com.addstar.monolith.chat.ChatMessageType;
import au.com.addstar.monolith.chat.Title;

public class NotificationManager {
	/**
	 * Broadcasts a message to all players on the server
	 * @param message The message to display
	 * @param target Where to display the message
	 */
	public void broadcast(String message, DisplayTarget target) {
		broadcast(message, target, Predicates.alwaysTrue());
	}
	
	/**
	 * Broadcasts a message to all players on the server that match the predicate
	 * @param message The message to display
	 * @param target Where to display the message
	 * @param predicate A predicate to select players who will receive the broadcast
	 */
	public void broadcast(String message, DisplayTarget target, Predicate<? super Player> predicate) {
		ChatMessage messageObject = ChatMessage.begin(message);
		
		// Prepare the title if needed
		Title title = null;
		if (target == DisplayTarget.Subtitle || target == DisplayTarget.Title) {
			title = new Title();
			title.setFadeInTime(500, TimeUnit.MILLISECONDS);
			title.setDisplayTime(5, TimeUnit.SECONDS);
			title.setFadeOutTime(1, TimeUnit.SECONDS);
			
			// Set the message
			if (target == DisplayTarget.Title) {
				title.setTitle(messageObject);
			} else {
				title.setSubtitle(messageObject);
			}
		}
		
		// Send out the message
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!predicate.apply(player)) {
				continue;
			}
			
			switch (target) {
			case ActionBar:
				messageObject.send(player, ChatMessageType.ActionBar);
				break;
			case Chat:
				messageObject.send(player, ChatMessageType.Standard);
				break;
			case SystemMessage:
				messageObject.send(player, ChatMessageType.System);
				break;
			case Subtitle:
			case Title:
				title.show(player);
				break;
			}
		}
	}
	
	/**
	 * Sends a message to a player
	 * @param player The player to message
	 * @param message The message to send
	 * @param target Where to display the message
	 */
	public void sendMessage(Player player, String message, DisplayTarget target) {
		ChatMessage messageObject = ChatMessage.begin(message);
		
		// Prepare the title if needed
		Title title = null;
		if (target == DisplayTarget.Subtitle || target == DisplayTarget.Title) {
			title = new Title();
			title.setFadeInTime(500, TimeUnit.MILLISECONDS);
			title.setDisplayTime(5, TimeUnit.SECONDS);
			title.setFadeOutTime(1, TimeUnit.SECONDS);
			
			// Set the message
			if (target == DisplayTarget.Title) {
				title.setTitle(messageObject);
			} else {
				title.setSubtitle(messageObject);
			}
		}
		
		// Send out the message
		switch (target) {
		case ActionBar:
			messageObject.send(player, ChatMessageType.ActionBar);
			break;
		case Chat:
			messageObject.send(player, ChatMessageType.Standard);
			break;
		case SystemMessage:
			messageObject.send(player, ChatMessageType.System);
			break;
		case Subtitle:
		case Title:
			title.show(player);
			break;
		}
	}
	
	/**
	 * Where to display a message
	 */
	public enum DisplayTarget {
		/**
		 * Message will be sent as standard chat
		 */
		Chat,
		/**
		 * Message will be sent as a system message
		 * (will be shown when chat is partially hidden)
		 */
		SystemMessage,
		/**
		 * Message will be shown above hotbar 
		 */
		ActionBar,
		/**
		 * Message will be shown in a title message
		 */
		Title,
		/**
		 * Message will be shown in a title message,
		 * but as the subtitle
		 */
		Subtitle
	}
}
