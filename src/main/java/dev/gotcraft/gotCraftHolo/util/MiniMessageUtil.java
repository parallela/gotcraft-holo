package dev.gotcraft.gotCraftHolo.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Utility class for MiniMessage formatting
 */
public class MiniMessageUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Parse MiniMessage string to Component
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(text);
    }

    /**
     * Parse MiniMessage with legacy color code support
     */
    public static Component parseWithLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        // Convert legacy color codes to MiniMessage format
        text = text.replace("&", "§");
        return miniMessage.deserialize(text);
    }

    /**
     * Strip all formatting from text
     */
    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return miniMessage.stripTags(text);
    }
}

