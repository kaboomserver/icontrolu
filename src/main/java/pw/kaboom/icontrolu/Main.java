package pw.kaboom.icontrolu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import pw.kaboom.icontrolu.commands.CommandIcu;
import pw.kaboom.icontrolu.utilities.PlayerList;

public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        /* Setup scoreboard team to prevent player collisions */
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        final Team team = scoreboard.getTeam("icuCollision");
        if (team != null) {
            team.unregister();
        }

        /* Commands */
        this.getCommand("icu").setExecutor(new CommandIcu());

        new Tick().runTaskTimer(this, 0, 1);
        this.getServer().getPluginManager().registerEvents(new ControlPlayer(), this);
    }

    @Override
    public void onDisable() {
        for (Player controller: Bukkit.getOnlinePlayers()) {
            final Player target = PlayerList.getTarget(controller.getUniqueId());

            if (target != null) {
                for (Player player: Bukkit.getOnlinePlayers()) {
                    player.showPlayer(this, controller);
                }

                final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                final Team team = scoreboard.getTeam("icuCollision");

                if (team != null) {
                    team.unregister();
                }

                controller.removePotionEffect(PotionEffectType.INVISIBILITY);
                controller.sendMessage("You are no longer controlling \"" + target.getName() + "\" due to server reload");
            }
        }
    }
}
