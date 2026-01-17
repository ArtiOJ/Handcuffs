package dev.artifabrian.handcuffs.commands;

import dev.artifabrian.handcuffs.Handcuffs;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class GetHandcuffsCommand implements CommandExecutor {
    private Handcuffs plugin;

    public GetHandcuffsCommand(Handcuffs plugin) {
        this.plugin = plugin;

        plugin.getCommand("gethandcuffs").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            Player player = (Player) sender;
            Inventory inventory = player.getInventory();
            inventory.addItem(plugin.getNewCuff());
            return true;
        }
        return false;
    }
}
