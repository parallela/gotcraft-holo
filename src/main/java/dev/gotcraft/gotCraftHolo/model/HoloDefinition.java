package dev.gotcraft.gotCraftHolo.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a hologram definition with all its properties
 */
public class HoloDefinition {

    private final String id;
    private HoloType type;
    private Location location;

    // Text hologram properties
    private String text;
    private List<String> lines; // For multi-line support
    private boolean shadow;
    private TextAlignment alignment;
    private int opacity;

    // Background properties
    private boolean backgroundEnabled;
    private int[] backgroundColor; // ARGB
    private int backgroundPadding;

    // View properties
    private double viewRange;
    private boolean seeThroughBlocks;

    // Transform properties
    private Vector scale;
    private BillboardMode billboard;
    private Vector translation;

    // Item/Block properties
    private Material material;
    private boolean glowing;
    private int[] glowColor; // RGB
    private boolean onFire;

    // Placeholder properties
    private boolean placeholdersEnabled;
    private int placeholderRefreshTicks;

    // Animation properties
    private boolean animated;
    private AnimationType animationType;
    private double animationSpeed;
    private double animationRadius;

    // Particle properties
    private boolean particlesEnabled;
    private String particleType;
    private int particleCount;
    private double particleRadius;

    // Text offset (for text below ITEM/BLOCK holograms)
    private double textOffset;

    public HoloDefinition(String id, HoloType type, Location location) {
        this.id = id;
        this.type = type;
        this.location = location;

        // Default values
        this.text = "";
        this.lines = new ArrayList<>();
        this.shadow = true;
        this.alignment = TextAlignment.CENTER;
        this.opacity = 255;

        this.backgroundEnabled = false;
        this.backgroundColor = new int[]{80, 0, 0, 0}; // ARGB
        this.backgroundPadding = 2;

        this.viewRange = 25.0;
        this.seeThroughBlocks = false;

        this.scale = new Vector(1.0, 1.0, 1.0);
        this.billboard = BillboardMode.CENTER;
        this.translation = new Vector(0, 0, 0);

        this.material = Material.STONE;
        this.glowing = false;
        this.glowColor = new int[]{255, 255, 255}; // RGB
        this.onFire = false;

        this.placeholdersEnabled = false;
        this.placeholderRefreshTicks = 20;

        this.animated = false;
        this.animationType = AnimationType.NONE;
        this.animationSpeed = 1.0;
        this.animationRadius = 0.5;

        this.particlesEnabled = false;

        this.textOffset = -0.9; // Default offset below block/item
        this.particleType = "FLAME";
        this.particleCount = 3;
        this.particleRadius = 0.5;
    }

    // Getters and Setters
    public String getId() { return id; }
    public HoloType getType() { return type; }
    public void setType(HoloType type) { this.type = type; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getText() {
        // Always regenerate text from lines
        if (lines != null && !lines.isEmpty()) {
            return String.join("<newline>", lines);
        }
        return text;
    }

    public void setText(String text) {
        this.text = text;
        // Also update lines when setting text directly
        if (text != null && !text.isEmpty()) {
            String[] splitLines = text.split("<newline>");
            this.lines = new ArrayList<>(java.util.Arrays.asList(splitLines));
        }
    }

    public List<String> getLines() { return lines; }
    public void setLines(List<String> lines) { this.lines = lines; }
    public void addLine(String line) { this.lines.add(line); }
    public void setLine(int index, String line) {
        while (lines.size() <= index) {
            lines.add("");
        }
        lines.set(index, line);
    }
    public String getLine(int index) {
        return index < lines.size() ? lines.get(index) : "";
    }
    public int getLineCount() { return lines.size(); }

    public boolean hasShadow() { return shadow; }
    public void setShadow(boolean shadow) { this.shadow = shadow; }
    public TextAlignment getAlignment() { return alignment; }
    public void setAlignment(TextAlignment alignment) { this.alignment = alignment; }
    public int getOpacity() { return opacity; }
    public void setOpacity(int opacity) { this.opacity = opacity; }

    public boolean isBackgroundEnabled() { return backgroundEnabled; }
    public void setBackgroundEnabled(boolean backgroundEnabled) { this.backgroundEnabled = backgroundEnabled; }
    public int[] getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int[] backgroundColor) { this.backgroundColor = backgroundColor; }
    public int getBackgroundPadding() { return backgroundPadding; }
    public void setBackgroundPadding(int backgroundPadding) { this.backgroundPadding = backgroundPadding; }

