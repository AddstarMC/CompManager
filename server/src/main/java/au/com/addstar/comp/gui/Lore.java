package au.com.addstar.comp.gui;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 23/01/2017.
 */
public class Lore
{
    private String[] lore;

    public static Lore fromString(String string)
    {
        String[] lore = string.split(",");

        return new Lore(lore);
    }

    public Lore(String... lore)
    {
        this.lore = lore;
    }

    public ArrayList<String> toArray()
    {
        String[] arrayOfString;
        int j = (arrayOfString = this.lore).length;
        ArrayList<String> loreList = new ArrayList<>(Arrays.asList(arrayOfString).subList(0, j));
        return loreList;
    }

    public String toString()
    {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < this.lore.length; i++) {
            result.append(this.lore[i]).append(i == this.lore.length - 1 ? "" : ",");
        }
        return result.toString();
    }
}
