package dev.gotcraft.gotCraftHolo.manager;

import dev.gotcraft.gotCraftHolo.GotCraftHolo;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for text animations
 * Handles loading animation definitions and processing animation placeholders
 */
public class TextAnimationManager {

    private final GotCraftHolo plugin;
    private final File animationsFolder;
    private final Map<String, TextAnimation> animations;
    private final Map<String, Integer> animationFrames; // Track current frame for each animation
    private final Map<String, Integer> tickCounters; // Track ticks for each animation
    private final Pattern animationPattern = Pattern.compile("\\{anim:([^}]+)}");

    public TextAnimationManager(GotCraftHolo plugin) {
        this.plugin = plugin;
        this.animationsFolder = new File(plugin.getDataFolder(), "text-animations");
        this.animations = new ConcurrentHashMap<>();
        this.animationFrames = new ConcurrentHashMap<>();
        this.tickCounters = new ConcurrentHashMap<>();

        // Create text-animations folder if it doesn't exist
        if (!animationsFolder.exists()) {
            animationsFolder.mkdirs();
            createDefaultAnimations();
        }

        loadAllAnimations();
    }

    /**
     * Create default animation files
     */
    private void createDefaultAnimations() {
        // Create wave animation
        createDefaultAnimation("wave", Arrays.asList(
            "▁▂▃▄▅▆▇█",
            "▂▃▄▅▆▇█▁",
            "▃▄▅▆▇█▁▂",
            "▄▅▆▇█▁▂▃",
            "▅▆▇█▁▂▃▄",
            "▆▇█▁▂▃▄▅",
            "▇█▁▂▃▄▅▆",
            "█▁▂▃▄▅▆▇"
        ), 5);

        // Create loading animation
        createDefaultAnimation("loading", Arrays.asList(
            "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"
        ), 3);

        // Create arrow animation
        createDefaultAnimation("arrow", Arrays.asList(
            "→", "↗", "↑", "↖", "←", "↙", "↓", "↘"
        ), 5);

        // Create dots animation
        createDefaultAnimation("dots", Arrays.asList(
            "   ", ".  ", ".. ", "..."
        ), 10);

        plugin.getLogger().info("Created default text animations");
    }

    /**
     * Create a default animation file
     */
    private void createDefaultAnimation(String name, List<String> frames, int speed) {
        try {
            File animFile = new File(animationsFolder, name + ".yml");
            if (!animFile.exists()) {
                YamlConfiguration config = new YamlConfiguration();
                config.set("name", name);
                config.set("speed", speed);
                config.set("frames", frames);
                config.save(animFile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create default animation: " + name);
        }
    }

    /**
     * Load all animations from the text-animations folder
     */
    public void loadAllAnimations() {
        animations.clear();
        animationFrames.clear();

        File[] files = animationsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("No text animations found to load");
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String name = config.getString("name", file.getName().replace(".yml", ""));
                int speed = config.getInt("speed", 5);
                List<String> frames = config.getStringList("frames");

                if (frames.isEmpty()) {
                    plugin.getLogger().warning("Animation " + name + " has no frames");
                    continue;
                }

                TextAnimation anim = new TextAnimation(name, frames, speed);
                animations.put(name.toLowerCase(), anim);
                animationFrames.put(name.toLowerCase(), 0);
                tickCounters.put(name.toLowerCase(), 0);
                loaded++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load animation from " + file.getName() + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " text animation(s)");
    }

    /**
     * Process animation placeholders in text
     * Format: {anim:animation_name}
     */
    public String processAnimations(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = animationPattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String animName = matcher.group(1).toLowerCase();
            TextAnimation anim = animations.get(animName);

            if (anim != null) {
                int currentFrame = animationFrames.getOrDefault(animName, 0);
                String replacement = anim.getFrame(currentFrame);
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // If animation not found, keep the placeholder
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Advance animation frames (called every tick)
     */
    public void tick() {
        for (Map.Entry<String, TextAnimation> entry : animations.entrySet()) {
            String name = entry.getKey();
            TextAnimation anim = entry.getValue();

            // Increment tick counter
            int ticks = tickCounters.getOrDefault(name, 0) + 1;
            tickCounters.put(name, ticks);

            // Only advance frame if we've reached the speed threshold
            if (ticks >= anim.getSpeed()) {
                // Reset tick counter
                tickCounters.put(name, 0);

                // Advance to next frame sequentially
                int currentFrame = animationFrames.getOrDefault(name, 0);
                int nextFrame = (currentFrame + 1) % anim.getFrameCount();
                animationFrames.put(name, nextFrame);
            }
        }
    }

    /**
     * Check if text contains animation placeholders
     */
    public boolean containsAnimations(String text) {
        return text != null && animationPattern.matcher(text).find();
    }

    /**
     * Get animation by name
     */
    public TextAnimation getAnimation(String name) {
        return animations.get(name.toLowerCase());
    }

    /**
     * Get all loaded animations
     */
    public Collection<TextAnimation> getAllAnimations() {
        return animations.values();
    }

    /**
     * Class representing a text animation
     */
    public static class TextAnimation {
        private final String name;
        private final List<String> frames;
        private final int speed; // Ticks between frames

        public TextAnimation(String name, List<String> frames, int speed) {
            this.name = name;
            this.frames = frames;
            this.speed = speed;
        }

        public String getName() {
            return name;
        }

        public List<String> getFrames() {
            return frames;
        }

        public int getSpeed() {
            return speed;
        }

        public int getFrameCount() {
            return frames.size();
        }

        public String getFrame(int index) {
            if (frames.isEmpty()) return "";
            return frames.get(index % frames.size());
        }
    }
}

