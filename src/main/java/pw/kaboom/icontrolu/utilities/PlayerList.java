package pw.kaboom.icontrolu.utilities;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;

public final class PlayerList {
    private PlayerList() {
    }

    private static HashMap<UUID, Player> controllerFor = new HashMap<UUID, Player>();
    private static HashMap<UUID, Player> targetFor = new HashMap<UUID, Player>();

    public static Player getController(final UUID playerUuid) {
        return controllerFor.get(playerUuid);
    }

    public static Player getTarget(final UUID playerUuid) {
        return targetFor.get(playerUuid);
    }

    public static void removeController(final UUID playerUuid) {
        controllerFor.remove(playerUuid);
    }

    public static void removeTarget(final UUID playerUuid) {
        targetFor.remove(playerUuid);
    }

    public static void setController(final UUID playerUuid, final Player player) {
        controllerFor.put(playerUuid, player);
    }

    public static void setTarget(final UUID playerUuid, final Player player) {
        targetFor.put(playerUuid, player);
    }
}
