package au.com.addstar.comp.lobby.placeholder;

import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.EntrantResult;
import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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
    
    /**
     * Cache for player-specific results (hasentered, prize, prizeclaimed)
     * Key: CacheKey(compId, playerUUID)
     */
    private final Cache<CacheKey, EntrantResult> resultCache;
    
    /**
     * Cache for competition-wide entrant counts
     * Key: compId
     */
    private final Cache<Integer, Integer> entrantCountCache;
    
    /**
     * Cache key for player-specific data
     */
    private static class CacheKey {
        private final int compId;
        private final UUID playerId;
        
        CacheKey(int compId, UUID playerId) {
            this.compId = compId;
            this.playerId = playerId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return compId == cacheKey.compId && playerId.equals(cacheKey.playerId);
        }
        
        @Override
        public int hashCode() {
            return 31 * compId + playerId.hashCode();
        }
    }

    public PlaceHolderHandler(LobbyPlugin plugin) {
        this.plugin = plugin;
        allServers = plugin.getManager().getAllOfflineServers();
        
        // Initialize caches
        resultCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();
        
        entrantCountCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(100)
            .build();
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
            r.add("timeuntilstart"+suffix);
            r.add("timeuntilend"+suffix);
            r.add("timeuntilvoteend"+suffix);
            r.add("state"+suffix);
            r.add("running"+suffix);
            r.add("fullstatus"+suffix);
            r.add("firstprize"+suffix);
            r.add("secondprize"+suffix);
            r.add("participationprize"+suffix);
            r.add("criteria"+suffix);
            r.add("hasentered"+suffix);
            r.add("prize"+suffix);
            r.add("prizeclaimed"+suffix);
            r.add("iswhitelisted"+suffix);
            r.add("spotsremaining"+suffix);
            r.add("maxentrants"+suffix);
            r.add("entrants"+suffix);
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
                case "timeuntilstart":
                    return CompUtils.formatTimeRemaining(plugin.getManager().getServer(serverID).getCurrentComp().getStartDate() - System.currentTimeMillis());
                case "timeuntilend":
                    return CompUtils.formatTimeRemaining(plugin.getManager().getServer(serverID).getCurrentComp().getEndDate() - System.currentTimeMillis());
                case "timeuntilvoteend":
                    return CompUtils.formatTimeRemaining(plugin.getManager().getServer(serverID).getCurrentComp().getVoteEndDate() - System.currentTimeMillis());
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
                case "hasentered":
                    return getHasEntered(player, serverID);
                case "prize":
                    return getPrize(player, serverID);
                case "prizeclaimed":
                    return getPrizeClaimed(player, serverID);
                case "iswhitelisted":
                    return getIsWhitelisted(player, serverID);
                case "spotsremaining":
                    return getSpotsRemaining(serverID);
                case "maxentrants":
                    return getMaxEntrants(serverID);
                case "entrants":
                    return getEntrants(serverID);
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
    
    /**
     * Gets whether a player has entered a competition on a specific server (cached)
     */
    private String getHasEntered(Player player, String serverId) {
        CompServer server = plugin.getManager().getServer(serverId);
        if (!server.isOnline() || server.getCurrentComp() == null) {
            return null;
        }
        
        EntrantResult result = getCachedResult(server.getCurrentComp(), player.getUniqueId());
        return Boolean.toString(result != null);
    }
    
    /**
     * Gets the prize a player won on a specific server (cached)
     */
    private String getPrize(Player player, String serverId) {
        CompServer server = plugin.getManager().getServer(serverId);
        if (!server.isOnline() || server.getCurrentComp() == null) {
            return null;
        }
        
        EntrantResult result = getCachedResult(server.getCurrentComp(), player.getUniqueId());
        if (result != null && result.getPrize().isPresent()) {
            return result.getPrize().get().toHumanReadable();
        }
        return null;
    }
    
    /**
     * Gets whether a player's prize has been claimed on a specific server (cached)
     */
    private String getPrizeClaimed(Player player, String serverId) {
        CompServer server = plugin.getManager().getServer(serverId);
        if (!server.isOnline() || server.getCurrentComp() == null) {
            return null;
        }
        
        EntrantResult result = getCachedResult(server.getCurrentComp(), player.getUniqueId());
        if (result != null) {
            return Boolean.toString(result.isPrizeClaimed());
        }
        return null;
    }
    
    /**
     * Gets whether a player is whitelisted.
     * Note: This is not implemented on the lobby server yet.
     * @return null (unimplemented)
     */
    private String getIsWhitelisted(Player player, String serverId) {
        // Whitelist is not implemented on the lobby server yet.
        return null;
    }
    
    /**
     * Gets the number of spots remaining on a specific server
     */
    private String getSpotsRemaining(String serverId) {
        CompServer server = plugin.getManager().getServer(serverId);
        if (!server.isOnline() || server.getCurrentComp() == null) {
            return null;
        }
        
        int maxEntrants = server.getCurrentComp().getMaxEntrants();
        int currentEntrants = getCachedEntrantCount(server.getCurrentComp());
        return Integer.toString(Math.max(0, maxEntrants - currentEntrants));
    }
    
    /**
     * Gets the maximum number of entrants on a specific server
     */
    private String getMaxEntrants(String serverId) {
        CompServer server = plugin.getManager().getServer(serverId);
        if (!server.isOnline() || server.getCurrentComp() == null) {
            return null;
        }
        
        return Integer.toString(server.getCurrentComp().getMaxEntrants());
    }
    
    /**
     * Gets the current number of entrants on a specific server (cached)
     */
    private String getEntrants(String serverId) {
        CompServer server = plugin.getManager().getServer(serverId);
        if (!server.isOnline() || server.getCurrentComp() == null) {
            return null;
        }
        
        return Integer.toString(getCachedEntrantCount(server.getCurrentComp()));
    }
    
    /**
     * Gets a cached result for a player in a competition, or queries the database if not cached
     */
    private EntrantResult getCachedResult(Competition comp, UUID playerId) {
        try {
            return resultCache.get(
                new CacheKey(comp.getCompId(), playerId),
                () -> {
                    try {
                        CompBackendManager backend = plugin.getManager().getBackend();
                        return backend.getResult(comp, playerId);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to get result", e);
                        return null; // Cache null to avoid repeated failures
                    }
                }
            );
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting cached result", e);
            return null;
        }
    }
    
    /**
     * Gets the cached entrant count for a competition, or queries the database if not cached
     */
    private int getCachedEntrantCount(Competition comp) {
        try {
            return entrantCountCache.get(
                comp.getCompId(),
                () -> {
                    try {
                        CompBackendManager backend = plugin.getManager().getBackend();
                        java.util.Collection<EntrantResult> results = backend.loadResults(comp, false);
                        return results.size();
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to get entrant count", e);
                        return 0;
                    }
                }
            );
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting cached entrant count", e);
            return 0;
        }
    }

}
