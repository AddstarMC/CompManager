package au.com.addstar.comp;

import com.google.common.collect.Lists;

import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.util.Date;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * Created for the AddstarMC IT Project.
 * Created by Narimm on 7/05/2019.
 */
public class CompPlaceHolder extends PlaceholderExpansion {

    private CompPlugin plugin;
    private static String identifier = "COMPMANAGER";
    @Override
    public String getIdentifier() {
        return identifier;
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
    public String onPlaceholderRequest(Player player, String s) {
        switch(s.toLowerCase()){
            case "theme":
                return plugin.getCompManager().getCurrentComp().getTheme();
            case "description":
                return plugin.getCompManager().getCurrentComp().getTheme();
            case "startTime":
                Date date = new Date(plugin.getCompManager().getCurrentComp().getStartDate());
                return DateFormat.getDateTimeInstance().format(date);
            case "endTime":
                date = new Date(plugin.getCompManager().getCurrentComp().getEndDate());
                return DateFormat.getDateTimeInstance().format(date);
            case "voteEnd":
                date = new Date(plugin.getCompManager().getCurrentComp().getVoteEndDate());
                return DateFormat.getDateTimeInstance().format(date);
            case "state":
                return plugin.getCompManager().getCurrentComp().getState().toString();
            case "running":
                return Boolean.toString(plugin.getCompManager().getCurrentComp().isRunning());
            case "fullStatus":
                return Boolean.toString(plugin.getCompManager().isFull());
            default:
                    return "NOT_FOUND";
        }

        return null;
    }
}
