package au.com.addstar.comp.gui;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 23/01/2017.
 */
public class Icon {

    final ItemStack itemStack;

    public Icon(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public ItemStack getItemStack()
    {
        return getItemStack(null, null);
    }

    public ItemStack getItemStack(String title, Lore lore) {
        ItemMeta newItemMeta = this.itemStack.getItemMeta();
        if(title !=null)newItemMeta.setDisplayName(title);
        if(lore !=null)newItemMeta.setLore(lore.toArray());
        this.itemStack.setItemMeta(newItemMeta);
        return itemStack;
    }



}
