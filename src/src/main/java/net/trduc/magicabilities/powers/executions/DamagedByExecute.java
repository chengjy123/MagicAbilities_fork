package net.trduc.magicabilities.powers.executions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamagedByExecute extends Execute{
    public DamagedByExecute(EntityDamageByEntityEvent event, Player player) {
        super(event, player);
    }

    public EntityDamageByEntityEvent getDamageEvent() {
        return (EntityDamageByEntityEvent) getRawEvent();
    }

    public Entity getDamager() {
        return getDamageEvent().getDamager();
    }

    public double getDamage() {
        return getDamageEvent().getDamage();
    }
}
