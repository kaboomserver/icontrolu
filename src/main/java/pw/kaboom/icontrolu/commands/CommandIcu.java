package pw.kaboom.icontrolu.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import pw.kaboom.icontrolu.Main;
import pw.kaboom.icontrolu.utilities.PlayerList;

public final class CommandIcu implements CommandExecutor {
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			sender.sendMessage("Command has to be run by a player");
		} else {
			final Player controller = (Player) sender;

			if (args.length == 0) {
				controller.sendMessage(ChatColor.RED + "Usage: /" + label + " <control|stop>");
			} else if (args[0].equalsIgnoreCase("control")) {
				if (args.length == 1) {
					controller.sendMessage(ChatColor.RED + "Usage: /" + label + " control <player>");
				} else {
					final Player target = Bukkit.getPlayer(args[1]);

					if (target != null) {
						if (target == controller) {
							controller.sendMessage("You are already controlling yourself");
						} else if (PlayerList.getTarget(controller.getUniqueId()) != null) {
							controller.sendMessage("You are already controlling \"" + target.getName() + "\"");
						} else if (PlayerList.getController(target.getUniqueId()) != null) {
							controller.sendMessage("Player \"" + target.getName() + "\" is already being controlled");
						} else if (PlayerList.getTarget(target.getUniqueId()) != null) {
							controller.sendMessage("Player \"" + target.getName() + "\" is already controlling another player");
						} else if (!controller.canSee(target)) {
							controller.sendMessage("You may not control this player");
						} else {
							controller.teleportAsync(target.getLocation());

							controller.getInventory().setContents(target.getInventory().getContents());

							PlayerList.setTarget(controller.getUniqueId(), target);
							PlayerList.setController(target.getUniqueId(), controller);

							controller.sendMessage("You are now controlling \"" + target.getName() + "\"");
						}
					} else {
						controller.sendMessage("Player \"" + args[1] + "\" not found");
					}
				}
			} else if (args[0].equalsIgnoreCase("stop")) {
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
							Team team = scoreboard.getTeam("icuDisableCollision");
							if (team != null && team.hasEntry(controller.getName())) {
								team.removeEntry(controller.getName());
							}

							controller.removePotionEffect(PotionEffectType.INVISIBILITY);
							controller.sendMessage("You are now visible");
						}
					}.runTaskLater(JavaPlugin.getPlugin(Main.class), tickDelay);

					controller.sendMessage("You are no longer controlling \"" + target.getName() + "\". You are invisible for 10 seconds.");
				} else {
					controller.sendMessage("You are not controlling anyone at the moment");
				}
			}
		}
		return true;
	}
}
