package net.trduc.magicabilities.data;

import net.trduc.magicabilities.players.PowerPlayer;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.PowerType;
import net.trduc.magicabilities.powers.RandomPowerAssigner;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static net.trduc.magicabilities.MagicAbilities.magicPlugin;
import static net.trduc.magicabilities.data.PlayerData.*;
import static net.trduc.magicabilities.players.PowerPlayer.players;

public class DataEventsHandler implements Listener {
    private final DbManager dbManager;

    public DataEventsHandler(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @EventHandler
    public void onSlotSwap(PlayerItemHeldEvent event){
        if (!players.containsKey(event.getPlayer())){
            return;
        }
        players.get(event.getPlayer()).setActiveSlot(event.getNewSlot());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        if (event.getPlayer().getName().contains(" ")){
            event.getPlayer().kickPlayer("Invalid Name!");
        }
        if (players.containsKey(event.getPlayer())) {
            players.get(event.getPlayer()).remove();
            players.remove(event.getPlayer());
        }
        setPlayerDataFromDb(event.getPlayer(), dbManager);
        new PowerPlayer(Power.getPowerFromPowerType(event.getPlayer(), getPlayerData(event.getPlayer()).getPower()), getPlayerData(event.getPlayer()).getBinds(), getPlayerData(event.getPlayer()).isEnabled(), getPlayerData(event.getPlayer()).isAuraEnabled());

        if (getPlayerData(event.getPlayer()).getPower() == PowerType.NONE) {
            PowerType assigned = RandomPowerAssigner.randomPower();
            players.get(event.getPlayer()).changePower(assigned);
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().sendMessage(ChatColor.GOLD + "✦ Welcome! You have received the power: "
                            + ChatColor.YELLOW + ChatColor.BOLD + assigned.name().replace('_', ' '));
                    event.getPlayer().sendMessage(ChatColor.GRAY + "Use /powerset off to disable your power when not needed.");
                }
            }.runTaskLater(magicPlugin, 20);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Your power is "
                            + (getPlayerData(event.getPlayer()).isEnabled() ? "enabled" : "disabled") + "!");
                }
            }.runTaskLater(magicPlugin, 1);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        savePlayerDataToDb(event.getPlayer(), dbManager);
        removePlayerData(event.getPlayer());
        if (players.containsKey(event.getPlayer())) {
            players.get(event.getPlayer()).remove();
            players.remove(event.getPlayer());
        }
    }
}
