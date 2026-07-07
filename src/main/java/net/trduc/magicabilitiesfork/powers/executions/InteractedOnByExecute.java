package net.trduc.magicabilitiesfork.powers.executions;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class InteractedOnByExecute extends Execute{
    public InteractedOnByExecute(PlayerInteractEntityEvent event, Player player) {
        super(event, player);
    }
}

