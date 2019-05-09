package au.com.addstar.comp;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import au.com.addstar.comp.entry.EnterHandler;
import au.com.addstar.comp.entry.EntryDeniedException;
import au.com.addstar.comp.redis.QueryException;
import au.com.addstar.comp.redis.RedisQueryHandler;

public class RemoteJoinManager implements RedisQueryHandler {
	private final Map<UUID, EnterHandler> pendingEntrants;
	private final TreeMultimap<Long, UUID> expiryTime;
	private final CompManager compManager;
	
	private final long expiryLength;
	
	public RemoteJoinManager(CompManager compManager, long expiryLength) {
		this.compManager = compManager;
		this.expiryLength = expiryLength;
		
		pendingEntrants = Maps.newHashMap();
		expiryTime = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
	}
	
	public void beginJoin(UUID playerId) throws EntryDeniedException {
		synchronized (pendingEntrants) {
			EnterHandler pending = pendingEntrants.get(playerId);
			if (pending != null) {
				return;
			}
			
			OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
			EnterHandler handler = compManager.enterComp(player);
			
			pendingEntrants.put(playerId, handler);
			expiryTime.put(System.currentTimeMillis() + expiryLength, playerId);
		}
	}
	
	public boolean confirmJoin(UUID playerId) {
		synchronized (pendingEntrants) {
			final EnterHandler handler = pendingEntrants.remove(playerId);
			if (handler != null) {
				Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(CompPlugin.class), handler::complete);
				return true;
			} else {
				return false;
			}
		}
	}
	
	public boolean abortJoin(UUID playerId) {
		synchronized (pendingEntrants) {
			final EnterHandler handler = pendingEntrants.remove(playerId);
			if (handler != null) {
				Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(CompPlugin.class), handler::abort);
				return true;
			} else {
				return false;
			}
		}
	}
	
	public void expireHandlers() {
		synchronized (pendingEntrants) {
			Iterator<Long> timeIterator = expiryTime.keySet().iterator();
			while (timeIterator.hasNext()) {
				long time = timeIterator.next();
				
				if (System.currentTimeMillis() >= time) {
					// They have expired
					Set<UUID> players = expiryTime.get(time);
					timeIterator.remove();
					
					for (UUID player : players) {
						EnterHandler handler = pendingEntrants.remove(player);
						handler.abort();
					}
				} else {
					// No more have expired
					break;
				}
			}
		}
	}
	
	@Override
	public String onQuery(String command, String[] arguments) throws QueryException {
		if (arguments.length < 1) {
			throw new QueryException("Invalid Query. Requires: <uuid>");
		}
		
		UUID playerId;
		try {
			playerId = UUID.fromString(arguments[0]);
		} catch (IllegalArgumentException e) {
			throw new QueryException("Invalid UUID");
		}
		
		// Parse the command
		if (command.equalsIgnoreCase("join_begin")) {
			try {
				beginJoin(playerId);
				return "true";
			} catch (EntryDeniedException e) {
				return "false," + e.getReason().name();
			}
		} else if (command.equalsIgnoreCase("join_confirm")) {
			if (confirmJoin(playerId)) {
				return "true";
			} else {
				return "false";
			}
		} else if (command.equalsIgnoreCase("join_abort")) {
			if (abortJoin(playerId)) {
				return "true";
			} else {
				return "false";
			}
		} else {
			throw new AssertionError("This handle should not be registed for the command " + command);
		}
	}
}
