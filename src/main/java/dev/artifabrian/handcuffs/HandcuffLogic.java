package dev.artifabrian.handcuffs;

import dev.artifabrian.handcuffs.util.Colorize;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;


public class HandcuffLogic implements Listener {

    private Handcuffs plugin;
    private Map<Player, Player> cuffedPlayersMap = new HashMap<>();
    private BukkitScheduler scheduler;
    private BukkitTask task;

    private static final double HORIZONTAL_PULL_MULTIPLIER = 0.02; // strength per block of distance
    private static final double MAX_PULL_STRENGTH = 3.0;           // max horizontal pull
    private static final double MIN_PULL_DISTANCE = 5.0;           // min distance to start pulling
    private static final double MIN_TELEPORT_DISTANCE = 15.0;      // distance to force teleport





    public HandcuffLogic(Handcuffs plugin) {
        this.plugin = plugin;

        scheduler = Bukkit.getScheduler();


        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!item.hasItemMeta()) return
        if (!item.getType().equals(Material.BLAZE_ROD) && !item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "cuff_item"))) return;
        if (!(event.getRightClicked() instanceof Player interactedPlayer)) return;

        if (cuffedPlayersMap.containsKey(interactedPlayer) && Objects.equals(cuffedPlayersMap.get(interactedPlayer), player)) {
            player.sendMessage(Colorize.format("&cYou have unhandcuffed " + interactedPlayer.getName()));
            cuffedPlayersMap.remove(interactedPlayer);
            return;
        }
        if (cuffedPlayersMap.containsValue(player)) {
            player.sendMessage(Colorize.format("&cYou cannot handcuff mutable players at a time!"));
            return;
        }
        if (cuffedPlayersMap.containsKey(interactedPlayer)) {
            player.sendMessage(Colorize.format("&cThis player is already handcuffed!"));
            return;
        }

        player.sendMessage(Colorize.format("&cYou have handcuffed " + interactedPlayer.getName()));
        cuffedPlayersMap.put(interactedPlayer, player);
        start();
    }

    private void start() {
        if (task != null) return;

        task = scheduler.runTaskTimer(plugin, this::cuffLogic, 0L, 1L);
    }

    private void stop() {
        if (task == null) return;

        task.cancel();
        task = null;
    }

    private void cuffLogic() {
        if (cuffedPlayersMap.isEmpty()) {
            stop();
            return;
        }

        for (Player player : cuffedPlayersMap.keySet()) {
            Player captor = cuffedPlayersMap.get(player);
            if (!player.getLocation().getWorld().equals(captor.getLocation().getWorld())) {
                player.teleport(captor);
                continue;
            }

            Vector dir = captor.getLocation().toVector().subtract(player.getLocation().toVector());
            double distance = dir.length();


            Vector forward = captor.getLocation().toVector().subtract(player.getLocation().toVector());
            forward.setY(0);
            forward.normalize();

            Vector left = new Vector(-forward.getZ(), 0, forward.getX());
            Vector right = new Vector(forward.getZ(), 0, -forward.getX());

            RayTraceResult frontHit = player.getWorld().rayTraceBlocks(
                    player.getLocation().add(0, 0.1, 0),
                    forward,
                    0.6,
                    FluidCollisionMode.NEVER,
                    true
            );
            RayTraceResult leftHit = player.getWorld().rayTraceBlocks(
                    player.getLocation().add(left.clone().multiply(0.3)).add(0, 0.1, 0),
                    forward,
                    0.6,
                    FluidCollisionMode.NEVER,
                    true
            );
            RayTraceResult rightHit = player.getWorld().rayTraceBlocks(
                    player.getLocation().add(right.clone().multiply(0.3)).add(0, 0.1, 0),
                    forward,
                    0.6,
                    FluidCollisionMode.NEVER,
                    true
            );

            boolean blocked = frontHit != null;
            boolean leftClear = leftHit == null;
            boolean rightClear = rightHit == null;


            Vector moveDir = forward;

            Vector v = new Vector(0, 0, 0);
            if (blocked) {
                if (leftClear) {
                    moveDir = left;
                } else if (rightClear) {
                    moveDir = right;
                } else {
                    if (player.isOnGround() && player.getVelocity().getY() <= 0.01) {
                        v = player.getVelocity();
                        v.setY(0.42);
                        player.setVelocity(v);
                    }
                    return;
                }
            }

            double strength = Math.min(HORIZONTAL_PULL_MULTIPLIER * distance, MAX_PULL_STRENGTH);
            Vector pull = moveDir.multiply(strength);
            pull.add(v);

            if (distance > MIN_PULL_DISTANCE) {
                if (distance > MIN_TELEPORT_DISTANCE) {
                    player.teleport(captor);
                    continue;
                }
                player.setVelocity(player.getVelocity().add(pull));
            }
        }
    }
}
