package au.com.addstar.comp.criterions;

import au.com.addstar.comp.criterions.criteria.BlockCriteria;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;

/**
 * Created for the Ark: Survival Evolved.
 * Created by Narimm on 1/02/2017.
 */
public class BlockIncludeCriterion extends BaseCriterion {

    private BlockCriteria blockCriteria;

    @Override
    public String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append("Required Block: ");
        builder.append(blockCriteria.getMaterial().name());
        builder.append(" atleast ");
        builder.append(blockCriteria.getNumber());
        return builder.toString();
    }

    @Override
    public CriterionStanding getStanding(Player player) {
        return CriterionStanding.NotApplicable;
    }

    @Override
    public void load(String data) {
        Gson gson = new Gson();
        Type type = new TypeToken<BlockCriteria>(){}.getType();
        blockCriteria = gson.fromJson(data,type);
    }
}
