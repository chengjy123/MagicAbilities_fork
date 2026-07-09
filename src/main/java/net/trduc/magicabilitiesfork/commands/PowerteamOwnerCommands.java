package net.trduc.magicabilitiesfork.commands;

import net.trduc.magicabilitiesfork.MagicAbilitiesfork;
import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.MessagesManager;
import net.trduc.magicabilitiesfork.data.PowerTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PowerteamOwnerCommands implements CommandExecutor, TabCompleter {
    private final DbManager db;
    private final MessagesManager messages = MessagesManager.getInstance();
    public PowerteamOwnerCommands(){ this.db = MagicAbilitiesfork.magicPlugin.getDbManager(); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("powerteamowner")) return false;
        if (args.length == 0){ sender.sendMessage(messages.get("commands.powerteamowner.help")); return true; }
        String sub = args[0].toLowerCase();
        switch (sub){
            case "transfer":
                if (!(sender instanceof Player)){ sender.sendMessage(messages.get("commands.powerteamowner.only_player", "action", "转移所有权")); return true; }
                if (args.length<3){ sender.sendMessage(messages.get("commands.powerteamowner.transfer_usage")); return true; }
                String team = args[1]; String newOwner = args[2];
                PowerTeam pt = db.getPowerTeam(team);
                if (pt==null){ sender.sendMessage(messages.get("commands.powerteamowner.not_found")); return true; }
                if (!pt.getOwner().equals(sender.getName()) && !sender.hasPermission("magic.admin")){ sender.sendMessage(messages.get("commands.powerteamowner.no_permission", "action", "转移所有权")); return true; }
                boolean ok = db.transferOwner(team, newOwner);
                sender.sendMessage(ok ? messages.get("commands.powerteamowner.transferred", "player", newOwner) : messages.get("commands.powerteamowner.transfer_failed"));
                return true;
            case "disband":
                if (!(sender instanceof Player)){ sender.sendMessage(messages.get("commands.powerteamowner.only_player", "action", "解散队伍")); return true; }
                if (args.length<2){ sender.sendMessage(messages.get("commands.powerteamowner.disband_usage")); return true; }
                String teamD = args[1]; PowerTeam ptD = db.getPowerTeam(teamD);
                if (ptD==null){ sender.sendMessage(messages.get("commands.powerteamowner.not_found")); return true; }
                if (!ptD.getOwner().equals(sender.getName()) && !sender.hasPermission("magic.admin")){ sender.sendMessage(messages.get("commands.powerteamowner.no_permission", "action", "解散队伍")); return true; }
                boolean okD = db.deletePowerTeam(teamD);
                sender.sendMessage(okD ? messages.get("commands.powerteamowner.disbanded") : messages.get("commands.powerteamowner.disband_failed"));
                return true;
            case "leave":
                if (!(sender instanceof Player)){ sender.sendMessage(messages.get("commands.powerteamowner.only_player", "action", "离开队伍")); return true; }
                String player = sender.getName(); String myTeam = db.getPlayerTeam(player);
                if (myTeam==null){ sender.sendMessage(messages.get("commands.powerteamowner.leave_not_in_team")); return true; }
                PowerTeam myPt = db.getPowerTeam(myTeam);
                if (myPt.getOwner().equals(player)){ sender.sendMessage(messages.get("commands.powerteamowner.owner_must_transfer")); return true; }
                boolean rem = db.removePlayerFromTeam(myTeam, player);
                sender.sendMessage(rem ? messages.get("commands.powerteamowner.left") : messages.get("commands.powerteamowner.leave_failed"));
                return true;
            case "coowner":
                if (args.length<3){ sender.sendMessage(messages.get("commands.powerteamowner.coowner_usage")); return true; }
                String op = args[1]; String target = args[2]; String my = db.getPlayerTeam(sender.getName());
                if (my==null){ sender.sendMessage(messages.get("commands.powerteamowner.leave_not_in_team")); return true; }
                PowerTeam myTeamObj = db.getPowerTeam(my);
                if (myTeamObj==null || !myTeamObj.getOwner().equals(sender.getName())){ sender.sendMessage(messages.get("commands.powerteamowner.no_permission_coowner")); return true; }
                if (op.equalsIgnoreCase("add")){
                    boolean okc = db.addCoowner(my, target);
                    sender.sendMessage(okc ? messages.get("commands.powerteamowner.coowner_added") : messages.get("commands.powerteamowner.coowner_add_failed"));
                    return true;
                } else if (op.equalsIgnoreCase("remove")){
                    boolean okc = db.removeCoowner(my, target);
                    sender.sendMessage(okc ? messages.get("commands.powerteamowner.coowner_removed") : messages.get("commands.powerteamowner.coowner_remove_failed"));
                    return true;
                } else { sender.sendMessage(messages.get("commands.powerteamowner.unknown_action")); return true; }
            default: sender.sendMessage(messages.get("commands.powerteamowner.unknown_command")); return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return new ArrayList<>();
    }
}