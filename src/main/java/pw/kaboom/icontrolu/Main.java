package pw.kaboom.icontrolu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;

import pw.kaboom.icontrolu.commands.CommandIcu;
import pw.kaboom.icontrolu.PlayerControl;

public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        /* Commands */
        this.getCommand("icu").setExecutor(new CommandIcu());

        /* Modules */
        PlayerControl.enable();
        this.getServer().getPluginManager().registerEvents(new PlayerControl(), this);
    }

    @Override
    public void onDisable() {
        PlayerControl.disable();
    }
}
