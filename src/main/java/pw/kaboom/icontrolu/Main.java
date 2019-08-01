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
	static HashMap<UUID, Player> controllerFor = new HashMap<UUID, Player>();
	static HashMap<UUID, Player> targetFor = new HashMap<UUID, Player>();

	public void onEnable() {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Team team = scoreboard.getTeam("iControlU_List");
		if (team != null) {
			team.unregister();
		}

		this.getCommand("icu").setExecutor(new CommandIcu(this));

		new Tick(this).runTaskTimer(this, 0, 1);
		this.getServer().getPluginManager().registerEvents(new Events(this), this);
	}

	public void onDisable() {
		for (Player controller: Bukkit.getOnlinePlayers()) {
			Player target = Main.targetFor.get(controller.getUniqueId());
			if (target != null) {
				for (Player player: Bukkit.getOnlinePlayers()) {
					player.showPlayer(this, controller);
				}

				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				Team team = scoreboard.getTeam("iControlU_List");
				if (team != null) {
					team.unregister();
				}

				controller.removePotionEffect(PotionEffectType.INVISIBILITY);
				controller.sendMessage("You are no longer controlling \"" + target.getName() + "\" due to server reload");
			}
		}
	}
}
