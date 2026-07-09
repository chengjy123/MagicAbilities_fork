package net.trduc.magicabilitiesfork.events;

import net.trduc.magicabilitiesfork.MagicAbilitiesfork;
import net.trduc.magicabilitiesfork.data.MessagesManager;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class ExecutionEvents implements Listener {

    private final Map<UUID, Long> lastMoveMs = new ConcurrentHashMap<>();
    private static final long MOVE_THROTTLE_MS = 50;
    private final MessagesManager messages = MessagesManager.getInstance();

    private final Map<UUID, Long> lastQuickToggleMs = new ConcurrentHashMap<>();
    private static final long QUICK_TOGGLE_COOLDOWN_MS = 500;

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        Player p = event.getPlayer();
        if (!players.containsKey(p)) return;
        if (event.getHand() == null || !event.getHand().equals(EquipmentSlot.HAND)) return;

        Power pow = players.get(p).getPower();
        if (!pow.isEnabled()) return;

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_AIR)){
            pow.executePower(new LeftClickExecute(event, p));
            return;
        }
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)){
            pow.executePower(new RightClickExecute(event, p));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event){
        Player p = event.getPlayer();
        if (!players.containsKey(p)) return;

        long now = System.currentTimeMillis();
        Long last = lastMoveMs.get(p.getUniqueId());
        if (last != null && now - last < MOVE_THROTTLE_MS) return;
        lastMoveMs.put(p.getUniqueId(), now);

        Power pow = players.get(p).getPower();
        pow.executePower(new MoveExecute(event, p));
    }

    @EventHandler
    public void onDamagedBy(EntityDamageByEntityEvent event){
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!players.containsKey(p)) return;
        players.get(p).getPower().executePower(new DamagedByExecute(event, p));
    }

    @EventHandler
    public void onDamageDealt(EntityDamageByEntityEvent event){

        Player attackerPlayer = null;
        if (event.getDamager() instanceof Player){
            attackerPlayer = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile){
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) attackerPlayer = (Player) proj.getShooter();
        } else if (event.getDamager() instanceof org.bukkit.entity.Tameable){
            org.bukkit.entity.Tameable t = (org.bukkit.entity.Tameable) event.getDamager();
            if (t.getOwner() instanceof Player) attackerPlayer = (Player) t.getOwner();
        }

        if (attackerPlayer == null){

            if (event.getDamager() instanceof Player) return;

            return;
        }

        if (!(event.getEntity() instanceof Player)) {

            Player p = attackerPlayer;
            if (!players.containsKey(p)) return;
            players.get(p).getPower().executePower(new DealDamageExecute(event, p));
            return;
        }

        Player p = attackerPlayer;
        Player target = (Player) event.getEntity();
        if (!players.containsKey(p)) return;

        double originalDamage = event.getDamage();
        boolean originalCancelled = event.isCancelled();

        players.get(p).getPower().executePower(new DealDamageExecute(event, p));

        try {
            String attackerTeam = MagicAbilitiesfork.magicPlugin.getDbManager().getPlayerTeam(p.getName());
            String targetTeam = MagicAbilitiesfork.magicPlugin.getDbManager().getPlayerTeam(target.getName());
            if (attackerTeam != null && attackerTeam.equals(targetTeam)){

                if (event.isCancelled() != originalCancelled || event.getDamage() != originalDamage){
                    event.setCancelled(true);
                }
            }
        } catch (Exception ignored){}
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event){
        if (!(event.getEntity() instanceof Player)) return;
        if (event instanceof EntityDamageByEntityEvent) return;
        Player p = (Player) event.getEntity();
        if (!players.containsKey(p)) return;
        players.get(p).getPower().executePower(new DamagedExecute(event, p));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event){
        Player p = event.getEntity();
        if (!players.containsKey(p)) return;
        players.get(p).getPower().executePower(new DeathExecute(event, p));
    }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent event){
        Player p = event.getPlayer();
        if (!players.containsKey(p)) return;
        players.get(p).getPower().executePower(new ConsumeExecute(event, p));
    }

    @EventHandler
    public void onEnClick(PlayerInteractEntityEvent event){
        if (!(event.getRightClicked() instanceof Player)) return;
        Player target = (Player) event.getRightClicked();
        if (!players.containsKey(target)) return;
        players.get(target).getPower().executePower(new InteractedOnByExecute(event, target));
    }

    @EventHandler
    public void onMine(BlockBreakEvent event){
        Player p = event.getPlayer();
        if (!players.containsKey(p)) return;
        Power pow = players.get(p).getPower();
        if (!pow.isEnabled()) return;
        pow.executePower(new MineExecute(event, p));
    }

    @EventHandler
    public void onShift(PlayerToggleSneakEvent event){
        if (!event.isSneaking()) return;
        Player p = event.getPlayer();
        if (!players.containsKey(p)) return;
        Power pow = players.get(p).getPower();
        if (!pow.isEnabled()) return;
        pow.executePower(new SneakExecute(event, p));
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event){
        Player p = event.getPlayer();
        if (!players.containsKey(p)) return;
        if (!p.isSneaking()) return;

        long now = System.currentTimeMillis();
        if (tryQuickTogglePower(p, now)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event){
        Player p = event.getPlayer();
        if (!players.containsKey(p)) return;
        event.setCancelled(false);
    }

    private boolean tryQuickTogglePower(Player p, long now) {
        Long lastToggle = lastQuickToggleMs.get(p.getUniqueId());
        if (lastToggle != null && now - lastToggle < QUICK_TOGGLE_COOLDOWN_MS) return false;

        lastQuickToggleMs.put(p.getUniqueId(), now);

        Power pow = players.get(p).getPower();
        boolean newState = !pow.isEnabled();

        pow.setEnabled(newState);
        getPlayerData(p).setEnabled(newState);

        if (newState) {
            p.sendMessage(messages.get("events.power_enabled"));
        } else {
            p.sendMessage(messages.get("events.power_disabled"));
        }
        return true;
    }

    public void cleanup(Player p) {
        lastMoveMs.remove(p.getUniqueId());
        lastQuickToggleMs.remove(p.getUniqueId());
    }
}