package net.trduc.magicabilitiesfork.guis;

import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.MessagesManager;
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
    private final MessagesManager messages = MessagesManager.getInstance();

    public PowerTeamGui(DbManager db){
        this.db = db;
    }

    public void openRequestsGui(Player owner, String team){
        List<PowerteamRequest> reqs = db.listRequestsForTeam(team);
        int size = 9;
        while (size < reqs.size()) size += 9;
        if (size > 54) size = 54;
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', messages.get("gui.team_requests_title", "team", team)));

        for (int i = 0; i < reqs.size() && i < size; i++){
            PowerteamRequest r = reqs.get(i);

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(r.getTarget())); } catch (Throwable ignored) {}
            meta.setDisplayName(ChatColor.YELLOW + r.getTarget());
            meta.setLore(java.util.Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', messages.get("gui.requester", "player", r.getRequester())),
                    ChatColor.translateAlternateColorCodes('&', messages.get("gui.left_click_approve")),
                    ChatColor.translateAlternateColorCodes('&', messages.get("gui.right_click_deny"))
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
            clicker.sendMessage(messages.get("gui.only_owner"));
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
                    clicker.sendMessage(messages.get("gui.approved", "player", targetName, "team", team));
                    Player tp = Bukkit.getPlayer(targetName);
                    if (tp!=null) tp.sendMessage(messages.get("gui.approved_notify", "player", targetName, "team", team));

                    openRequestsGui(clicker, team);
                } else {
                    clicker.sendMessage(messages.get("gui.approve_failed"));
                }
                break;
            case RIGHT:
            case SHIFT_RIGHT:
                boolean ok2 = db.denyRequest(team, targetName, owner);
                if (ok2){
                    clicker.sendMessage(messages.get("gui.denied", "player", targetName));
                    Player tp2 = Bukkit.getPlayer(targetName);
                    if (tp2!=null) tp2.sendMessage(messages.get("gui.denied_notify", "player", targetName, "team", team));

                    openRequestsGui(clicker, team);
                } else {
                    clicker.sendMessage(messages.get("gui.deny_failed"));
                }
                break;
            default:

        }
    }
}