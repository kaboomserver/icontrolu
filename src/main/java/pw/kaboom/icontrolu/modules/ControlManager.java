package pw.kaboom.icontrolu.modules;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.BiConsumer;

public final class ControlManager {
    // <controller, target>
    private final BiMap<@NotNull UUID, @NotNull UUID> map = HashBiMap.create();

    public void control(final Player controller, final Player target) {
        map.put(controller.getUniqueId(), target.getUniqueId());
    }

    public boolean isController(final UUID controllerUUID) {
        return map.containsKey(controllerUUID);
    }

    public boolean isTarget(final UUID targetUUID) {
        return map.containsValue(targetUUID);
    }

    public Player getTarget(final UUID controllerUUID) {
        final UUID targetUUID = map.get(controllerUUID);
        if (targetUUID == null) return null;

        final Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) map.remove(controllerUUID);
        return target;
    }

    public Player getController(final UUID targetUUID) {
        final UUID controllerUUID = map.inverse().get(targetUUID);
        if (controllerUUID == null) return null;

        final Player controller = Bukkit.getPlayer(controllerUUID);
        if (controller == null) map.remove(controllerUUID);
        return controller;
    }

    public void forEach(final BiConsumer<@NotNull Player, @NotNull Player> consumer) {
        this.map.entrySet().removeIf(e -> {
            final Player controller = Bukkit.getPlayer(e.getKey());
            if (controller == null) return true;
            final Player target = Bukkit.getPlayer(e.getValue());
            if (target == null) return true;

            consumer.accept(controller, target);
            return false;
        });
    }

    public Player removeController(final UUID controllerUUID) {
        final UUID targetUUID = map.remove(controllerUUID);
        if (targetUUID == null) return null;

        return Bukkit.getPlayer(targetUUID);
    }

    public Player removeTarget(final UUID targetUUID) {
        final UUID controlerUUID = map.inverse().remove(targetUUID);
        if (controlerUUID == null) return null;

        return Bukkit.getPlayer(controlerUUID);
    }
}
