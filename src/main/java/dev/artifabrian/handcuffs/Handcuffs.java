package dev.artifabrian.handcuffs;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.intellij.lang.annotations.JdkConstants;

import java.util.List;

public final class Handcuffs extends JavaPlugin {

    HandcuffsData handcuffsData;

    @Override
    public void onEnable() {
        // Plugin startup logic
        handcuffsData = new HandcuffsData(this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public ItemStack getNewCuff() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(this, "cuff_item");
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.BOOLEAN, true);
        meta.displayName(Component.text("Temporary Cuff Item"));
        meta.lore(List.of(Component.text("Temporary Lore Text")));
        item.setItemMeta(meta);
        return item;
    }
}
