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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import pw.kaboom.icontrolu.Main;
import pw.kaboom.icontrolu.utilities.PlayerList;

public final class CommandIcu implements CommandExecutor {
    private void controlCommand(final Player controller, final String label, final String[] args) {
        if (args.length == 1) {
            controller.sendMessage(Component
                .text("Usage: /" + label + " control <player>", NamedTextColor.RED));
        } else {
            Player target = Bukkit.getPlayer(args[1]);

            if (target == null && args[1].matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")) {
                target = Bukkit.getPlayer(UUID.fromString(args[1]));
            }

            if (target != null) {
                if (target == controller) {
                    controller.sendMessage(Component.text("You are already controlling yourself"));
                } else if (PlayerList.getTarget(controller.getUniqueId()) != null) {
                    controller.sendMessage(
                        Component.text("You are already controlling \"")
                            .append(Component.text(target.getName()))
                            .append(Component.text("\""))
                        );
                } else if (PlayerList.getController(target.getUniqueId()) != null) {
                    controller.sendMessage(
                        Component.text("Player \"")
                            .append(Component.text(target.getName()))
                            .append(Component.text("\" is already being controlled"))
                    );
                } else if (PlayerList.getTarget(target.getUniqueId()) != null) {
                    controller.sendMessage(
                        Component.text("Player \"")
                            .append(Component.text(target.getName()))
                            .append(Component.text("\" is already controlling another player"))
                    );
                } else if (!controller.canSee(target)) {
                    controller.sendMessage(Component.text("You may not control this player"));
                } else {
                    controller.teleportAsync(target.getLocation());

                    controller.getInventory().setContents(target.getInventory().getContents());

                    PlayerList.setTarget(controller.getUniqueId(), target);
                    PlayerList.setController(target.getUniqueId(), controller);

                    controller.sendMessage(
                        Component.text("You are now controlling \"")
                            .append(Component.text(target.getName()))
                            .append(Component.text("\""))
                    );
                }
            } else {
                controller.sendMessage(
                    Component.text("Player \"")
                        .append(Component.text(args[1]))
                        .append(Component.text("\" not found"))
                );
            }
        }
    }

    private void stopCommand(final Player controller) {
        final Player target = PlayerList.getTarget(controller.getUniqueId());

        if (target != null) {
            PlayerList.removeTarget(controller.getUniqueId());
            PlayerList.removeController(target.getUniqueId());

            final int tickDelay = 200;

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player: Bukkit.getOnlinePlayers()) {
                        player.showPlayer(JavaPlugin.getPlugin(Main.class), controller);
                    }

                    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Team team = scoreboard.getTeam("icuCollision");
                    if (team != null && team.hasEntry(controller.getName())) {
                        team.removeEntry(controller.getName());
                    }

                    controller.removePotionEffect(PotionEffectType.INVISIBILITY);
                    controller.sendMessage(Component.text("You are now visible"));
                }
            }.runTaskLater(JavaPlugin.getPlugin(Main.class), tickDelay);

            final int seconds = tickDelay / 20;

            controller.sendMessage(
                Component.text("You are no longer controlling \"")
                    .append(Component.text(target.getName()))
                    .append(Component.text("\". You are invisible for "))
                    .append(Component.text(seconds))
                    .append(Component.text(" seconds."))
            );
        } else {
            controller.sendMessage(Component.text("You are not controlling anyone at the moment"));
        }
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
        } else if (args[0].equalsIgnoreCase("control")) {
            controlCommand(controller, label, args);
        } else if (args[0].equalsIgnoreCase("stop")) {
            stopCommand(controller);
        }
        return true;
    }
}
