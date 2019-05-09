package au.com.addstar.comp.lobby.placeholder;

import au.com.addstar.comp.lobby.LobbyPlugin;

import org.bukkit.entity.Player;

import java.util.List;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * Created for the AddstarMC IT Project.
 * Created by Narimm on 7/05/2019.
 */
public class PAPIExtension extends PlaceholderExpansion {

    public PAPIExtension(LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    private final LobbyPlugin plugin;

    @Override
    public String getIdentifier() {
        return PlaceHolderHandler.getIdentifier();
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
        return plugin.getPlaceHolderHandler().getPlaceholders();
    }
    @Override
    public String onPlaceholderRequest(Player player, String s) {
        return plugin.getPlaceHolderHandler().onPlaceholderRequest(player,s);
    }
}
