package org.LClear.lClear;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    private boolean placeholderAPIEnabled;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createConfig();
        checkPlaceholderAPI();
    }

    private void checkPlaceholderAPI() {
        placeholderAPIEnabled = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (placeholderAPIEnabled) {
            plugin.getLogger().info("PlaceholderAPI bulundu! Placeholder desteği aktif.");
        } else {
            plugin.getLogger().info("PlaceholderAPI bulunamadı! Placeholder desteği pasif.");
        }
    }

    private void createConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public String getMessage(String path) {
        String message = config.getString("messages." + path, "&cMesaj bulunamadı: " + path);
        String prefix = config.getString("prefix", "&8[&6LClear&8] &7");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            prefix + org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
    }

    public String getMessage(String path, Player player) {
        String message = getMessage(path);
        if (placeholderAPIEnabled && player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public String getMessageWithoutPrefix(String path) {
        String message = config.getString("messages." + path, "&cMesaj bulunamadı: " + path);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessageWithoutPrefix(String path, Player player) {
        String message = getMessageWithoutPrefix(path);
        if (placeholderAPIEnabled && player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public String formatMessage(String path, String placeholder, String value) {
        return getMessageWithoutPrefix(path).replace("{" + placeholder + "}", value);
    }

    public String formatMessage(String path, String placeholder, String value, Player player) {
        String message = formatMessage(path, placeholder, value);
        if (placeholderAPIEnabled && player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public String formatCountdownMessage(String message, int time) {
        String formattedTime;
        if (time >= 60) {
            int minutes = time / 60;
            formattedTime = getMessageWithoutPrefix("countdown-format-minutes").replace("{minutes}", String.valueOf(minutes));
        } else {
            formattedTime = getMessageWithoutPrefix("countdown-format-seconds").replace("{seconds}", String.valueOf(time));
        }
        return getMessageWithoutPrefix(message).replace("{time}", formattedTime);
    }

    public String formatCountdownMessage(String message, int time, Player player) {
        String formatted = formatCountdownMessage(message, time);
        if (placeholderAPIEnabled && player != null) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }
        return formatted;
    }

    public String formatTimeMessage(int time) {
        return getMessageWithoutPrefix("time-message").replace("{time}", String.valueOf(time));
    }

    public String formatTimeMessage(int time, Player player) {
        String formatted = formatTimeMessage(time);
        if (placeholderAPIEnabled && player != null) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }
        return formatted;
    }

    public int getAutoClearInterval() {
        return config.getInt("settings.auto-clear-interval", 150);
    }

    public boolean isAutoClearEnabled() {
        return config.getBoolean("settings.auto-clear-enabled", true);
    }

    public boolean isCountdownEnabled() {
        return config.getBoolean("settings.countdown-enabled", true);
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public List<Integer> getCountdownTimes() {
        return config.getIntegerList("settings.countdown-times");
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Config dosyası kaydedilemedi: " + e.getMessage());
        }
    }
}
