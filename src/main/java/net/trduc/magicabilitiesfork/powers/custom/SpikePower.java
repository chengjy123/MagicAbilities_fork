package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;

public class SpikePower extends Power implements Removeable {

    private static final String CD_STRIKE = "spike.strike";
    private static final String CD_BURST  = "spike.burst";
    private static final String CD_GRASP  = "spike.grasp";
    private static final String CD_WALL   = "spike.wall";
    private static final String CD_STORM  = "spike.storm";

    private static final Color C_IRON  = Color.fromRGB( 35,  35,  38);
    private static final Color C_STEEL = Color.fromRGB( 70,  70,  75);
    private static final Color C_RUST  = Color.fromRGB( 60,  40,  35);
    private static final Color C_SPARK = Color.fromRGB(150, 150, 160);

    private final Random rng = new Random();

    private BukkitRunnable wallTask  = null;
    private BukkitRunnable stormTask = null;

    public SpikePower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeft((LeftClickExecute) ex);
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: tendrilStrike(p); break;
            case 1: tendrilBurst(p);  break;
            case 2: tendrilGrasp(p);  break;
            case 3: tendrilWall(p);   break;
            case 4: tendrilStorm(p);  break;
        }
    }

    private void tendrilStrike(Player p) {
        if (onCd(CD_STRIKE, p, this)) return;

        LivingEntity target = getInSight(p, 6, 0.7);
        if (target == null) { sendActionBar(p, "§7No target in sight!"); return; }

        final LivingEntity tgt = target;
        final Location origin  = originBehindBack(p);
        final int tendrilCount = 6;

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 0.9f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HOGLIN_ATTACK, 0.6f, 0.7f);

        for (int i = 0; i < tendrilCount; i++) {
            final int idx   = i;
            final int delay = i * 2;

            final double sideOff = (rng.nextDouble() - 0.5) * 2.4;
            final double vertOff = (rng.nextDouble() - 0.5) * 1.6;

            new BukkitRunnable() {
                @Override public void run() {
                    if (!tgt.isValid() || tgt.isDead()) return;
                    fireTendril(p, origin, tgt, sideOff, vertOff, 3.0, idx);
                }
            }.runTaskLater(magicPlugin, delay);
        }

        sendActionBar(p, "§8⚡ Tendril Strike");
        addCd(CD_STRIKE, p);
    }

    private void fireTendril(Player p, Location origin, LivingEntity target, double sideOff, double vertOff, double damage, int idx) {
        final Vector right = p.getLocation().getDirection().clone().setY(0).normalize();
        final Vector side  = new Vector(-right.getZ(), 0, right.getX());

        new BukkitRunnable() {
            int t = 0;
            final int steps = 9;

            @Override public void run() {
                if (t > steps || !target.isValid() || target.isDead()) { cancel(); return; }

                double progress = (double) t / steps;

                double bulge = Math.sin(progress * Math.PI) * 1.0;

                Location targetPoint = target.getLocation().clone().add(0, 1, 0);
                Location straight    = origin.clone().add(
                        targetPoint.toVector().subtract(origin.toVector()).multiply(progress));

                Location pos = straight.clone()
                        .add(side.clone().multiply(sideOff * bulge))
                        .add(0, vertOff * bulge, 0);

                particleApi.spawnColoredParticles(pos, C_IRON,  1.6f, 3, 0.06, 0.06, 0.06);
                particleApi.spawnColoredParticles(pos, C_STEEL, 1.2f, 2, 0.08, 0.08, 0.08);

                if (t % 3 == 0)
                    pos.getWorld().playSound(pos, Sound.BLOCK_NETHERITE_BLOCK_HIT, 0.2f, 1.5f + idx * 0.05f);

                if (t == steps) {
                    target.damage(damage, p);
                    Vector pull = p.getLocation().toVector().subtract(target.getLocation().toVector());
                    if (isVecFinite(pull) && pull.lengthSquared() > 0.01) {
                        pull.normalize().multiply(0.35).setY(0.12);
                        target.setVelocity(target.getVelocity().add(pull));
                    }
                    particleApi.spawnColoredParticles(targetPoint, C_SPARK, 1.8f, 4, 0.15, 0.15, 0.15);
                    target.getWorld().playSound(targetPoint, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.6f);
                }

                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }

    private void tendrilBurst(Player p) {
        if (onCd(CD_BURST, p, this)) return;

        final Location origin = originBehindBack(p);
        final int spikes = 10;
        final double radius = 4.5;

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK,      1.0f, 0.3f);

        for (int i = 0; i < spikes; i++) {
            double angle = (Math.PI * 2 / spikes) * i;
            final Location dir = p.getLocation().clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

            new BukkitRunnable() {
                int t = 0;
                final int steps = 6;

                @Override public void run() {
                    if (t > steps) { cancel(); return; }

                    double progress = (double) t / steps;
                    Location pos = origin.clone().add(
                            dir.toVector().subtract(origin.toVector()).multiply(progress)).add(0, 1, 0);

                    particleApi.spawnColoredParticles(pos, C_IRON, 1.6f, 3, 0.08, 0.08, 0.08);
                    particleApi.spawnColoredParticles(pos, C_RUST, 1.2f, 2, 0.08, 0.08, 0.08);

                    if (t == steps) {
                        for (Entity e : pos.getWorld().getNearbyEntities(pos, 1.1, 1.1, 1.1)) {
                            if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                            LivingEntity le = (LivingEntity) e;
                            le.damage(7.0, p);
                            Vector away = knockbackVector(p.getLocation(), le, 1.3, 0.45);
                            le.setVelocity(away);
                        }
                        particleApi.spawnColoredParticles(pos, C_SPARK, 1.5f, 3, 0.1, 0.1, 0.1);
                    }
                    t++;
                }
            }.runTaskTimer(magicPlugin, 0L, 1L);
        }

        sendActionBar(p, "§8✸ Tendril Burst");
        addCd(CD_BURST, p);
    }

    private void tendrilGrasp(Player p) {
        if (onCd(CD_GRASP, p, this)) return;

        LivingEntity target = getInSight(p, 14, 0.85);
        if (target == null) { sendActionBar(p, "§7No target in sight!"); return; }

        final LivingEntity tgt = target;
        final Location origin  = originBehindBack(p);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);

        new BukkitRunnable() {
            int t = 0;
            final int steps = 10;

            @Override public void run() {
                if (t > steps || !tgt.isValid() || tgt.isDead()) { cancel(); return; }

                double progress = (double) t / steps;
                Location targetPoint = tgt.getLocation().clone().add(0, 1, 0);
                Location pos = origin.clone().add(
                        targetPoint.toVector().subtract(origin.toVector()).multiply(progress));

                particleApi.spawnColoredParticles(pos, C_IRON,  1.8f, 4, 0.05, 0.05, 0.05);
                particleApi.spawnColoredParticles(pos, C_STEEL, 1.3f, 2, 0.06, 0.06, 0.06);

                if (t == steps) {
                    tgt.damage(5.0, p);
                    new BukkitRunnable() {
                        int pull = 0;
                        @Override public void run() {
                            if (pull > 8 || !tgt.isValid() || tgt.isDead()) { cancel(); return; }
                            Vector toCaster = p.getLocation().toVector().subtract(tgt.getLocation().toVector());
                            if (isVecFinite(toCaster) && toCaster.lengthSquared() > 1.5) {
                                toCaster.normalize().multiply(0.85).setY(0.15);
                                tgt.setVelocity(toCaster);
                            } else {
                                cancel();
                            }
                            particleApi.spawnColoredParticles(tgt.getLocation().add(0, 1, 0), C_RUST, 1.4f, 2, 0.1, 0.1, 0.1);
                            pull++;
                        }
                    }.runTaskTimer(magicPlugin, 0L, 1L);

                    tgt.getWorld().playSound(targetPoint, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.8f);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);

        sendActionBar(p, "§8⛓ Tendril Grasp");
        addCd(CD_GRASP, p);
    }

    private void tendrilWall(Player p) {
        if (onCd(CD_WALL, p, this)) return;
        if (wallTask != null) { wallTask.cancel(); wallTask = null; }

        Vector fwd   = p.getLocation().getDirection().clone().setY(0).normalize();
        Vector right = new Vector(-fwd.getZ(), 0, fwd.getX());
        Location wallCenter = p.getLocation().clone().add(fwd.clone().multiply(3.0));
        final Location origin = originBehindBack(p);

        final List<Location> spikePoints = new ArrayList<>();
        for (int i = -3; i <= 3; i++) {
            spikePoints.add(wallCenter.clone().add(right.clone().multiply(i)));
        }

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HOGLIN_ATTACK, 0.7f, 0.6f);

        for (int i = 0; i < spikePoints.size(); i++) {
            final Location dest = spikePoints.get(i);
            final int delay = Math.abs(i - 3) * 2;
            new BukkitRunnable() {
                @Override public void run() {
                    riseTendril(origin, dest);
                }
            }.runTaskLater(magicPlugin, delay);
        }

        wallTask = new BukkitRunnable() {
            int life = 0;
            @Override public void run() {
                if (life > 100) { cancel(); wallTask = null; return; }

                for (Location loc : spikePoints) {
                    if (life % 5 == 0)
                        particleApi.spawnColoredParticles(loc.clone().add(0, 1, 0), C_IRON, 1.3f, 1, 0.15, 0.4, 0.15);

                    for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0, 1, 0), 0.7, 1.0, 0.7)) {
                        if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                        LivingEntity le = (LivingEntity) e;
                        applyPotion(le, PotionEffectType.SLOWNESS, 10, 2);
                        if (life % 10 == 0) {
                            le.damage(4.0, p);
                            particleApi.spawnColoredParticles(le.getLocation().add(0, 1, 0), C_SPARK, 1.4f, 3, 0.1, 0.1, 0.1);
                        }
                    }
                }
                life++;
            }
        };
        wallTask.runTaskTimer(magicPlugin, 14L, 1L);

        sendActionBar(p, "§8▦ Tendril Wall");
        addCd(CD_WALL, p);
    }

    private void riseTendril(Location origin, Location dest) {
        new BukkitRunnable() {
            int t = 0;
            final int steps = 7;

            @Override public void run() {
                if (t > steps) { cancel(); return; }
                double progress = (double) t / steps;
                Location pos = origin.clone().add(
                        dest.toVector().subtract(origin.toVector()).multiply(progress)).add(0, progress * 1.2, 0);

                particleApi.spawnColoredParticles(pos, C_IRON,  1.6f, 3, 0.08, 0.08, 0.08);
                particleApi.spawnColoredParticles(pos, C_STEEL, 1.2f, 2, 0.08, 0.08, 0.08);

                if (t % 3 == 0)
                    pos.getWorld().playSound(pos, Sound.BLOCK_NETHERITE_BLOCK_HIT, 0.25f, 1.4f);

                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }

    private void tendrilStorm(Player p) {
        if (onCd(CD_STORM, p, this)) return;
        if (stormTask != null) { stormTask.cancel(); stormTask = null; }

        LivingEntity initial = getInSight(p, 6, 0.6);
        if (initial == null) { sendActionBar(p, "§7No target in sight!"); return; }

        final Location origin = originBehindBack(p);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT,  0.8f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK,    1.0f, 0.3f);
        sendActionBar(p, "§c▼▼▼ TENDRIL STORM ▼▼▼");

        stormTask = new BukkitRunnable() {
            int wave = 0;
            final int totalWaves = 16;

            @Override public void run() {
                if (wave >= totalWaves) { cancel(); stormTask = null; return; }

                LivingEntity target = getInSight(p, 6, 0.5);
                if (target == null) target = getNearestEnemy(p, 6);

                if (target != null) {
                    final LivingEntity tgt = target;
                    final double sideOff = (rng.nextDouble() - 0.5) * 1.6;
                    final double vertOff = (rng.nextDouble() - 0.5) * 1.2;
                    fireTendril(p, origin, tgt, sideOff, vertOff, 2.5, wave);
                }

                if (wave % 4 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HOGLIN_ATTACK, 0.5f, 0.6f + wave * 0.02f);

                wave++;
            }
        };
        stormTask.runTaskTimer(magicPlugin, 0L, 3L);

        addCd(CD_STORM, p);
    }

    private Location originBehindBack(Player p) {
        Vector back = p.getLocation().getDirection().clone().setY(0).normalize().multiply(-0.6);
        return p.getLocation().clone().add(back).add(0, 1.2, 0);
    }

    @Override
    public void remove() {
        if (wallTask != null)  { wallTask.cancel();  wallTask  = null; }
        if (stormTask != null) { stormTask.cancel(); stormTask = null; }
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§8⚡ 卷须打击";
            case 1: return "§8✸ 卷须爆发";
            case 2: return "§8⛓ 卷须抓取";
            case 3: return "§8▦ 卷须墙";
            case 4: return "§c▼ 卷须风暴 (终极)";
            default: return "§7none";
        }
    }
}

