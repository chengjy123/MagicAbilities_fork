package net.trduc.magicabilitiesfork.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.data.PlayerData.savePlayerDataToDb;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class Powerset implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("powerset")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令。");
            return true;
        }

        Player p = (Player) sender;
        if (!players.containsKey(p)) {
            p.sendMessage(ChatColor.RED + "您没有被分配技能。");
            return true;
        }

        boolean current = players.get(p).getPower().isEnabled();
        boolean target;

        if (args.length == 0) {
            target = !current;
        } else {
            switch (args[0].toLowerCase()) {
                case "on":  target = true;  break;
                case "off": target = false; break;
                default:
                    p.sendMessage(ChatColor.RED + "用法: /powerset [on|off]");
                    return true;
            }
        }

        if (target == current) {
            p.sendMessage(ChatColor.YELLOW + "您的技能已经是"
                    + (current ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + ChatColor.YELLOW + "状态。");
            return true;
        }

        players.get(p).getPower().setEnabled(target);
        getPlayerData(p).setEnabled(target);
        savePlayerDataToDb(p, magicPlugin.getDbManager());

        if (target) {
            p.sendMessage(ChatColor.GREEN + "✦ 技能已" + ChatColor.BOLD + "开启" + ChatColor.RESET + ChatColor.GREEN + "。");
        } else {
            p.sendMessage(ChatColor.RED + "✦ 技能已" + ChatColor.BOLD + "关闭" + ChatColor.RESET + ChatColor.RED + "。您将不会发出被动效果或触发技能。");
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