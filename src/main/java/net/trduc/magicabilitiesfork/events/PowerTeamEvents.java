package net.trduc.magicabilitiesfork.events;

import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.PowerteamRequest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PowerTeamEvents implements Listener {
    private final DbManager db;

    public PowerTeamEvents(DbManager db){
        this.db = db;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player p = event.getPlayer();
        try{
            List<String> owned = db.listTeamsOwnedBy(p.getName());
            for (String team : owned){
                java.util.List<PowerteamRequest> reqs = db.listRequestsForTeam(team);
                if (reqs != null && !reqs.isEmpty()){
                    p.sendMessage(ChatColor.GOLD + "You have " + reqs.size() + " pending team requests for " + ChatColor.AQUA + team + ChatColor.GOLD + ".");
                    p.sendMessage(ChatColor.GRAY + "Use /powerteam requests to view or /powerteam approve <player> /powerteam deny <player> to respond.");
                    for (PowerteamRequest r : reqs){
                        p.sendMessage(ChatColor.AQUA + r.getRequester() + " -> " + r.getTarget() + " (requested at " + new java.util.Date(r.getTs()) + ")");
                    }
                }
            }
        } catch (Exception ignored){}
    }
}
