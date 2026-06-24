package net.trduc.magicabilities.powers.executions;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamagedExecute extends Execute{
    public DamagedExecute(EntityDamageEvent event, Player player) {
        super(event, player);
    }
}
