package au.com.addstar.comp.lobby;

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

    public CompPlaceHolder(LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    private LobbyPlugin plugin;

    @Override
    public String getIdentifier() {
        return "COMPLOBBYMANAGER";
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
        r.add("theme<serverId>");
        r.add("startTime<serverId>");
        r.add("endTime<serverId>");
        r.add("voteend<serverId>");
        r.add("state<serverId>");
        r.add("firstprize<serverId>");
        return r;
    }
    @Override
    public String onPlaceholderRequest(Player player, String s) {
        if ("servers".equals(s.toLowerCase())) {
            return plugin.getManager().getServerIds().toString();
        }else if(s.toLowerCase().startsWith("theme")){
            String serverId = s.substring("theme".length());
            return plugin.getManager().getServer(serverId).getCurrentComp().getTheme();
        }else if(s.toLowerCase().startsWith("starttime")){
            String serverId = s.substring("starttime".length());
            return formatDate(plugin.getManager().getServer(serverId).getCurrentComp().getStartDate());
        }else if(s.toLowerCase().startsWith("endtime")){
            String serverId = s.substring("endtime".length());
            return formatDate(plugin.getManager().getServer(serverId).getCurrentComp().getEndDate());
        }else if(s.toLowerCase().startsWith("voteend")){
            String serverId = s.substring("voteend".length());
            return formatDate(plugin.getManager().getServer(serverId).getCurrentComp().getVoteEndDate());
        }else if(s.toLowerCase().startsWith("state")){
            String serverId = s.substring("state".length());
            return plugin.getManager().getServer(serverId).getCurrentComp().getState().toString();
        }else if(s.toLowerCase().startsWith("firstprize")){
            String serverId = s.substring("firstprize".length());
            return plugin.getManager().getServer(serverId).getCurrentComp().getFirstPrize().toHumanReadable();
        }
        return null;
    }
    private String formatDate(Long date){
        Date out = new Date(date);
        return DateFormat.getDateTimeInstance().format(out);
    }
}
