package pw.kaboom.icontrolu;

import org.bukkit.plugin.java.JavaPlugin;

import pw.kaboom.icontrolu.commands.CommandIcu;
import pw.kaboom.icontrolu.modules.PlayerControl;

public final class Main extends JavaPlugin {
    private final PlayerControl controlModule = new PlayerControl();

    @Override
    public void onEnable() {
        /* Commands */
        this.getCommand("icu").setExecutor(new CommandIcu(controlModule));

        /* Modules */
        controlModule.enable();
        this.getServer().getPluginManager().registerEvents(controlModule, this);
    }

    @Override
    public void onDisable() {
        controlModule.disable();
    }
}
