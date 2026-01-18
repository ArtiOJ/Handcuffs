package dev.artifabrian.handcuffs;

import dev.artifabrian.handcuffs.util.Colorize;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;


public class HandcuffLogic implements Listener {

    private Handcuffs plugin;
    private Map<Player, Player> cuffedPlayersMap = new HashMap<>();
    private BukkitScheduler scheduler;
    private BukkitTask task;

    private int cuffDistance;




    public HandcuffLogic(Handcuffs plugin, int cuffDistance) {
        this.plugin = plugin;
        this.cuffDistance = cuffDistance;
        scheduler = Bukkit.getScheduler();


        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!item.getType().equals(Material.BLAZE_ROD) && !item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "cuff_item"))) return;
        if (!(event.getRightClicked() instanceof Player interactedPlayer)) return;

        if (cuffedPlayersMap.containsKey(interactedPlayer) && Objects.equals(cuffedPlayersMap.get(interactedPlayer), player)) {
            Bukkit.broadcastMessage(Colorize.format("&cYou have unhandcuffed " + interactedPlayer.getName()));
            cuffedPlayersMap.remove(player);
            return;
        }
        if (cuffedPlayersMap.containsValue(player)) {
            player.sendMessage(Colorize.format("&cYou cannot handcuff mutable players at a time!"));
            return;
        }
        if (cuffedPlayersMap.containsKey(interactedPlayer)) {
            player.sendMessage("&cThis player is already handcuffed!");
            return;
        }

        Bukkit.broadcastMessage(Colorize.format("&cYou have handcuffed " + interactedPlayer.getName()));
        cuffedPlayersMap.put(interactedPlayer, player);
    }

    private void start() {
        if (task != null) return;

        task = scheduler.runTaskTimerAsynchronously(plugin, this::cuffLogic, 0L, 1L);
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
            if (player.getLocation().getWorld().equals(captor.getLocation().getWorld())) {
                player.teleport(captor);
                continue;
            }
            Vector dir = captor.getLocation().toVector().subtract(player.getLocation().toVector());
            double distance = dir.length();
            dir.normalize();

            double strength = Math.min(0.2 * distance, 3.0);
            Vector pull = dir.multiply(strength);

            player.setVelocity(player.getVelocity().add(pull));
        }
    }
}
