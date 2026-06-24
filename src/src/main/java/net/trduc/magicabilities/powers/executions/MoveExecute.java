package net.trduc.magicabilities.powers.executions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveExecute extends Execute{
    public MoveExecute(PlayerMoveEvent event, Player player) {
        super(event, player);
    }

    public PlayerMoveEvent getMoveEvent() {
        return (PlayerMoveEvent) getRawEvent();
    }

    public Location getFrom() {
        return getMoveEvent().getFrom();
    }

    public Location getTo() {
        return getMoveEvent().getTo();
    }
}
