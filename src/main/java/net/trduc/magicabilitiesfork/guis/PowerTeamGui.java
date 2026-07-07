package net.trduc.magicabilitiesfork.guis;

import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.PowerteamRequest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerTeamGui implements Listener {
    private final DbManager db;
    private final Map<Inventory, String> invTeam = new HashMap<>();
    private final Map<Inventory, String> invOwner = new HashMap<>();

    public PowerTeamGui(DbManager db){
        this.db = db;
    }

    public void openRequestsGui(Player owner, String team){
        List<PowerteamRequest> reqs = db.listRequestsForTeam(team);
        int size = 9;
        while (size < reqs.size()) size += 9;
        if (size > 54) size = 54;
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_AQUA + "Team Requests: " + ChatColor.AQUA + team);

        for (int i = 0; i < reqs.size() && i < size; i++){
            PowerteamRequest r = reqs.get(i);

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(r.getTarget())); } catch (Throwable ignored) {}
            meta.setDisplayName(ChatColor.YELLOW + r.getTarget());
            meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Requested by: " + r.getRequester(),
                    ChatColor.GREEN + "Left-click to Approve",
                    ChatColor.RED + "Right-click to Deny"
            ));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        String ownerName = owner.getName();
        java.util.List<Inventory> toRemove = new java.util.ArrayList<>();
        for (Map.Entry<Inventory, String> e : invOwner.entrySet()){
            if (e.getValue().equals(ownerName)) toRemove.add(e.getKey());
        }
        for (Inventory iRem : toRemove){
            invOwner.remove(iRem);
            invTeam.remove(iRem);
        }

        invTeam.put(inv, team);
        invOwner.put(inv, owner.getName());
        owner.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        Inventory inv = e.getInventory();
        if (!invTeam.containsKey(inv)) return;
        e.setCancelled(true);
        Player clicker = (Player) e.getWhoClicked();
        String team = invTeam.get(inv);
        String owner = invOwner.get(inv);
        if (!clicker.getName().equals(owner)){
            clicker.sendMessage(ChatColor.RED + "Only the team owner can manage requests.");
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String targetName = ChatColor.stripColor(meta.getDisplayName());

        switch (e.getClick()){
            case LEFT:
            case SHIFT_LEFT:
                boolean ok = db.approveRequest(team, targetName, owner);
                if (ok){
                    clicker.sendMessage(ChatColor.GREEN + "Approved and added " + targetName + " to " + team);
                    Player tp = Bukkit.getPlayer(targetName);
                    if (tp!=null) tp.sendMessage(ChatColor.GREEN + "You were added to team " + team + " by owner.");

                    openRequestsGui(clicker, team);
                } else {
                    clicker.sendMessage(ChatColor.RED + "Approve failed.");
                }
                break;
            case RIGHT:
            case SHIFT_RIGHT:
                boolean ok2 = db.denyRequest(team, targetName, owner);
                if (ok2){
                    clicker.sendMessage(ChatColor.YELLOW + "Denied request for " + targetName);
                    Player tp2 = Bukkit.getPlayer(targetName);
                    if (tp2!=null) tp2.sendMessage(ChatColor.RED + "Your join request to team " + team + " was denied.");

                    openRequestsGui(clicker, team);
                } else {
                    clicker.sendMessage(ChatColor.RED + "Deny failed or no such request.");
                }
                break;
            default:

        }
    }
}
