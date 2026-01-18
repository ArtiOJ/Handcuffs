package dev.artifabrian.handcuffs;

import dev.artifabrian.handcuffs.commands.GetHandcuffsCommand;
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

        new GetHandcuffsCommand(this);
        new HandcuffLogic(this);
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
        meta.displayName(Component.text("&6&lHandcuffs"));
        meta.lore(List.of(Component.text(
                "&7Standard-issue restraints used by law enforcement\n" +
                "&7to safely detain and control civilians.\n" +
                "&7Designed to limit movement and prevent resistance.\n" +
                "\n" +
                "&8&oRight-click a player to handcuff or unhandcuff them.\n" +
                "&8&oLeft-click a post to secure a handcuffed player.\n" +
                "&8&oHandcuffed players cannot attack or break blocks.")));
        item.setItemMeta(meta);
        return item;
    }
}
