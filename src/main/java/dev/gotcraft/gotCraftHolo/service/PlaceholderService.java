package dev.gotcraft.gotCraftHolo.service;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Service for handling PlaceholderAPI integration
 */
public class PlaceholderService {

    private static boolean placeholderAPIEnabled = false;

    /**
     * Initialize the placeholder service
     */
    public static void init() {
        placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Check if PlaceholderAPI is available
     */
    public static boolean isEnabled() {
        return placeholderAPIEnabled;
    }

    /**
     * Replace placeholders in text
     * If no player is provided, uses offline player context
     */
    public static String setPlaceholders(String text) {
        if (!placeholderAPIEnabled || text == null) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(null, text);
    }

    /**
     * Replace placeholders in text with player context
     */
    public static String setPlaceholders(Player player, String text) {
        if (!placeholderAPIEnabled || text == null) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /**
     * Check if text contains any placeholders
     */
    public static boolean containsPlaceholders(String text) {
        if (text == null) {
            return false;
        }
        return text.contains("%");
    }
}

