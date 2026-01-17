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

import java.util.*;


public class HandcuffLogic implements Listener {

    private Handcuffs plugin;
    private Map<Player, Player> cuffedPlayersMap = new HashMap<>();
    private BukkitScheduler scheduler;
    private BukkitTask task;


    public HandcuffLogic(Handcuffs plugin) {
        this.plugin = plugin;
        scheduler = Bukkit.getScheduler();
        task = scheduler.runTaskTimerAsynchronously(plugin, () -> {



        }, 0, 1L);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!item.getType().equals(Material.BLAZE_ROD) && !item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "cuff_item"))) return;
        if (!(event.getRightClicked() instanceof Player interactedPlayer)) return;

        if (cuffedPlayersMap.containsKey(player) && Objects.equals(cuffedPlayersMap.get(player), interactedPlayer)) {
            Bukkit.broadcastMessage("Already Contained!");
        }
        if (cuffedPlayersMap.containsKey(player)) {
            player.sendMessage(Colorize.format("&cYou cannot handcuff mutable players at a time!"));
            return;
        }
        if (cuffedPlayersMap.containsValue(interactedPlayer)) {
            player.sendMessage("&cThis player is already handcuffed!");
            return;
        }


        cuffedPlayersMap.put(player, interactedPlayer);
        // Cuff logic here

    }

}
