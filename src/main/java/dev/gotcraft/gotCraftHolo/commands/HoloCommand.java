package dev.gotcraft.gotCraftHolo.commands;

import dev.gotcraft.gotCraftHolo.GotCraftHolo;
import dev.gotcraft.gotCraftHolo.manager.HoloManager;
import dev.gotcraft.gotCraftHolo.model.HoloDefinition;
import dev.gotcraft.gotCraftHolo.model.HoloType;
import dev.gotcraft.gotCraftHolo.model.LeaderboardConfig;
import dev.gotcraft.gotCraftHolo.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main command handler for /holo
 */
public class HoloCommand implements CommandExecutor, TabCompleter {

    private final GotCraftHolo plugin;
    private final HoloManager holoManager;

    public HoloCommand(GotCraftHolo plugin, HoloManager holoManager) {
        this.plugin = plugin;
        this.holoManager = holoManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("gotcraftholo.admin")) {
            sendMessage(sender, "<red>You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "create":
                return handleCreate(sender, args);
            case "remove":
            case "delete":
                return handleRemove(sender, args);
            case "settext":
                return handleSetText(sender, args);
            case "addline":
                return handleAddLine(sender, args);
            case "setline":
                return handleSetLine(sender, args);
            case "textoffset":
                return handleTextOffset(sender, args);
            case "list":
                return handleList(sender);
            case "tp":
            case "teleport":
                return handleTeleport(sender, args);
            case "movehere":
                return handleMoveHere(sender, args);
            case "setpos":
            case "setposition":
                return handleSetPos(sender, args);
            case "near":
            case "nearby":
                return handleNear(sender, args);
            case "migrate":
                return handleMigrate(sender);
            case "scale":
                return handleScale(sender, args);
            case "shadow":
                return handleShadow(sender, args);
            case "align":
                return handleAlign(sender, args);
            case "opacity":
                return handleOpacity(sender, args);
            case "background":
                return handleBackground(sender, args);
            case "billboard":
                return handleBillboard(sender, args);
            case "rotate":
            case "rotation":
                return handleRotate(sender, args);
            case "seethrough":
                return handleSeeThrough(sender, args);
            case "viewrange":
                return handleViewRange(sender, args);
            case "placeholders":
                return handlePlaceholders(sender, args);
            case "animate":
            case "animation":
                return handleAnimate(sender, args);
            case "particle":
            case "particles":
                return handleParticle(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendMessage(sender, "<red>Unknown subcommand. Use /holo for help.");
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "<red>Only players can create holograms!");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo create <id> <text|item|block|leaderboard> [material]");
            return true;
        }

        Player player = (Player) sender;
        String id = args[1];
        String typeStr = args[2].toUpperCase();

        HoloType type;
        try {
            type = HoloType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "<red>Invalid type! Use: text, item, block, or leaderboard");
            return true;
        }

        if (holoManager.getDefinition(id) != null) {
            sendMessage(sender, "<red>A hologram with ID <white>" + id + "</white> already exists!");
            return true;
        }

        HoloDefinition def = holoManager.createHologram(id, type, player.getLocation());

        if (type == HoloType.ITEM || type == HoloType.BLOCK) {
            if (args.length < 4) {
                sendMessage(sender, "<yellow>Usage: /holo create <id> " + typeStr.toLowerCase() + " <material>");
                holoManager.removeHologram(id); // Clean up the definition we just created
                return true;
            }

            Material material;
            try {
                material = Material.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                sendMessage(sender, "<red>Invalid material!");
                holoManager.removeHologram(id); // Clean up the definition we just created
                return true;
            }

            def.setMaterial(material);
            holoManager.updateHologram(def); // This will save and spawn with correct material
        } else if (type == HoloType.TEXT) {
            // Set default visible text for text holograms
            def.addLine("<gray>New Hologram</gray>");
            def.addLine("<dark_gray>Use /holo settext " + id + " <text></dark_gray>");
            holoManager.updateHologram(def); // This will save and spawn
        } else if (type == HoloType.LEADERBOARD) {
            // Create default leaderboard configuration
            LeaderboardConfig lbConfig = new LeaderboardConfig();
            lbConfig.setTitle("Leaderboard");
            lbConfig.setMaxDisplayEntries(5);
            lbConfig.setSuffix("points");

            // Add example entries
            for (int i = 1; i <= 5; i++) {
                lbConfig.getEntries().add(new LeaderboardConfig.LeaderboardEntry(
                    i,
                    "Player " + i,
                    String.valueOf((6 - i) * 100)
                ));
            }

            def.setLeaderboardConfig(lbConfig);
            def.setPlaceholdersEnabled(true); // Always enable for leaderboards
            holoManager.updateHologram(def);

            sendMessage(sender, "<gray>Leaderboard created! Edit the config file to add placeholders.</gray>");
        }

