package net.trduc.magicabilitiesfork.data;

import net.trduc.magicabilitiesfork.events.ExecutionEvents;
import net.trduc.magicabilitiesfork.players.PowerPlayer;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.PowerType;
import net.trduc.magicabilitiesfork.powers.RandomPowerAssigner;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.data.PlayerData.*;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class DataEventsHandler implements Listener {
    private final DbManager dbManager;
    private final ExecutionEvents executionEvents;
    private final MessagesManager messages = MessagesManager.getInstance();

    public DataEventsHandler(DbManager dbManager, ExecutionEvents executionEvents) {
        this.dbManager = dbManager;
        this.executionEvents = executionEvents;
    }

    @EventHandler
    public void onSlotSwap(PlayerItemHeldEvent event){
        if (!players.containsKey(event.getPlayer())) return;
        players.get(event.getPlayer()).setActiveSlot(event.getNewSlot());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        if (event.getPlayer().getName().contains(" ")){
            event.getPlayer().kickPlayer("无效的名称！");
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
                    event.getPlayer().sendMessage(messages.get("data.welcome") + ChatColor.YELLOW + ChatColor.BOLD + assigned.name().replace('_', ' '));
                    event.getPlayer().sendMessage(messages.get("data.welcome_hint"));
                }
            }.runTaskLater(magicPlugin, 20);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().sendMessage(messages.get("data.power_status") + (getPlayerData(event.getPlayer()).isEnabled() ? "已开启" : "已关闭") + "！");
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
        executionEvents.cleanup(event.getPlayer());
    }
}