    public double getViewRange() { return viewRange; }
    public void setViewRange(double viewRange) { this.viewRange = viewRange; }
    public boolean isSeeThroughBlocks() { return seeThroughBlocks; }
    public void setSeeThroughBlocks(boolean seeThroughBlocks) { this.seeThroughBlocks = seeThroughBlocks; }

    public Vector getScale() { return scale; }
    public void setScale(Vector scale) { this.scale = scale; }
    public BillboardMode getBillboard() { return billboard; }
    public void setBillboard(BillboardMode billboard) { this.billboard = billboard; }
    public Vector getTranslation() { return translation; }
    public void setTranslation(Vector translation) { this.translation = translation; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public int[] getGlowColor() { return glowColor; }
    public void setGlowColor(int[] glowColor) { this.glowColor = glowColor; }
    public boolean isOnFire() { return onFire; }
    public void setOnFire(boolean onFire) { this.onFire = onFire; }

    public boolean isPlaceholdersEnabled() { return placeholdersEnabled; }
    public void setPlaceholdersEnabled(boolean placeholdersEnabled) { this.placeholdersEnabled = placeholdersEnabled; }
    public int getPlaceholderRefreshTicks() { return placeholderRefreshTicks; }
    public void setPlaceholderRefreshTicks(int placeholderRefreshTicks) { this.placeholderRefreshTicks = placeholderRefreshTicks; }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean animated) { this.animated = animated; }
    public AnimationType getAnimationType() { return animationType; }
    public void setAnimationType(AnimationType animationType) { this.animationType = animationType; }
    public double getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(double animationSpeed) { this.animationSpeed = animationSpeed; }
    public double getAnimationRadius() { return animationRadius; }
    public void setAnimationRadius(double animationRadius) { this.animationRadius = animationRadius; }

    public boolean isParticlesEnabled() { return particlesEnabled; }
    public void setParticlesEnabled(boolean particlesEnabled) { this.particlesEnabled = particlesEnabled; }
    public String getParticleType() { return particleType; }
    public void setParticleType(String particleType) { this.particleType = particleType; }
    public int getParticleCount() { return particleCount; }
    public void setParticleCount(int particleCount) { this.particleCount = particleCount; }
    public double getParticleRadius() { return particleRadius; }
    public void setParticleRadius(double particleRadius) { this.particleRadius = particleRadius; }

    public double getTextOffset() { return textOffset; }
    public void setTextOffset(double textOffset) { this.textOffset = textOffset; }


