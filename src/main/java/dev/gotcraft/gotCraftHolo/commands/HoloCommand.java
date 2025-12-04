package dev.gotcraft.gotCraftHolo.commands;

import dev.gotcraft.gotCraftHolo.GotCraftHolo;
import dev.gotcraft.gotCraftHolo.manager.HoloManager;
import dev.gotcraft.gotCraftHolo.model.HoloDefinition;
import dev.gotcraft.gotCraftHolo.model.HoloType;
import dev.gotcraft.gotCraftHolo.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            sendMessage(sender, "<yellow>Usage: /holo create <id> <text|item|block> [material]");
            return true;
        }

        Player player = (Player) sender;
        String id = args[1];
        String typeStr = args[2].toUpperCase();

        HoloType type;
        try {
            type = HoloType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "<red>Invalid type! Use: text, item, or block");
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
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "<red>Invalid billboard mode! Use: none, vertical, horizontal, or center");
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
        sendMessage(sender, "<yellow>/holo scale <id> <x> <y> <z></yellow> - Set scale");
        sendMessage(sender, "<yellow>/holo billboard <id> <mode></yellow> - Set rotation");
        sendMessage(sender, "<yellow>/holo background <id> <action></yellow> - Configure background");
        sendMessage(sender, "<yellow>/holo seethrough <id> <true|false></yellow> - See through blocks");
        sendMessage(sender, "<yellow>/holo viewrange <id> <distance></yellow> - Set view range");
        sendMessage(sender, "<yellow>/holo animate <id> <type> [speed] [radius]</yellow> - Animate hologram");
        sendMessage(sender, "<yellow>/holo particle <id> <type|off> [count] [radius]</yellow> - Add particles");
        sendMessage(sender, "<yellow>/holo placeholders <id> <true|false></yellow> - Enable PlaceholderAPI");
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
                               "scale", "shadow", "align", "opacity", "background",
                               "billboard", "seethrough", "viewrange", "placeholders", "animate", "particle", "reload");
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
                    return Arrays.asList("text", "item", "block");
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

