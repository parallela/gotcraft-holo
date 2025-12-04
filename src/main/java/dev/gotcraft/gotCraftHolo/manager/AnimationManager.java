package dev.gotcraft.gotCraftHolo.manager;

import dev.gotcraft.gotCraftHolo.GotCraftHolo;
import dev.gotcraft.gotCraftHolo.model.HoloDefinition;
import dev.gotcraft.gotCraftHolo.model.HoloType;
import com.maximde.hologramlib.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages hologram animations and particle effects
 */
public class AnimationManager {

    private final GotCraftHolo plugin;
    private final HoloManager holoManager;
    private final Map<String, BukkitTask> animationTasks;
    private final Map<String, BukkitTask> particleTasks;
    private final Map<String, Double> animationTimers; // Track animation progress
    private final Map<String, Location> baseLocations; // Store original locations

    public AnimationManager(GotCraftHolo plugin, HoloManager holoManager) {
        this.plugin = plugin;
        this.holoManager = holoManager;
        this.animationTasks = new ConcurrentHashMap<>();
        this.particleTasks = new ConcurrentHashMap<>();
        this.animationTimers = new ConcurrentHashMap<>();
        this.baseLocations = new ConcurrentHashMap<>();
    }

    /**
     * Start animation for a hologram
     */
    public void startAnimation(String id, HoloDefinition def, Location baseLocation) {
        stopAnimation(id); // Stop any existing animation

        if (!def.isAnimated() || def.getAnimationType() == HoloDefinition.AnimationType.NONE) {
            return;
        }

        baseLocations.put(id, baseLocation.clone());
        animationTimers.put(id, 0.0);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Hologram<?> hologram = holoManager.getActiveHologram(id);
                if (hologram == null) {
                    stopAnimation(id);
                    return;
                }

                double timer = animationTimers.getOrDefault(id, 0.0);
                timer += def.getAnimationSpeed() * 0.1; // Increment based on speed
                animationTimers.put(id, timer);

                Location base = baseLocations.get(id);
                if (base == null) {
                    stopAnimation(id);
                    return;
                }

                Location newLoc = calculateAnimatedLocation(base, def, timer);

                // Actually move the hologram in-game
                hologram.teleport(newLoc);

                // Also move text hologram below if it exists (for ITEM/BLOCK holograms)
                Hologram<?> textHologram = holoManager.getActiveHologram(id + "_text");
                if (textHologram != null) {
                    Location textLoc = newLoc.clone();

                    // Calculate offsets based on billboard mode
                    double xOffset = 0;
                    double yOffset = def.getTextOffset(); // Use custom text offset
                    double zOffset = 0;

                    if (def.getBillboard() == HoloDefinition.BillboardMode.NONE && def.getType() == HoloType.BLOCK) {
                        xOffset = (0.5 * def.getScale().getX()) + def.getTranslation().getX();
                        zOffset = (0.5 * def.getScale().getZ()) + def.getTranslation().getZ();
                    }

                    textLoc.add(xOffset, yOffset, zOffset);
                    textHologram.teleport(textLoc);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick

        animationTasks.put(id, task);
    }

    /**
     * Stop animation for a hologram
     */
    public void stopAnimation(String id) {
        BukkitTask task = animationTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        animationTimers.remove(id);
        baseLocations.remove(id);
    }

    /**
     * Start particle effects for a hologram
     */
    public void startParticles(String id, HoloDefinition def) {
        stopParticles(id); // Stop any existing particles

        if (!def.isParticlesEnabled()) {
            return;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(def.getParticleType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + def.getParticleType());
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            private double angle = 0;

            @Override
            public void run() {
                // Get the actual hologram object to get its current location
                Hologram<?> hologram = holoManager.getActiveHologram(id);
                if (hologram == null) {
                    stopParticles(id);
                    return;
                }

                Location loc = hologram.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    stopParticles(id);
                    return;
                }

                // Spawn particles in a circle around the hologram
                double radius = def.getParticleRadius();
                int count = def.getParticleCount();

                // Calculate offset based on billboard mode and hologram type
                double xOffset = 0;
                double yOffset;
                double zOffset = 0;

                // For block holograms with FIXED billboard (NONE), we need special handling
                if (def.getBillboard() == HoloDefinition.BillboardMode.NONE && def.getType() == HoloType.BLOCK) {
                    // When billboard is NONE (FIXED), Minecraft renders blocks with their
                    // bottom-southwest corner at the entity's location, not centered.
                    // This means the visual block appears offset from the spawn point.
                    // We need to shift particles to match the visual center:

                    // X offset: +0.5 scaled by X scale, plus translation X
                    xOffset = (0.5 * def.getScale().getX()) + def.getTranslation().getX();

                    // Y offset: +0.5 scaled by Y scale, plus translation Y
                    yOffset = (0.5 * def.getScale().getY()) + def.getTranslation().getY();

                    // Z offset: +0.5 scaled by Z scale, plus translation Z
                    zOffset = (0.5 * def.getScale().getZ()) + def.getTranslation().getZ();
                } else {
                    // For CENTER billboard or other types, particles spawn at a standard offset
                    yOffset = 0.5;
                }

                for (int i = 0; i < count; i++) {
                    double particleAngle = angle + (2 * Math.PI * i / count);
                    double x = loc.getX() + xOffset + radius * Math.cos(particleAngle);
                    double z = loc.getZ() + zOffset + radius * Math.sin(particleAngle);
                    double y = loc.getY() + yOffset;

                    Location particleLoc = new Location(loc.getWorld(), x, y, z);
                    loc.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
                }

                angle += 0.1; // Rotate particles
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every 2 ticks

        particleTasks.put(id, task);
    }

    /**
     * Stop particle effects for a hologram
     */
    public void stopParticles(String id) {
        BukkitTask task = particleTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Stop all animations and particles
     */
    public void stopAll() {
        animationTasks.values().forEach(BukkitTask::cancel);
        animationTasks.clear();
        particleTasks.values().forEach(BukkitTask::cancel);
        particleTasks.clear();
        animationTimers.clear();
    }

    /**
     * Calculate animated location based on animation type
     */
    private Location calculateAnimatedLocation(Location base, HoloDefinition def, double timer) {
        Location newLoc = base.clone();
        double radius = def.getAnimationRadius();

        switch (def.getAnimationType()) {
            case ROTATE:
                // Just rotate yaw, don't move position
                newLoc.setYaw((float) (timer * 20 % 360));
                break;

            case BOUNCE:
                // Move up and down
                double bounceHeight = Math.sin(timer) * radius;
                newLoc.add(0, bounceHeight, 0);
                break;

            case CIRCLE:
                // Move in a horizontal circle
                double circleX = Math.cos(timer) * radius;
                double circleZ = Math.sin(timer) * radius;
                newLoc.add(circleX, 0, circleZ);
                break;

            case SPIRAL:
                // Move in a spiral (circle + vertical movement)
                double spiralX = Math.cos(timer) * radius;
                double spiralZ = Math.sin(timer) * radius;
                double spiralY = (timer % (2 * Math.PI)) / (2 * Math.PI) * radius * 2 - radius;
                newLoc.add(spiralX, spiralY, spiralZ);
                break;

            case SHAKE:
                // Random shake/vibrate
                double shakeX = (Math.random() - 0.5) * radius * 0.2;
                double shakeY = (Math.random() - 0.5) * radius * 0.2;
                double shakeZ = (Math.random() - 0.5) * radius * 0.2;
                newLoc.add(shakeX, shakeY, shakeZ);
                break;

            case NONE:
            default:
                break;
        }

        return newLoc;
    }
}

