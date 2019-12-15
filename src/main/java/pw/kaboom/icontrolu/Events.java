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

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import org.bukkit.scheduler.BukkitRunnable;

class Tick extends BukkitRunnable {
	public void run() {
		for (Player target: Bukkit.getOnlinePlayers()) {
			final Player controller = Main.controllerFor.get(target.getUniqueId());

			if (controller != null) {
				for (int i = 0; i < controller.getInventory().getSize(); i++) {
					if (controller.getInventory().getItem(i) != null) {
						if (!controller.getInventory().getItem(i).equals(target.getInventory().getItem(i))) {
							target.getInventory().setItem(i, controller.getInventory().getItem(i));
						}
					} else {
						target.getInventory().setItem(i, null);
					}
				}

				if (target.getHealth() > 0) {
					target.teleportAsync(controller.getLocation());
				}

				target.setAllowFlight(controller.getAllowFlight());
				target.setExhaustion(controller.getExhaustion());
				target.setFlying(controller.isFlying());
				target.setFoodLevel(controller.getFoodLevel());
				target.setMaxHealth(controller.getMaxHealth());
				target.setHealth(controller.getHealth());
				target.setLevel(controller.getLevel());
				target.setSneaking(controller.isSneaking());
				target.setSprinting(controller.isSprinting());

				for (Player player: Bukkit.getOnlinePlayers()) {
					player.hidePlayer(JavaPlugin.getPlugin(Main.class), controller);
				}
				
				final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				Team team = scoreboard.getTeam("icuDisableCollision");

				if (team == null) {
					team = scoreboard.registerNewTeam("icuDisableCollision");
				}

				team.setCanSeeFriendlyInvisibles​(false);
				team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

				if (!team.hasEntry(controller.getName())) {
					team.addEntry(controller.getName());
				}

				controller.addPotionEffect(
					new PotionEffect(
						PotionEffectType.INVISIBILITY,
						99999,
						0,
						false,
						false
					)
				);
			}
		}
	}
}

class Events implements Listener {
	@EventHandler
	void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();

		if (Main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}

		if (Main.targetFor.containsKey(player.getUniqueId())) {
			final Player target = Main.targetFor.get(player.getUniqueId());

			target.chat(event.getMessage());
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onEntityDamage(EntityDamageEvent event) {
		final Entity player = event.getEntity();

		if (Main.targetFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		final Player player = event.getPlayer();

		if (Main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerDropItem(PlayerDropItemEvent event) {
		final Player player = event.getPlayer();

		if (Main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();

		if (Main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerMove(PlayerMoveEvent event) {
		final Player player = event.getPlayer();

		if (Main.controllerFor.containsKey(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();

		for (Player otherPlayer: Bukkit.getOnlinePlayers()) {
			/* Target disconnects */
			if (Main.controllerFor.containsKey(player.getUniqueId()) &&
				Main.controllerFor.get(player.getUniqueId()).equals(otherPlayer)) {
				Main.targetFor.remove(otherPlayer.getUniqueId());
				Main.controllerFor.remove(player.getUniqueId());

				final Player controller = otherPlayer;

				new BukkitRunnable() {
					public void run() {
						for (Player allPlayers: Bukkit.getOnlinePlayers()) {
							allPlayers.showPlayer(JavaPlugin.getPlugin(Main.class), controller);
						}

						final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
						final Team team = scoreboard.getTeam("icuDisableCollision");

						if (team != null &&
							team.hasEntry(controller.getName()) == true) {
							team.removeEntry(controller.getName());
						}

						controller.removePotionEffect(PotionEffectType.INVISIBILITY);
						controller.sendMessage("You are now visible");
					}
				}.runTaskLater(JavaPlugin.getPlugin(Main.class), 200);

				otherPlayer.sendMessage("The player you were controlling has disconnected. You are invisible for 10 seconds.");
			}

			/* Controller disconnects */
			if (Main.targetFor.containsKey(player.getUniqueId()) &&
				Main.targetFor.get(player.getUniqueId()).equals(otherPlayer)) {
				Main.targetFor.remove(player.getUniqueId());
				Main.controllerFor.remove(otherPlayer.getUniqueId());
			}
		}
	}

	@EventHandler
	void onPlayerRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();

		if (Main.controllerFor.containsKey(player.getUniqueId())) {
			final Player controller = Main.controllerFor.get(player.getUniqueId());

			controller.teleportAsync(player.getLocation());
		}
	}
}
