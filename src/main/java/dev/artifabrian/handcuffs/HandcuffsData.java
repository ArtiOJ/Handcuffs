package dev.artifabrian.handcuffs;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class HandcuffsData {
    private Handcuffs plugin;
    private File file;
    private YamlConfiguration config;

    public HandcuffsData(Handcuffs plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "config.yml");

        plugin.saveResource("config.yml", false);

        config = new YamlConfiguration();
        config.options().parseComments(false);

        try {
            config.load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void set(String path, Object object) {
        config.set(path, object);
        save();
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}
