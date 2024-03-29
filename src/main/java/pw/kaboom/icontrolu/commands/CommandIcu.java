package pw.kaboom.icontrolu.commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import pw.kaboom.icontrolu.Main;
import pw.kaboom.icontrolu.PlayerControl;

public final class CommandIcu implements CommandExecutor {
    private void controlCommand(final Player controller, final String label, final String[] args) {
        if (args.length == 1) {
            controller.sendMessage(Component
                .text("Usage: /" + label + " control <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null && args[1].matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")) {
            target = Bukkit.getPlayer(UUID.fromString(args[1]));
        }

        if (target == null) {
            controller.sendMessage(
                Component.text("Player \"")
                    .append(Component.text(args[1]))
                    .append(Component.text("\" not found"))
            );
            return;
        }

        if (target == controller) {
            controller.sendMessage(Component.text("You are already controlling yourself"));
            return;
        }

        if (PlayerControl.getTarget(controller.getUniqueId()) != null) {
            controller.sendMessage(
                Component.text("You are already controlling \"")
                    .append(Component.text(target.getName()))
                    .append(Component.text("\""))
                );
            return;
        }

        if (PlayerControl.getController(target.getUniqueId()) != null) {
            controller.sendMessage(
                Component.text("Player \"")
                    .append(Component.text(target.getName()))
                    .append(Component.text("\" is already being controlled"))
            );
            return;
        }

        if (PlayerControl.getTarget(target.getUniqueId()) != null) {
            controller.sendMessage(
                Component.text("Player \"")
                    .append(Component.text(target.getName()))
                    .append(Component.text("\" is already controlling another player"))
            );
            return;
        }

        if (!controller.canSee(target)) {
            controller.sendMessage(Component.text("You may not control this player"));
            return;
        }

        controller.teleportAsync(target.getLocation());
        controller.getInventory().setContents(target.getInventory().getContents());

        PlayerControl.setTarget(controller.getUniqueId(), target);
        PlayerControl.setController(target.getUniqueId(), controller);

        controller.sendMessage(
            Component.text("You are now controlling \"")
                .append(Component.text(target.getName()))
                .append(Component.text("\""))
        );
    }

    private void stopCommand(final Player controller) {
        final Player target = PlayerControl.getTarget(controller.getUniqueId());

        if (target == null) {
            controller.sendMessage(Component.text("You are not controlling anyone at the moment"));
            return;
        }

        PlayerControl.removeTarget(controller.getUniqueId());
        PlayerControl.removeController(target.getUniqueId());
        PlayerControl.scheduleVisibility(controller.getUniqueId());

        controller.sendMessage(
            Component.text("You are no longer controlling \"")
                .append(Component.text(target.getName()))
                .append(Component.text("\". You are invisible for "))
                .append(Component.text(PlayerControl.getVisibilityDelay()))
                .append(Component.text(" seconds."))
        );
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label,
                             final String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(Component.text("Command has to be run by a player"));
            return true;
        }

        final Player controller = (Player) sender;

        if (args.length == 0) {
            controller.sendMessage(Component
                .text("Usage: /" + label + " <control|stop>", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("control")) {
            controlCommand(controller, label, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            stopCommand(controller);
        }
        return true;
    }
}
