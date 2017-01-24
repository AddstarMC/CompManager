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
        boolean found = false;
        Iterable<Plot> plots = bridge.getOrderedPlots(bridge.getUsedPlotCount());
        Plot tpPlot;
        if (prev) {
            tpPlot = getPrevPlot(plot);
        } else {
            tpPlot = getNextPlot(plot);
        }
        tpPlot.teleportPlayer(new BukkitPlayer(player));
        player.sendMessage("Teleported to PLot");
    }

    private Plot getNextPlot(Plot plot) {
        Iterable<Plot> plots = bridge.getOrderedPlots(bridge.getUsedPlotCount());
        boolean found = false;
        Plot p = null;
        for (Plot newPlot : plots) {
            if (found) {
                for (UUID id : newPlot.getOwners()) {
                    OfflinePlayer entrant = Bukkit.getOfflinePlayer(id);
                    if (manager.hasEntered(entrant)) {
                        p = newPlot;
                        return p;
                    }
                }
            }
            if (newPlot.getId() == plot.getId()) found = true;
        }
        if (!found) {
            p = bridge.getOrderedPlots(1).iterator().next();
        }
        return p;
    }

    private Plot getPrevPlot(Plot plot) {
        Iterable<Plot> plots = bridge.getOrderedPlots(bridge.getUsedPlotCount());
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
        bridge.getOrderedPlots(bridge.getUsedPlotCount());
        int x = 0;
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
        if (p == null) p = bridge.getOrderedPlots(1).iterator().next();
        return p;
    }

}

