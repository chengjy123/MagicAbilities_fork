package net.trduc.magicabilities.commands;

import net.trduc.magicabilities.data.PlayerData;
import net.trduc.magicabilities.powers.PowerType;
import net.trduc.magicabilities.powers.RandomPowerAssigner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static net.trduc.magicabilities.players.PowerPlayer.players;

public class Setpower implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("setpower")) return false;
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setpower <player|@a> <power>");
            return true;
        }

        final boolean isRandom = args[1].equalsIgnoreCase("random");
        PowerType powerType = null;
        if (!isRandom) {
            try {
                powerType = PowerType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Unknown power: " + args[1]
                        + ". Use 'random' or a valid power name.");
                return true;
            }
        }
        List<Player> targets = new ArrayList<>();
        if (args[0].equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online: " + args[0]);
                return true;
            }
            targets.add(target);
        }
        int success = 0;
        for (Player target : targets) {
            if (!players.containsKey(target)) {
                sender.sendMessage(ChatColor.YELLOW + "Warning: " + target.getName() + " has no PowerPlayer entry, skipping.");
                continue;
            }
            try {
                PowerType finalType = powerType;
                if (isRandom) {
                    PlayerData pd =
                            PlayerData.getPlayerData(target);
                    if (pd.getPower() != PowerType.NONE) {
                        sender.sendMessage(ChatColor.YELLOW + target.getName()
                                + " already has a power (" + pd.getPower().name().toLowerCase()
                                + ") — skipped.");
                        continue;
                    }
                    finalType = RandomPowerAssigner.randomPower();
                    target.sendMessage(ChatColor.GOLD + "✦ You have received a random power: "
                            + ChatColor.YELLOW + ChatColor.BOLD + finalType.name().replace('_', ' '));
                }
                players.get(target).changePower(finalType);
                success++;
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Failed for " + target.getName() + ": " + e.getMessage());
            }
        }
        sender.sendMessage(success > 0
                ? ChatColor.GREEN + "Set power " + (isRandom ? "random" : powerType.name().toLowerCase()) + " for " + success + " player(s)."
                : ChatColor.RED + "No players were updated.");
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("setpower")) return null;
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            addMatch("@a", args[0], out);
            for (Player p : Bukkit.getOnlinePlayers()) addMatch(p.getName(), args[0], out);
        } else if (args.length == 2) {
            for (PowerType pt : PowerType.values()) addMatch(pt.name().toLowerCase(), args[1], out);
        }
        return out;
    }

    private void addMatch(String val, String input, List<String> list) {
        if (val.regionMatches(true, 0, input, 0, input.length()) || input.isEmpty()) list.add(val);
    }
}
