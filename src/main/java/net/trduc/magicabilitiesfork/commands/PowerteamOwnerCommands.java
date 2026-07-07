package net.trduc.magicabilitiesfork.commands;

import net.trduc.magicabilitiesfork.MagicAbilitiesfork;
import net.trduc.magicabilitiesfork.data.DbManager;
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
    public PowerteamOwnerCommands(){ this.db = MagicAbilitiesfork.magicPlugin.getDbManager(); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("powerteamowner")) return false;
        if (args.length == 0){ sender.sendMessage(ChatColor.YELLOW + "Owner commands: transfer/disband/leave/coowner"); return true; }
        String sub = args[0].toLowerCase();
        switch (sub){
            case "transfer":
                if (!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"Only players can transfer."); return true; }
                if (args.length<3){ sender.sendMessage(ChatColor.RED+"Usage: /powerteamowner transfer <team> <newOwner>"); return true; }
                String team = args[1]; String newOwner = args[2];
                PowerTeam pt = db.getPowerTeam(team);
                if (pt==null){ sender.sendMessage(ChatColor.RED+"Team not found."); return true; }
                if (!pt.getOwner().equals(sender.getName()) && !sender.hasPermission("magic.admin")){ sender.sendMessage(ChatColor.RED+"Only owner can transfer."); return true; }
                boolean ok = db.transferOwner(team, newOwner);
                sender.sendMessage(ok ? ChatColor.GREEN+"Transferred ownership to " + newOwner : ChatColor.RED+"Transfer failed.");
                return true;
            case "disband":
                if (!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"Only players can disband."); return true; }
                if (args.length<2){ sender.sendMessage(ChatColor.RED+"Usage: /powerteamowner disband <team>"); return true; }
                String teamD = args[1]; PowerTeam ptD = db.getPowerTeam(teamD);
                if (ptD==null){ sender.sendMessage(ChatColor.RED+"Team not found."); return true; }
                if (!ptD.getOwner().equals(sender.getName()) && !sender.hasPermission("magic.admin")){ sender.sendMessage(ChatColor.RED+"Only owner can disband."); return true; }
                boolean okD = db.deletePowerTeam(teamD);
                sender.sendMessage(okD ? ChatColor.GREEN+"Team disbanded." : ChatColor.RED+"Disband failed.");
                return true;
            case "leave":
                if (!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"Only players can leave."); return true; }
                String player = sender.getName(); String myTeam = db.getPlayerTeam(player);
                if (myTeam==null){ sender.sendMessage(ChatColor.RED+"You are not in a team."); return true; }
                PowerTeam myPt = db.getPowerTeam(myTeam);
                if (myPt.getOwner().equals(player)){ sender.sendMessage(ChatColor.RED+"Owner must transfer ownership or disband the team before leaving."); return true; }
                boolean rem = db.removePlayerFromTeam(myTeam, player);
                sender.sendMessage(rem ? ChatColor.GREEN+"You left the team." : ChatColor.RED+"Leave failed.");
                return true;
            case "coowner":
                if (args.length<3){ sender.sendMessage(ChatColor.RED+"Usage: /powerteamowner coowner <add|remove> <player>"); return true; }
                String op = args[1]; String target = args[2]; String my = db.getPlayerTeam(sender.getName());
                if (my==null){ sender.sendMessage(ChatColor.RED+"You are not in a team."); return true; }
                PowerTeam myTeamObj = db.getPowerTeam(my);
                if (myTeamObj==null || !myTeamObj.getOwner().equals(sender.getName())){ sender.sendMessage(ChatColor.RED+"Only owner can manage co-owners."); return true; }
                if (op.equalsIgnoreCase("add")){
                    boolean okc = db.addCoowner(my, target);
                    sender.sendMessage(okc ? ChatColor.GREEN+"Added co-owner." : ChatColor.RED+"Failed to add co-owner.");
                    return true;
                } else if (op.equalsIgnoreCase("remove")){
                    boolean okc = db.removeCoowner(my, target);
                    sender.sendMessage(okc ? ChatColor.GREEN+"Removed co-owner." : ChatColor.RED+"Failed to remove co-owner.");
                    return true;
                } else { sender.sendMessage(ChatColor.RED+"Unknown op."); return true; }
            default: sender.sendMessage(ChatColor.RED+"Unknown owner command."); return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return new ArrayList<>();
    }
}
