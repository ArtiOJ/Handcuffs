package dev.artifabrian.handcuffs;

import dev.artifabrian.handcuffs.util.Colorize;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
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
    private BossBar bar;
    private Map<Player, BossBar> cuffBars = new HashMap<>();
    private Map<Player, Player> disconnectedMap = new HashMap<>();
    private Map<Player, Block> blockCuffMap = new HashMap<>();


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

        if (item.getItemMeta() == null) return;
        if (!item.getType().equals(Material.BLAZE_ROD) && !item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "cuff_item"))) return;
        if (!(event.getRightClicked() instanceof Player interactedPlayer)) return;

        if (cuffedPlayersMap.containsKey(interactedPlayer) && Objects.equals(cuffedPlayersMap.get(interactedPlayer), player)) {
            free(interactedPlayer, player);
            return;
        }
        if (cuffedPlayersMap.containsValue(player)) {
            player.sendMessage(Colorize.format("&cYou cannot handcuff mutable players at a time!"));
            return;
        }
        if (cuffedPlayersMap.containsKey(interactedPlayer) || blockCuffMap.containsKey(interactedPlayer)) {
            player.sendMessage(Colorize.format("&cThis player is already handcuffed!"));
            return;
        }

        capture(interactedPlayer, player);
    }

    @EventHandler
    public void onPlayerHit(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();

        if (!block.getType().toString().endsWith("_FENCE")) return;

        Player player = event.getPlayer();
        if (!cuffedPlayersMap.containsValue(player)) return;

        Player captive = getKeyByValue(cuffedPlayersMap, player);
        tieToPost(captive, block);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!cuffedPlayersMap.containsValue(player) && !cuffedPlayersMap.containsKey(player)) return;
        if (cuffedPlayersMap.containsKey(player)) {
            free(player, cuffedPlayersMap.get(player));
            return;
        }

        Player captive = getKeyByValue(cuffedPlayersMap, player);
        free(captive, player);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        playerDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!disconnectedMap.containsKey(player)) return;

        capture(player, disconnectedMap.get(player));
        disconnectedMap.remove(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!blockCuffMap.containsValue(event.getBlock())) return;
        Player freed = getKeyByValue(blockCuffMap, event.getBlock());
        freeFromBlock(freed, player);
    }

    private void playerDisconnect(Player player) {
        if (!cuffedPlayersMap.containsValue(player) && !cuffedPlayersMap.containsKey(player)) return;
        if (cuffedPlayersMap.containsKey(player)) {
            disconnectedMap.put(player, cuffedPlayersMap.get(player));
            cuffedPlayersMap.remove(player);
            return;
        }

        Player captive = getKeyByValue(cuffedPlayersMap, player);
        free(captive, player);
    }


    private void cuffLogic() {
        if (cuffedPlayersMap.isEmpty() && blockCuffMap.isEmpty()) {
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
            if (forward.lengthSquared() < 0.0001) {
                continue;
            }
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
                    continue;
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

        for (Player player : blockCuffMap.keySet()) {
            Block block = blockCuffMap.get(player);
            if (!player.getLocation().getWorld().equals(block.getLocation().getWorld())) {
                player.teleport(block.getLocation().add(0, 1, 0));
                continue;
            }

            Vector dir = block.getLocation().toVector().subtract(player.getLocation().toVector());
            double distance = dir.length();
            dir.normalize();

            double strength = Math.min(HORIZONTAL_PULL_MULTIPLIER * distance, MAX_PULL_STRENGTH);
            Vector pull = dir.multiply(strength);

            if (distance > MIN_PULL_DISTANCE) {
                if (distance > MIN_TELEPORT_DISTANCE) {
                    player.teleport(block.getLocation().add(0, 1, 0));
                    continue;
                }
                player.setVelocity(player.getVelocity().add(pull));
            }
        }
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

    private <K, V> K getKeyByValue(Map<K, V> map, V targetValue) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(targetValue)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void free(Player player, Player captor) {
        captor.sendMessage(Colorize.format("&cYou have unhandcuffed " + player.getName()));
        player.sendMessage(Colorize.format("&cYou have been uncuffed by by " + ChatColor.GOLD + captor.getName()));
        BossBar bar = cuffBars.get(player);
        bar.removePlayer(player);
        cuffBars.remove(player);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 0.8f, 1.2f);
        cuffedPlayersMap.remove(player);
    }

    private void freeFromBlock(Player player, Player liberator) {
        liberator.sendMessage(Colorize.format("&cYou have freed " + ChatColor.GOLD + player.getName()));
        player.sendMessage(Colorize.format("&cYou have been freed by " + ChatColor.GOLD + liberator.getName()));

        BossBar bar = cuffBars.get(player);
        bar.removePlayer(player);
        cuffBars.remove(player);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_LEAD_BREAK, 0.9F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_HIT, 0.5F, 1.2F);
        blockCuffMap.remove(player);
    }

    private void capture(Player player, Player captor) {
        captor.sendMessage(Colorize.format("&cYou have handcuffed " + player.getName()));

        bar = Bukkit.createBossBar(ChatColor.RED + "⛓ You have been cuffed by " + ChatColor.GOLD + captor.getName(),
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );
        bar.setProgress(1.0);
        bar.addPlayer(player);
        bar.setVisible(true);

        cuffBars.putIfAbsent(player, bar);

        player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
        player.getLocation().getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.5f, 1f);

        cuffedPlayersMap.putIfAbsent(player, captor);
        start();
    }

    private void tieToPost(Player player, Block block) {
        player.getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
        player.getLocation().getWorld().playSound(block.getLocation(), Sound.ITEM_LEAD_TIED, 0.7F, 1.1F);

        player.sendMessage(Colorize.format("&cYou have been tied to a fence post!"));

        blockCuffMap.put(player, block);
        cuffedPlayersMap.remove(player);
    }
}