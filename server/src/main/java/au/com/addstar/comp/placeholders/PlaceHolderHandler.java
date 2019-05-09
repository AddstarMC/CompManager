package au.com.addstar.comp.placeholders;

import au.com.addstar.comp.CompPlugin;

import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by narimm on 9/05/2019.
 */
public class PlaceHolderHandler {
    private static final String identifier = "COMPMANAGER";
    private final CompPlugin plugin;

    public PlaceHolderHandler(CompPlugin plugin) {
        this.plugin = plugin;
    }

    static String getIdentifier() {
        return identifier;
    }

    static List<String> getPlaceholders(){
        List<String> r = new ArrayList<>();
        r.add("theme");
        r.add("description");
        r.add("startTime");
        r.add("endTime");
        r.add("voteendtime");
        r.add("state");
        r.add("running");
        r.add("fullstatus");
        r.add("firstprize");
        r.add("secondprize");
        r.add("participationprize");
        r.add("criteria");
        return r;
    }

    String  getPlacHolderReplaceMent(Player player,String s){
        switch(s.toLowerCase()){
            case "theme":
                return plugin.getCompManager().getCurrentComp().getTheme();
            case "description":
                return plugin.getCompManager().getCurrentComp().getTheme();
            case "starttime":
                Date date = new Date(plugin.getCompManager().getCurrentComp().getStartDate());
                return DateFormat.getDateTimeInstance().format(date);
            case "endtime":
                date = new Date(plugin.getCompManager().getCurrentComp().getEndDate());
                return DateFormat.getDateTimeInstance().format(date);
            case "voteend":
                date = new Date(plugin.getCompManager().getCurrentComp().getVoteEndDate());
                return DateFormat.getDateTimeInstance().format(date);
            case "state":
                return plugin.getCompManager().getCurrentComp().getState().toString();
            case "running":
                return Boolean.toString(plugin.getCompManager().getCurrentComp().isRunning());
            case "fullstatus":
                return Boolean.toString(plugin.getCompManager().isFull());
            case "firstprize":
                return plugin.getCompManager().getCurrentComp().getFirstPrize().toHumanReadable();
            case "secondprize":
                return plugin.getCompManager().getCurrentComp().getSecondPrize().toHumanReadable();
            case "participationprize":
                return plugin.getCompManager().getCurrentComp().getParticipationPrize().toHumanReadable();
            case "criteria":
                return plugin.getCompManager().getCurrentComp().getCriteria().toString();
            default:
                return null;
        }
    }

}
