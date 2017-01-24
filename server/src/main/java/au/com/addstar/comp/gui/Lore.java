package au.com.addstar.comp.gui;

import java.util.ArrayList;

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
        ArrayList<String> loreList = new ArrayList();
        String[] arrayOfString;
        int j = (arrayOfString = this.lore).length;
        for (int i = 0; i < j; i++)
        {
            String loreLine = arrayOfString[i];
            loreList.add(loreLine);
        }
        return loreList;
    }

    public String toString()
    {
        String result = "";
        for (int i = 0; i < this.lore.length; i++) {
            result = result + this.lore[i] + (i == this.lore.length - 1 ? "" : ",");
        }
        return result;
    }
}
