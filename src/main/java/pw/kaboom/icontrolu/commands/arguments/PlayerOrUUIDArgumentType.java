package pw.kaboom.icontrolu.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PlayerOrUUIDArgumentType implements
        CustomArgumentType<Player, EntitySelectorArgumentResolver> {
    public static Player getPlayer(final CommandContext<CommandSourceStack> context,
                                   final String name) {
        return context.getArgument(name, Player.class);
    }

    public static PlayerOrUUIDArgumentType playerOrUUID() {
        return new PlayerOrUUIDArgumentType();
    }

    // We lie to clients and tell them we're an entity selector so that UUIDs don't show up as red.
    @Override
    public ArgumentType<EntitySelectorArgumentResolver> getNativeType() {
        return ArgumentTypes.entity();
    }

    @Override
    public Player parse(final StringReader reader) {
        throw new IllegalStateException("method should never be called as we implement override");
    }

    @Override
    public <S> Player parse(final StringReader reader, final S source)
            throws CommandSyntaxException {
        if (!(source instanceof final CommandSourceStack stack))
            throw new IllegalStateException("source was not a CommandSourceStack");

        final int cursor = reader.getCursor();
        final String string = reader.readString();

        try {
            final UUID uuid = UUID.fromString(string);
            final Player player = Bukkit.getPlayer(uuid);

            if (player != null) return player;
        } catch (final IllegalArgumentException ignored) {
        }

        reader.setCursor(cursor);
        return ArgumentTypes.player().parse(reader).resolve(stack).getFirst();
    }
}
