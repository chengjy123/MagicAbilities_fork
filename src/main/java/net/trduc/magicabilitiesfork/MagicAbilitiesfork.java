package net.trduc.magicabilitiesfork;

import net.trduc.magicabilitiesfork.commands.*;
import net.trduc.magicabilitiesfork.cooldowns.Cooldowns;
import net.trduc.magicabilitiesfork.data.DataEventsHandler;
import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.PlayerData;
import net.trduc.magicabilitiesfork.events.ExecutionEvents;
import net.trduc.magicabilitiesfork.guis.AnimationManager;
import net.trduc.magicabilitiesfork.guis.GuiManager;
import net.trduc.magicabilitiesfork.commands.PowerteamOwnerCommands;
import net.trduc.magicabilitiesfork.misc.ConfigMigrator;
import net.trduc.magicabilitiesfork.misc.CooldownValidator;
import net.trduc.magicabilitiesfork.misc.ParticleApi;
import net.trduc.magicabilitiesfork.players.PowerPlayer;
import net.trduc.magicabilitiesfork.powers.Power;
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

import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public final class MagicAbilitiesfork extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    public static ParticleApi particleApi;
    private DbManager dbManager;
    private static FileConfiguration config;
    public static MagicAbilitiesfork magicPlugin;
    public net.trduc.magicabilitiesfork.guis.PowerTeamGui powerTeamGui;

    @Override
    public void onEnable() {
        magicPlugin = this;
        saveDefaultConfig();
        ConfigMigrator.migrate(this);
        config = getConfig();
        net.trduc.magicabilitiesfork.data.MessagesManager.getInstance().init();

        int pluginId = 32200;
        Metrics metrics = new Metrics(this, pluginId);

        Cooldowns cdInstance = new Cooldowns(createCooldownsConfig());
        CooldownValidator.validate(this, cdInstance);
        particleApi = new ParticleApi(this);
        dbManager = new DbManager(this);
        dbManager.init();
        checkDb(dbManager);
        setPlayerData(dbManager);
        ExecutionEvents executionEvents = new ExecutionEvents();
        getServer().getPluginManager().registerEvents(new DataEventsHandler(dbManager, executionEvents), this);
        getServer().getPluginManager().registerEvents(executionEvents, this);
        getServer().getPluginManager().registerEvents(new net.trduc.magicabilitiesfork.events.PowerTeamEvents(dbManager), this);

        net.trduc.magicabilitiesfork.guis.PowerTeamGui ptGui = new net.trduc.magicabilitiesfork.guis.PowerTeamGui(dbManager);
        this.powerTeamGui = ptGui;
        getServer().getPluginManager().registerEvents(ptGui, this);

        net.trduc.magicabilitiesfork.guis.PowerGui powerGui = new net.trduc.magicabilitiesfork.guis.PowerGui();
        getServer().getPluginManager().registerEvents(powerGui, this);

        registerCommand(new Binds(), "binds");
        registerCommand(new Destination(), "destination");
        registerCommand(new Setpower(), "setpower");
        registerCommand(new Enable(), "enable");
        registerCommand(new Disable(), "disable");
        registerCommand(new Powerset(), "powerset");
        registerCommand(new Powersetaura(), "powersetaura");
        registerCommand(new PowerGuiCommand(powerGui), "powergui");
        registerCommand(new Combos(), "combos");
        registerCommand(new PowerTeamCommand(), "powerteam");
        registerCommand(new PowerteamOwnerCommands(), "powerteamowner");
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
        getCommand(cmdName).setExecutor(cmd);
        if (cmd instanceof TabCompleter){
            getCommand(cmdName).setTabCompleter((TabCompleter) cmd);
        }
    }

    public static Logger getLog(){
        return log;
    }

    public DbManager getDbManager() {
        return dbManager;
    }

    public static void debugLog(String msg, boolean warning){
        if (!config.getBoolean("debug")){
            return;
        }
        if (warning) log.warning("[MagicAbilitiesfork:Debug] " + msg);
        else log.info("[MagicAbilitiesfork:Debug] " + msg);
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

