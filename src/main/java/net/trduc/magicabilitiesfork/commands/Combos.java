package net.trduc.magicabilitiesfork.commands;

import net.trduc.magicabilitiesfork.data.PlayerData;
import net.trduc.magicabilitiesfork.misc.ComboRegistry;
import net.trduc.magicabilitiesfork.powers.PowerType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class Combos implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("combos")) return false;
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        if (!players.containsKey(p)) {
            p.sendMessage(ChatColor.RED + "Something went wrong!");
            return true;
        }

        PowerType typeToShow;
        if (args.length == 0) {
            typeToShow = PlayerData.getPlayerData(p).getPower();
        } else {
            try {
                typeToShow = PowerType.valueOf(args[0].toUpperCase());
            } catch (Exception e) {
                p.sendMessage(ChatColor.RED + "Unknown power: " + args[0]);
                return true;
            }
        }

        List<String> combos = ComboRegistry.getCombosFor(typeToShow);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7---- &aCombos for &e" + typeToShow.name() + " &7----"));
        for (String line : combos) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7" + line));
        }
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7-------------------------------"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("combos")) return null;
        if (args.length == 1) {
            ArrayList<String> comp = new ArrayList<>();
            for (PowerType pt : PowerType.values()){
                if (pt.name().toLowerCase().startsWith(args[0].toLowerCase())){
                    comp.add(pt.name());
                }
            }
            return comp;
        }
        return Arrays.asList();
    }
}

