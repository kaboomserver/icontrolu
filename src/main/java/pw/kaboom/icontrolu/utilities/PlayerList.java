package pw.kaboom.icontrolu.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

public final class PlayerList {

    private static final int VISIBILITY_DELAY_MS = 10000;
    private static Map<UUID, Player> controllerFor = new HashMap<>();
    private static Map<UUID, Player> targetFor = new HashMap<>();
    private static Map<UUID, Long> scheduledVisibilities = new HashMap<>();

    public static Player getController(final UUID playerUUID) {
        return controllerFor.get(playerUUID);
    }

    public static Set<UUID> getControllers() {
        return targetFor.keySet();
    }

    public static Player getTarget(final UUID playerUUID) {
        return targetFor.get(playerUUID);
    }

    public static Map getScheduledVisibilities() {
        return scheduledVisibilities;
    }

    public static boolean hasControllers() {
        return !targetFor.isEmpty();
    }

    public static boolean hasScheduledVisibilities() {
        return !scheduledVisibilities.isEmpty();
    }

    public static void removeController(final UUID playerUUID) {
        controllerFor.remove(playerUUID);
    }

    public static void removeTarget(final UUID playerUUID) {
        targetFor.remove(playerUUID);
    }

    public static void setController(final UUID playerUUID, final Player player) {
        controllerFor.put(playerUUID, player);
    }

    public static void setTarget(final UUID playerUUID, final Player player) {
        targetFor.put(playerUUID, player);
    }
    
    public static void scheduleVisibility(final UUID playerUUID) {
        scheduledVisibilities.put(playerUUID, System.currentTimeMillis() + VISIBILITY_DELAY_MS);
    }
}
