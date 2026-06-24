package net.trduc.magicabilities.data;

import net.trduc.magicabilities.powers.PowerType;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PlayerData {
    private static final HashMap<Player, PlayerData> playerData = new HashMap<>();
    private final String name;
    private PowerType powerType;
    private HashMap<Integer, Integer> binds;
    private boolean enabled;
    private boolean auraEnabled;

    public PlayerData(String name, PowerType powerType, HashMap<Integer, Integer> binds, boolean enabled, boolean auraEnabled) {
        this.name = name;
        this.powerType = powerType;
        this.binds = binds;
        this.enabled = enabled;
        this.auraEnabled = auraEnabled;
    }
    public static PlayerData getPlayerData(Player p){
        return playerData.get(p);
    }
    public static void setPlayerDataFromDb(Player p, DbManager db){
        String playerName = p.getName();
        playerData.put(p, db.getPlayerData(playerName));
    }
    public static void savePlayerDataToDb(Player p, DbManager db){
        String playerName = p.getName();
        db.updatePlayer(playerName, playerData.get(p));
    }
    public static void removePlayerData(Player p){
        playerData.remove(p);
    }
    public String getName() {
        return name;
    }

    public PowerType getPower() {
        return powerType;
    }

    public void setPower(PowerType powerType) {
        this.powerType = powerType;
    }

    public HashMap<Integer, Integer> getBinds() {
        return binds;
    }

    public void setBinds(HashMap<Integer, Integer> binds) {
        this.binds = binds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAuraEnabled() {
        return auraEnabled;
    }

    public void setAuraEnabled(boolean auraEnabled) {
        this.auraEnabled = auraEnabled;
    }
}
