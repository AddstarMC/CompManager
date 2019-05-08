package au.com.addstar.comp.placeholders;

import au.com.addstar.comp.CompPlugin;

import org.bukkit.entity.Player;

import java.util.List;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * Created for the AddstarMC IT Project.
 * Created by Narimm on 7/05/2019.
 */
public class PAPIPlaceHolderExtension extends PlaceholderExpansion {

    public PAPIPlaceHolderExtension(CompPlugin plugin) {
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
        return PlaceHolderHandler.getPlaceholders();
    }
    @Override
    public String onPlaceholderRequest(Player player, String s) {
        return this.plugin.getPlaceHolderHandler().getPlacHolderReplaceMent(player,s);
    }
}
