package au.com.addstar.comp.gui;

import au.com.addstar.comp.gui.listeners.ButtonClickListener;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 23/01/2017.
 */
public class HotbarButton extends HotbarComponent {

    private ArrayList<ButtonClickListener> listeners = new ArrayList<>();
    private Icon icon;
    private String title = "Button";
    private Lore lore = new Lore(new String[] { "" });

    public HotbarButton(int slot, String title)
    {
        super(slot);
        this.title = title;
        setIcon(new Icon(new ItemStack(Material.WOOL)));
    }

    public HotbarButton(int slot, String title, DyeColor color)
    {
        super(slot);
        this.title = title;
        setIcon(new Icon(new ItemStack(Material.WOOL,1,color.getDyeData())));
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Lore getLore() {
        return lore;
    }

    public void setLore(Lore lore) {
        this.lore = lore;
    }

    @Override
    public void onClick(Player player)
    {
        for (ButtonClickListener listener : this.listeners) {
            listener.onClick(player);
        }
    }

    @Override
    public void onSelect(Player player) {
        player.sendMessage("This button :" + getTitle());
        player.sendMessage("Action:" + getLore().toString());
    }

    public void addClickListener(ButtonClickListener listener)
    {
        this.listeners.add(listener);
    }
    @Override
    public void render() {
        renderItem(this.slot, this.icon, this.title, this.lore);
    }
}
