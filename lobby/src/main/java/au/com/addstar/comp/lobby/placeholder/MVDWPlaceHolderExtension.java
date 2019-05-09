package au.com.addstar.comp.lobby.placeholder;

import au.com.addstar.comp.lobby.LobbyPlugin;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;

/**
 * Created for the AddstarMC IT Project.
 * Created by narimm on 9/05/2019.
 */
public class MVDWPlaceHolderExtension implements PlaceholderReplacer {

    private final PlaceHolderHandler handler;

    public MVDWPlaceHolderExtension(LobbyPlugin plugin) {
        handler = plugin.getPlaceHolderHandler();
        for(String s:plugin.getPlaceHolderHandler().getFullPlaceHolders()) {
            PlaceholderAPI.registerPlaceholder(plugin,s,this );
        }
    }

    @Override
    public String onPlaceholderReplace(PlaceholderReplaceEvent placeholderReplaceEvent) {
        return handler.onPlaceholderRequest(placeholderReplaceEvent.getPlayer(),placeholderReplaceEvent.getPlaceholder());
    }
}
