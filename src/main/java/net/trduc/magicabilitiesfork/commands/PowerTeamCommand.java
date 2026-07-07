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

public class PowerTeamCommand implements CommandExecutor, TabCompleter {
    private final DbManager db;

    public PowerTeamCommand(){
        this.db = MagicAbilitiesfork.magicPlugin.getDbManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("powerteam")) return false;
        if (args.length == 0){
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub){
            case "create":
                if (!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.RED + "Only players can create teams.");
                    return true;
                }
                if (args.length < 3){
                    sender.sendMessage(ChatColor.RED + "Usage: /powerteam create <name> <color>");
                    return true;
                }
                String teamName = args[1];
                String color = args[2];
                String owner = sender.getName();
                boolean ok = db.createPowerTeam(teamName, owner, color);
                if (!ok){
                    sender.sendMessage(ChatColor.RED + "Team already exists or creation failed.");
                    return true;
                }

                db.addPlayerToTeam(teamName, sender.getName());
                sender.sendMessage(ChatColor.GREEN + "Created team " + teamName + " and added you as member.");
                return true;
            case "add":
                if (args.length == 2 && sender instanceof Player){
                    String playerToAdd = args[1];
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){
                        sender.sendMessage(ChatColor.RED + "You are not in a team. Create one or specify a team name.");
                        return true;
                    }

