package dev.gotcraft.gotCraftHolo.manager;

import dev.gotcraft.gotCraftHolo.GotCraftHolo;
import dev.gotcraft.gotCraftHolo.model.HoloDefinition;
import dev.gotcraft.gotCraftHolo.model.HoloType;
import dev.gotcraft.gotCraftHolo.service.PlaceholderService;
import com.maximde.hologramlib.hologram.HologramManager;
import com.maximde.hologramlib.hologram.Hologram;
import com.maximde.hologramlib.hologram.TextHologram;
import com.maximde.hologramlib.hologram.ItemHologram;
import com.maximde.hologramlib.hologram.BlockHologram;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manager for all holograms
 */
public class HoloManager {

    private final GotCraftHolo plugin;
    private final Map<String, HoloDefinition> definitions;
    private final Map<String, Hologram<?>> activeHolograms;
    private final File dataFolder;
    private HologramManager hologramManager;
    private AnimationManager animationManager;

    public HoloManager(GotCraftHolo plugin) {
        this.plugin = plugin;
        this.definitions = new ConcurrentHashMap<>();
        this.activeHolograms = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        // Note: AnimationManager needs this HoloManager, so we initialize it after
        this.animationManager = null;

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Initialize the HologramLib manager
     */
    public void init(HologramManager manager) {
        this.hologramManager = manager;
        // Initialize AnimationManager after HoloManager is fully set up
        this.animationManager = new AnimationManager(plugin, this);
    }

    /**
     * Load all holograms from disk
     */
    public void loadAll() {
        definitions.clear();
        activeHolograms.clear();

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("No holograms found to load.");
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                HoloDefinition def = HoloDefinition.load(file);
                definitions.put(def.getId(), def);
                spawnHologram(def);
                loaded++;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load hologram from " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " hologram(s).");
    }

    /**
     * Save all holograms to disk
     */
    public void saveAll() {
        for (HoloDefinition def : definitions.values()) {
            try {
                def.save(dataFolder);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save hologram " + def.getId(), e);
            }
        }
    }

    /**
     * Get an active hologram by ID
     */
    public Hologram<?> getActiveHologram(String id) {
        return activeHolograms.get(id);
    }

    /**
     * Unload all holograms
     */
    public void unloadAll() {
        // Stop all animations and particles first
        if (animationManager != null) {
            animationManager.stopAll();
        }

        for (Hologram<?> hologram : activeHolograms.values()) {
            hologramManager.remove(hologram);
        }
        activeHolograms.clear();
    }

    /**
     * Create a new hologram
     */
    public HoloDefinition createHologram(String id, HoloType type, Location location) {
        if (definitions.containsKey(id)) {
            return null;
        }

        HoloDefinition def = new HoloDefinition(id, type, location);
        definitions.put(id, def);

        // Don't spawn or save yet - let the command handler configure it first
        // This prevents spawning with default STONE material for items/blocks

        return def;
    }

    /**
     * Remove a hologram
     */
    public boolean removeHologram(String id) {
        HoloDefinition def = definitions.remove(id);
        if (def == null) {
            return false;
        }

        // Stop animations and particles
        if (animationManager != null) {
            animationManager.stopAnimation(id);
            animationManager.stopParticles(id);
        }

        // Remove main hologram
        Hologram<?> hologram = activeHolograms.remove(id);
        if (hologram != null) {
            hologramManager.remove(hologram);
        }

        // Remove text hologram below item/block if it exists
        Hologram<?> textHologram = activeHolograms.remove(id + "_text");
        if (textHologram != null) {
            hologramManager.remove(textHologram);
        }

        File file = new File(dataFolder, id + ".yml");
        if (file.exists()) {
            file.delete();
        }

        return true;
    }

    /**
     * Get a hologram definition
     */
    public HoloDefinition getDefinition(String id) {
        return definitions.get(id);
    }

    /**
     * Get all hologram definitions
     */
    public Collection<HoloDefinition> getAllDefinitions() {
        return definitions.values();
    }

    /**
     * Update a hologram
     */
    public void updateHologram(HoloDefinition def) {
        try {
            def.save(dataFolder);

            // IMPORTANT: Stop animations and particles FIRST before removing holograms
            // This prevents tasks from trying to access deleted holograms
            if (animationManager != null) {
                animationManager.stopAnimation(def.getId());
                animationManager.stopParticles(def.getId());
            }

            // Remove old main hologram
            Hologram<?> oldHologram = activeHolograms.remove(def.getId());
            if (oldHologram != null) {
                hologramManager.remove(oldHologram);
            }

            // Remove old text hologram below item/block if it exists
            Hologram<?> oldTextHologram = activeHolograms.remove(def.getId() + "_text");
            if (oldTextHologram != null) {
                hologramManager.remove(oldTextHologram);
            }

            // Spawn new hologram (and text below if applicable)
            // This will start animations/particles if enabled in the definition
            spawnHologram(def);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update hologram " + def.getId(), e);
        }
    }

    /**
     * Refresh hologram text (for placeholders)
     */
    public void refreshHologram(String id) {
        HoloDefinition def = definitions.get(id);
        if (def == null) {
            return;
        }

        // Handle TEXT type holograms
        if (def.getType() == HoloType.TEXT) {
            Hologram<?> hologram = activeHolograms.get(id);
            if (!(hologram instanceof TextHologram)) {
                return;
            }

            TextHologram textHologram = (TextHologram) hologram;
            String text = def.getText();
            if (PlaceholderService.isEnabled() && PlaceholderService.containsPlaceholders(text)) {
                text = PlaceholderService.setPlaceholders(text);
            }

            textHologram.setMiniMessageText(text).update();
        }
        // Handle text below ITEM/BLOCK holograms
        else if (def.getType() == HoloType.ITEM || def.getType() == HoloType.BLOCK) {
            // Get the text hologram that appears below the item/block
            Hologram<?> hologram = activeHolograms.get(id + "_text");
            if (!(hologram instanceof TextHologram)) {
                return;
            }

            TextHologram textHologram = (TextHologram) hologram;
            String text = def.getText();

            // If no text, remove the text hologram
            if (text == null || text.isEmpty() || def.getLineCount() == 0) {
                hologramManager.remove(textHologram);
                activeHolograms.remove(id + "_text");
                return;
            }

            if (PlaceholderService.isEnabled() && PlaceholderService.containsPlaceholders(text)) {
                text = PlaceholderService.setPlaceholders(text);
            }

            textHologram.setMiniMessageText(text).update();
        }
    }

    /**
     * Spawn a hologram from definition
     */
    private void spawnHologram(HoloDefinition def) {
        try {
            Location loc = def.getLocation();

            switch (def.getType()) {
                case TEXT:
                    String text = def.getText();
                    if (PlaceholderService.isEnabled() && PlaceholderService.containsPlaceholders(text)) {
                        text = PlaceholderService.setPlaceholders(text);
                    }

                    TextHologram textHologram = new TextHologram(def.getId())
                        .setMiniMessageText(text)
                        .setShadow(def.hasShadow())
                        .setAlignment(convertAlignment(def.getAlignment()))
                        .setTextOpacity((byte) def.getOpacity())
                        .setSeeThroughBlocks(def.isSeeThroughBlocks())
                        .setViewRange(def.getViewRange())
                        .setScale((float) def.getScale().getX(), (float) def.getScale().getY(), (float) def.getScale().getZ())
                        .setBillboard(convertBillboard(def.getBillboard()))
                        .setTranslation((float) def.getTranslation().getX(), (float) def.getTranslation().getY(), (float) def.getTranslation().getZ());

                    if (def.isBackgroundEnabled()) {
                        int[] bgColor = def.getBackgroundColor();
                        // Create ARGB color: (alpha << 24) | (red << 16) | (green << 8) | blue
                        int argb = (bgColor[0] << 24) | (bgColor[1] << 16) | (bgColor[2] << 8) | bgColor[3];
                        textHologram.setBackgroundColor(argb);
                    } else {
                        textHologram.setBackgroundColor(0);
                    }

                    hologramManager.spawn(textHologram, loc);
                    activeHolograms.put(def.getId(), textHologram);
                    break;

                case ITEM:
                    plugin.getLogger().info("Spawning ITEM hologram: " + def.getId());
                    plugin.getLogger().info("Material: " + def.getMaterial());

                    ItemType itemType = ItemTypes.getByName("minecraft:" + def.getMaterial().name().toLowerCase());
                    if (itemType == null) {
                        plugin.getLogger().severe("ItemType is NULL for material: " + def.getMaterial());
                        return;
                    }

                    ItemStack itemStack = ItemStack.builder().type(itemType).amount(1).build();

                    ItemHologram itemHologram = new ItemHologram(def.getId())
                        .setItem(itemStack)
                        .setGlowing(def.isGlowing())
                        .setOnFire(def.isOnFire())
                        .setViewRange(def.getViewRange())
                        .setScale((float) def.getScale().getX(), (float) def.getScale().getY(), (float) def.getScale().getZ())
                        .setBillboard(convertBillboard(def.getBillboard()))
                        .setTranslation((float) def.getTranslation().getX(), (float) def.getTranslation().getY(), (float) def.getTranslation().getZ());

                    if (def.isGlowing()) {
                        int[] color = def.getGlowColor();
                        itemHologram.setGlowColor(new java.awt.Color(color[0], color[1], color[2]));
                    }

                    plugin.getLogger().info("Spawning at location: " + loc);
                    hologramManager.spawn(itemHologram, loc);
                    activeHolograms.put(def.getId(), itemHologram);
                    plugin.getLogger().info("ITEM hologram spawned successfully!");

                    // Spawn text hologram below if text lines exist
                    spawnTextBelowHologram(def, loc);
                    break;

                case BLOCK:
                    plugin.getLogger().info("Spawning BLOCK hologram: " + def.getId());
                    plugin.getLogger().info("Material: " + def.getMaterial());

                    // Use PacketEvents StateTypes to get the block state (same API as ItemTypes)
                    String blockName = "minecraft:" + def.getMaterial().name().toLowerCase();
                    StateType stateType = StateTypes.getByName(blockName);

                    if (stateType == null) {
                        plugin.getLogger().severe("StateType is NULL for material: " + def.getMaterial());
                        plugin.getLogger().severe("Tried: " + blockName);
                        return;
                    }

                    plugin.getLogger().info("Found StateType: " + stateType.getName());

                    // Create block state (block data) and get its global ID
                    WrappedBlockState blockState = stateType.createBlockState();
                    int blockStateId = blockState.getGlobalId();
                    plugin.getLogger().info("Block state ID: " + blockStateId);


                    BlockHologram blockHologram = new BlockHologram(def.getId())
                        .setBlock(blockStateId)
                        .setOnFire(def.isOnFire())
                        .setViewRange(def.getViewRange())
                        .setScale((float) def.getScale().getX(), (float) def.getScale().getY(), (float) def.getScale().getZ())
                        .setBillboard(convertBillboard(def.getBillboard()))
                        .setTranslation((float) def.getTranslation().getX(), (float) def.getTranslation().getY(), (float) def.getTranslation().getZ());

                    plugin.getLogger().info("Spawning at location: " + loc);
                    hologramManager.spawn(blockHologram, loc);
                    activeHolograms.put(def.getId(), blockHologram);
                    plugin.getLogger().info("BLOCK hologram spawned successfully!");

                    // Spawn text hologram below if text lines exist
                    spawnTextBelowHologram(def, loc);
                    break;

                default:
                    return;
            }

            // Start animations and particles if enabled
            if (animationManager != null) {
                if (def.isAnimated()) {
                    animationManager.startAnimation(def.getId(), def, loc);
                }
                if (def.isParticlesEnabled()) {
                    animationManager.startParticles(def.getId(), def);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to spawn hologram " + def.getId(), e);
            plugin.getLogger().severe("Hologram type: " + def.getType());
            plugin.getLogger().severe("Location: " + def.getLocation());
            if (def.getType() == HoloType.ITEM || def.getType() == HoloType.BLOCK) {
                plugin.getLogger().severe("Material: " + def.getMaterial());
            }
            e.printStackTrace();
        }
    }

    /**
     * Spawn a text hologram below an item or block hologram
     */
    private void spawnTextBelowHologram(HoloDefinition def, Location loc) {
        // Only spawn if there are text lines
        if (def.getLineCount() == 0) {
            return;
        }

        String text = def.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        plugin.getLogger().info("Spawning text below " + def.getType() + " hologram");

        // Process placeholders if enabled
        if (PlaceholderService.isEnabled() && PlaceholderService.containsPlaceholders(text)) {
            text = PlaceholderService.setPlaceholders(text);
        }

        // Calculate position below the item/block
        Location textLoc = loc.clone();

        // Calculate offsets based on billboard mode
        double xOffset = 0;
        double yOffset = def.getTextOffset(); // Use custom text offset
        double zOffset = 0;

        switch (def.getBillboard()) {
            case NONE: // FIXED billboard
                if (def.getType() == HoloType.BLOCK) {
                    // For FIXED billboard blocks, the visual center is offset
                    xOffset = (0.5 * def.getScale().getX()) + def.getTranslation().getX();
                    zOffset = (0.5 * def.getScale().getZ()) + def.getTranslation().getZ();
                }
                break;

            case VERTICAL:
            case HORIZONTAL:
            case CENTER:
            default:
                // For other billboard modes, Y offset is enough
                break;
        }

        textLoc.add(xOffset, yOffset, zOffset);

        TextHologram textHologram = new TextHologram(def.getId() + "_text")
            .setMiniMessageText(text)
            .setShadow(def.hasShadow())
            .setAlignment(convertAlignment(def.getAlignment()))
            .setTextOpacity((byte) def.getOpacity())
            .setSeeThroughBlocks(def.isSeeThroughBlocks())
            .setViewRange(def.getViewRange())
            .setScale((float) def.getScale().getX(), (float) def.getScale().getY(), (float) def.getScale().getZ())
            .setBillboard(Display.Billboard.CENTER); // Always center for text below items/blocks

        if (def.isBackgroundEnabled()) {
            int[] bgColor = def.getBackgroundColor();
            int argb = (bgColor[0] << 24) | (bgColor[1] << 16) | (bgColor[2] << 8) | bgColor[3];
            textHologram.setBackgroundColor(argb);
        } else {
            textHologram.setBackgroundColor(0);
        }

        hologramManager.spawn(textHologram, textLoc);
        activeHolograms.put(def.getId() + "_text", textHologram);
        plugin.getLogger().info("Text hologram spawned below " + def.getType());
    }

    /**
     * Convert billboard mode to Display.Billboard
     */
    private Display.Billboard convertBillboard(HoloDefinition.BillboardMode mode) {
        return switch (mode) {
            case NONE -> Display.Billboard.FIXED;
            case VERTICAL -> Display.Billboard.VERTICAL;
            case HORIZONTAL -> Display.Billboard.HORIZONTAL;
            case CENTER -> Display.Billboard.CENTER;
        };
    }

    /**
     * Convert text alignment
     */
    private org.bukkit.entity.TextDisplay.TextAlignment convertAlignment(HoloDefinition.TextAlignment alignment) {
        return switch (alignment) {
            case LEFT -> org.bukkit.entity.TextDisplay.TextAlignment.LEFT;
            case RIGHT -> org.bukkit.entity.TextDisplay.TextAlignment.RIGHT;
            case CENTER -> org.bukkit.entity.TextDisplay.TextAlignment.CENTER;
        };
    }
}

