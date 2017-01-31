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
import java.util.logging.Logger;

/**
 * Created for the AddstarMC
 * Created by Narimm on 24/01/2017.
 */
public class PlotMoveClickListener implements ButtonClickListener {

    private final P2Bridge bridge;
    private final Messages messages;
    private boolean prev;

    public PlotMoveClickListener(CompPlugin plugin) {
        this(plugin,false);
    }

    public PlotMoveClickListener(CompPlugin plugin,boolean prev) {
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
        for (Plot newPlot : plots) {
            if (found) {
                for (UUID id : newPlot.getOwners()) {
                    if (id != null) {
                                return newPlot;
                    }
                }
            }
            if(plot == null){
                for (UUID id : newPlot.getOwners()) {
                    if(id != null) {
                        return newPlot;
                    }
                }
            }else {
                if (newPlot.getId() == plot.getId()) found = true;
            }
        }
        return null;
    }
    private Plot getPrevPlot(Plot plot) {
        Iterable<Plot> plots = bridge.getOwnedPlots();
        boolean found = false;
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

                        return newPlot;
                    }

            if( x>i)break;
            x++;
        }
        return null;
    }
}

