package net.trduc.magicabilities;

import net.trduc.magicabilities.commands.*;
import net.trduc.magicabilities.cooldowns.Cooldowns;
import net.trduc.magicabilities.data.DataEventsHandler;
import net.trduc.magicabilities.data.DbManager;
import net.trduc.magicabilities.data.PlayerData;
import net.trduc.magicabilities.events.ExecutionEvents;
import net.trduc.magicabilities.guis.AnimationManager;
import net.trduc.magicabilities.guis.GuiManager;
import net.trduc.magicabilities.misc.ParticleApi;
import net.trduc.magicabilities.players.PowerPlayer;
import net.trduc.magicabilities.powers.Power;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.players.PowerPlayer.players;

public final class MagicAbilities extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    public static ParticleApi particleApi;
    private DbManager dbManager;
    private static FileConfiguration config;
    public static JavaPlugin magicPlugin;

    @Override
    public void onEnable() {
        magicPlugin=this;
        saveDefaultConfig();
        config = getConfig();
        new Cooldowns(createCooldownsConfig());
        particleApi = new ParticleApi(this);
        dbManager = new DbManager(this);
        dbManager.init();
        checkDb(dbManager);
        setPlayerData(dbManager);
        getServer().getPluginManager().registerEvents(new DataEventsHandler(dbManager), this);
        getServer().getPluginManager().registerEvents(new ExecutionEvents(), this);
        registerCommand(new Binds(), "binds");
        registerCommand(new Destination(), "destination");
        registerCommand(new Setpower(), "setpower");
        registerCommand(new Enable(), "enable");
        registerCommand(new Disable(), "disable");
        registerCommand(new Powerset(), "powerset");
        registerCommand(new Powersetaura(), "powersetaura");
        final GuiManager guiManager = new GuiManager(this);
        final AnimationManager animationManager = new AnimationManager(this, guiManager);

        particleApi.spawnParticles(Bukkit.getWorlds().get(0).getSpawnLocation(), Particle.ASH, 1, 1, 1, 1, 1);
    }

    @Override
    public void onDisable() {
        dbManager.disconnect();
        savePlayers(dbManager);
    }

    private void registerCommand(CommandExecutor cmd, String cmdName){
        if (cmd instanceof TabCompleter){
            getCommand(cmdName).setExecutor(cmd);
            getCommand(cmdName).setTabCompleter((TabCompleter) cmd);
        } else {
            throw new RuntimeException("Provided object is not a command executor and a tab completer at the same time!");
        }
    }

    public static Logger getLog(){
        return log;
    }

    public static void debugLog(String msg, boolean warning){
        if (!config.getBoolean("debug")){
            return;
        }
        if (warning) log.warning("[MagicAbilities:Debug] " + msg);
        else log.info("[MagicAbilities:Debug] " + msg);
    }
    private void checkDb(DbManager db){
        db.connect();
        if (db.isDbEnabled()){
            log.info("Database is operational");
        } else log.warning("Database is offline!");
    }
    private void setPlayerData(DbManager db){
        for (Player p : getServer().getOnlinePlayers()){
            PlayerData.setPlayerDataFromDb(p, db);
            new PowerPlayer(Power.getPowerFromPowerType(p, getPlayerData(p).getPower()), getPlayerData(p).getBinds(), getPlayerData(p).isEnabled(), getPlayerData(p).isAuraEnabled());
        }
    }

    private void savePlayers(DbManager db){
        for (Player p : getServer().getOnlinePlayers()){
            PlayerData.savePlayerDataToDb(p, db);
            PlayerData.removePlayerData(p);
            if (players.containsKey(p)) {
                players.get(p).remove();
                players.remove(p);
            }
        }
    }

    private FileConfiguration createCooldownsConfig() {
        File customConfigFile = new File(getDataFolder(), "cooldowns.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("cooldowns.yml", false);
        }

        return YamlConfiguration.loadConfiguration(customConfigFile);
    }
}
