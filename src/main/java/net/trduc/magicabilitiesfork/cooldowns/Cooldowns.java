package net.trduc.magicabilitiesfork.cooldowns;

import net.trduc.magicabilitiesfork.misc.CooldownValidator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.logging.Logger;

public class Cooldowns {
    public static Cooldowns cooldowns = null;
    private static final HashMap<String, Double> cds = new HashMap<>();
    private final FileConfiguration config;

    public Cooldowns(FileConfiguration config) {
        if (cooldowns!=null) throw new RuntimeException("Cooldowns instance already exists!");
        cooldowns = this;
        this.config=config;
        try {
            createCooldowns();
        } catch (Exception e){
            Bukkit.getServer().getLogger().warning("Couldn't create cooldowns!");
        }
    }

    private void createCooldowns(){
        for (String s: config.getKeys(false)){
            if (config.get(s) instanceof MemorySection) {
                for (String key: ((MemorySection) config.get(s)).getKeys(false)){
                    String fullKey = s+"."+key;
                    if (cds.containsKey(fullKey)) cds.replace(fullKey, config.getDouble(fullKey));
                    else cds.put(fullKey, config.getDouble(fullKey));
                }
            }
        }
        for (String s : cds.keySet()){
            CooldownApi.createCooldown(s, cds.get(s));
        }
    }

    public Double get(String s){
        if (!cds.containsKey(s)) {
            Logger log = Bukkit.getServer().getLogger();
            double fallback = CooldownValidator.getFallback();
            log.warning("[MagicAbilitiesfork] Cooldown key \"" + s
                    + "\" does not exist in cooldowns.yml! Using fallback "
                    + fallback + "s to avoid crash.");
            cds.put(s, fallback);
            try {
                CooldownApi.createCooldown(s, fallback);
            } catch (IllegalArgumentException ignored) {}
            return fallback;
        }
        return cds.get(s);
    }

    public boolean containsKey(String s){
        return cds.containsKey(s);
    }
}

