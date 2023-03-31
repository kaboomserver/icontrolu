package pw.kaboom.icontrolu;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;

import net.kyori.adventure.text.Component;

public final class PlayerControl implements Listener {

    private static final String CHAT_PREFIX = "\ud800iControlUChat\ud800";
    private static final int VISIBILITY_DELAY_MS = 10000;

    private static Map<UUID, Player> controllers = new HashMap<>();
    private static Map<UUID, Player> targets = new HashMap<>();
    private static Map<UUID, Long> scheduledVisibilities = new HashMap<>();

    public static void enable() {
        /* Setup scoreboard team to prevent player collisions */
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        final Team team = scoreboard.getTeam("icuCollision");
        if (team != null) {
            team.unregister();
        }
    }

    public static void disable() {
        for (UUID controllerUUID : targets.keySet()) {
            final Player controller = Bukkit.getPlayer(controllerUUID);
            final Player target = getTarget(controller.getUniqueId());

            for (Player player: Bukkit.getOnlinePlayers()) {
                player.showPlayer(JavaPlugin.getPlugin(Main.class), controller);
            }

            final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            final Team team = scoreboard.getTeam("icuCollision");

            if (team != null) {
                team.unregister();
            }

            controller.removePotionEffect(PotionEffectType.INVISIBILITY);
            controller.sendMessage(
                Component.text("You are no longer controlling \"")
                    .append(Component.text(target.getName()))
                    .append(Component.text("\" due to server reload"))
            );
        }
    }

    public static Player getController(final UUID playerUUID) {
        return controllers.get(playerUUID);
    }

    public static Player getTarget(final UUID playerUUID) {
        return targets.get(playerUUID);
    }

    public static void removeController(final UUID playerUUID) {
        controllers.remove(playerUUID);
    }

    public static void removeTarget(final UUID playerUUID) {
        targets.remove(playerUUID);
    }

    public static void setController(final UUID playerUUID, final Player player) {
        controllers.put(playerUUID, player);
    }

    public static void setTarget(final UUID playerUUID, final Player player) {
        targets.put(playerUUID, player);
    }

    public static void scheduleVisibility(final UUID playerUUID) {
        scheduledVisibilities.put(playerUUID, System.currentTimeMillis() + VISIBILITY_DELAY_MS);
    }

    private void controlPlayers() {
        if (targets.isEmpty()) {
            return;
        }

        for (UUID controllerUUID : targets.keySet()) {
            final Player controller = Bukkit.getPlayer(controllerUUID);
            final Player target = getTarget(controllerUUID);

            for (int i = 0; i < controller.getInventory().getSize(); i++) {
                if (controller.getInventory().getItem(i) != null) {
                    if (!controller.getInventory().getItem(i).equals(
                            target.getInventory().getItem(i))) {
                        target.getInventory().setItem(i, controller.getInventory().getItem(i));
                    }
                } else {
                    target.getInventory().setItem(i, null);
                }
            }

            if (target.getHealth() > 0) {
                target.teleportAsync(controller.getLocation());
            }

            target.setAllowFlight(controller.getAllowFlight());
            target.setExhaustion(controller.getExhaustion());
            target.setFlying(controller.isFlying());
            target.setFoodLevel(controller.getFoodLevel());

            if (controller.getMaxHealth() > 0) {
                target.setMaxHealth(controller.getMaxHealth());
                target.setHealth(controller.getHealth());
            }

            target.setLevel(controller.getLevel());
            target.setSneaking(controller.isSneaking());
            target.setSprinting(controller.isSprinting());

            for (Player player: Bukkit.getOnlinePlayers()) {
                player.hidePlayer(JavaPlugin.getPlugin(Main.class), controller);
            }

            final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam("icuCollision");

            if (team == null) {
                team = scoreboard.registerNewTeam("icuCollision");
            }

            team.setCanSeeFriendlyInvisibles(false);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

            if (!team.hasEntry(controller.getName())) {
                team.addEntry(controller.getName());
            }

            final int duration = 99999;
            final int amplifier = 0;
            final boolean ambient = false;
            final boolean particles = false;

            controller.addPotionEffect(
                new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    duration,
                    amplifier,
                    ambient,
                    particles
                )
            );
        }
    }

    private void checkVisibility() {
        if (scheduledVisibilities.isEmpty()) {
            return;
        }

        Iterator<Entry<UUID, Long>> iterator = scheduledVisibilities.entrySet().iterator();

        while (iterator.hasNext()) {
            final Entry<UUID, Long> entry = iterator.next();
            final UUID playerUUID = entry.getKey();
            long visibilityTime = entry.getValue();

            if (System.currentTimeMillis() < visibilityTime) {
                continue;
            }

            final Player controller = Bukkit.getPlayer(playerUUID);

            for (Player onlinePlayer: Bukkit.getOnlinePlayers()) {
                onlinePlayer.showPlayer(JavaPlugin.getPlugin(Main.class), controller);
            }

            final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            final Team team = scoreboard.getTeam("icuCollision");

            if (team != null && team.hasEntry(controller.getName())) {
                team.removeEntry(controller.getName());
            }

            controller.removePotionEffect(PotionEffectType.INVISIBILITY);
            controller.sendMessage(Component.text("You are now visible"));
            iterator.remove();
        }
    }

    @EventHandler
    private void onEntityDamage(final EntityDamageEvent event) {
        final Entity player = event.getEntity();

        if (getTarget(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerAnimation(final PlayerAnimationEvent event) {
        final Player player = event.getPlayer();

        if (getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerChat(final PlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();

        if (getController(playerUUID) != null) {
            if (event.getMessage().startsWith(CHAT_PREFIX)) {
                event.setMessage(event.getMessage().substring(CHAT_PREFIX.length()));
                return;
            }
            event.setCancelled(true);
            return;
        }

        final Player target = getTarget(playerUUID);

        if (target != null) {
            target.chat(CHAT_PREFIX + event.getMessage());
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        if (getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        if (getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (getController(player.getUniqueId()) != null) {
            event.setCancelled(true);

        } else if ((event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK)
                && getTarget(player.getUniqueId()) != null) {
            final Player target = getTarget(player.getUniqueId());

            if (event.getHand() == EquipmentSlot.HAND) {
                target.swingMainHand();
            } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
                target.swingOffHand();
            }
        }
    }

    @EventHandler
    private void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        if (getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();
        final Player controller = getController(playerUUID);

        if (controller != null) {
            /*
              Target disconnects
              */
            removeTarget(controller.getUniqueId());
            removeController(playerUUID);
            scheduleVisibility(controller.getUniqueId());

            controller.sendMessage(Component.text("The player you were controlling has "
                                   + "disconnected. You are invisible for 10 seconds."));
            return;
        }

        final Player target = getTarget(playerUUID);

        if (target != null) {
            /*
              Controller disconnects
              */
            removeTarget(playerUUID);
            removeController(target.getUniqueId());
        }
    }

    @EventHandler
    private void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Player controller = getController(player.getUniqueId());

        if (controller != null) {
            controller.teleportAsync(player.getLocation());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTickStart(ServerTickStartEvent event) {
        controlPlayers();
        checkVisibility();
    }
}
