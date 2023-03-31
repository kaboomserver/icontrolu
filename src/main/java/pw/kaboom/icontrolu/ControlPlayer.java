package pw.kaboom.icontrolu;

import java.util.Iterator;
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

import pw.kaboom.icontrolu.utilities.PlayerList;

class ControlPlayer implements Listener {
    private static String CHAT_PREFIX = "\ud800iControlUChat\ud800";

    private void controlPlayers() {
        if (!PlayerList.hasControllers()) {
            return;
        }

        for (UUID controllerUUID : PlayerList.getControllers()) {
            final Player controller = Bukkit.getPlayer(controllerUUID);
            final Player target = PlayerList.getTarget(controllerUUID);

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
        if (!PlayerList.hasScheduledVisibilities()) {
            return;
        }

        Iterator<Entry<UUID, Long>> iterator = PlayerList.getScheduledVisibilities().entrySet()
            .iterator();

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

        if (PlayerList.getTarget(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerAnimation(final PlayerAnimationEvent event) {
        final Player player = event.getPlayer();

        if (PlayerList.getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerChat(final PlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();

        if (PlayerList.getController(playerUUID) != null) {
            if (event.getMessage().startsWith(CHAT_PREFIX)) {
                event.setMessage(event.getMessage().substring(CHAT_PREFIX.length()));
                return;
            }
            event.setCancelled(true);
            return;
        }

        final Player target = PlayerList.getTarget(playerUUID);

        if (target != null) {
            target.chat(CHAT_PREFIX + event.getMessage());
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        if (PlayerList.getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        if (PlayerList.getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (PlayerList.getController(player.getUniqueId()) != null) {
            event.setCancelled(true);

        } else if ((event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK)
                && PlayerList.getTarget(player.getUniqueId()) != null) {
            final Player target = PlayerList.getTarget(player.getUniqueId());

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

        if (PlayerList.getController(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();
        final Player controller = PlayerList.getController(playerUUID);

        if (controller != null) {
            /*
              Target disconnects
              */
            PlayerList.removeTarget(controller.getUniqueId());
            PlayerList.removeController(playerUUID);
            PlayerList.scheduleVisibility(controller.getUniqueId());

            controller.sendMessage(Component.text("The player you were controlling has "
                                   + "disconnected. You are invisible for 10 seconds."));
            return;
        }

        final Player target = PlayerList.getTarget(playerUUID);

        if (target != null) {
            /*
              Controller disconnects
              */
            PlayerList.removeTarget(playerUUID);
            PlayerList.removeController(target.getUniqueId());
        }
    }

    @EventHandler
    private void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();

        if (PlayerList.getController(player.getUniqueId()) != null) {
            final Player controller = PlayerList.getController(player.getUniqueId());

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
