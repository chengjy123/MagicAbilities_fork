package net.trduc.magicabilitiesfork.players;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.PowerType;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.IdleExecute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;

public class PowerPlayer {
    public static final HashMap<Player, PowerPlayer> players = new HashMap<>();

    private Power power;
    private int activeSlot;
    private BukkitRunnable idlePower = null;
    private final HashMap<Integer, Integer> binds;
    private boolean auraEnabled;

    public PowerPlayer(Power power, HashMap<Integer, Integer> binds, boolean enabled, boolean auraEnabled) {
        this.power = power;
        this.activeSlot=this.power.getOwner().getInventory().getHeldItemSlot();
        this.binds = binds;
        this.auraEnabled = auraEnabled;
        if (players.containsKey(power.getOwner())){
            throw new RuntimeException("Power players Hashmap already has this Player!");
        }
        players.put(power.getOwner(), this);
        if (power instanceof IdlePower){
            idlePower=((IdlePower) power).executeIdle(new IdleExecute(null, power.getOwner()));
        }
        power.setEnabled(enabled);
    }

    public Power getPower() {
        return power;
    }

    public void changePower(PowerType power){
        if (getPower() instanceof Removeable) ((Removeable) getPower()).remove();
        if (idlePower!=null) idlePower.cancel();
        idlePower=null;
        Player owner = this.power.getOwner();
        getPlayerData(owner).setPower(power);
        this.power=Power.getPowerFromPowerType(owner, power);
        if (this.power instanceof IdlePower){
            idlePower=((IdlePower) this.power).executeIdle(new IdleExecute(null, owner));
        }
    }

    public void remove(){
        if (power instanceof Removeable) ((Removeable) power).remove();
        if (idlePower!=null) idlePower.cancel();
        idlePower=null;
    }

    public int getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(int activeSlot) {
        this.activeSlot = activeSlot;
    }

    public void changeBind(int ab, int slot){
        binds.replace(ab, slot);
        getPlayerData(power.getOwner()).setBinds(binds);
    }

    public void resetBinds(){
        binds.clear();
        for (int i = 0; i<9; i++){
            binds.put(i, i);
        }
        getPlayerData(power.getOwner()).setBinds(binds);
    }

    public HashMap<Integer, Integer> getBinds() {
        return binds;
    }

    public boolean isAuraEnabled() {
        return auraEnabled;
    }

    public void setAuraEnabled(boolean auraEnabled) {
        this.auraEnabled = auraEnabled;
    }
}

