package org.LClear.lClear;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class LClearExpansion extends PlaceholderExpansion {

    private final LClear plugin;

    public LClearExpansion(LClear plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "lclear";
    }

    @Override
    public String getAuthor() {
        return "lilpekker";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equals("time")) {
            return plugin.getRemainingTime();
        }
        
        if (params.equals("time_formatted")) {
            return plugin.getFormattedRemainingTime();
        }
        
        return null;
    }
}
