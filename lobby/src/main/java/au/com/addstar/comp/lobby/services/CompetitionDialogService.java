package au.com.addstar.comp.lobby.services;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.event.ClickCallback;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.util.CompUtils;

/**
 * Service for creating competition dialogs using PaperMC's Dialog API.
 */
public class CompetitionDialogService {

    public CompetitionDialogService() {
    }

    /**
     * Creates a dialog for a competition with Join and View options.
     *
     * @param comp        The competition to display
     * @param server      The competition server
     * @param joinService The join service to use for join callbacks
     * @param viewService The view service to use for view callbacks
     * @param player      The player who will see the dialog
     * @return The created dialog
     */
    public Dialog createCompetitionDialog(Competition comp, CompServer server,
                                          CompetitionJoinService joinService,
                                          CompetitionViewService viewService,
                                          Player player) {
        // Build dialog body with competition information
        List<DialogBody> bodyParts = new ArrayList<>();

        // End date and time remaining
        long timeRemaining = comp.getEndDate() - System.currentTimeMillis();
        bodyParts.add(DialogBody.plainMessage(Component.empty()));
        bodyParts.add(DialogBody.plainMessage(
                Component.text("Ends: ", NamedTextColor.GRAY)
                        .append(Component.text(CompUtils.formatDate(comp.getEndDate()), NamedTextColor.YELLOW))
                        .append(Component.text("\nTime remaining: ", NamedTextColor.GRAY))
                        .append(Component.text(CompUtils.formatTimeRemaining(timeRemaining), NamedTextColor.YELLOW))
        ));

        // Prizes
        if (comp.getFirstPrize() != null) {
            String firstPrize = comp.getFirstPrize().toHumanReadable();
            String secondPrize = comp.getSecondPrize() != null ? comp.getSecondPrize().toHumanReadable() : "none";

            bodyParts.add(DialogBody.plainMessage(
                    Component.text("Prizes:", NamedTextColor.WHITE, TextDecoration.BOLD)
                            .append(Component.text("First: ", NamedTextColor.GRAY))
                            .append(Component.text(firstPrize, NamedTextColor.GOLD))
                            .append(Component.text("\nSecond: ", NamedTextColor.GRAY))
                            .append(Component.text(secondPrize, NamedTextColor.GOLD))

            ));
        }

        // Criteria
        if (!comp.getCriteria().isEmpty()) {
            bodyParts.add(DialogBody.plainMessage(Component.empty()));
            bodyParts.add(DialogBody.plainMessage(
                    Component.text("Criteria:", NamedTextColor.WHITE, TextDecoration.BOLD)
            ));
            for (BaseCriterion criterion : comp.getCriteria()) {
                bodyParts.add(DialogBody.plainMessage(Component.text(criterion.getName(), NamedTextColor.YELLOW)
                        .append(Component.text(("\n" + criterion.getDescription()), NamedTextColor.WHITE))
                ));
            }
        }

        // Explanatory text
        bodyParts.add(DialogBody.plainMessage(
                Component.text("Join: ", NamedTextColor.GREEN)
                        .append(Component.text("Participate and get a plot", NamedTextColor.WHITE))
                        .append(Component.text("\nView: ", NamedTextColor.AQUA))
                        .append(Component.text("Explore without joining", NamedTextColor.WHITE))
        ));

        // Create callbacks for buttons
        DialogAction joinAction = DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player targetPlayer) {
                        // Trigger join process (async, errors handled by service)
                        joinService.initiateJoin(targetPlayer, server);
                    }
                },
                ClickCallback.Options.builder()
                        .uses(1)
                        .build()
        );

        DialogAction viewAction = DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player targetPlayer) {
                        // Trigger view process
                        boolean success = viewService.viewCompetition(targetPlayer, server);
                        if (!success) {
                            // Error message already sent by viewService
                            // Could add additional feedback here if needed
                        }
                    }
                },
                ClickCallback.Options.builder()
                        .uses(1)
                        .build()
        );

        // Create buttons
        ActionButton joinButton = ActionButton.create(
                Component.text("Join", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text("Join this competition and get a plot", NamedTextColor.WHITE),
                100,
                joinAction
        );

        ActionButton viewButton = ActionButton.create(
                Component.text("View", NamedTextColor.AQUA, TextDecoration.BOLD),
                Component.text("View this competition without joining", NamedTextColor.WHITE),
                100,
                viewAction
        );

        // Cancel button with null action - closes dialog without doing anything
        ActionButton cancelButton = ActionButton.create(
                Component.text("Cancel", NamedTextColor.WHITE),
                Component.text("Close this dialog", NamedTextColor.WHITE),
                100,
                null  // null action just closes the dialog
        );

        // Build and return dialog
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(comp.getTheme(), NamedTextColor.GOLD, TextDecoration.BOLD))
                        .body(bodyParts)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(List.of(joinButton, viewButton, cancelButton)).build())
        );
    }
}
