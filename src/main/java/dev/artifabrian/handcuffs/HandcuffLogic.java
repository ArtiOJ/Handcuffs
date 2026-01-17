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
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;


public class HandcuffLogic implements Listener {

    private Handcuffs plugin;

    public HandcuffLogic(Handcuffs plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!item.getType().equals(Material.BLAZE_ROD) && !item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "cuff_item"))) return;
        if (!(event.getRightClicked() instanceof Player interactedPlayer)) return;


        // Cuff logic here
        Bukkit.broadcastMessage(Colorize.format("&cCUFF EM!"));
    }

}
