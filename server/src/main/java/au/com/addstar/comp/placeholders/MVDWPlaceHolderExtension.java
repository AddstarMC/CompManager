package au.com.addstar.comp.placeholders;

import au.com.addstar.comp.CompPlugin;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 9/05/2019.
 */
public class MVDWPlaceHolderExtension implements PlaceholderReplacer {
    private PlaceHolderHandler handler;

    public MVDWPlaceHolderExtension(CompPlugin plugin) {
        handler = plugin.getPlaceHolderHandler();
        for(String s:PlaceHolderHandler.getPlaceholders()){
            PlaceholderAPI.registerPlaceholder(plugin,s,this);
        }
    }

    @Override
    public String onPlaceholderReplace(PlaceholderReplaceEvent placeholderReplaceEvent) {
        return handler.getPlacHolderReplaceMent(placeholderReplaceEvent.getPlayer(),placeholderReplaceEvent.getPlaceholder());
    }
}
