package net.trduc.magicabilities.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.players.PowerPlayer.players;

public class Powersetaura implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("powersetaura")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;
        if (!players.containsKey(p)) {
            p.sendMessage(ChatColor.RED + "You don't have a power assigned.");
            return true;
        }

        boolean current = players.get(p).isAuraEnabled();
        boolean target;

        if (args.length == 0) {
            target = !current;
        } else {
            switch (args[0].toLowerCase()) {
                case "on":  target = true;  break;
                case "off": target = false; break;
                default:
                    p.sendMessage(ChatColor.RED + "Usage: /powersetaura [on|off]");
                    return true;
            }
        }

        if (target == current) {
            p.sendMessage(ChatColor.YELLOW + "Your aura is already "
                    + (current ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.YELLOW + ".");
            return true;
        }

        players.get(p).setAuraEnabled(target);
        getPlayerData(p).setAuraEnabled(target);

        if (target) {
            p.sendMessage(ChatColor.GREEN + "✦ Aura " + ChatColor.BOLD + "enabled" + ChatColor.RESET + ChatColor.GREEN + ".");
        } else {
            p.sendMessage(ChatColor.RED + "✦ Aura " + ChatColor.BOLD + "disabled" + ChatColor.RESET + ChatColor.RED + ". Your passive particle effects will no longer be displayed.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = Arrays.asList("on", "off");
            List<String> result = new ArrayList<>();
            for (String o : opts) {
                if (o.startsWith(args[0].toLowerCase())) result.add(o);
            }
            return result;
        }
        return new ArrayList<>();
    }
}
