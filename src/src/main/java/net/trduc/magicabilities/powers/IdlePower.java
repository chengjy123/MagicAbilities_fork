package net.trduc.magicabilities.powers;

import net.trduc.magicabilities.powers.executions.IdleExecute;
import org.bukkit.scheduler.BukkitRunnable;

public interface IdlePower {
    BukkitRunnable executeIdle(IdleExecute ex);
}
