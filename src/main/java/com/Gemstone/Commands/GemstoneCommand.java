package com.Gemstone.Commands;

import com.Gemstone.Items.GemGenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GemstoneCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        // 🌍 Check if command is allowed in this world
        if (!((com.Gemstone.Main) player.getServer().getPluginManager().getPlugin("RisersGemstone"))
                .isWorldEnabled(player.getWorld().getName())) {
            player.sendMessage(ChatColor.RED + "Gemstone commands are not enabled in this world.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        if (args.length >= 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            String gemName = args[2];
            ItemStack gem = getGemByName(gemName.toLowerCase());

            if (gem == null) {
                player.sendMessage(ChatColor.RED + "Unknown gemstone: " + gemName);
                return true;
            }

            String displayName = gem.getItemMeta() != null ? gem.getItemMeta().getDisplayName() : "Gem";

            if (args[0].equalsIgnoreCase("give")) {
                target.getInventory().addItem(gem);
                target.sendMessage(ChatColor.GREEN + "You have received a " + displayName + "!");
                player.sendMessage(ChatColor.GREEN + "Gave " + displayName + " to " + target.getName());
            } else {
                boolean removed = false;
                for (ItemStack item : target.getInventory()) {
                    if (item != null && item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && displayName.equals(meta.getDisplayName())) {
                            target.getInventory().removeItem(item);
                            removed = true;
                            break;
                        }
                    }
                }

                if (removed) {
                    target.sendMessage(ChatColor.RED + "Your " + displayName + " has been taken!");
                    player.sendMessage(ChatColor.GREEN + "Removed " + displayName + " from " + target.getName());
                } else {
                    player.sendMessage(ChatColor.RED + target.getName() + " does not have that gem!");
                }
            }

            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /gemstone give <player> <gem> OR /gemstone take <player> <gem>");
        return true;
    }

    private ItemStack getGemByName(String gemName) {
        return switch (gemName) {
            case "strength" -> GemGenerator.createGem("Strength Gem", ChatColor.RED, 11003);
            case "fire"     -> GemGenerator.createGem("Fire Gem", ChatColor.GOLD, 11002);
            case "speed"    -> GemGenerator.createGem("Speed Gem", ChatColor.GREEN, 11004);
            case "spirit"   -> GemGenerator.createGem("Spirit Gem", ChatColor.GRAY, 11005);
            case "luck"     -> GemGenerator.createGem("Luck Gem", ChatColor.YELLOW, 11006);
            case "curse"    -> GemGenerator.createGem("Curse Gem", ChatColor.DARK_RED, 11007);
            case "healing"  -> GemGenerator.createGem("Healing Gem", ChatColor.LIGHT_PURPLE, 11008);
            case "air"      -> GemGenerator.createGem("Air Gem", ChatColor.WHITE, 11001);
            case "shock"    -> GemGenerator.createGem("Shock Gem", ChatColor.AQUA, 12002);
            case "water"    -> GemGenerator.createGem("Water Gem", ChatColor.BLUE, 12001);
            default         -> null;
        };
    }
}