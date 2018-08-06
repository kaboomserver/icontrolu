package pw.kaboom.icontrolu;

import org.bukkit.Bukkit;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageEvent;

import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import org.bukkit.scheduler.BukkitRunnable;

class Tick extends BukkitRunnable {
	Main main;
	Tick(Main main) {
		this.main = main;
	}

	public void run() {
		for (Player target: Bukkit.getOnlinePlayers()) {
			Player controller = main.controllerFor.get(target.getUniqueId());
			if (controller != null) {
				for (int i = 0; i < 40; ++i) {
					if (controller.getInventory().getItem(i) != target.getInventory().getItem(i)) {
						target.getInventory().setItem(i, controller.getInventory().getItem(i));
					}
				}

				if (target.getHealth() > 0) {
					target.teleport(controller);
				}

				target.setExhaustion(controller.getExhaustion());
				target.setFlying(controller.isFlying());
				target.setFoodLevel(controller.getFoodLevel());
				target.setHealth(controller.getHealth());
				target.setLevel(controller.getLevel());
				target.setSneaking(controller.isSneaking());
				target.setSprinting(controller.isSprinting());

				for (Player player: Bukkit.getOnlinePlayers()) {
					player.hidePlayer(controller);
				}
				
				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				Team team = scoreboard.getTeam("iControlU_List");
				if (team == null) {
					team = scoreboard.registerNewTeam("iControlU_List");
				}

				team.setCanSeeFriendlyInvisibles​(false);
				team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

				if (team.hasPlayer(controller) == false) {
					team.addPlayer(controller);
				}

				controller.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 0));
			}
		}
	}
}

class Events implements Listener {
	Main main;
	Events(Main main) {
		this.main = main;
	}

	@EventHandler
	void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();

		if (main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}

		if (main.targetFor.containsKey(player.getUniqueId())) {
			Player target = main.targetFor.get(player.getUniqueId());
			target.chat(event.getMessage());
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onEntityDamage(EntityDamageEvent event) {
		Entity player = event.getEntity();

		if (main.targetFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	/*@EventHandler
	void onPlayerAnimation(PlayerAnimationEvent event) {
		Player controller = event.getPlayer();
		Player target = main.targetFor.get(controller.getUniqueId());

		if (target != null) {
			event.setCancelled(true);
		}
	}*/

	@EventHandler
	void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();

		if (main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();

		if (main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		if (main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		for (Player otherPlayer: Bukkit.getOnlinePlayers()) {
			/* Target disconnects */
			if (main.controllerFor.get(player.getUniqueId()).equals(otherPlayer)) {
				main.targetFor.remove(otherPlayer.getUniqueId());
				main.controllerFor.remove(player.getUniqueId());

				final Player controller = otherPlayer;

				Bukkit.getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
					public void run() {
						for (Player allPlayers: Bukkit.getOnlinePlayers()) {
							allPlayers.showPlayer(controller);
						}

						Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
						Team team = scoreboard.getTeam("iControlU_List");
						if (team != null && team.hasPlayer(controller) == true) {
							team.removePlayer(controller);
						}

						controller.removePotionEffect(PotionEffectType.INVISIBILITY);
						controller.sendMessage("You are now visible");
					}
				}, 200L);

				otherPlayer.sendMessage("The player you were controlling has disconnected. You are invisible for 10 seconds.");
			}

			/* Controller disconnects */
			if (main.targetFor.get(player.getUniqueId()).equals(otherPlayer)) {
				main.targetFor.remove(player.getUniqueId());
				main.controllerFor.remove(otherPlayer.getUniqueId());
			}
		}
	}

	@EventHandler
	void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		if (main.controllerFor.containsKey(player.getUniqueId())) {
			Player controller = main.controllerFor.get(player.getUniqueId());
			controller.teleport(player);
		}
	}
}
