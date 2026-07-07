package net.trduc.magicabilitiesfork.powers.executions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DealDamageExecute extends Execute{
    public DealDamageExecute(EntityDamageByEntityEvent event, Player player) {
        super(event, player);
    }

    public EntityDamageByEntityEvent getDamageEvent() {
        return (EntityDamageByEntityEvent) getRawEvent();
    }

    public double getDamage() {
        return getDamageEvent().getDamage();
    }

    public Entity getTarget() {
        return getDamageEvent().getEntity();
    }
}

