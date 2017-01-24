package au.com.addstar.comp.gui.listeners;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.voting.Vote;
import au.com.addstar.comp.voting.VoteStorage;
import au.com.addstar.comp.voting.likedislike.LDVote;
import au.com.addstar.comp.voting.likedislike.LikeDislikeStrategy;
import com.intellectualcrafters.plot.object.Plot;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Created for the AddstarMC
 * @author Narimm
 */
public class LDVoteClickListener implements ButtonClickListener {

    private LDVote.Type type;
    private final CompManager manager;
    private final P2Bridge bridge;
    private final Messages messages;

    public LDVoteClickListener(CompPlugin plugin, LikeDislikeStrategy strategy, LDVote.Type type) {
        this.type = type;
        manager = plugin.getCompManager();
        bridge = plugin.getBridge();
        messages = plugin.messages;
    }


    @Override
    public void onClick(Player player) {
        Plot plot = bridge.getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(messages.get("vote.denied.no-plot"));
            return;
        }
        if (!plot.hasOwner()) {
            player.sendMessage(messages.get("vote.denied.no-owner"));
            return;
        }

        try {
            if (plot.getOwners().isEmpty()) {
                player.sendMessage(messages.get("vote.denied.no-owner"));
                return;
            }
        } catch (Exception e) {
            player.sendMessage("Debug: plot.getOwners() access exception: " + e.getMessage());

            UUID ownerDeprecated = plot.owner;

            if (ownerDeprecated == null) {
                player.sendMessage("Debug: ownerDeprecated == null");
                player.sendMessage(messages.get("vote.denied.no-owner"));
                return;
            }
        }
        VoteStorage<Vote> storage = (VoteStorage<Vote>)manager.getVoteStorage();
        Vote vote;
        try {
            UUID owner = plot.getOwners().iterator().next();
            vote = new LDVote(plot.getId(),owner,type);
        } catch (IllegalArgumentException e) {
            player.sendMessage(e.getMessage());
            return;
        }
        try {
            storage.recordVote(player, vote);
            player.sendMessage(messages.get("vote.done"));
        } catch (IllegalArgumentException e) {
            player.sendMessage(messages.get("vote.denied.revote"));
        }

    }
}
