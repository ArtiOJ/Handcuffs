package dev.artifabrian.handcuffs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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
}
