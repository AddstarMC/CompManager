package au.com.addstar.comp.gui.listeners;

import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.gui.Hotbar;
import au.com.addstar.comp.voting.AbstractVotingStrategy;
import au.com.addstar.comp.voting.VotingStrategies;
import com.google.common.base.Strings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.*;

import java.util.logging.Logger;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 24/01/2017.
 */
public class HotbarListener implements Listener {

    private final Logger logger;
    private CompPlugin plugin;

    public HotbarListener(CompPlugin plugin) {

        logger = plugin.getLogger();
        this.plugin = plugin;

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        removePlayerHotbar(event.getPlayer());
        if(plugin.getCompManager().getState() == CompState.Voting){
            String s = plugin.getCompManager().getCurrentComp().getVotingStrategy();
            AbstractVotingStrategy strategy = null;
            if (Strings.isNullOrEmpty(s)) {
                strategy = VotingStrategies.getDefault();
            } else {
                strategy = VotingStrategies.getStrategy(s);
                if (strategy == null) {
                    if (s != null) {
                        logger.warning("Failed to find voting strategy " + s + ". Falling back to default strategy");
                    }
                    strategy = VotingStrategies.getDefault();
                }
            }
            if(strategy.hasHotbar()){
                CompPlugin.setHotbar(strategy.createHotbar(), event.getPlayer());
            }
        }
            }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        removePlayerHotbar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        removePlayerHotbar(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (CompPlugin.getCurrentHotbars().containsKey(event.getPlayer())) {
            Hotbar hotbar = (Hotbar) CompPlugin.getCurrentHotbars().get(event.getPlayer());
            hotbar.onClick(event.getPlayer().getInventory().getHeldItemSlot());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (CompPlugin.getCurrentHotbars().containsKey(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if ((event.getEntity() instanceof Player)) {
            Player player = (Player) event.getEntity();
            if (CompPlugin.getCurrentHotbars().containsKey(player)) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
        }

    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event)
    {
        Player player = (Player)event.getWhoClicked();
        if (CompPlugin.getCurrentHotbars().containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event)
    {
        if ((event.getInventory().getHolder() instanceof Player))
        {
            Player player = (Player)event.getInventory().getHolder();
            if (CompPlugin.getCurrentHotbars().containsKey(player)) {
                event.setCancelled(true);
            }
        }
    }

    public void removePlayerHotbar(Player player) {
         if (CompPlugin.getCurrentHotbars().containsKey(player)) {
            CompPlugin.removeHotbar(player);
        }
    }
}
