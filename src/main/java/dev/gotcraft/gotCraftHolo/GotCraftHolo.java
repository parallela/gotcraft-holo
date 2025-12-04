package dev.gotcraft.gotCraftHolo;

import com.maximde.hologramlib.HologramLib;
import dev.gotcraft.gotCraftHolo.commands.HoloCommand;
import dev.gotcraft.gotCraftHolo.manager.HoloManager;
import dev.gotcraft.gotCraftHolo.manager.RefreshTask;
import dev.gotcraft.gotCraftHolo.service.PlaceholderService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * GotCraftHolo - Professional Hologram Plugin for GotCraft Network
 *
 * @author GotCraft Development Team
 * @version 1.0.0
 */
public final class GotCraftHolo extends JavaPlugin {

    private HoloManager holoManager;
    private RefreshTask refreshTask;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    @Override
    public void onLoad() {
        // HologramLib is used as a plugin dependency, no initialization needed
    }

    @Override
    public void onEnable() {
        // ASCII Banner
        getLogger().info("  ____       _    ____            __ _   _   _       _       ");
        getLogger().info(" / ___| ___ | |_ / ___|_ __ __ _ / _| |_| | | | ___ | | ___  ");
        getLogger().info("| |  _ / _ \\| __| |   | '__/ _` | |_| __| |_| |/ _ \\| |/ _ \\ ");
        getLogger().info("| |_| | (_) | |_| |___| | | (_| |  _| |_|  _  | (_) | | (_) |");
        getLogger().info(" \\____|\\___/ \\__|\\____|_|  \\__,_|_|  \\__|_| |_|\\___/|_|\\___/ ");
        getLogger().info("");
        getLogger().info("Version: " + getPluginMeta().getVersion());
        getLogger().info("Author: GotCraft Network");
        getLogger().info("");

        // Create plugin directories
        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdirs();
            if (!created) {
                getLogger().warning("Could not create plugin data folder");
            }
        }

        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                getLogger().warning("Could not create holograms data folder");
            }
        }

        // Load configs
        saveDefaultConfig();
        loadMessages();

        // Initialize services
        PlaceholderService.init();
        if (PlaceholderService.isEnabled()) {
            getLogger().info("✓ PlaceholderAPI hooked successfully!");
        } else {
            getLogger().warning("✗ PlaceholderAPI not found. Placeholder support disabled.");
        }

        // Initialize HologramLib manager
        holoManager = new HoloManager(this);

        HologramLib.getManager().ifPresentOrElse(
            manager -> {
                getLogger().info("✓ HologramLib initialized successfully!");
                holoManager.init(manager);
                holoManager.loadAll();
            },
            () -> {
                getLogger().severe("✗ Failed to initialize HologramLib manager!");
                getLogger().severe("Please ensure HologramLib is properly installed.");
                getServer().getPluginManager().disablePlugin(this);
            }
        );

        // Register commands
        HoloCommand holoCommand = new HoloCommand(this, holoManager);
        if (getCommand("holo") != null) {
            getCommand("holo").setExecutor(holoCommand);
            getCommand("holo").setTabCompleter(holoCommand);
        }

        // Start refresh task
        refreshTask = new RefreshTask(this, holoManager);
        refreshTask.runTaskTimerAsynchronously(this, 20L, 1L); // Run every tick

        getLogger().info("✓ GotCraftHolo enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Cancel refresh task
        if (refreshTask != null) {
            refreshTask.cancel();
        }

        // Save and unload all holograms
        if (holoManager != null) {
            holoManager.saveAll();
            holoManager.unloadAll();
        }

        getLogger().info("✓ GotCraftHolo disabled successfully!");
    }

    /**
     * Load messages configuration
     */
    public void loadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            try {
                InputStream in = getResource("messages.yml");
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                } else {
                    boolean created = messagesFile.createNewFile();
                    if (!created) {
                        getLogger().warning("Could not create messages.yml");
                    }
                }
            } catch (IOException e) {
                getLogger().severe("Could not create messages.yml!");
                getLogger().severe(e.getMessage());
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Reload messages configuration
     */
    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Get a message from messages.yml
     */
    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        return message != null ? message : "<red>Message '" + key + "' not found!";
    }

    /**
     * Get the hologram manager
     */
    public HoloManager getHoloManager() {
        return holoManager;
    }
}
