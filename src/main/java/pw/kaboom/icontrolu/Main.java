package pw.kaboom.icontrolu;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;

import org.bukkit.entity.Player;

import org.bukkit.potion.PotionEffectType;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Main extends JavaPlugin {
	static HashMap<UUID, Player> controllerFor = new HashMap<>();
	static HashMap<UUID, Player> targetFor = new HashMap<>();

	public void onEnable() {
		/* Setup scoreboard team to prevent player collisions */
		final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		final Team team = scoreboard.getTeam("icuDisableCollision");
		if (team != null) {
			team.unregister();
		}

		/* Commands */
		this.getCommand("icu").setExecutor(new CommandIcu());

		new Tick().runTaskTimer(this, 0, 1);
		this.getServer().getPluginManager().registerEvents(new Events(), this);
	}

	public void onDisable() {
		for (Player controller: Bukkit.getOnlinePlayers()) {
			final Player target = Main.targetFor.get(controller.getUniqueId());

			if (target != null) {
				for (Player player: Bukkit.getOnlinePlayers()) {
					player.showPlayer(this, controller);
				}

				final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				final Team team = scoreboard.getTeam("icuDisableCollision");

				if (team != null) {
					team.unregister();
				}

				controller.removePotionEffect(PotionEffectType.INVISIBILITY);
				controller.sendMessage("You are no longer controlling \"" + target.getName() + "\" due to server reload");
			}
		}
	}
}
