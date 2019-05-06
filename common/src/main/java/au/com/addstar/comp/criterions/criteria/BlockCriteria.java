package au.com.addstar.comp.criterions.criteria;

import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Created for the Ark: Survival Evolved.
 * Created by Narimm on 1/02/2017.
 */
public class BlockCriteria{
private int number;
private Material material;

public BlockCriteria(int number, Block block) {
        this.number = number;
        this.material = material;
        }


public int getNumber() {
        return number;
        }

public Material getMaterial() {
        return material;
        }

public void setMaterial(Material material) {
        this.material = material;
        }
        }
