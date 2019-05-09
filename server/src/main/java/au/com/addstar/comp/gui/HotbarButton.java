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

    private final ArrayList<ButtonClickListener> listeners = new ArrayList<>();
    private Icon icon;
    private String title = "Button";
    private Lore lore = new Lore("");

    public HotbarButton(int slot, String title)
    {
        super(slot);
        this.title = title;
        setIcon(new Icon(new ItemStack(Material.WHITE_WOOL)));
    }

    public HotbarButton(int slot, String title, DyeColor color)
    {
        super(slot);
        this.title = title;

        setIcon(new Icon(getColor(color,Material.WHITE_STAINED_GLASS_PANE)));
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

    private ItemStack getColor(DyeColor color, Material base){
        Material mat;
        switch(base){
            case WHITE_WOOL:
                switch (color) {
                    case WHITE:
                        mat = Material.WHITE_WOOL;
                        break;
                    case GRAY:
                        mat = Material.GRAY_WOOL;
                        break;

                    case YELLOW:
                        mat = Material.YELLOW_WOOL;
                        break;

                    case MAGENTA:
                        mat = Material.MAGENTA_WOOL;
                        break;

                    case BLUE:
                        mat = Material.BLUE_WOOL;
                        break;

                    case BLACK:
                        mat = Material.BLACK_WOOL;
                        break;

                    case RED:
                        mat = Material.RED_WOOL;
                        break;

                    case CYAN:
                        mat = Material.CYAN_WOOL;
                        break;

                    case LIME:
                        mat = Material.LIME_WOOL;
                        break;

                    case PINK:
                        mat = Material.PINK_WOOL;
                        break;

                    case BROWN:
                        mat = Material.BROWN_WOOL;
                        break;

                    case GREEN:
                        mat = Material.GREEN_WOOL;
                        break;

                    case ORANGE:
                        mat = Material.ORANGE_WOOL;
                        break;

                    case PURPLE:
                        mat = Material.PURPLE_WOOL;
                        break;

                    case LIGHT_BLUE:
                        mat = Material.LIGHT_BLUE_WOOL;
                        break;

                    case LIGHT_GRAY:
                        mat = Material.LIGHT_GRAY_WOOL;
                        break;
                    default:
                        mat = Material.WHITE_WOOL;
                        break;


                    }
                break;

            case WHITE_STAINED_GLASS_PANE:
                switch (color) {
                    case WHITE:
                        mat = Material.WHITE_STAINED_GLASS_PANE;
                        break;
                    case GRAY:
                        mat = Material.GRAY_STAINED_GLASS_PANE;
                        break;
                    case YELLOW:
                        mat = Material.YELLOW_STAINED_GLASS_PANE;
                        break;
                    case MAGENTA:
                        mat = Material.MAGENTA_STAINED_GLASS_PANE;
                        break;
                    case BLUE:
                        mat = Material.BLUE_STAINED_GLASS_PANE;
                        break;
                    case BLACK:
                        mat = Material.BLACK_STAINED_GLASS_PANE;
                        break;
                    case RED:
                        mat = Material.RED_STAINED_GLASS_PANE;
                        break;
                    case CYAN:
                        mat = Material.CYAN_STAINED_GLASS_PANE;
                        break;
                    case LIME:
                        mat = Material.LIME_STAINED_GLASS_PANE;
                        break;
                    case PINK:
                        mat = Material.PINK_STAINED_GLASS_PANE;
                        break;
                    case BROWN:
                        mat = Material.BROWN_STAINED_GLASS_PANE;
                        break;
                    case GREEN:
                        mat = Material.GREEN_STAINED_GLASS_PANE;
                        break;
                    case ORANGE:
                        mat = Material.ORANGE_STAINED_GLASS_PANE;
                        break;
                    case PURPLE:
                        mat = Material.PURPLE_STAINED_GLASS_PANE;
                        break;
                    case LIGHT_BLUE:
                        mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                        break;
                    case LIGHT_GRAY:
                        mat = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                        break;
                    default:
                        mat = Material.WHITE_STAINED_GLASS_PANE;
                        break;
                }
                break;
                default:
                    mat = Material.WHITE_WOOL;
                    break;
        }
        return new ItemStack(mat,1);
    }
}
