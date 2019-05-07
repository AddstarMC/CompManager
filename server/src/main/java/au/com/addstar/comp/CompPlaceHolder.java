package au.com.addstar.comp;

import com.google.common.collect.Lists;

import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * Created for the AddstarMC IT Project.
 * Created by Narimm on 7/05/2019.
 */
public class CompPlaceHolder extends PlaceholderExpansion {

    public CompPlaceHolder(CompPlugin plugin) {
        this.plugin = plugin;
    }

    private CompPlugin plugin;

    @Override
    public String getIdentifier() {
        return "COMPMANAGER";
    }

    @Override
    public boolean persist(){
        return true;
    }
    @Override
    public String getPlugin() {
        return plugin.getName();
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public List<String> getPlaceholders(){
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
    @Override
    public String onPlaceholderRequest(Player player, String s) {
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
