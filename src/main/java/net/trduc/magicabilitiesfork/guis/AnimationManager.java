package net.trduc.magicabilitiesfork.guis;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class AnimationManager {
    public static final HashMap<Player, Boolean> skipAnim = new HashMap<>();

    private final GuiManager guis;
    private final JavaPlugin plugin;
    public AnimationManager(JavaPlugin plugin, GuiManager guis){
        this.plugin = plugin;
        this.guis = guis;
    }

    public static void skipAnimation(Player p){
        if (skipAnim.containsKey(p)){
            skipAnim.replace(p, true);
        }
    }
}

