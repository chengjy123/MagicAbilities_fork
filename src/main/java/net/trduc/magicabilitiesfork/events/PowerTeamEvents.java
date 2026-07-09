package net.trduc.magicabilitiesfork.events;

import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.MessagesManager;
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
    private final MessagesManager messages = MessagesManager.getInstance();

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
                    p.sendMessage(messages.get("events.team_request_notify", "team", team, "count", String.valueOf(reqs.size())));
                    p.sendMessage(messages.get("events.team_request_hint"));
                    for (PowerteamRequest r : reqs){
                        p.sendMessage(ChatColor.AQUA + r.getRequester() + " -> " + r.getTarget() + " (请求于 " + new java.util.Date(r.getTs()) + ")");
                    }
                }
            }
        } catch (Exception ignored){}
    }
}