                    PowerTeam myTeamObj = db.getPowerTeam(myTeam);
                    boolean isOwner = myTeamObj != null && myTeamObj.getOwner().equals(sender.getName());
                    if (isOwner || sender.hasPermission("magic.admin")){
                        boolean added = db.addPlayerToTeam(myTeam, playerToAdd);
                        sender.sendMessage(added ? ChatColor.GREEN + "Added " + playerToAdd + " to " + myTeam : ChatColor.RED + "Failed to add player (exists or team missing).");
                        return true;
                    } else {

                        boolean req = db.requestAddToTeam(myTeam, sender.getName(), playerToAdd);
                        sender.sendMessage(req ? ChatColor.YELLOW + "Request sent to team owner for approval." : ChatColor.RED + "Failed to send request or request already exists.");

                        if (req && myTeamObj != null){
                            String owner = myTeamObj.getOwner();
                            Player ownerP = Bukkit.getPlayer(owner);
                            if (ownerP != null){
                                ownerP.sendMessage(ChatColor.AQUA + "Approval request: " + sender.getName() + " wants to add " + playerToAdd + " to team " + myTeam + ". Use /powerteam requests to review.");
                                try{
                                    ownerP.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(ChatColor.YELLOW + "New team request for " + myTeam));
                                } catch (Exception ignored){}
                            }
                        }
                        return true;
                    }
                } else if (args.length == 3){
                    String team = args[1];
                    String playerToAdd = args[2];

                    PowerTeam pt = db.getPowerTeam(team);
                    String owner = pt == null ? "" : pt.getOwner();
                    boolean isCo = db.isCoowner(team, sender.getName());
                    if (sender.hasPermission("magic.admin") || sender.getName().equals(owner) || isCo){
                        boolean added = db.addPlayerToTeam(team, playerToAdd);
                        sender.sendMessage(added ? ChatColor.GREEN + "Added " + playerToAdd + " to " + team : ChatColor.RED + "Failed to add player (exists or team missing).");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only the team owner, co-owner or admins can add players directly. Use request flow.");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /powerteam add <player>   (requests adding to your team)\nOr: /powerteam add <team> <player> (owner/admin/co-owner)");
                    return true;
                }
            case "invite":
                if (args.length == 2 && sender instanceof Player){
                    String target = args[1];
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){ sender.sendMessage(ChatColor.RED + "You are not in a team. Use /powerteam invite <team> <player> as admin."); return true; }
                    PowerTeam myTeamObj = db.getPowerTeam(myTeam);
                    boolean isOwner = myTeamObj != null && myTeamObj.getOwner().equals(sender.getName());
                    boolean isCo = db.isCoowner(myTeam, sender.getName());
                    if (!(isOwner || isCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(ChatColor.RED + "Only owner, co-owner, or admins can invite."); return true; }
                    boolean ok = db.createInvite(myTeam, sender.getName(), target);
                    sender.sendMessage(ok ? ChatColor.GREEN + "Invite sent to " + target : ChatColor.RED + "Invite failed or already exists.");
                    Player tp = Bukkit.getPlayer(target);
                    if (tp!=null){
                        tp.sendMessage(ChatColor.AQUA + "You were invited to join team " + myTeam + " by " + sender.getName() + ". Use /powerteam accept " + myTeam + " or /powerteam decline " + myTeam);
                        try{ tp.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(ChatColor.YELLOW + "Invite to team " + myTeam)); } catch (Exception ignored){}
                    }
                    return true;
                } else if (args.length == 3){
                    String team = args[1]; String target = args[2];
                    PowerTeam pt = db.getPowerTeam(team);
                    String owner = pt==null?"":pt.getOwner();
                    boolean isCo = db.isCoowner(team, sender.getName());
                    if (!(sender.hasPermission("magic.admin") || sender.getName().equals(owner) || isCo)){ sender.sendMessage(ChatColor.RED + "Only owner, co-owner, or admins can invite to that team."); return true; }
                    boolean ok = db.createInvite(team, sender.getName(), target);
                    sender.sendMessage(ok ? ChatColor.GREEN + "Invite sent to " + target : ChatColor.RED + "Invite failed or already exists.");
                    Player tp = Bukkit.getPlayer(target);
                    if (tp!=null){ tp.sendMessage(ChatColor.AQUA + "You were invited to join team " + team + " by " + sender.getName() + ". Use /powerteam accept " + team + " or /powerteam decline " + team); }
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Usage: /powerteam invite <player> (invite to your team)\nOr: /powerteam invite <team> <player> (owner/admin)");
                return true;
            case "accept":
                if (!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED + "Only players can accept invites."); return true; }
                if (args.length < 2){ sender.sendMessage(ChatColor.RED + "Usage: /powerteam accept <team>"); return true; }
                String teamToAccept = args[1];
                boolean okAccept = db.acceptInvite(teamToAccept, sender.getName());
                sender.sendMessage(okAccept ? ChatColor.GREEN + "You joined " + teamToAccept : ChatColor.RED + "Accept failed or no invite.");
                return true;
            case "decline":
                if (!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED + "Only players can decline invites."); return true; }
                if (args.length < 2){ sender.sendMessage(ChatColor.RED + "Usage: /powerteam decline <team>"); return true; }
                String teamToDecline = args[1];
                boolean okDecline = db.denyInvite(teamToDecline, sender.getName());
                sender.sendMessage(okDecline ? ChatColor.GREEN + "Invite declined." : ChatColor.RED + "Decline failed or no invite.");
                return true;
            case "remove":
                if (args.length == 2 && sender instanceof Player){
                    String playerToRemove = args[1];
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){
                        sender.sendMessage(ChatColor.RED + "You are not in a team.");
                        return true;
                    }
                    PowerTeam myTeamObj = db.getPowerTeam(myTeam);
                    boolean isOwner = myTeamObj != null && myTeamObj.getOwner().equals(sender.getName());
                    if (isOwner || sender.hasPermission("magic.admin")){
                        boolean rem = db.removePlayerFromTeam(myTeam, playerToRemove);
                        sender.sendMessage(rem ? ChatColor.GREEN + "Removed " + playerToRemove + " from " + myTeam : ChatColor.RED + "Failed to remove player.");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only the team owner or admins can remove members.");
                        return true;
                    }
                } else if (args.length == 3){
                    String team = args[1];
                    String playerToRemove = args[2];
                    PowerTeam pt = db.getPowerTeam(team);
                    String owner = pt == null ? "" : pt.getOwner();
                    if (sender.hasPermission("magic.admin") || sender.getName().equals(owner)){
                        boolean rem = db.removePlayerFromTeam(team, playerToRemove);
                        sender.sendMessage(rem ? ChatColor.GREEN + "Removed " + playerToRemove + " from " + team : ChatColor.RED + "Failed to remove player.");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only the team owner or admins can remove members.");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /powerteam remove <player>   (removes from your team)\nOr: /powerteam remove <team> <player> (owner/admin)");
                    return true;
                }
            case "list":

                if (args.length == 1 && sender instanceof Player){
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){

                        List<String> teams = db.listPowerTeams();
                        sender.sendMessage(ChatColor.YELLOW + "Available teams:");
                        for (String t : teams){
                            PowerTeam pt = db.getPowerTeam(t);
                            sender.sendMessage(ChatColor.AQUA + t + ChatColor.GRAY + " (" + (pt==null?0:pt.getMembers().size()) + " members)");
                        }
                        return true;
                    } else {
                        PowerTeam pt = db.getPowerTeam(myTeam);
                        sender.sendMessage(ChatColor.GREEN + "Team " + myTeam + " members:");
                        for (String m : pt.getMembers()){
                            sender.sendMessage(ChatColor.AQUA + m);
                        }
                        return true;
                    }
                } else if (args.length == 2){
                    String team = args[1];
                    PowerTeam pt = db.getPowerTeam(team);
                    if (pt == null){
                        sender.sendMessage(ChatColor.RED + "Team not found: " + team);
                        return true;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Team " + team + " (owner: " + pt.getOwner() + ") members:");
                    for (String m : pt.getMembers()){
                        sender.sendMessage(ChatColor.AQUA + m);
                    }
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /powerteam list [team]");
                    return true;
                }
            case "requests":
            case "invites":
                if (sub.equals("requests")){
                    if (!(sender instanceof Player)){
                        sender.sendMessage(ChatColor.RED + "Only players can view requests.");
                        return true;
                    }
                    String myTeam2 = db.getPlayerTeam(sender.getName());
                    if (myTeam2 == null){ sender.sendMessage(ChatColor.RED + "You are not in a team."); return true; }
                    PowerTeam teamObj2 = db.getPowerTeam(myTeam2);
                    boolean viewIsCo = db.isCoowner(myTeam2, sender.getName());
                    if (teamObj2 == null || !(teamObj2.getOwner().equals(sender.getName()) || viewIsCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(ChatColor.RED + "Only team owner, co-owner, or admins can view requests."); return true; }
                    java.util.List<net.trduc.magicabilitiesfork.data.PowerteamRequest> reqs = db.listRequestsForTeam(myTeam2);
                    if (reqs.isEmpty()){ sender.sendMessage(ChatColor.YELLOW + "No pending requests."); return true; }

                    Player ownerP = (Player) sender;
                    if (MagicAbilitiesfork.magicPlugin != null && MagicAbilitiesfork.magicPlugin.powerTeamGui != null){
                        MagicAbilitiesfork.magicPlugin.powerTeamGui.openRequestsGui(ownerP, myTeam2);
                    } else {

                        java.util.List<net.trduc.magicabilitiesfork.data.PowerteamRequest> reqs2 = db.listRequestsForTeam(myTeam2);
                        sender.sendMessage(ChatColor.GREEN + "Pending requests for " + myTeam2 + ":");
                        for (net.trduc.magicabilitiesfork.data.PowerteamRequest r : reqs2){
                            sender.sendMessage(ChatColor.AQUA + r.getRequester() + " -> " + r.getTarget() + " (" + new java.util.Date(r.getTs()) + ")");
                        }
                    }
                    return true;
                } else {

                    if (!(sender instanceof Player)){
                        sender.sendMessage(ChatColor.RED + "Only players can view invites.");
                        return true;
                    }
                    String playerName = sender.getName();
                    java.util.List<net.trduc.magicabilitiesfork.data.PowerteamRequest> inv = db.listInvitesForPlayer(playerName);
                    if (inv.isEmpty()){ sender.sendMessage(ChatColor.YELLOW + "You have no invites."); return true; }
                    sender.sendMessage(ChatColor.GREEN + "Your invites:");
                    for (net.trduc.magicabilitiesfork.data.PowerteamRequest r : inv){
                        sender.sendMessage(ChatColor.AQUA + r.getTeamName() + " invited by " + r.getRequester() + " (" + new java.util.Date(r.getTs()) + ")");
                    }
                    return true;
                }
            case "approve":
                if (args.length < 2){ sender.sendMessage(ChatColor.RED + "Usage: /powerteam approve <player>"); return true; }
                if (!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED + "Only players can approve."); return true; }
                String approverTeam = db.getPlayerTeam(sender.getName());
                if (approverTeam == null){ sender.sendMessage(ChatColor.RED + "You are not in a team."); return true; }
                PowerTeam approverTeamObj = db.getPowerTeam(approverTeam);
                boolean approverIsCo = db.isCoowner(approverTeam, sender.getName());
                if (approverTeamObj == null || !(approverTeamObj.getOwner().equals(sender.getName()) || approverIsCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(ChatColor.RED + "Only team owner, co-owner, or admins can approve requests."); return true; }
                String target = args[1];
                boolean okApprove = db.approveRequest(approverTeam, target, sender.getName());
                sender.sendMessage(okApprove ? ChatColor.GREEN + "Approved and added " + target : ChatColor.RED + "Approve failed.");
                Player tp = Bukkit.getPlayer(target);
                if (tp!=null){ tp.sendMessage(ChatColor.GREEN + "You were added to team " + approverTeam + " by owner."); }
                return true;
            case "deny":
                if (args.length < 2){ sender.sendMessage(ChatColor.RED + "Usage: /powerteam deny <player>"); return true; }
                if (!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED + "Only players can deny."); return true; }
                String denyTeam = db.getPlayerTeam(sender.getName());
                if (denyTeam == null){ sender.sendMessage(ChatColor.RED + "You are not in a team."); return true; }
                PowerTeam denyTeamObj = db.getPowerTeam(denyTeam);
                boolean denyIsCo = db.isCoowner(denyTeam, sender.getName());
                if (denyTeamObj == null || !(denyTeamObj.getOwner().equals(sender.getName()) || denyIsCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(ChatColor.RED + "Only team owner, co-owner, or admins can deny requests."); return true; }
                String denyTarget = args[1];
                boolean okDeny = db.denyRequest(denyTeam, denyTarget, sender.getName());
                sender.sendMessage(okDeny ? ChatColor.GREEN + "Denied request for " + denyTarget : ChatColor.RED + "Deny failed or no such request.");
                Player dt = Bukkit.getPlayer(denyTarget);
                if (dt!=null){ dt.sendMessage(ChatColor.RED + "Your join request to team " + denyTeam + " was denied."); }
                return true;
            case "info":
                if (args.length < 2){
                    sender.sendMessage(ChatColor.RED + "Usage: /powerteam info <team>");
                    return true;
                }
                PowerTeam infoTeam = db.getPowerTeam(args[1]);
                if (infoTeam == null){
                    sender.sendMessage(ChatColor.RED + "Team not found.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Team: " + infoTeam.getName() + " (owner: " + infoTeam.getOwner() + ", color: " + infoTeam.getColor() + ")");
                sender.sendMessage(ChatColor.GREEN + "Members:");
                for (String m : infoTeam.getMembers()){
                    sender.sendMessage(ChatColor.AQUA + m);
                }
                return true;
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender){
        sender.sendMessage(ChatColor.YELLOW + "PowerTeam commands:");
        sender.sendMessage(ChatColor.AQUA + "/powerteam create <name> <color>" + ChatColor.GRAY + " - create a team and add yourself");
        sender.sendMessage(ChatColor.AQUA + "/powerteam add <player>" + ChatColor.GRAY + " - add player to your team");
        sender.sendMessage(ChatColor.AQUA + "/powerteam remove <player>" + ChatColor.GRAY + " - remove player from your team");
        sender.sendMessage(ChatColor.AQUA + "/powerteam list [team]" + ChatColor.GRAY + " - list teams or members");
        sender.sendMessage(ChatColor.AQUA + "/powerteam info <team>" + ChatColor.GRAY + " - show team info");
        sender.sendMessage(ChatColor.AQUA + "/powerteam invite <player>" + ChatColor.GRAY + " - invite a player to your team");
        sender.sendMessage(ChatColor.AQUA + "/powerteam accept <team>" + ChatColor.GRAY + " - accept an invite");
        sender.sendMessage(ChatColor.AQUA + "/powerteam decline <team>" + ChatColor.GRAY + " - decline an invite");
        sender.sendMessage(ChatColor.AQUA + "/powerteam requests" + ChatColor.GRAY + " - view pending join requests (owner/co-owner)");
        sender.sendMessage(ChatColor.AQUA + "/powerteam invites" + ChatColor.GRAY + " - view invites sent to you");
        sender.sendMessage(ChatColor.AQUA + "/powerteam approve <player>" + ChatColor.GRAY + " - approve a join request");
        sender.sendMessage(ChatColor.AQUA + "/powerteam deny <player>" + ChatColor.GRAY + " - deny a join request");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> comp = new ArrayList<>();
        if (args.length == 1){
            for (String s : new String[]{"create","add","remove","list","info","invite","accept","decline","requests","invites","approve","deny"}){
                if (s.startsWith(args[0].toLowerCase())) comp.add(s);
            }
            return comp;
        }
        if (args.length == 2){
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove")){

                if (sender instanceof Player && args.length == 2){
                    for (Player p : Bukkit.getOnlinePlayers()){
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) comp.add(p.getName());
                    }
                    return comp;
                }

                List<String> teams = db.listPowerTeams();
                for (String t : teams){ if (t.toLowerCase().startsWith(args[1].toLowerCase())) comp.add(t); }
                return comp;
            }
            if (sub.equals("list") || sub.equals("info") || sub.equals("accept") || sub.equals("decline")){
                List<String> teams = db.listPowerTeams();
                for (String t : teams){ if (t.toLowerCase().startsWith(args[1].toLowerCase())) comp.add(t); }
                return comp;
            }
            if (sub.equals("invite") || sub.equals("approve") || sub.equals("deny")){
                for (Player p : Bukkit.getOnlinePlayers()){
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) comp.add(p.getName());
                }
                return comp;
            }
        }
        if (args.length == 3){
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove")){
                for (Player p : Bukkit.getOnlinePlayers()){
                    if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) comp.add(p.getName());
                }
                return comp;
            }
        }
        return comp;
    }
}