    /**
     * Save this hologram definition to a file
     */
    public void save(File dataFolder) throws IOException {
        File holoFile = new File(dataFolder, id + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("id", id);
        config.set("type", type.name());
        config.set("location", serializeLocation(location));

        if (type == HoloType.TEXT) {
            // Only save lines, not text field
            config.set("lines", lines);
            config.set("shadow", shadow);
            config.set("alignment", alignment.name());
            config.set("opacity", opacity);

            config.set("background.enabled", backgroundEnabled);
            config.set("background.color", backgroundColor[0] + "," + backgroundColor[1] + "," +
                      backgroundColor[2] + "," + backgroundColor[3]);
            config.set("background.padding", backgroundPadding);

            config.set("placeholders.enabled", placeholdersEnabled);
            config.set("placeholders.refresh", placeholderRefreshTicks);
        } else if (type == HoloType.ITEM || type == HoloType.BLOCK) {
            config.set("material", material.name());
            config.set("glowing", glowing);
            config.set("glow-color", glowColor[0] + "," + glowColor[1] + "," + glowColor[2]);
            config.set("on-fire", onFire);

            // Save text lines for item/block holograms (text below)
            if (lines != null && !lines.isEmpty()) {
                config.set("lines", lines);
            }

            // Save placeholder settings for text below blocks/items
            config.set("placeholders.enabled", placeholdersEnabled);
            config.set("placeholders.refresh", placeholderRefreshTicks);
        }
        config.set("view.see-through-blocks", seeThroughBlocks);

        config.set("scale", scale.getX() + "," + scale.getY() + "," + scale.getZ());
        config.set("billboard", billboard.name());
        config.set("translation", translation.getX() + "," + translation.getY() + "," + translation.getZ());

        // Save animation properties
        config.set("animation.enabled", animated);
        config.set("animation.type", animationType.name());
        config.set("animation.speed", animationSpeed);
        config.set("animation.radius", animationRadius);

        // Save particle properties
        config.set("particles.enabled", particlesEnabled);
        config.set("particles.type", particleType);
        config.set("particles.count", particleCount);
        config.set("particles.radius", particleRadius);

        // Save text offset (for text below ITEM/BLOCK holograms)
        config.set("text-offset", textOffset);


        config.save(holoFile);
    }

    /**
     * Load a hologram definition from a file
     */
    public static HoloDefinition load(File file) throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String id = config.getString("id");
        HoloType type = HoloType.valueOf(config.getString("type"));
        Location location = deserializeLocation(config.getString("location"));

        HoloDefinition holo = new HoloDefinition(id, type, location);

        if (type == HoloType.TEXT) {
            // Load lines from config
            List<String> savedLines = config.getStringList("lines");
            if (savedLines != null && !savedLines.isEmpty()) {
                holo.setLines(new ArrayList<>(savedLines));
                // Generate text field from lines for internal use
                holo.setText(String.join("<newline>", savedLines));
            } else {
                // Default empty line if nothing saved
                holo.setLines(new ArrayList<>());
                holo.setText("");
            }

            holo.setShadow(config.getBoolean("shadow", true));
            holo.setAlignment(TextAlignment.valueOf(config.getString("alignment", "CENTER")));
            holo.setOpacity(config.getInt("opacity", 255));

            holo.setBackgroundEnabled(config.getBoolean("background.enabled", false));
            String bgColor = config.getString("background.color", "80,0,0,0");
            holo.setBackgroundColor(parseIntArray(bgColor));
            holo.setBackgroundPadding(config.getInt("background.padding", 2));

            holo.setPlaceholdersEnabled(config.getBoolean("placeholders.enabled", false));
        } else if (type == HoloType.ITEM || type == HoloType.BLOCK) {
            holo.setMaterial(Material.valueOf(config.getString("material", "STONE")));
            holo.setGlowing(config.getBoolean("glowing", false));
            String glowColor = config.getString("glow-color", "255,255,255");
            holo.setGlowColor(parseIntArray(glowColor));
            holo.setOnFire(config.getBoolean("on-fire", false));

            // Load text lines for item/block holograms (text below)
            List<String> savedLines = config.getStringList("lines");
            if (savedLines != null && !savedLines.isEmpty()) {
                holo.setLines(new ArrayList<>(savedLines));
            }

            // Load placeholder settings for text below blocks/items
            holo.setPlaceholdersEnabled(config.getBoolean("placeholders.enabled", false));
            holo.setPlaceholderRefreshTicks(config.getInt("placeholders.refresh", 20));
        }

        holo.setViewRange(config.getDouble("view.view-range", 25.0));
        holo.setSeeThroughBlocks(config.getBoolean("view.see-through-blocks", false));

        String scale = config.getString("scale", "1.0,1.0,1.0");
        holo.setScale(parseVector(scale));
        holo.setBillboard(BillboardMode.valueOf(config.getString("billboard", "CENTER")));
        String translation = config.getString("translation", "0,0,0");
        holo.setTranslation(parseVector(translation));

        // Load animation properties
        holo.setAnimated(config.getBoolean("animation.enabled", false));
        holo.setAnimationType(AnimationType.valueOf(config.getString("animation.type", "NONE")));
        holo.setAnimationSpeed(config.getDouble("animation.speed", 1.0));
        holo.setAnimationRadius(config.getDouble("animation.radius", 0.5));

        // Load particle properties
        holo.setParticlesEnabled(config.getBoolean("particles.enabled", false));
        holo.setParticleType(config.getString("particles.type", "FLAME"));
        holo.setParticleCount(config.getInt("particles.count", 3));
        holo.setParticleRadius(config.getDouble("particles.radius", 0.5));

        // Load text offset (for text below ITEM/BLOCK holograms)
        holo.setTextOffset(config.getDouble("text-offset", -0.9));

        return holo;
    }

    private static String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() +
               "," + loc.getYaw() + "," + loc.getPitch();
    }

    private static Location deserializeLocation(String str) {
        String[] parts = str.split(",");
        return new Location(
            org.bukkit.Bukkit.getWorld(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3]),
            parts.length > 4 ? Float.parseFloat(parts[4]) : 0,
            parts.length > 5 ? Float.parseFloat(parts[5]) : 0
        );
    }

    private static int[] parseIntArray(String str) {
        String[] parts = str.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    private static Vector parseVector(String str) {
        String[] parts = str.split(",");
        return new Vector(
            Double.parseDouble(parts[0].trim()),
            Double.parseDouble(parts[1].trim()),
            Double.parseDouble(parts[2].trim())
        );
    }

    public enum TextAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum BillboardMode {
        NONE, VERTICAL, HORIZONTAL, CENTER
    }

    public enum AnimationType {
        NONE,           // No animation
        ROTATE,         // Rotate around Y axis
        BOUNCE,         // Move up and down
        CIRCLE,         // Move in a circle
        SPIRAL,         // Move in a spiral
        SHAKE           // Shake/vibrate effect
    }
}

