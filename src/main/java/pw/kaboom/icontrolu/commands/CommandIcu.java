package pw.kaboom.icontrolu.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import pw.kaboom.icontrolu.modules.ControlManager;
import pw.kaboom.icontrolu.modules.PlayerControl;

import static io.papermc.paper.command.brigadier.Commands.*;
import static io.papermc.paper.command.brigadier.argument.ArgumentTypes.player;

public final class CommandIcu {
    private static final SimpleCommandExceptionType EX_NOT_CONTROLLING =
            new SimpleCommandExceptionType(
                    new LiteralMessage("You are not controlling anyone at the moment")
            );
    private static final SimpleCommandExceptionType EX_TARGET_SELF =
            new SimpleCommandExceptionType(
                    new LiteralMessage("You are already controlling yourself")
            );
    private static final DynamicCommandExceptionType EX_ALREADY_IN_CONTROL =
            new DynamicCommandExceptionType(player ->
                    new LiteralMessage(
                            "You are already controlling \"" +
                                    getPlayerName(player) +
                                    "\""
                    )
            );
    private static final DynamicCommandExceptionType EX_CONTROL_BY_OTHER =
            new DynamicCommandExceptionType(player ->
                    new LiteralMessage(
                            "Player \"" +
                                    getPlayerName(player) +
                                    "\" is already being controlled"
                    )
            );
    private static final SimpleCommandExceptionType EX_CANTSEE =
            new SimpleCommandExceptionType(
                    new LiteralMessage("You may not control this player")
            );
    private final PlayerControl controlModule;

    public CommandIcu(final PlayerControl controlModule) {
        this.controlModule = controlModule;
    }

    public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder
                .requires(restricted(src ->
                        src.getSender().hasPermission("icu.command")
                                && src.getSender() instanceof Player
                ))
                .then(literal("stop")
                        .executes(ctx -> {
                            final Player controller = getPlayer(ctx);
                            final Player target = controlModule.manager.removeController(
                                    controller.getUniqueId()
                            );

                            if (target == null) {
                                throw EX_NOT_CONTROLLING.create();
                            }
                            controlModule.scheduleVisibility(controller.getUniqueId());
                            controller.sendMessage(
                                    Component.text("You are no longer controlling \"")
                                            .append(Component.text(target.getName()))
                                            .append(Component.text("\". You are invisible for "))
                                            .append(Component.text(
                                                    PlayerControl.getVisibilityDelay()
                                            ))
                                            .append(Component.text(" seconds."))
                            );
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(literal("control")
                        .then(argument("player", player())
                                .executes(ctx -> {
                                    final PlayerSelectorArgumentResolver resolver = ctx.getArgument(
                                            "player",
                                            PlayerSelectorArgumentResolver.class
                                    );
                                    final Player target =
                                            resolver.resolve(ctx.getSource()).getFirst();
                                    final Player controller = getPlayer(ctx);

                                    if (target == controller) {
                                        throw EX_TARGET_SELF.create();
                                    }

                                    final ControlManager manager = controlModule.manager;
                                    final Player otherTarget =
                                            manager.getTarget(controller.getUniqueId());
                                    if (otherTarget != null) {
                                        throw EX_ALREADY_IN_CONTROL.create(otherTarget);
                                    }

                                    if (manager.isTarget(target.getUniqueId())) {
                                        throw EX_CONTROL_BY_OTHER.create(target);
                                    }

                                    if (!controller.canSee(target)) {
                                        throw EX_CANTSEE.create();
                                    }

                                    controller.teleportAsync(target.getLocation());
                                    controller.getInventory().setContents(
                                            target.getInventory().getContents()
                                    );
                                    manager.control(controller, target);
                                    controller.sendMessage(
                                            Component.text("You are now controlling \"")
                                                    .append(Component.text(target.getName()))
                                                    .append(Component.text("\""))
                                    );

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private static Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        return (Player) ctx.getSource().getSender();
    }

    private static String getPlayerName(Object player) {
        if (player instanceof Player player1) {
            return player1.getName();
        }
        throw new IllegalArgumentException("player must be an object");
    }
}
