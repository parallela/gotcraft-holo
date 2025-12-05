package dev.gotcraft.gotCraftHolo.manager;

import dev.gotcraft.gotCraftHolo.GotCraftHolo;
import dev.gotcraft.gotCraftHolo.model.HoloDefinition;
import dev.gotcraft.gotCraftHolo.model.HoloType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task for refreshing holograms with placeholders
 */
public class RefreshTask extends BukkitRunnable {

    private final GotCraftHolo plugin;
    private final HoloManager holoManager;
    private int tickCounter = 0;

    public RefreshTask(GotCraftHolo plugin, HoloManager holoManager) {
        this.plugin = plugin;
        this.holoManager = holoManager;
    }

    @Override
    public void run() {
        tickCounter++;

        // Tick text animations
        if (plugin.getTextAnimationManager() != null) {
            plugin.getTextAnimationManager().tick();
        }

        for (HoloDefinition def : holoManager.getAllDefinitions()) {
            // Refresh TEXT holograms with placeholders or animations enabled
            if (def.getType() == HoloType.TEXT && (def.isPlaceholdersEnabled() || hasTextAnimations(def))) {
                // Check if it's time to refresh this hologram
                if (tickCounter % def.getPlaceholderRefreshTicks() == 0) {
                    holoManager.refreshHologram(def.getId());
                }
            }
            // Also refresh ITEM/BLOCK holograms that have text below them with placeholders or animations enabled
            else if ((def.getType() == HoloType.ITEM || def.getType() == HoloType.BLOCK)
                     && (def.isPlaceholdersEnabled() || hasTextAnimations(def))
                     && def.getLineCount() > 0) {
                // Check if it's time to refresh this hologram
                if (tickCounter % def.getPlaceholderRefreshTicks() == 0) {
                    holoManager.refreshHologram(def.getId());
                }
            }
        }
    }

    /**
     * Check if hologram text contains animation placeholders
     */
    private boolean hasTextAnimations(HoloDefinition def) {
        if (plugin.getTextAnimationManager() == null) {
            return false;
        }

        String text = def.getText();
        return text != null && plugin.getTextAnimationManager().containsAnimations(text);
    }
}

