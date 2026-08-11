package pw.kaboom.icontrolu.modules;

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
import pw.kaboom.icontrolu.Main;

public final class PlayerControl implements Listener {

    private static final String CHAT_PREFIX = "\ud800iControlUChat\ud800";
    private static final int VISIBILITY_DELAY_MS = 10000;

    private final Map<UUID, Long> scheduledVisibilities = new HashMap<>();
    public final ControlManager manager = new ControlManager();

    public static int getVisibilityDelay() {
        return VISIBILITY_DELAY_MS / 1000;
    }

    public void enable() {
        /* Setup scoreboard team to prevent player collisions */
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        final Team team = scoreboard.getTeam("icuCollision");
        if (team != null) {
            team.unregister();
        }
    }

    public void disable() {
        manager.forEach((controller, target) -> {
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
        });
    }

    public void scheduleVisibility(final UUID playerUUID) {
        scheduledVisibilities.put(playerUUID, System.currentTimeMillis() + VISIBILITY_DELAY_MS);
    }

    private void controlPlayers() {
        manager.forEach((controller, target) -> {
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
        });
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

            iterator.remove();
            final Player controller = Bukkit.getPlayer(playerUUID);
            if (controller == null) return;

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
        }
    }

    @EventHandler
    private void onEntityDamage(final EntityDamageEvent event) {
        final Entity player = event.getEntity();

        if (manager.isController(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerAnimation(final PlayerAnimationEvent event) {
        final Player player = event.getPlayer();

        if (manager.isTarget(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerChat(final PlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();

        if (manager.isTarget(playerUUID)) {
            if (event.getMessage().startsWith(CHAT_PREFIX)) {
                event.setMessage(event.getMessage().substring(CHAT_PREFIX.length()));
                return;
            }
            event.setCancelled(true);
            return;
        }

        final Player target = manager.getTarget(playerUUID);
        if (target != null) {
            target.chat(CHAT_PREFIX + event.getMessage());
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        if (manager.isTarget(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        if (manager.isTarget(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (manager.isTarget(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        final Player target = manager.getTarget(player.getUniqueId());
        if ((event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK)
                && target != null) {
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

        if (manager.isTarget(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerQuit(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        if (manager.removeController(uuid) != null) return;

        final Player controller = manager.removeTarget(uuid);
        if (controller != null) {
            scheduleVisibility(controller.getUniqueId());

            controller.sendMessage(Component.text("The player you were controlling has "
                                   + "disconnected. You are invisible for 10 seconds."));
        }
    }

    @EventHandler
    private void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Player controller = manager.getController(player.getUniqueId());

        if (controller != null) {
            controller.teleportAsync(player.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTickStart(final ServerTickStartEvent event) {
        controlPlayers();
        checkVisibility();
    }
}
