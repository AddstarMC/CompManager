package au.com.addstar.comp.lobby.placeholder;

import au.com.addstar.comp.lobby.LobbyPlugin;

import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by narimm on 9/05/2019.
 */
public class PlaceHolderHandler {
    private final static String identifier = "COMPLOBBYMANAGER".toLowerCase();
    private final LobbyPlugin plugin;
    private @Nullable
    final Set<String> allServers;

    public PlaceHolderHandler(LobbyPlugin plugin) {
        this.plugin = plugin;
        allServers = plugin.getManager().getAllOfflineServers();
    }

    static String getIdentifier() {
        return identifier;
    }

    protected List<String> getFullPlaceHolders(){
        return getPlaceholders().stream().map(s -> identifier+"_"+s).collect(Collectors.toList());
    }

    protected List<String> getPlaceholders(){
        List<String> r = new ArrayList<>();
        if(allServers != null)
        for(String server: allServers){
            String suffix = "_"+server;
            r.add("theme"+suffix);
            r.add("startTime"+suffix);
            r.add("endTime"+suffix);
            r.add("voteendtime"+suffix);
            r.add("state"+suffix);
            r.add("running"+suffix);
            r.add("fullstatus"+suffix);
            r.add("firstprize"+suffix);
            r.add("secondprize"+suffix);
            r.add("participationprize"+suffix);
            r.add("criteria"+suffix);
        }
        r.add("onlineservers");
        r.add("allservers");
        return r;
    }
    private String trim(String s){
        if(s.toLowerCase().startsWith(identifier+"_")){
            s = s.toLowerCase().replace(identifier+"_","");
        }
        return s;
    }

    private String getPlaceHolderRaw(String s){
        String o = trim(s);
        if(o.contains("_")){
            String[] parts = o.split("_");
            return parts[0];
        }
        else return o;
    }

    private String getServerId(String s) throws IllegalArgumentException{
        String o = trim(s);
        if(o.contains("_")){
            String[] parts = o.split("_");
            if(allServers !=null && allServers.contains(parts[1])){
                return parts[1];
            } else throw new IllegalArgumentException(parts[1] + " is not a valid server ID");
        }
        else return o;
    }
    protected String onPlaceholderRequest(Player player, String s) {
        String serverID;
        try {
            serverID = getServerId(s);
        }catch (IllegalArgumentException e){
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Invalid placeholder request", e);
            return null;
        }
        String filteredString = getPlaceHolderRaw(s);
        if ("onlineservers".equals(filteredString))
            return plugin.getManager().getServerIds().toString();
        if ("allservers".equals(filteredString))
            return allServers!=null?allServers.toString():null;
        if(plugin.getManager().getServer(serverID).isOnline()) {
            switch (filteredString) {
                case "theme":
                    return plugin.getManager().getServer(serverID).getCurrentComp().getTheme();
                case "starttime":
                    return formatDate(plugin.getManager().getServer(serverID).getCurrentComp().getStartDate());
                case "endtime":
                    return formatDate(plugin.getManager().getServer(serverID).getCurrentComp().getEndDate());
                case "voteend":
                    return formatDate(plugin.getManager().getServer(serverID).getCurrentComp().getVoteEndDate());
                case "state":
                    return plugin.getManager().getServer(serverID).getCurrentComp().getState().toString();
                case "firstprize":
                    return plugin.getManager().getServer(serverID).getCurrentComp().getFirstPrize().toHumanReadable();
                case "secondprize":
                    return plugin.getManager().getServer(serverID).getCurrentComp().getSecondPrize().toHumanReadable();
                case "participationprize":
                    return plugin.getManager().getServer(serverID).getCurrentComp().getParticipationPrize().toHumanReadable();
                case "criteria":
                    return plugin.getManager().getServer(serverID).getCurrentComp().getCriteria().toString();
                default:
                    return null;
            }
        }else {
            return plugin.getManager().getMessage("state.offline");
        }
    }

    private String formatDate(Long date){
        Date out = new Date(date);
        return DateFormat.getDateTimeInstance().format(out);
    }

}
