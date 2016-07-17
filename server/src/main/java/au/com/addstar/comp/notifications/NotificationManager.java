package au.com.addstar.comp.notifications;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.intellectualcrafters.configuration.ConfigurationSection;
import com.intellectualcrafters.configuration.InvalidConfigurationException;
import com.intellectualcrafters.configuration.file.YamlConfiguration;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.monolith.chat.ChatMessage;
import au.com.addstar.monolith.chat.ChatMessageType;
import au.com.addstar.monolith.chat.Title;

public class NotificationManager {
	private static final long ActionBarDisplayTime = 2000;
	
	private final File notificationsFile;
	private final CompManager compManager;
	
	// Auto broadcast settings
	private long broadcastInterval;
	private long broadcastDisplayTime;
	private boolean broadcastInSequence;
	private DisplayTarget broadcastLocation;
	
	// Auto broadcast state
	private List<Notification> autoBroadcasts;
	private long nextBroadcastTime;
	private int broadcastIndex;

	// State change broadcasts
	private Map<CompState, StateChangeNotification> stateStateChangeNotifications;
	
	// Map of end time to runnable
	private TreeMultimap<Long, Runnable> displayRefreshers;
	
	public NotificationManager(File notificationsFile, CompManager compManager) {
		this.notificationsFile = notificationsFile;
		this.compManager = compManager;
		autoBroadcasts = Lists.newArrayList();
		displayRefreshers = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
		stateStateChangeNotifications = Maps.newHashMap();
	}
	
