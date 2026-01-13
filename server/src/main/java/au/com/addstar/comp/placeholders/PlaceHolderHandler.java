package au.com.addstar.comp.placeholders;

import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.EntrantResult;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.util.CompUtils;

import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by narimm on 9/05/2019.
 */
public class PlaceHolderHandler {
    private static final String identifier = "COMPMANAGER".toLowerCase();
    private final CompPlugin plugin;

    public PlaceHolderHandler(CompPlugin plugin) {
        this.plugin = plugin;
    }

    protected static String getIdentifier() {
        return identifier;
    }

    protected static List<String> getFullPlaceHolders(){
        return getPlaceholders().stream().map(s -> identifier+"_"+s).collect(Collectors.toList());
    }

    protected static List<String> getPlaceholders(){
        List<String> r = new ArrayList<>();
        r.add("theme");
        r.add("startTime");
        r.add("endTime");
        r.add("voteendtime");
        r.add("timeuntilstart");
        r.add("timeuntilend");
        r.add("timeuntilvoteend");
        r.add("state");
        r.add("running");
        r.add("fullstatus");
        r.add("firstprize");
        r.add("secondprize");
        r.add("participationprize");
        r.add("criteria");
        r.add("hasentered");
        r.add("prize");
        r.add("prizeclaimed");
        r.add("iswhitelisted");
        r.add("spotsremaining");
        r.add("maxentrants");
        r.add("entrants");
        return r;
    }

    private String trim(String s){
        if(s.toLowerCase().startsWith(identifier+"_")){
            s = s.toLowerCase().replace(identifier+"_","");
        }
        return s;
    }

    protected String getPlaceHolderReplacement(Player player, String s){
        String filtered = trim(s);
        
        // Handle indexed criteria placeholders: criteria_<index>_name or criteria_<index>_description
        if (filtered.startsWith("criteria_")) {
            String criteriaValue = getCriteriaPlaceholder(filtered);
            if (criteriaValue != null) {
                return criteriaValue;
            }
        }
        
        switch(filtered){
            case "theme":
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
            case "timeuntilstart":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                return CompUtils.formatTimeRemaining(plugin.getCompManager().getCurrentComp().getStartDate() - System.currentTimeMillis());
            case "timeuntilend":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                return CompUtils.formatTimeRemaining(plugin.getCompManager().getCurrentComp().getEndDate() - System.currentTimeMillis());
            case "timeuntilvoteend":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                return CompUtils.formatTimeRemaining(plugin.getCompManager().getCurrentComp().getVoteEndDate() - System.currentTimeMillis());
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
            case "hasentered":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                return Boolean.toString(plugin.getCompManager().hasEntered(player));
            case "prize":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                try {
                    EntrantResult result = plugin.getCompManager().getBackend().getResult(
                        plugin.getCompManager().getCurrentComp(), player.getUniqueId());
                    if (result != null && result.getPrize().isPresent()) {
                        return result.getPrize().get().toHumanReadable();
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to get prize for " + player.getName(), e);
                }
                return null;
            case "prizeclaimed":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                try {
                    EntrantResult result = plugin.getCompManager().getBackend().getResult(
                        plugin.getCompManager().getCurrentComp(), player.getUniqueId());
                    if (result != null) {
                        return Boolean.toString(result.isPrizeClaimed());
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to get prize claimed status for " + player.getName(), e);
                }
                return null;
            case "iswhitelisted":
                try {
                    return Boolean.toString(plugin.getCompManager().getWhitelist().isWhitelisted(player));
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to check whitelist status for " + player.getName(), e);
                    return "false";
                }
            case "spotsremaining":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                int maxEntrants = plugin.getCompManager().getCurrentComp().getMaxEntrants();
                int currentEntrants = plugin.getBridge().getUsedPlotCount();
                return Integer.toString(Math.max(0, maxEntrants - currentEntrants));
            case "maxentrants":
                if (plugin.getCompManager().getCurrentComp() == null) return null;
                return Integer.toString(plugin.getCompManager().getCurrentComp().getMaxEntrants());
            case "entrants":
                return Integer.toString(plugin.getBridge().getUsedPlotCount());
            default:
                return null;
        }
    }
    
    /**
     * Handles indexed criteria placeholders: criteria_<index>_name or criteria_<index>_description
     * @param placeholder The placeholder string (e.g., "criteria_0_name")
     * @return The criteria name or description, or null if invalid
     */
    private String getCriteriaPlaceholder(String placeholder) {
        if (plugin.getCompManager().getCurrentComp() == null) {
            return null;
        }
        
        String[] parts = placeholder.split("_");
        if (parts.length != 3 || !parts[0].equals("criteria")) {
            return null;
        }
        
        try {
            int index = Integer.parseInt(parts[1]);
            String field = parts[2];
            
            List<BaseCriterion> criteria = plugin.getCompManager().getCurrentComp().getCriteria();
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

}
