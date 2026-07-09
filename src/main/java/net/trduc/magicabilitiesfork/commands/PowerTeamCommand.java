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

public class PowerTeamCommand implements CommandExecutor, TabCompleter {
    private final DbManager db;
    private final MessagesManager messages = MessagesManager.getInstance();

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
                    sender.sendMessage(messages.get("commands.powerteam.only_player_create"));
                    return true;
                }
                if (args.length < 3){
                    sender.sendMessage(messages.get("commands.powerteam.create_usage"));
                    return true;
                }
                String teamName = args[1];
                String color = args[2];
                String owner = sender.getName();
                boolean ok = db.createPowerTeam(teamName, owner, color);
                if (!ok){
                    sender.sendMessage(messages.get("commands.powerteam.already_exists"));
                    return true;
                }

                db.addPlayerToTeam(teamName, sender.getName());
                sender.sendMessage(messages.get("commands.powerteam.created", "team", teamName));
                return true;
            case "add":
                if (args.length == 2 && sender instanceof Player){
                    String playerToAdd = args[1];
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){
                        sender.sendMessage(messages.get("commands.powerteam.not_in_team"));
                        return true;
                    }

                    PowerTeam myTeamObj = db.getPowerTeam(myTeam);
                    boolean isOwner = myTeamObj != null && myTeamObj.getOwner().equals(sender.getName());
                    if (isOwner || sender.hasPermission("magic.admin")){
                        boolean added = db.addPlayerToTeam(myTeam, playerToAdd);
                        sender.sendMessage(added ? messages.get("commands.powerteam.added", "player", playerToAdd, "team", myTeam) : messages.get("commands.powerteam.add_failed"));
                        return true;
                    } else {

                        boolean req = db.requestAddToTeam(myTeam, sender.getName(), playerToAdd);
                        sender.sendMessage(req ? messages.get("commands.powerteam.request_sent") : messages.get("commands.powerteam.request_failed"));

                        if (req && myTeamObj != null){
                            String owner2 = myTeamObj.getOwner();
                            Player ownerP = Bukkit.getPlayer(owner2);
                            if (ownerP != null){
                                ownerP.sendMessage(messages.get("commands.powerteam.request_notify", "sender", sender.getName(), "player", playerToAdd, "team", myTeam));
                                try{
                                    ownerP.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(messages.get("commands.powerteam.request_actionbar", "team", myTeam)));
                                } catch (Exception ignored){}
                            }
                        }
                        return true;
                    }
                } else if (args.length == 3){
                    String team = args[1];
                    String playerToAdd = args[2];

                    PowerTeam pt = db.getPowerTeam(team);
                    String owner2 = pt == null ? "" : pt.getOwner();
                    boolean isCo = db.isCoowner(team, sender.getName());
                    if (sender.hasPermission("magic.admin") || sender.getName().equals(owner2) || isCo){
                        boolean added = db.addPlayerToTeam(team, playerToAdd);
                        sender.sendMessage(added ? messages.get("commands.powerteam.added", "player", playerToAdd, "team", team) : messages.get("commands.powerteam.add_failed"));
                        return true;
                    } else {
                        sender.sendMessage(messages.get("commands.powerteam.no_permission_add"));
                        return true;
                    }
                } else {
                    sender.sendMessage(messages.get("commands.powerteam.add_usage"));
                    return true;
                }
            case "invite":
                if (args.length == 2 && sender instanceof Player){
                    String target = args[1];
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){ sender.sendMessage(messages.get("commands.powerteam.not_in_team_invite")); return true; }
                    PowerTeam myTeamObj = db.getPowerTeam(myTeam);
                    boolean isOwner = myTeamObj != null && myTeamObj.getOwner().equals(sender.getName());
                    boolean isCo = db.isCoowner(myTeam, sender.getName());
                    if (!(isOwner || isCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(messages.get("commands.powerteam.no_permission_invite")); return true; }
                    boolean ok1 = db.createInvite(myTeam, sender.getName(), target);
                    sender.sendMessage(ok1 ? messages.get("commands.powerteam.invite_sent", "player", target) : messages.get("commands.powerteam.invite_failed"));
                    Player tp = Bukkit.getPlayer(target);
                    if (tp!=null){
                        tp.sendMessage(messages.get("commands.powerteam.invite_received", "team", myTeam, "sender", sender.getName()));
                        try{ tp.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(messages.get("commands.powerteam.invite_actionbar", "team", myTeam))); } catch (Exception ignored){}
                    }
                    return true;
                } else if (args.length == 3){
                    String team = args[1]; String target = args[2];
                    PowerTeam pt = db.getPowerTeam(team);
                    String owner2 = pt==null?"":pt.getOwner();
                    boolean isCo = db.isCoowner(team, sender.getName());
                    if (!(sender.hasPermission("magic.admin") || sender.getName().equals(owner2) || isCo)){ sender.sendMessage(messages.get("commands.powerteam.no_permission_invite_team")); return true; }
                    boolean ok2 = db.createInvite(team, sender.getName(), target);
                    sender.sendMessage(ok2 ? messages.get("commands.powerteam.invite_sent", "player", target) : messages.get("commands.powerteam.invite_failed"));
                    Player tp = Bukkit.getPlayer(target);
                    if (tp!=null){ tp.sendMessage(messages.get("commands.powerteam.invite_received", "team", team, "sender", sender.getName())); }
                    return true;
                }
                sender.sendMessage(messages.get("commands.powerteam.invite_usage"));
                return true;
            case "accept":
                if (!(sender instanceof Player)){ sender.sendMessage(messages.get("commands.powerteam.only_player_accept")); return true; }
                if (args.length < 2){ sender.sendMessage(messages.get("commands.powerteam.accept_usage")); return true; }
                String teamToAccept = args[1];
                boolean okAccept = db.acceptInvite(teamToAccept, sender.getName());
                sender.sendMessage(okAccept ? messages.get("commands.powerteam.accepted", "team", teamToAccept) : messages.get("commands.powerteam.accept_failed"));
                return true;
            case "decline":
                if (!(sender instanceof Player)){ sender.sendMessage(messages.get("commands.powerteam.only_player_decline")); return true; }
                if (args.length < 2){ sender.sendMessage(messages.get("commands.powerteam.decline_usage")); return true; }
                String teamToDecline = args[1];
                boolean okDecline = db.denyInvite(teamToDecline, sender.getName());
                sender.sendMessage(okDecline ? messages.get("commands.powerteam.declined") : messages.get("commands.powerteam.decline_failed"));
                return true;
            case "remove":
                if (args.length == 2 && sender instanceof Player){
                    String playerToRemove = args[1];
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){
                        sender.sendMessage(messages.get("commands.powerteam.leave_not_in_team"));
                        return true;
                    }
                    PowerTeam myTeamObj = db.getPowerTeam(myTeam);
                    boolean isOwner = myTeamObj != null && myTeamObj.getOwner().equals(sender.getName());
                    if (isOwner || sender.hasPermission("magic.admin")){
                        boolean rem = db.removePlayerFromTeam(myTeam, playerToRemove);
                        sender.sendMessage(rem ? messages.get("commands.powerteam.removed", "player", playerToRemove) : messages.get("commands.powerteam.remove_failed"));
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "只有队伍所有者或管理员可以移除成员。");
                        return true;
                    }
                } else if (args.length == 3){
                    String team = args[1];
                    String playerToRemove = args[2];
                    PowerTeam pt = db.getPowerTeam(team);
                    String owner2 = pt == null ? "" : pt.getOwner();
                    if (sender.hasPermission("magic.admin") || sender.getName().equals(owner2)){
                        boolean rem = db.removePlayerFromTeam(team, playerToRemove);
                        sender.sendMessage(rem ? messages.get("commands.powerteam.removed", "player", playerToRemove) : messages.get("commands.powerteam.remove_failed"));
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "只有队伍所有者或管理员可以移除成员。");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "用法: /powerteam remove <玩家>   (从您的队伍移除)\n或: /powerteam remove <队伍> <玩家> (所有者/管理员)");
                    return true;
                }
            case "list":

                if (args.length == 1 && sender instanceof Player){
                    String myTeam = db.getPlayerTeam(sender.getName());
                    if (myTeam == null){

                        List<String> teams = db.listPowerTeams();
                        sender.sendMessage(ChatColor.YELLOW + "可用队伍:");
                        for (String t : teams){
                            PowerTeam pt = db.getPowerTeam(t);
                            sender.sendMessage(ChatColor.AQUA + t + ChatColor.GRAY + " (" + (pt==null?0:pt.getMembers().size()) + " 名成员)");
                        }
                        return true;
                    } else {
                        PowerTeam pt = db.getPowerTeam(myTeam);
                        sender.sendMessage(ChatColor.GREEN + "队伍 " + myTeam + " 成员:");
                        for (String m : pt.getMembers()){
                            sender.sendMessage(ChatColor.AQUA + m);
                        }
                        return true;
                    }
                } else if (args.length == 2){
                    String team = args[1];
                    PowerTeam pt = db.getPowerTeam(team);
                    if (pt == null){
                        sender.sendMessage(ChatColor.RED + "未找到队伍: " + team);
                        return true;
                    }
                    sender.sendMessage(ChatColor.GREEN + "队伍 " + team + " (所有者: " + pt.getOwner() + ") 成员:");
                    for (String m : pt.getMembers()){
                        sender.sendMessage(ChatColor.AQUA + m);
                    }
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "用法: /powerteam list [队伍]");
                    return true;
                }
            case "requests":
            case "invites":
                if (sub.equals("requests")){
                    if (!(sender instanceof Player)){
                        sender.sendMessage(messages.get("commands.powerteam.only_player_requests"));
                        return true;
                    }
                    String myTeam2 = db.getPlayerTeam(sender.getName());
                    if (myTeam2 == null){ sender.sendMessage(messages.get("commands.powerteam.leave_not_in_team")); return true; }
                    PowerTeam teamObj2 = db.getPowerTeam(myTeam2);
                    boolean viewIsCo = db.isCoowner(myTeam2, sender.getName());
                    if (teamObj2 == null || !(teamObj2.getOwner().equals(sender.getName()) || viewIsCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(ChatColor.RED + "只有队伍所有者、副队长或管理员可以查看请求。"); return true; }
                    java.util.List<net.trduc.magicabilitiesfork.data.PowerteamRequest> reqs = db.listRequestsForTeam(myTeam2);
                    if (reqs.isEmpty()){ sender.sendMessage(messages.get("commands.powerteam.no_requests")); return true; }

                    Player ownerP = (Player) sender;
                    if (MagicAbilitiesfork.magicPlugin != null && MagicAbilitiesfork.magicPlugin.powerTeamGui != null){
                        MagicAbilitiesfork.magicPlugin.powerTeamGui.openRequestsGui(ownerP, myTeam2);
                    } else {

                        java.util.List<net.trduc.magicabilitiesfork.data.PowerteamRequest> reqs2 = db.listRequestsForTeam(myTeam2);
                        sender.sendMessage(ChatColor.GREEN + "队伍 " + myTeam2 + " 的待处理请求:");
                        for (net.trduc.magicabilitiesfork.data.PowerteamRequest r : reqs2){
                            sender.sendMessage(ChatColor.AQUA + r.getRequester() + " -> " + r.getTarget() + " (" + new java.util.Date(r.getTs()) + ")");
                        }
                    }
                    return true;
                } else {

                    if (!(sender instanceof Player)){
                        sender.sendMessage(ChatColor.RED + "只有玩家可以查看邀请。");
                        return true;
                    }
                    String playerName = sender.getName();
                    java.util.List<net.trduc.magicabilitiesfork.data.PowerteamRequest> inv = db.listInvitesForPlayer(playerName);
                    if (inv.isEmpty()){ sender.sendMessage(ChatColor.YELLOW + "您没有邀请。"); return true; }
                    sender.sendMessage(ChatColor.GREEN + "您的邀请:");
                    for (net.trduc.magicabilitiesfork.data.PowerteamRequest r : inv){
                        sender.sendMessage(ChatColor.AQUA + r.getTeamName() + " 邀请，发送者: " + r.getRequester() + " (" + new java.util.Date(r.getTs()) + ")");
                    }
                    return true;
                }
            case "approve":
                if (args.length < 2){ sender.sendMessage(messages.get("commands.powerteam.approve_usage")); return true; }
                if (!(sender instanceof Player)){ sender.sendMessage(messages.get("commands.powerteam.only_player_approve")); return true; }
                String approverTeam = db.getPlayerTeam(sender.getName());
                if (approverTeam == null){ sender.sendMessage(messages.get("commands.powerteam.leave_not_in_team")); return true; }
                PowerTeam approverTeamObj = db.getPowerTeam(approverTeam);
                boolean approverIsCo = db.isCoowner(approverTeam, sender.getName());
                if (approverTeamObj == null || !(approverTeamObj.getOwner().equals(sender.getName()) || approverIsCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(ChatColor.RED + "只有队伍所有者、副队长或管理员可以批准请求。"); return true; }
                String target = args[1];
                boolean okApprove = db.approveRequest(approverTeam, target, sender.getName());
                sender.sendMessage(okApprove ? messages.get("commands.powerteam.approved", "player", target, "team", approverTeam) : messages.get("commands.powerteam.approve_failed"));
                Player tp = Bukkit.getPlayer(target);
                if (tp!=null){ tp.sendMessage(messages.get("commands.powerteam.approved_notify", "player", target, "team", approverTeam)); }
                return true;
            case "deny":
                if (args.length < 2){ sender.sendMessage(messages.get("commands.powerteam.approve_usage")); return true; }
                if (!(sender instanceof Player)){ sender.sendMessage(messages.get("commands.powerteam.only_player_approve")); return true; }
                String denyTeam = db.getPlayerTeam(sender.getName());
                if (denyTeam == null){ sender.sendMessage(messages.get("commands.powerteam.leave_not_in_team")); return true; }
                PowerTeam denyTeamObj = db.getPowerTeam(denyTeam);
                boolean denyIsCo = db.isCoowner(denyTeam, sender.getName());
                if (denyTeamObj == null || !(denyTeamObj.getOwner().equals(sender.getName()) || denyIsCo || sender.hasPermission("magic.admin"))){ sender.sendMessage(ChatColor.RED + "只有队伍所有者、副队长或管理员可以拒绝请求。"); return true; }
                String denyTarget = args[1];
                boolean okDeny = db.denyRequest(denyTeam, denyTarget, sender.getName());
                sender.sendMessage(okDeny ? messages.get("commands.powerteam.denied", "player", denyTarget) : messages.get("commands.powerteam.deny_failed"));
                Player dt = Bukkit.getPlayer(denyTarget);
                if (dt!=null){ dt.sendMessage(messages.get("commands.powerteam.denied_notify", "player", denyTarget, "team", denyTeam)); }
                return true;
            case "info":
                if (args.length < 2){
                    sender.sendMessage(ChatColor.RED + "用法: /powerteam info <队伍>");
                    return true;
                }
                PowerTeam infoTeam = db.getPowerTeam(args[1]);
                if (infoTeam == null){
                    sender.sendMessage(ChatColor.RED + "未找到队伍。");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "队伍: " + infoTeam.getName() + " (所有者: " + infoTeam.getOwner() + ", 颜色: " + infoTeam.getColor() + ")");
                sender.sendMessage(ChatColor.GREEN + "成员:");
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
        sender.sendMessage(ChatColor.YELLOW + "技能队伍命令:");
        sender.sendMessage(ChatColor.AQUA + "/powerteam create <名称> <颜色>" + ChatColor.GRAY + " - 创建队伍并添加自己");
        sender.sendMessage(ChatColor.AQUA + "/powerteam add <玩家>" + ChatColor.GRAY + " - 添加玩家到您的队伍");
        sender.sendMessage(ChatColor.AQUA + "/powerteam remove <玩家>" + ChatColor.GRAY + " - 从您的队伍移除玩家");
        sender.sendMessage(ChatColor.AQUA + "/powerteam list [队伍]" + ChatColor.GRAY + " - 列出队伍或成员");
        sender.sendMessage(ChatColor.AQUA + "/powerteam info <队伍>" + ChatColor.GRAY + " - 显示队伍信息");
        sender.sendMessage(ChatColor.AQUA + "/powerteam invite <玩家>" + ChatColor.GRAY + " - 邀请玩家加入您的队伍");
        sender.sendMessage(ChatColor.AQUA + "/powerteam accept <队伍>" + ChatColor.GRAY + " - 接受邀请");
        sender.sendMessage(ChatColor.AQUA + "/powerteam decline <队伍>" + ChatColor.GRAY + " - 拒绝邀请");
        sender.sendMessage(ChatColor.AQUA + "/powerteam requests" + ChatColor.GRAY + " - 查看待处理的加入请求（所有者/副队长）");
        sender.sendMessage(ChatColor.AQUA + "/powerteam invites" + ChatColor.GRAY + " - 查看发送给您的邀请");
        sender.sendMessage(ChatColor.AQUA + "/powerteam approve <玩家>" + ChatColor.GRAY + " - 批准加入请求");
        sender.sendMessage(ChatColor.AQUA + "/powerteam deny <玩家>" + ChatColor.GRAY + " - 拒绝加入请求");
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