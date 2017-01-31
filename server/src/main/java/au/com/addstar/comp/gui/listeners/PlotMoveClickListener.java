package au.com.addstar.comp.gui.listeners;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.util.P2Bridge;
import com.intellectualcrafters.plot.object.Plot;
import com.plotsquared.bukkit.object.BukkitPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;


import java.util.UUID;

/**
 * Created for the AddstarMC
 * Created by Narimm on 24/01/2017.
 */
public class PlotMoveClickListener implements ButtonClickListener {

    private final CompManager manager;
    private final P2Bridge bridge;
    private final Messages messages;
    private boolean prev;

    public PlotMoveClickListener(CompPlugin plugin) {
        this(plugin,false);
    }

    public PlotMoveClickListener(CompPlugin plugin,boolean prev) {
        manager = plugin.getCompManager();
        bridge = plugin.getBridge();
        messages = plugin.messages;
        this.prev = prev;
    }

    @Override
    public void onClick(Player player) {
        Plot plot = bridge.getPlotAt(player.getLocation());
        Plot tpPlot;
        if(plot != null) {
            if (prev) {
                tpPlot = getPrevPlot(plot);
            } else {
                tpPlot = getNextPlot(plot);
            }
            if (tpPlot == null) {
                player.sendMessage(messages.get("teleport.no.more.plots"));
                return;
            }
        }else{
            tpPlot = getNextPlot(null);
        }

        tpPlot.teleportPlayer(new BukkitPlayer(player));
        player.sendMessage(messages.get("teleport.next.plot"));
        player.sendMessage("Owned by " + Bukkit.getOfflinePlayer(tpPlot.guessOwner()).getName());


    }

    private Plot getNextPlot(Plot plot) {
        Iterable<Plot> plots = bridge.getOwnedPlots();
        boolean found = false;
        Plot p = null;
        for (Plot newPlot : plots) {
            if (found) {
                for (UUID id : newPlot.getOwners()) {
                    if (id != null) {
                        OfflinePlayer entrant = Bukkit.getOfflinePlayer(id);
                        if (entrant != null) {
                            if (manager.hasEntered(entrant)) {
                                p = newPlot;
                                return p;
                            }
                        }
                    }
                }
            }
            if(plot == null){
                for (UUID id : newPlot.getOwners()) {
                    OfflinePlayer entrant = Bukkit.getOfflinePlayer(id);
                    if (manager.hasEntered(entrant)) {
                        p = newPlot;
                        return p;
                    }
                }
            }else {
                if (newPlot.getId() == plot.getId()) found = true;
            }
        }
        if (!found) {
            p = null;
        }
        return p;
    }

    private Plot getPrevPlot(Plot plot) {
        Iterable<Plot> plots = bridge.getOwnedPlots();
        boolean found = false;
        Plot p = null;
        int i = 0;
        for (Plot newPlot : plots) {

            if (newPlot.getId() == plot.getId()) found = true;
            if (found) {
                break;
            }
            i++;
        }
        int x = 0;
        if(i==0){
          return null;
        }
        for (Plot newPlot : plots) {
            if (x == i - 1) {
                for (UUID id : newPlot.getOwners()) {
                    OfflinePlayer entrant = Bukkit.getOfflinePlayer(id);
                    if (manager.hasEntered(entrant)) {
                        p = newPlot;
                        return p;
                    } else {
                        return getPrevPlot(newPlot);
                    }
                }
                break;
            }
            x++;
        }
        return p;
    }

}

