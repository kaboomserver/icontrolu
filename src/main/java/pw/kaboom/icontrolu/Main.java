package pw.kaboom.icontrolu;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import pw.kaboom.icontrolu.commands.CommandIcu;
import pw.kaboom.icontrolu.modules.PlayerControl;

public final class Main extends JavaPlugin {
    private final PlayerControl controlModule = new PlayerControl();

    @Override
    public void onEnable() {
        /* Commands */
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            CommandIcu command = new CommandIcu(controlModule);
            LiteralArgumentBuilder<CommandSourceStack> builder
                    = Commands.literal("icu");
            command.build(builder);
            registrar.register(
                    builder.build(), "Control another player's movements, inventory and chat"
            );
        });

        /* Modules */
        controlModule.enable();
        this.getServer().getPluginManager().registerEvents(controlModule, this);
    }

    @Override
    public void onDisable() {
        controlModule.disable();
    }
}
