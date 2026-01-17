package dev.artifabrian.handcuffs.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public class Colorize {
    public static @NotNull String format(String str){
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}