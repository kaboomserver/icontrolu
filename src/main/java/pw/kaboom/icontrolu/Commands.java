package pw.kaboom.icontrolu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import org.bukkit.entity.Player;

import org.bukkit.potion.PotionEffectType;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

class CommandIcu implements CommandExecutor {
	Main main;
	CommandIcu(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			sender.sendMessage("Command has to be run by a player");
		} else {
			Player controller = (Player) sender;

			if (args.length == 0) {
				controller.sendMessage(ChatColor.RED + "Usage: /" + label + " <control|stop>");
			} else if (args[0].equalsIgnoreCase("control")) {
				if (args.length == 1) {
					controller.sendMessage(ChatColor.RED + "Usage: /" + label + " control <player>");
				} else {
					Player target = Bukkit.getPlayer(args[1]);
					if (target != null) {
						if (target == controller) {
							controller.sendMessage("You are already controlling yourself");
						} else if (main.targetFor.containsKey(controller.getUniqueId())) {
							controller.sendMessage("You are already controlling \"" + target.getName() + "\"");
						} else if (main.controllerFor.containsKey(target.getUniqueId())) {
							controller.sendMessage("Player \"" + target.getName() + "\" is already being controlled");
						} else if (main.targetFor.containsKey(target.getUniqueId())) {
							controller.sendMessage("Player \"" + target.getName() + "\" is already controlling another player");
						} else if (controller.canSee(target) == false) {
							controller.sendMessage("You may not control this player");
						} else {
							controller.teleport(target);
	
							controller.getInventory().setContents(target.getInventory().getContents());
	
							main.targetFor.put(controller.getUniqueId(), target);
							main.controllerFor.put(target.getUniqueId(), controller);
	
							controller.sendMessage("You are now controlling \"" + target.getName() + "\"");
						}
					} else {
						controller.sendMessage("Player \"" + args[1] + "\" not found");
					}
				}
			} else if (args[0].equalsIgnoreCase("stop")) {
				Player target = main.targetFor.get(controller.getUniqueId());

				if (target != null) {
					main.targetFor.remove(controller.getUniqueId());
					main.controllerFor.remove(target.getUniqueId());
	
					final Player controllerRun = controller;
	
					Bukkit.getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
						public void run() {
							for (Player player: Bukkit.getOnlinePlayers()) {
								player.showPlayer(main, controllerRun);
							}
	
							Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
							Team team = scoreboard.getTeam("iControlU_List");
							if (team != null && team.hasEntry(controllerRun.getName()) == true) {
								team.removeEntry(controllerRun.getName());
							}
	
							controllerRun.removePotionEffect(PotionEffectType.INVISIBILITY);
							controllerRun.sendMessage("You are now visible");
						}
					}, 200L);
	
					controller.sendMessage("You are no longer controlling \"" + target.getName() + "\". You are invisible for 10 seconds.");
				} else {
					controller.sendMessage("You are not controlling anyone at the moment");
				}
			}
		}
		return true;
	}
}
