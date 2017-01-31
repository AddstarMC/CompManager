package au.com.addstar.comp.gui;

import org.bukkit.entity.Player;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 23/01/2017.
 */
public abstract class HotbarComponent {

    Hotbar hotbar;
    int slot;

    public HotbarComponent(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        if (slot < 0) {
            slot = 0;
        }
        this.slot = slot;
    }

    public Hotbar getHotbar() {
        return hotbar;
    }

    public void initialize(Hotbar hotbar) {
        this.hotbar = hotbar;
    }

    public void renderItem(int slot, Icon item, String title, Lore lore)
    {
        this.hotbar.getPlayer().getInventory().setItem(slot, item.getItemStack(title, lore));
    }

    public abstract void onClick(Player paramPlayer);

    public abstract void onSelect(Player paramPlayer);

    public abstract void render();
}

