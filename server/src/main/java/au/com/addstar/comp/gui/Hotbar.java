package au.com.addstar.comp.gui;

import au.com.addstar.comp.CompPlugin;
import org.bukkit.entity.Player;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 23/01/2017.
 */
public class Hotbar {

    private final Player player;
    private HotbarComponent[] components = new HotbarComponent[9];

    public Hotbar(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void showHotbar() {
        render();
    }


    public void add(HotbarComponent component) {
        if ((component.getSlot() < 0) || (component.getSlot() > 8)) {
            return;
        }
        this.components[component.getSlot()] = component;
        component.initialize(this);
    }

    public void render() {
        int j = components.length;
        for (HotbarComponent component : components) {
            if (component != null) {
                component.render();
            }
        }
    }

    public void close() {
        CompPlugin.removeHotbar(this.player);
    }

    public void onClick(int slot) {
        if (this.components[slot] == null) {
            return;
        }
        if (slot < components.length && slot >= 0) {
            this.components[slot].onClick(this.player);
        }

    }

    public void onSelect(int slot) {
        if (this.components[slot] == null) {
            return;
        }
        if (slot < components.length && slot >= 0) {
            this.components[slot].onSelect(this.player);
        }
    }


}
