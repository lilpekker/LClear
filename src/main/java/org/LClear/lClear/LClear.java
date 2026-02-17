package org.LClear.lClear;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public final class LClear extends JavaPlugin {

    private ConfigManager configManager;
    private int autoClearTaskId;
    private int countdownTaskId;
    private int currentCountdown;
    private List<Integer> sortedCountdownTimes;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        
        // PlaceholderAPI expansion'ı kaydet
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LClearExpansion(this).register();
            getLogger().info("LClear PlaceholderAPI expansion kaydedildi!");
        }
        
        if (configManager.isAutoClearEnabled()) {
            startAutoClear();
        }
        
        getLogger().info(configManager.getMessage("plugin-enabled"));
    }

    @Override
    public void onDisable() {
        if (autoClearTaskId != 0) {
            Bukkit.getScheduler().cancelTask(autoClearTaskId);
        }
        if (countdownTaskId != 0) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
        }
        getLogger().info(configManager.getMessage("plugin-disabled"));
    }

    private void startAutoClear() {
        // Geri sayım zamanlarını sırala
        sortedCountdownTimes = configManager.getCountdownTimes().stream()
                .sorted((a, b) -> Integer.compare(b, a))
                .collect(Collectors.toList());
        
        int interval = configManager.getAutoClearInterval() * 20; // saniyeyi tick'e çevir
        currentCountdown = configManager.getAutoClearInterval();
        
        autoClearTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                currentCountdown--;
                
                // Geri sayım mesajlarını kontrol et
                if (configManager.isCountdownEnabled() && sortedCountdownTimes.contains(currentCountdown)) {
                    sendCountdownMessage(currentCountdown);
                }
                
                // Temizlik zamanı geldiğinde
                if (currentCountdown <= 0) {
                    clearItems();
                    Bukkit.broadcastMessage(configManager.getMessage("auto-clear-message"));
                    currentCountdown = configManager.getAutoClearInterval();
                }
            }
        }, 20, 20); // Her saniye çalış
    }

    private void clearItems() {
        int clearedCount = 0;
        for (var world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                item.remove();
                clearedCount++;
            }
        }
        getLogger().info(configManager.formatMessage("items-cleared-console", "count", String.valueOf(clearedCount)));
    }

    private void killMobs() {
        int killedCount = 0;
        for (var world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster && !(entity instanceof Player)) {
                    entity.remove();
                    killedCount++;
                }
            }
        }
        getLogger().info(configManager.formatMessage("mobs-killed-console", "count", String.valueOf(killedCount)));
    }

    private void sendCountdownMessage(int time) {
        String prefix = configManager.getMessageWithoutPrefix("prefix");
        String message;
        if (time <= 5) {
            // Son 5 saniye için basit mesaj
            message = configManager.getMessageWithoutPrefix("final-countdown").replace("{time}", String.valueOf(time));
        } else {
            // Diğer zamanlar için detaylı mesaj
            message = configManager.formatCountdownMessage("countdown-message", time);
        }
        
        // PlaceholderAPI ile tüm oyunculara gönder
        for (Player player : Bukkit.getOnlinePlayers()) {
            String formattedMessage = configManager.formatCountdownMessage("countdown-message", time, player);
            if (time <= 5) {
                formattedMessage = prefix + configManager.getMessageWithoutPrefix("final-countdown").replace("{time}", String.valueOf(time));
                if (configManager.isPlaceholderAPIEnabled() && player != null) {
                    formattedMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formattedMessage);
                }
            }
            player.sendMessage(formattedMessage);
        }
    }

    private void stopAutoClear() {
        if (autoClearTaskId != 0) {
            Bukkit.getScheduler().cancelTask(autoClearTaskId);
            autoClearTaskId = 0;
        }
        if (countdownTaskId != 0) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = 0;
        }
    }

    private void restartAutoClear() {
        stopAutoClear();
        if (configManager.isAutoClearEnabled()) {
            startAutoClear();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("lclear")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("lclear.clear")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }

            clearItems();
            String message = configManager.getMessage("clear-message");
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(configManager.getMessage("clear-message", player));
            } else {
                // Konsol ise PlaceholderAPI olmadan gönder
                Bukkit.broadcastMessage(message);
            }
            
            restartAutoClear(); // süreyi sıfırla
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lclear.reload")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }

            configManager.reloadConfig();
            restartAutoClear(); // yeniden başlat
            sender.sendMessage(configManager.getMessage("config-reloaded"));
            return true;
        }

        if (args[0].equalsIgnoreCase("killmobs")) {
            if (!sender.hasPermission("lclear.killmobs")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }

            killMobs();
            String message = configManager.getMessage("killmobs-message");
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(configManager.getMessage("killmobs-message", player));
            } else {
                // Konsol ise PlaceholderAPI olmadan gönder
                Bukkit.broadcastMessage(message);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("time")) {
            if (!sender.hasPermission("lclear.time")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }

            if (!configManager.isAutoClearEnabled()) {
                sender.sendMessage(configManager.getMessage("time-no-autoclear"));
                return true;
            }

            String timeMessage;
            if (sender instanceof Player) {
                Player player = (Player) sender;
                timeMessage = configManager.formatTimeMessage(currentCountdown, player);
            } else {
                timeMessage = configManager.formatTimeMessage(currentCountdown);
            }
            
            sender.sendMessage(timeMessage);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        
        sender.sendMessage(configManager.getMessage("help-header", player));
        
        if (sender.hasPermission("lclear.clear")) {
            sender.sendMessage(configManager.getMessage("help-clear", player));
        }
        
        if (sender.hasPermission("lclear.killmobs")) {
            sender.sendMessage(configManager.getMessage("help-killmobs", player));
        }
        
        if (sender.hasPermission("lclear.time")) {
            sender.sendMessage(configManager.getMessage("help-time", player));
        }
        
        if (sender.hasPermission("lclear.reload")) {
            sender.sendMessage(configManager.getMessage("help-reload", player));
        }
        
        sender.sendMessage(configManager.getMessage("help-footer", player));
    }

    public String getRemainingTime() {
        return String.valueOf(currentCountdown);
    }

    public String getFormattedRemainingTime() {
        if (currentCountdown >= 60) {
            int minutes = currentCountdown / 60;
            return configManager.getMessageWithoutPrefix("time-format-minutes").replace("{minutes}", String.valueOf(minutes));
        } else {
            return configManager.getMessageWithoutPrefix("time-format-seconds").replace("{seconds}", String.valueOf(currentCountdown));
        }
    }
}
