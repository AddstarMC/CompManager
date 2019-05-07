package au.com.addstar.comp.criterions;

import au.com.addstar.comp.criterions.criteria.BlockCriteria;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.bukkit.entity.Player;

import java.lang.reflect.Type;

/**
 * Created for the AddstarMC Server
 * Created by Narimm on 1/02/2017.
 */
public class BlockIncludeCriterion extends BaseCriterion {

    private BlockCriteria blockCriteria;

    @Override
    public String describe() {
        String builder = "Required Block: " +
                blockCriteria.getMaterial().name() +
                " atleast " +
                blockCriteria.getNumber();
        return builder;
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