	/**
	 * Reloads the notifications file
	 * @throws IOException Thrown if an IOException occurs while reading the file
	 */
	public void reload() throws IOException {
		try {
			YamlConfiguration config = new YamlConfiguration();
			config.load(notificationsFile);

			// Load settings
			ConfigurationSection settingsSection = config.getConfigurationSection("broadcast-settings");
			try {
				broadcastInterval = CompUtils.parseDateDiff(settingsSection.getString("interval", "15m"));
			} catch (IllegalArgumentException e) {
				// Fallback to default value
				broadcastInterval = TimeUnit.MINUTES.toMillis(15);
				// TODO: Notify of error
			}
			
			broadcastInSequence = settingsSection.getBoolean("in-sequence");
			
			switch (settingsSection.getString("location")) {
			case "action":
				broadcastLocation = DisplayTarget.ActionBar;
				break;
			case "chat":
				broadcastLocation = DisplayTarget.Chat;
				break;
			case "system":
				broadcastLocation = DisplayTarget.SystemMessage;
				break;
			case "title":
				broadcastLocation = DisplayTarget.Title;
				break;
			case "subtitle":
				broadcastLocation = DisplayTarget.Subtitle;
				break;
			}
			
			try {
				broadcastDisplayTime = CompUtils.parseDateDiff(settingsSection.getString("display-time", "5s"));
			} catch (IllegalArgumentException e) {
				// Fallback to default value
				broadcastDisplayTime = TimeUnit.SECONDS.toMillis(5);
				// TODO: Notify of error
			}
			
			// Load broadcasts
			ConfigurationSection broadcastsSection = config.getConfigurationSection("broadcasts");
			if (broadcastsSection != null) {
				List<Notification> notifications = Lists.newArrayList();

				for (String key : broadcastsSection.getKeys(false)) {
					ConfigurationSection keySection = broadcastsSection.getConfigurationSection(key);

					if (!keySection.contains("message")) {
						continue;
					}

					Notification notification = new Notification();
					notification.load(keySection);
					notifications.add(notification);
				}

				autoBroadcasts = notifications;
			}

			// Load state change broadcasts
			ConfigurationSection stateChangeSection = config.getConfigurationSection("state-broadcasts");
			if (stateChangeSection != null) {
				Map<CompState, StateChangeNotification> stateChangeNotifications = Maps.newHashMap();

				for (String key : stateChangeSection.getKeys(false)) {
					ConfigurationSection keySection = stateChangeSection.getConfigurationSection(key);

					if (!keySection.contains("message") && !keySection.contains("location") && !keySection.contains("display-time")) {
						continue;
					}

					// Determine the state
					CompState state;
					switch (key.toLowerCase()) {
					case "open":
						state = CompState.Open;
						break;
					case "closed":
						state = CompState.Closed;
						break;
					case "voting":
						state = CompState.Voting;
						break;
					case "visit":
						state = CompState.Visit;
						break;
					default:
						// Invalid state
						continue;
					}

					StateChangeNotification notification = new StateChangeNotification();
					notification.load(keySection);
					stateChangeNotifications.put(state, notification);
				}

				this.stateStateChangeNotifications = stateChangeNotifications;
			}
		} catch (InvalidConfigurationException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * Gets all notifications that this player can receive based on conditions
	 * @param player The player to filter for
	 * @return A list of notifications
	 */
	private List<Notification> findApplicableNotifications(Player player) {
		List<Notification> notifications = Lists.newArrayList();
		for (Notification notification : autoBroadcasts) {
			if (notification.getIfEntrant().isPresent()) {
				boolean isEntrant = compManager.hasEntered(player);
				
				if (isEntrant != notification.getIfEntrant().get()) {
					continue;
				}
			}
			
			if (notification.getIfFull().isPresent()) {
				boolean isFull = compManager.isFull();
				
				if (isFull != notification.getIfFull().get()) {
					continue;
				}
			}
			
			if (notification.getIfState().isPresent()) {
				CompState state = compManager.getState();
				
				if (state != notification.getIfState().get()) {
					continue;
				}
			}
			
			notifications.add(notification);
		}
		
		return notifications;
	}
	
	/**
	 * Sends out periodic broadcasts if the timer has elapsed
	 */
	public void doBroadcasts() {
		updateRefreshers();
		
		if (System.currentTimeMillis() < nextBroadcastTime) {
			return;
		}
		
		// Since notifications can have conditions, this makes notifications an almost
		// per player experience so its easier to just determine what notifications each
		// player can have and go from there
		for (Player player : Bukkit.getOnlinePlayers()) {
			List<Notification> notifications = findApplicableNotifications(player);
			
			if (notifications.isEmpty()) {
				continue;
			}
			
			Notification toDisplay;
			if (broadcastInSequence) {
				toDisplay = notifications.get(broadcastIndex % notifications.size());
			} else {
				toDisplay = notifications.get(RandomUtils.nextInt(notifications.size()));
			}
			
			sendMessage(player, toDisplay.formatMessage(compManager), broadcastLocation, broadcastDisplayTime);
		}
		
		++broadcastIndex;
		nextBroadcastTime = System.currentTimeMillis() + broadcastInterval;
	}
	
	/**
	 * Periodically refresh action bar displays as they only stay for a small time
	 */
	private void updateRefreshers() {
		Iterator<Long> it = displayRefreshers.keySet().iterator();
		
		while (it.hasNext()) {
			Long key = it.next();
			
			for (Runnable refresher : displayRefreshers.get(key)) {
				refresher.run();
			}
			
			if (System.currentTimeMillis() > key) {
				it.remove();
			}
		}
	}
	
	/**
	 * Broadcasts a message to all players on the server
	 * @param message The message to display
	 * @param target Where to display the message
	 * @param displayTime The time in ms to display the message for. Not applicable to Chat or SystemMessage targets
	 */
	public void broadcast(String message, DisplayTarget target, long displayTime) {
		broadcast(message, target, displayTime, Predicates.alwaysTrue());
	}
	
	/**
	 * Broadcasts a message to all players on the server that match the predicate
	 * @param message The message to display
	 * @param target Where to display the message
	 * @param displayTime The time in ms to display the message for. Not applicable to Chat or SystemMessage targets
	 * @param predicate A predicate to select players who will receive the broadcast
	 */
	public void broadcast(String message, DisplayTarget target, long displayTime, Predicate<? super Player> predicate) {
		final ChatMessage messageObject = ChatMessage.begin(message);
		
		// Prepare the title if needed
		Title title = null;
		if (target == DisplayTarget.Subtitle || target == DisplayTarget.Title) {
			title = new Title();
			title.setFadeInTime(500, TimeUnit.MILLISECONDS);
			title.setDisplayTime(displayTime, TimeUnit.MILLISECONDS);
			title.setFadeOutTime(1, TimeUnit.SECONDS);
			
			// Set the message
			if (target == DisplayTarget.Title) {
				title.setTitle(messageObject);
			} else {
				title.setSubtitle(messageObject);
			}
		}
		
		// Send out the message
		for (final Player player : Bukkit.getOnlinePlayers()) {
			if (!predicate.apply(player)) {
				continue;
			}
			
			switch (target) {
			case ActionBar:
				messageObject.send(player, ChatMessageType.ActionBar);
				long refreshTime = displayTime - ActionBarDisplayTime;
				// At least one tick
				if (refreshTime >= 50) {
					displayRefreshers.put(System.currentTimeMillis() + refreshTime, new Runnable() {
						public void run() {
							messageObject.send(player, ChatMessageType.ActionBar);
						}
					});
				}
				break;
			case Chat:
				messageObject.send(player, ChatMessageType.Standard);
				break;
			case SystemMessage:
				messageObject.send(player, ChatMessageType.System);
				break;
			case Subtitle:
			case Title:
				System.out.println("Showing title " + title);
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
	 * @param displayTime The time in ms to display the message for. Not applicable to Chat or SystemMessage targets
	 */
	public void sendMessage(final Player player, String message, DisplayTarget target, long displayTime) {
		final ChatMessage messageObject = ChatMessage.begin(message);
		
		// Prepare the title if needed
		Title title = null;		
		if (target == DisplayTarget.Subtitle || target == DisplayTarget.Title) {
			title = new Title();
			title.setFadeInTime(500, TimeUnit.MILLISECONDS);
			title.setDisplayTime(displayTime, TimeUnit.MILLISECONDS);
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
			long refreshTime = displayTime - ActionBarDisplayTime;
			// At least one tick
			if (refreshTime >= 50) {
				displayRefreshers.put(System.currentTimeMillis() + refreshTime, new Runnable() {
					public void run() {
						messageObject.send(player, ChatMessageType.ActionBar);
					}
				});
			}
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

	public void broadcastStateChange(CompState newState) {
		StateChangeNotification notification = stateStateChangeNotifications.get(newState);
		if (notification == null) {
			return;
		}

		broadcast(notification.formatMessage(compManager), notification.getDisplayTarget(), notification.getDisplayTime());
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
