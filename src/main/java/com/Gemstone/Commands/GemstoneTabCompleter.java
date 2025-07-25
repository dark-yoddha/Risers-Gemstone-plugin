package com.Gemstone.Commands;

import java.util.*;
import org.bukkit.command.*;

public class GemstoneTabCompleter implements TabCompleter {
    private final List<String> gems = Arrays.asList("strength", "fire", "speed", "spirit", "luck", "curse", "healing", "air", "shock", "water");

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "take");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            return null; // Let Bukkit handle player name suggestions
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            return gems;
        }

        return Collections.emptyList();
    }
}