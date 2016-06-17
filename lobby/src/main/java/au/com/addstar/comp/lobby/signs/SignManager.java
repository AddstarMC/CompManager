package au.com.addstar.comp.lobby.signs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.util.Messages;

public class SignManager {
	private final File storageFile;
	private final CompManager compManager;
	private final Messages messages;
	private final ConfirmationManager confirmationManager;
	
	private final Map<Block, BaseSign> signs;
	private final ListMultimap<String, BaseSign> serverSigns;
	
	private final Map<Player, Function<Block, BaseSign>> pendingSigns;
	
	public SignManager(File storageFile, CompManager compManager, Messages messages, ConfirmationManager confirmationManager) {
		this.storageFile = storageFile;
		this.compManager = compManager;
		this.messages = messages;
		this.confirmationManager = confirmationManager;
		
		signs = Maps.newHashMap();
		serverSigns = ArrayListMultimap.create();
		pendingSigns = Maps.newHashMap();
	}
	
	/**
	 * Marks that a player will be placing a sign.
	 * @param player The player to listen for
	 * @param creationFunction A function which makes the sign given the clicked sign
	 */
	public void addPendingSign(Player player, Function<Block, BaseSign> creationFunction) {
		pendingSigns.put(player, creationFunction);
	}
	
	/**
	 * Removes a pending sign
	 * @param player The player to remove
	 */
	public void removePendingSign(Player player) {
		pendingSigns.remove(player);
	}
	
	/**
	 * Handles a pending sign if present
	 * @param player The player who interacted
	 * @param block The sign block
	 * @return True if it was handled
	 */
	boolean handlePending(Player player, Block block) {
		Function<Block, BaseSign> creationFunction = pendingSigns.remove(player);
		
		if (creationFunction == null) {
			return false;
		}
		
		BaseSign sign = creationFunction.apply(block);
		if (sign == null) {
			return false;
		}
		
		addSign(sign);
		sign.refresh();
		
		// TODO: Maybe not the best place for this
		try {
			save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Adds a sign
	 * @param sign The sign to add
	 */
	public void addSign(BaseSign sign) {
		signs.put(sign.getBlock(), sign);
		serverSigns.put(sign.getServerId(), sign);
	}
	
	/**
	 * Removes a sign
	 * @param sign The sign to remove
	 */
	public void removeSign(BaseSign sign) {
		BaseSign existing = getSign(sign.getBlock());
		if (existing == sign) {
			signs.remove(sign.getBlock());
			serverSigns.remove(sign.getServerId(), sign);
		}
	}
	
	/**
	 * Removes all signs for a server
	 * @param serverId The id of the server to remove signs for
	 */
	public void removeAll(String serverId) {
		List<BaseSign> signs = serverSigns.removeAll(serverId);
		for (BaseSign sign : signs) {
			this.signs.remove(sign.getBlock());
		}
	}
	
	/**
	 * Gets the sign object on a block if present
	 * @param block The block to get a sign for
	 * @return The sign object or null
	 */
	public BaseSign getSign(Block block) {
		return signs.get(block);
	}
	
	/**
	 * Gets all signs for a server
	 * @param serverId The id of the server to get signs for
	 * @return An unmodifiable list of signs
	 */
	public List<BaseSign> getSigns(String serverId) {
		return Collections.unmodifiableList(serverSigns.get(serverId));
	}
	
	/**
	 * Gets all added signs
	 * @return An unmodifiable collection of signs
	 */
	public Collection<BaseSign> getAllSigns() {
		return Collections.unmodifiableCollection(signs.values());
	}
	
	/**
	 * Loads the signs from the storage file
	 * @throws IOException Thrown if an IOException occurs while loading
	 */
	public void load() throws IOException {
		if (!storageFile.exists()) {
			return;
		}
		
		YamlConfiguration storage = new YamlConfiguration();
		try {
			storage.load(storageFile);
			
			for (String key : storage.getKeys(false)) {
				ConfigurationSection signData = storage.getConfigurationSection(key);
				
				// Load the location
				Block block;
				World world = Bukkit.getWorld(signData.getString("world"));
				if (world == null) {
					// Ignore the sign
					continue;
				}
				
				block = world.getBlockAt(signData.getInt("x"), signData.getInt("y"), signData.getInt("z"));
				
				// Load the other commons
				String serverId = signData.getString("server");
				
				// Create the sign
				BaseSign sign;
				switch (signData.getString("type")) {
				case "info":
					sign = new InfoSign(serverId, block, compManager);
					break;
				case "join":
					sign = new JoinSign(serverId, block, compManager, messages, confirmationManager);
					break;
				case "visit":
					sign = new VisitSign(serverId, block, compManager, messages);
					break;
				default:
					// Ignore the sign
					continue;
				}
				
				sign.load(signData);
				
				addSign(sign);
			}
		} catch (InvalidConfigurationException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * Saves the signs to the storage file
	 * @throws IOException Thrown if an IOException occurs while saving
	 */
	public void save() throws IOException {
		YamlConfiguration storage = new YamlConfiguration();
		
		int nextId = 0;
		for (BaseSign sign : signs.values()) {
			ConfigurationSection signData = storage.createSection(String.valueOf(nextId++));
			
			if (sign instanceof InfoSign) {
				signData.set("type", "info");
			} else if (sign instanceof JoinSign) {
				signData.set("type", "join");
			} else if (sign instanceof VisitSign) {
				signData.set("type", "visit");
			} else {
				// Ignore it
				continue;
			}
			
			// Save location
			Block block = sign.getBlock();
			signData.set("world", block.getWorld().getName());
			signData.set("x", block.getX());
			signData.set("y", block.getY());
			signData.set("z", block.getZ());
			
			// Save common data
			signData.set("server", sign.getServerId());
			
			// Save specific data
			sign.save(signData);
		}
		
		storage.save(storageFile);
	}
	
	public JoinSign makeJoinSign(String serverId, Block block) {
		return new JoinSign(serverId, block, compManager, messages, confirmationManager);
	}
	
	public VisitSign makeVisitSign(String serverId, Block block) {
		return new VisitSign(serverId, block, compManager, messages);
	}

	public InfoSign makeInfoSign(String serverId, InfoSign.InfoType type, Block block) {
		return new InfoSign(serverId, type, block, compManager);
	}
}