        sendMessage(sender, plugin.getMessage("created").replace("<id>", id));
        sendMessage(sender, "");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "<yellow>Usage: /holo remove <id>");
            return true;
        }

        String id = args[1];
        if (holoManager.removeHologram(id)) {
            sendMessage(sender, plugin.getMessage("removed").replace("<id>", id));
        } else {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
        }
        return true;
    }

    private boolean handleSetText(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo settext <id> <text...>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        if (def.getType() != HoloType.TEXT) {
            sendMessage(sender, "<red>This hologram is not a text hologram!");
            return true;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        def.setText(text);
        holoManager.updateHologram(def);

        sendMessage(sender, plugin.getMessage("updated"));
        return true;
    }

    private boolean handleAddLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo addline <id> <text...>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        // Allow text lines for TEXT holograms and text below ITEM/BLOCK holograms
        if (def.getType() != HoloType.TEXT && def.getType() != HoloType.ITEM && def.getType() != HoloType.BLOCK) {
            sendMessage(sender, "<red>Cannot add text lines to this hologram type!");
            return true;
        }

        String newLine = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        def.addLine(newLine);

        holoManager.updateHologram(def);

        int lineNumber = def.getLineCount();
        if (def.getType() == HoloType.TEXT) {
            sendMessage(sender, "<green>✓ Line #" + lineNumber + " added to hologram!");
        } else {
            sendMessage(sender, "<green>✓ Line #" + lineNumber + " added below " + def.getType().name().toLowerCase() + " hologram!");
        }
        return true;
    }

    private boolean handleSetLine(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMessage(sender, "<yellow>Usage: /holo setline <id> <line#> <text...>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        // Allow text lines for TEXT holograms and text below ITEM/BLOCK holograms
        if (def.getType() != HoloType.TEXT && def.getType() != HoloType.ITEM && def.getType() != HoloType.BLOCK) {
            sendMessage(sender, "<red>Cannot edit text lines on this hologram type!");
            return true;
        }

        if (def.getType() != HoloType.TEXT) {
            sendMessage(sender, "<red>This hologram is not a text hologram!");
            return true;
        }

        try {
            int lineNumber = Integer.parseInt(args[2]);
            if (lineNumber < 1) {
                sendMessage(sender, "<red>Line number must be 1 or greater!");
                return true;
            }

            String newLineText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            def.setLine(lineNumber - 1, newLineText); // Convert to 0-based index

            holoManager.updateHologram(def);

            sendMessage(sender, "<green>✓ Line #" + lineNumber + " updated!");
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid line number!");
        }
        return true;
    }

    private boolean handleTextOffset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo textoffset <id> <offset>");
            sendMessage(sender, "<gray>Example: /holo textoffset myblock -0.5");
            sendMessage(sender, "<gray>Negative values move text down, positive values move it up");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        // Only works for ITEM/BLOCK holograms
        if (def.getType() != HoloType.ITEM && def.getType() != HoloType.BLOCK) {
            sendMessage(sender, "<red>Text offset only applies to item and block holograms!");
            return true;
        }

        try {
            double offset = Double.parseDouble(args[2]);
            def.setTextOffset(offset);
            holoManager.updateHologram(def);
            sendMessage(sender, "<green>✓ Text offset set to " + offset + " for " + def.getType().name().toLowerCase() + " hologram!");
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid number!");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Collection<HoloDefinition> holograms = holoManager.getAllDefinitions();

        if (holograms.isEmpty()) {
            sendMessage(sender, "<yellow>No holograms found.");
            return true;
        }

        sendMessage(sender, "<gradient:#00F8F8:#00F542><b>Holograms (" + holograms.size() + "):</b></gradient>");
        for (HoloDefinition def : holograms) {
            sendMessage(sender, "<white>• " + def.getId() + "</white> <gray>(" + def.getType() + ")</gray>");
        }
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "<red>Only players can teleport!");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "<yellow>Usage: /holo tp <id>");
            return true;
        }

        Player player = (Player) sender;
        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        player.teleport(def.getLocation());
        sendMessage(sender, "<green>Teleported to hologram <white>" + id + "</white>!");
        return true;
    }

    private boolean handleMoveHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "<red>Only players can move holograms!");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "<yellow>Usage: /holo movehere <id>");
            return true;
        }

        Player player = (Player) sender;
        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        def.setLocation(player.getLocation());
        holoManager.updateHologram(def);
        sendMessage(sender, plugin.getMessage("updated"));
        return true;
    }

    private boolean handleSetPos(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sendMessage(sender, "<yellow>Usage: /holo setpos <id> <x> <y> <z>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);

            Location newLoc = def.getLocation().clone();
            newLoc.setX(x);
            newLoc.setY(y);
            newLoc.setZ(z);

            def.setLocation(newLoc);
            holoManager.updateHologram(def);
            sendMessage(sender, "<green>✓ Hologram <white>" + id + "</white> moved to <white>" +
                       String.format("%.2f, %.2f, %.2f", x, y, z) + "</white>");
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid coordinates!");
        }
        return true;
    }

    private boolean handleNear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "<red>Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        double distance = 10.0; // Default distance

        if (args.length >= 2) {
            try {
                distance = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sendMessage(sender, "<red>Invalid distance!");
                return true;
            }
        }

        Location playerLoc = player.getLocation();
        List<HoloDefinition> nearbyHolograms = new ArrayList<>();

        for (HoloDefinition def : holoManager.getAllDefinitions()) {
            Location holoLoc = def.getLocation();
            if (holoLoc.getWorld() != null && holoLoc.getWorld().equals(playerLoc.getWorld())) {
                double dist = holoLoc.distance(playerLoc);
                if (dist <= distance) {
                    nearbyHolograms.add(def);
                }
            }
        }

        if (nearbyHolograms.isEmpty()) {
            sendMessage(sender, "<yellow>No holograms found within <white>" + distance + "</white> blocks.");
            return true;
        }

        // Sort by distance
        nearbyHolograms.sort((a, b) -> {
            double distA = a.getLocation().distance(playerLoc);
            double distB = b.getLocation().distance(playerLoc);
            return Double.compare(distA, distB);
        });

        sendMessage(sender, "<gradient:#00F8F8:#00F542><b>Nearby Holograms (" + nearbyHolograms.size() + "):</b></gradient>");
        for (HoloDefinition def : nearbyHolograms) {
            double dist = def.getLocation().distance(playerLoc);
            sendMessage(sender, "<white>• " + def.getId() + "</white> <gray>(" + def.getType() + ") - " +
                       String.format("%.1f", dist) + "m away</gray>");
        }
        return true;
    }

    private boolean handleMigrate(CommandSender sender) {
        sendMessage(sender, "<yellow>Starting migration from DecentHolograms...");

        // Look for DecentHolograms holograms folder in plugins directory
        File pluginsFolder = plugin.getDataFolder().getParentFile(); // Gets plugins/ folder
        File dhHologramsFolder = new File(pluginsFolder, "DecentHolograms/holograms");

        if (!dhHologramsFolder.exists() || !dhHologramsFolder.isDirectory()) {
            sendMessage(sender, "<red>DecentHolograms holograms folder not found!");
            sendMessage(sender, "<gray>Looking for: " + dhHologramsFolder.getAbsolutePath());
            sendMessage(sender, "<yellow>Expected location: plugins/DecentHolograms/holograms/");
            return true;
        }

        File[] dhFiles = dhHologramsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (dhFiles == null || dhFiles.length == 0) {
            sendMessage(sender, "<red>No DecentHolograms files found to migrate!");
            sendMessage(sender, "<gray>Folder exists but contains no .yml files");
            return true;
        }

        sendMessage(sender, "<green>Found " + dhFiles.length + " hologram(s) to migrate");

        int migrated = 0;
        int skipped = 0;
        int errors = 0;

        for (File dhFile : dhFiles) {
            try {
                String id = dhFile.getName().replace(".yml", "");

                // Skip if already exists
                if (holoManager.getDefinition(id) != null) {
                    skipped++;
                    continue;
                }

                YamlConfiguration dhConfig = YamlConfiguration.loadConfiguration(dhFile);

                // Parse location (format: world:x:y:z or spawn:387.132:206.624:-82.423)
                String locationStr = dhConfig.getString("location", "");
                if (locationStr.isEmpty()) {
                    plugin.getLogger().warning("No location found in " + dhFile.getName());
                    errors++;
                    continue;
                }

                Location location = parseDecentHologramsLocation(locationStr);
                if (location == null) {
                    plugin.getLogger().warning("Invalid location in " + dhFile.getName() + ": " + locationStr);
                    errors++;
                    continue;
                }

                // Get pages and lines
                List<Map<?, ?>> pages = dhConfig.getMapList("pages");
                if (pages.isEmpty()) {
                    plugin.getLogger().warning("No pages found in " + dhFile.getName());
                    errors++;
                    continue;
                }

                // Get first page
                Map<?, ?> firstPage = pages.get(0);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dhLines = (List<Map<String, Object>>) firstPage.get("lines");

                if (dhLines == null || dhLines.isEmpty()) {
                    plugin.getLogger().warning("No lines found in " + dhFile.getName());
                    errors++;
                    continue;
                }

                // Process all lines - skip item/head markers, convert text, filter player placeholders
                List<String> textLines = new ArrayList<>();
                boolean hasPlaceholders = false;
                int skippedPlayerLines = 0;

                for (Map<String, Object> line : dhLines) {
                    String content = (String) line.get("content");
                    if (content == null) continue;

                    // Skip item/head markers (don't include in text)
                    // DecentHolograms item syntax: #HEAD:, #ICON:, #SMALLHEAD:, #SMALLICON:
                    if (content.startsWith("#HEAD:") || content.startsWith("#ICON:") ||
                        content.startsWith("#SMALLHEAD:") || content.startsWith("#SMALLICON:")) {
                        // Skip these lines - we're creating TEXT holograms only
                        continue;
                    }

                    // Skip empty height markers
                    if (content.trim().isEmpty()) {
                        textLines.add("");
                        continue;
                    }

                    // Convert legacy color codes and DecentHolograms animations
                    String convertedContent = convertDecentHologramsText(content);

                    // Check if line contains player-specific placeholders and skip it
                    if (containsPlayerPlaceholder(convertedContent)) {
                        plugin.getLogger().info("Skipping player-specific placeholder line in " + id + ": " + convertedContent);
                        skippedPlayerLines++;
                        continue; // Skip this line entirely
                    }

                    textLines.add(convertedContent);

                    // Check for server-wide placeholders
                    if (convertedContent.contains("%") || convertedContent.contains("{")) {
                        hasPlaceholders = true;
                    }
                }

                if (skippedPlayerLines > 0) {
                    plugin.getLogger().info("Skipped " + skippedPlayerLines + " player-specific lines in " + id);
                }

                // Create hologram as TEXT type only
                HoloDefinition def = holoManager.createHologram(id, HoloType.TEXT, location);

                // Set default scale to 1.0
                def.setScale(new Vector(1.0, 1.0, 1.0));

                // Set lines
                for (String line : textLines) {
                    def.addLine(line);
                }

                // Enable placeholders if detected
                if (hasPlaceholders) {
                    def.setPlaceholdersEnabled(true);
                    def.setPlaceholderRefreshTicks(20);
                }

                // Save and spawn
                holoManager.updateHologram(def);
                migrated++;

            } catch (Exception e) {
                plugin.getLogger().warning("Error migrating " + dhFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
                errors++;
            }
        }

        sendMessage(sender, "<green>✓ Migration complete!");
        sendMessage(sender, "<white>Migrated: <green>" + migrated);
        sendMessage(sender, "<white>Skipped: <yellow>" + skipped);
        sendMessage(sender, "<white>Errors: <red>" + errors);
        return true;
    }

    private Location parseDecentHologramsLocation(String locationStr) {
        try {
            // Format: world:x:y:z or spawn:387.132:206.624:-82.423
            String[] parts = locationStr.split(":");
            if (parts.length < 4) return null;

            String worldName = parts[0];
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
                // Try default world
                world = org.bukkit.Bukkit.getWorlds().get(0);
            }

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private String convertDecentHologramsText(String text) {
        if (text == null) return "";

        // Remove ALL legacy § formatting codes first (§f, §r, etc.)
        text = text.replaceAll("§[0-9a-fklmnor]", "");

        // Convert legacy & color codes to MiniMessage
        text = text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&l", "<bold>")
                   .replace("&o", "<italic>")
                   .replace("&n", "<underlined>")
                   .replace("&m", "<strikethrough>")
                   .replace("&k", "<obfuscated>")
                   .replace("&r", "<reset>");

        // Convert hex colors (&#RRGGBB or &#ffe327)
        text = java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})")
                .matcher(text)
                .replaceAll("<#$1>");

        // Convert DecentHolograms animations <#ANIM:type:colors>text</#ANIM>
        // For now, we'll keep the text but remove the animation tags
        text = java.util.regex.Pattern.compile("<#ANIM:[^>]+>")
                .matcher(text)
                .replaceAll("");
        text = text.replace("</#ANIM>", "");

        return text;
    }

    /**
     * Check if text contains player-specific placeholders
     * These don't work properly in global holograms
     */
    private boolean containsPlayerPlaceholder(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();

        // Common player-specific placeholder patterns
        String[] playerPlaceholders = {
            // Basic player placeholders
            "player_name",
            "player_displayname",
            "player_display_name",
            "player_uuid",
            "player_world",
            "player_x",
            "player_y",
            "player_z",
            "player_health",
            "player_max_health",
            "player_food",
            "player_food_level",
            "player_level",
            "player_exp",
            "player_total_exp",
            "player_gamemode",
            "player_ping",
            "player_locale",
            "player_item_in_hand",

            // Economy placeholders
            "vault_eco_balance",
            "player_balance",
            "player_money",
            "essentials_balance",

            // ajLeaderboards player positions (these cause NullPointerException)
            "ajleaderboards_lb_player_",
            "_player_pos",
            "_player_value",
            "_player_name",

            // Statistics placeholders
            "statistic_",

            // Other common player-specific
            "player_armor",
            "player_bed_",
            "player_compass_",
            "player_direction",
            "player_first_join",
            "player_has_permission",
            "player_ip",
            "player_is_",
            "player_last_",
        };

        for (String placeholder : playerPlaceholders) {
            if (lowerText.contains(placeholder)) {
                return true;
            }
        }

        return false;
    }

    private boolean handleScale(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sendMessage(sender, "<yellow>Usage: /holo scale <id> <x> <y> <z>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);

            def.setScale(new Vector(x, y, z));
            holoManager.updateHologram(def);
            sendMessage(sender, plugin.getMessage("updated"));
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid numbers!");
        }
        return true;
    }

    private boolean handleShadow(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo shadow <id> <true|false>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        if (def.getType() != HoloType.TEXT) {
            sendMessage(sender, "<red>This hologram is not a text hologram!");
            return true;
        }

        boolean shadow = Boolean.parseBoolean(args[2]);
        def.setShadow(shadow);
        holoManager.updateHologram(def);
        sendMessage(sender, plugin.getMessage("updated"));
        return true;
    }

    private boolean handleAlign(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo align <id> <left|center|right>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        if (def.getType() != HoloType.TEXT) {
            sendMessage(sender, "<red>This hologram is not a text hologram!");
            return true;
        }

        try {
            HoloDefinition.TextAlignment alignment = HoloDefinition.TextAlignment.valueOf(args[2].toUpperCase());
            def.setAlignment(alignment);
            holoManager.updateHologram(def);
            sendMessage(sender, plugin.getMessage("updated"));
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "<red>Invalid alignment! Use: left, center, or right");
        }
        return true;
    }

    private boolean handleOpacity(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo opacity <id> <0-255>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        if (def.getType() != HoloType.TEXT) {
            sendMessage(sender, "<red>This hologram is not a text hologram!");
            return true;
        }

        try {
            int opacity = Integer.parseInt(args[2]);
            if (opacity < 0 || opacity > 255) {
                sendMessage(sender, "<red>Opacity must be between 0 and 255!");
                return true;
            }
            def.setOpacity(opacity);
            holoManager.updateHologram(def);
            sendMessage(sender, plugin.getMessage("updated"));
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid number!");
        }
        return true;
    }

    private boolean handleBackground(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo background <id> <enable|disable|color|size>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        if (def.getType() != HoloType.TEXT) {
            sendMessage(sender, "<red>This hologram is not a text hologram!");
            return true;
        }

        String action = args[2].toLowerCase();

        switch (action) {
            case "enable":
                def.setBackgroundEnabled(true);
                holoManager.updateHologram(def);
                sendMessage(sender, plugin.getMessage("updated"));
                break;
            case "disable":
                def.setBackgroundEnabled(false);
                holoManager.updateHologram(def);
                sendMessage(sender, plugin.getMessage("updated"));
                break;
            case "color":
                if (args.length < 7) {
                    sendMessage(sender, "<yellow>Usage: /holo background <id> color <a> <r> <g> <b>");
                    return true;
                }
                try {
                    int a = Integer.parseInt(args[3]);
                    int r = Integer.parseInt(args[4]);
                    int g = Integer.parseInt(args[5]);
                    int b = Integer.parseInt(args[6]);
                    def.setBackgroundColor(new int[]{a, r, g, b});
                    holoManager.updateHologram(def);
                    sendMessage(sender, plugin.getMessage("updated"));
                } catch (NumberFormatException e) {
                    sendMessage(sender, "<red>Invalid color values!");
                }
                break;
            case "size":
            case "padding":
                if (args.length < 4) {
                    sendMessage(sender, "<yellow>Usage: /holo background <id> size <padding>");
                    return true;
                }
                try {
                    int padding = Integer.parseInt(args[3]);
                    def.setBackgroundPadding(padding);
                    holoManager.updateHologram(def);
                    sendMessage(sender, plugin.getMessage("updated"));
                } catch (NumberFormatException e) {
                    sendMessage(sender, "<red>Invalid padding value!");
                }
                break;
            default:
                sendMessage(sender, "<yellow>Usage: /holo background <id> <enable|disable|color|size>");
                break;
        }
        return true;
    }

    private boolean handleBillboard(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo billboard <id> <none|vertical|horizontal|center>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        try {
            HoloDefinition.BillboardMode mode = HoloDefinition.BillboardMode.valueOf(args[2].toUpperCase());
            def.setBillboard(mode);
            holoManager.updateHologram(def);
            sendMessage(sender, plugin.getMessage("updated"));

            if (mode == HoloDefinition.BillboardMode.NONE) {
                sendMessage(sender, "<gray>Tip: Use /holo rotate <id> <yaw> [pitch] to set rotation");
            }
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "<red>Invalid billboard mode! Use: none, vertical, horizontal, or center");
        }
        return true;
    }

    private boolean handleRotate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo rotate <id> <yaw> [pitch]");
            sendMessage(sender, "<gray>Yaw (0-360): 0=South, 90=West, 180=North, 270=East");
            sendMessage(sender, "<gray>Pitch (-90 to 90): 0=Level, -90=Up, 90=Down");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        try {
            float yaw = Float.parseFloat(args[2]);
            float pitch = args.length >= 4 ? Float.parseFloat(args[3]) : 0.0f;

            Location loc = def.getLocation();
            loc.setYaw(yaw);
            loc.setPitch(pitch);
            def.setLocation(loc);

            holoManager.updateHologram(def);
            sendMessage(sender, "<green>✓ Rotation set: Yaw=" + yaw + "°, Pitch=" + pitch + "°");

            if (def.getBillboard() != HoloDefinition.BillboardMode.NONE) {
                sendMessage(sender, "<yellow>⚠ Billboard is not NONE - rotation may not be visible");
                sendMessage(sender, "<gray>Use /holo billboard <id> none to disable billboard");
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid number!");
        }
        return true;
    }

    private boolean handleSeeThrough(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo seethrough <id> <true|false>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        boolean seeThrough = Boolean.parseBoolean(args[2]);
        def.setSeeThroughBlocks(seeThrough);
        holoManager.updateHologram(def);
        sendMessage(sender, plugin.getMessage("updated"));
        return true;
    }

    private boolean handleViewRange(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo viewrange <id> <distance>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        try {
            double viewRange = Double.parseDouble(args[2]);
            def.setViewRange(viewRange);
            holoManager.updateHologram(def);
            sendMessage(sender, plugin.getMessage("updated"));
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid number!");
        }
        return true;
    }

    private boolean handlePlaceholders(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo placeholders <id> <true|false|refresh>");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        // Allow placeholders for TEXT and for text below ITEM/BLOCK holograms
        if (def.getType() != HoloType.TEXT && def.getType() != HoloType.ITEM && def.getType() != HoloType.BLOCK) {
            sendMessage(sender, "<red>Placeholders are only supported for text, item, and block holograms!");
            return true;
        }

        String action = args[2].toLowerCase();

        if (action.equals("refresh")) {
            if (args.length < 4) {
                sendMessage(sender, "<yellow>Usage: /holo placeholders <id> refresh <ticks>");
                return true;
            }
            try {
                int ticks = Integer.parseInt(args[3]);
                def.setPlaceholderRefreshTicks(ticks);
                holoManager.updateHologram(def);
                sendMessage(sender, plugin.getMessage("updated"));
            } catch (NumberFormatException e) {
                sendMessage(sender, "<red>Invalid number!");
            }
        } else {
            boolean enabled = Boolean.parseBoolean(action);
            def.setPlaceholdersEnabled(enabled);
            holoManager.updateHologram(def);

            if (def.getType() == HoloType.TEXT) {
                sendMessage(sender, plugin.getMessage("updated"));
            } else {
                sendMessage(sender, "<green>Placeholders " + (enabled ? "enabled" : "disabled") + " for text below " + def.getType().name().toLowerCase() + " hologram!");
            }
        }
        return true;
    }

    private boolean handleAnimate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo animate <id> <type> [speed] [radius]");
            sendMessage(sender, "<gray>Types: none, rotate, bounce, circle, spiral, shake");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        try {
            HoloDefinition.AnimationType type = HoloDefinition.AnimationType.valueOf(args[2].toUpperCase());
            def.setAnimationType(type);
            def.setAnimated(type != HoloDefinition.AnimationType.NONE);

            if (args.length > 3) {
                double speed = Double.parseDouble(args[3]);
                def.setAnimationSpeed(speed);
            }

            if (args.length > 4) {
                double radius = Double.parseDouble(args[4]);
                def.setAnimationRadius(radius);
            }

            holoManager.updateHologram(def);
            sendMessage(sender, "<green>Animation set to: <white>" + type.name().toLowerCase());
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "<red>Invalid animation type!");
            sendMessage(sender, "<gray>Types: none, rotate, bounce, circle, spiral, shake");
        }
        return true;
    }

    private boolean handleParticle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<yellow>Usage: /holo particle <id> <type|off> [count] [radius]");
            sendMessage(sender, "<gray>Examples: FLAME, HEART, VILLAGER_HAPPY, ENCHANTMENT_TABLE");
            return true;
        }

        String id = args[1];
        HoloDefinition def = holoManager.getDefinition(id);

        if (def == null) {
            sendMessage(sender, plugin.getMessage("not-found").replace("<id>", id));
            return true;
        }

        String particleType = args[2].toUpperCase();

        if (particleType.equals("OFF") || particleType.equals("NONE")) {
            def.setParticlesEnabled(false);
            holoManager.updateHologram(def);
            sendMessage(sender, "<green>Particles disabled");
            return true;
        }

        // Validate particle type
        try {
            org.bukkit.Particle.valueOf(particleType);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "<red>Invalid particle type!");
            sendMessage(sender, "<gray>Examples: FLAME, HEART, VILLAGER_HAPPY, ENCHANTMENT_TABLE");
            return true;
        }

        def.setParticleType(particleType);
        def.setParticlesEnabled(true);

        if (args.length > 3) {
            try {
                int count = Integer.parseInt(args[3]);
                def.setParticleCount(count);
            } catch (NumberFormatException e) {
                sendMessage(sender, "<red>Invalid particle count!");
                return true;
            }
        }

        if (args.length > 4) {
            try {
                double radius = Double.parseDouble(args[4]);
                def.setParticleRadius(radius);
            } catch (NumberFormatException e) {
                sendMessage(sender, "<red>Invalid particle radius!");
                return true;
            }
        }

        holoManager.updateHologram(def);
        sendMessage(sender, "<green>Particles enabled: <white>" + particleType.toLowerCase());
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.reloadMessages();
        holoManager.unloadAll();
        holoManager.loadAll();
        sendMessage(sender, plugin.getMessage("reloaded"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "<gradient:#00F8F8:#00F542><b>═══ GotCraftHolo Commands ═══</b></gradient>");
        sendMessage(sender, "<yellow>/holo create <id> <type> [material]</yellow> - Create hologram");
        sendMessage(sender, "<yellow>/holo remove <id></yellow> - Remove hologram");
        sendMessage(sender, "<yellow>/holo settext <id> <text></yellow> - Set text");
        sendMessage(sender, "<yellow>/holo addline <id> <text></yellow> - Add line to hologram");
        sendMessage(sender, "<yellow>/holo setline <id> <line#> <text></yellow> - Edit specific line");
        sendMessage(sender, "<yellow>/holo list</yellow> - List all holograms");
        sendMessage(sender, "<yellow>/holo tp <id></yellow> - Teleport to hologram");
        sendMessage(sender, "<yellow>/holo movehere <id></yellow> - Move hologram here");
        sendMessage(sender, "<yellow>/holo setpos <id> <x> <y> <z></yellow> - Set hologram position");
        sendMessage(sender, "<yellow>/holo near [distance]</yellow> - Show nearby holograms");
        sendMessage(sender, "<yellow>/holo scale <id> <x> <y> <z></yellow> - Set scale");
        sendMessage(sender, "<yellow>/holo billboard <id> <mode></yellow> - Set rotation mode");
        sendMessage(sender, "<yellow>/holo rotate <id> <yaw> [pitch]</yellow> - Rotate hologram (for billboard: none)");
        sendMessage(sender, "<yellow>/holo background <id> <action></yellow> - Configure background");
        sendMessage(sender, "<yellow>/holo seethrough <id> <true|false></yellow> - See through blocks");
        sendMessage(sender, "<yellow>/holo viewrange <id> <distance></yellow> - Set view range");
        sendMessage(sender, "<yellow>/holo animate <id> <type> [speed] [radius]</yellow> - Animate hologram");
        sendMessage(sender, "<yellow>/holo particle <id> <type|off> [count] [radius]</yellow> - Add particles");
        sendMessage(sender, "<yellow>/holo placeholders <id> <true|false></yellow> - Enable PlaceholderAPI");
        sendMessage(sender, "<yellow>/holo migrate</yellow> - Migrate from DecentHolograms");
        sendMessage(sender, "<yellow>/holo reload</yellow> - Reload all holograms");
    }

    private void sendMessage(CommandSender sender, String message) {
        String prefix = plugin.getMessage("prefix");
        Component component = MiniMessageUtil.parse(prefix + message);
        sender.sendMessage(component);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("gotcraftholo.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("create", "remove", "settext", "addline", "setline", "list", "tp", "movehere",
                               "setpos", "near", "scale", "shadow", "align", "opacity", "background",
                               "billboard", "rotate", "seethrough", "viewrange", "placeholders", "animate", "particle",
                               "migrate", "reload");
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("create")) {
                return Collections.emptyList();
            }
            // Return hologram IDs for most commands
            return holoManager.getAllDefinitions().stream()
                    .map(HoloDefinition::getId)
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "create":
                    return Arrays.asList("text", "item", "block", "leaderboard");
                case "shadow":
                case "seethrough":
                case "placeholders":
                    return Arrays.asList("true", "false");
                case "align":
                    return Arrays.asList("left", "center", "right");
                case "billboard":
                    return Arrays.asList("none", "vertical", "horizontal", "center");
                case "background":
                    return Arrays.asList("enable", "disable", "color", "size");
                case "animate":
                case "animation":
                    return Arrays.asList("none", "rotate", "bounce", "circle", "spiral", "shake");
                case "particle":
                case "particles":
                    return Arrays.asList("off", "FLAME", "HEART", "VILLAGER_HAPPY", "ENCHANTMENT_TABLE", "PORTAL", "END_ROD");
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            String type = args[2].toLowerCase();
            if (type.equals("item") || type.equals("block")) {
                return Arrays.stream(Material.values())
                        .map(Material::name)
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}

