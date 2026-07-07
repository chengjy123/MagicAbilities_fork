package net.trduc.magicabilitiesfork.powers;

import net.trduc.magicabilitiesfork.powers.executions.IdleExecute;
import org.bukkit.scheduler.BukkitRunnable;

public interface IdlePower {
    BukkitRunnable executeIdle(IdleExecute ex);
}

