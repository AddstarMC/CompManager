package au.com.addstar.comp;

import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.gui.Hotbar;
import au.com.addstar.comp.voting.AbstractVotingStrategy;
import au.com.addstar.comp.voting.VotingStrategies;
import com.google.common.base.Strings;
import com.plotsquared.bukkit.events.PlayerEnterPlotEvent;
import com.plotsquared.bukkit.events.PlotEvent;
import com.plotsquared.bukkit.listeners.PlayerEvents;
import com.sun.media.jfxmedia.events.PlayerStateEvent;
import net.minecraft.server.v1_11_R1.PlayerInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

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
            Hotbar hotbar = CompPlugin.getCurrentHotbars().get(event.getPlayer());
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
    public void onPlotEntry(PlayerEnterPlotEvent e){
        if (CompPlugin.instance.getCompManager().getState() != CompState.Voting) {
            return;
        }
        if (!CompPlugin.getCurrentHotbars().containsKey(e.getPlayer())) {
            AbstractVotingStrategy strategy = VotingStrategies.getStrategy(CompPlugin.instance.getCompManager().getCurrentComp().getVotingStrategy());
            if(strategy ==  null){
                strategy = VotingStrategies.getDefault();
            }
            if(strategy.hasHotbar()){
                e.getPlayer().sendMessage("Strategy has a hotbar - creating");
                CompPlugin.setHotbar(strategy.getHotbar(), e.getPlayer());
            }else{
                e.getPlayer().sendMessage("Strategy has no hotbar." + strategy.toString());
            }
        }else{
            CompPlugin.setHotbar(CompPlugin.getCurrentHotbars().get(e.getPlayer()),e.getPlayer());
        }
    }


    @EventHandler
    public void onChangeItemHeld(PlayerItemHeldEvent event){
        if (CompPlugin.getCurrentHotbars().containsKey(event.getPlayer())) {
            Hotbar hotbar = CompPlugin.getCurrentHotbars().get(event.getPlayer());
            //hotbar.onClick(event.getNewSlot())
        }

    }

    @EventHandler
    public void onCreativeInventory(InventoryCreativeEvent event){
        if(event.getAction().equals(InventoryAction.HOTBAR_SWAP) || event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD) ){
        if(event.getInventory().getHolder() instanceof Player){
            if (CompPlugin.getCurrentHotbars().containsKey((Player) event.getInventory().getHolder())) {
                event.setCancelled(true);
            }
            }
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
        if(event.getWhoClicked() instanceof Player){
        Player player = (Player)event.getWhoClicked();
        logger.info(player.getDisplayName() + " clicked slot " + event.getSlot() + "button=" + event.getHotbarButton());
        if (CompPlugin.getCurrentHotbars().containsKey(player)) {
            event.setCancelled(true);
        }
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
