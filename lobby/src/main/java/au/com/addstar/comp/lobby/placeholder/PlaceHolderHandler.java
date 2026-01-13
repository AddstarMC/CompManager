package au.com.addstar.comp.lobby.placeholder;

import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.criterions.BaseCriterion;

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
        String trimmed = trim(s);
        
        // Handle indexed criteria placeholders: criteria_<serverid>_<index>_name or criteria_<serverid>_<index>_description
        if (trimmed.startsWith("criteria_")) {
            String criteriaValue = getCriteriaPlaceholder(trimmed);
            if (criteriaValue != null) {
                return criteriaValue;
            }
        }
        
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
    
    /**
     * Handles indexed criteria placeholders: criteria_<serverid>_<index>_name or criteria_<serverid>_<index>_description
     * @param placeholder The placeholder string (e.g., "criteria_serverid_0_name")
     * @return The criteria name or description, or null if invalid
     */
    private String getCriteriaPlaceholder(String placeholder) {
        String[] parts = placeholder.split("_");
        // Format: criteria_<serverid>_<index>_<field>
        // So we need at least 4 parts: criteria, serverid, index, field
        if (parts.length < 4 || !parts[0].equals("criteria")) {
            return null;
        }
        
        // Extract server ID (parts[1])
        String serverId = parts[1];
        if (allServers == null || !allServers.contains(serverId)) {
            return null;
        }
        
        CompServer server = plugin.getManager().getServer(serverId);
        if (!server.isOnline() || server.getCurrentComp() == null) {
            return null;
        }
        
        try {
            // Extract index (parts[2])
            int index = Integer.parseInt(parts[2]);
            // Extract field (parts[3])
            String field = parts[3];
            
            List<BaseCriterion> criteria = server.getCurrentComp().getCriteria();
            if (index < 0 || index >= criteria.size()) {
                return null;
            }
            
            BaseCriterion criterion = criteria.get(index);
            if ("name".equals(field)) {
                return criterion.getName();
            } else if ("description".equals(field)) {
                return criterion.getDescription();
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }

    private String formatDate(Long date){
        Date out = new Date(date);
        return DateFormat.getDateTimeInstance().format(out);
    }

}
