package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilities.MagicAbilities.magicPlugin;
import static net.trduc.magicabilities.MagicAbilities.particleApi;
import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.misc.PowerUtils.*;
import static net.trduc.magicabilities.players.PowerPlayer.players;

public class BloodPower extends Power implements IdlePower {

    private static final String CD_SCYTHE    = "blood.scythe";
    private static final String CD_PUPPET    = "blood.puppet";
    private static final String CD_BURST     = "blood.burst";
    private static final String CD_VEIL      = "blood.veil";
    private static final String CD_SACRIFICE = "blood.sacrifice";

    private static final Color C_DARK   = Color.fromRGB(100, 0,   0);
    private static final Color C_MID    = Color.fromRGB(170, 10,  10);
    private static final Color C_BRIGHT = Color.fromRGB(220, 40,  40);

    private final Random rng = new Random();

    private boolean veilActive = false;
    private final Set<UUID> veilBlinded = new HashSet<>();

    public BloodPower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DealDamageExecute) {
            passiveLifesteal((DealDamageExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) {
            onLeft((LeftClickExecute) ex);
        }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: bleedingScythe(p);  break;
            case 1: bloodPuppet(p);     break;
            case 2: sanguineBurst(p);   break;
            case 3: crimsonVeil(p);     break;
            case 4: bloodSacrifice(p);  break;
        }
    }

    private void bleedingScythe(Player p) {
        if (onCd(CD_SCYTHE, p, this)) return;
        if (!costHp(p, 2.0)) return;

        Vector forward = p.getEyeLocation().getDirection().clone();
        forward.setY(0);
        if (forward.lengthSquared() < 0.01) forward = new Vector(1, 0, 0);
        forward.normalize();

        int[] offsets = {-40, -20, 0, 20, 40};
        for (int deg : offsets) {
            final Vector dir = rotateY(forward.clone(), deg).add(new Vector(0, 0.05, 0)).normalize();
            final ArmorStand blade = spawnProjectile(p);
            new BukkitRunnable() {
                int t = 0;
                final List<Entity> hit = new ArrayList<>();
                @Override public void run() {
                    if (blade.isDead() || t > 22) { safeRemove(blade); cancel(); return; }
                    blade.teleport(blade.getLocation().add(dir.clone().multiply(1.2)));

                    particleApi.spawnColoredParticles(blade.getLocation(), C_BRIGHT, 1.2f, 2, 0.08, 0.08, 0.08);

                    for (Entity e : blade.getLocation().getChunk().getEntities()) {
                        if (e instanceof ArmorStand || e.equals(p) || hit.contains(e)) continue;
                        if (e instanceof LivingEntity && blade.getLocation().distanceSquared(e.getLocation()) <= 3.5) {
                            ((LivingEntity) e).damage(5.0, p);
                            hit.add(e);
                            spawnBloodBurst(blade.getLocation(), 8);
                            safeRemove(blade); cancel(); return;
                        }
                    }
                    if (!blade.getLocation().getBlock().isPassable()) {
                        spawnBloodBurst(blade.getLocation(), 6);
                        safeRemove(blade); cancel(); return;
                    }
                    t++;
                }
            }.runTaskTimer(magicPlugin, 0, 1);
        }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        addCd(CD_SCYTHE, p);
    }

    private void bloodPuppet(Player p) {
        if (onCd(CD_PUPPET, p, this)) return;
        LivingEntity puppet = getInSight(p, 12, 0.92);
        if (puppet == null) puppet = getNearestTarget(p, 6);
        if (puppet == null) { sendActionBar(p, "§cNo target!"); return; }
        if (!costHp(p, 3.0)) return;

        final LivingEntity target = puppet;

        target.setVelocity(new Vector(0, 0.8, 0));
        applyPotion(target, PotionEffectType.SLOWNESS, 20, 10);

        particleLine(p.getLocation().add(0,1,0), target.getLocation().add(0,1,0), 0.4, C_MID, 2f);

        new BukkitRunnable() {
            @Override public void run() {
                if (!target.isValid()) return;

                LivingEntity throwTo = null;
                double best = 20;
                for (Entity e : target.getWorld().getNearbyEntities(target.getLocation(), 12, 12, 12)) {
                    if (!(e instanceof LivingEntity) || e.equals(p) || e.equals(target)) continue;
                    double d = e.getLocation().distanceSquared(target.getLocation());
                    if (d < best) { best = d; throwTo = (LivingEntity) e; }
                }

                Vector throwVec;
                if (throwTo != null) {
                    throwVec = throwTo.getLocation().subtract(target.getLocation()).toVector().normalize().multiply(1.8).add(new Vector(0, 0.3, 0));
                } else {

                    throwVec = p.getEyeLocation().getDirection().clone().multiply(1.6).add(new Vector(0, 0.3, 0));
                }

                applyPotion(target, PotionEffectType.MINING_FATIGUE, 20 * 2, 0);
                target.setVelocity(throwVec);
                spawnBloodBurst(target.getLocation().add(0,1,0), 14);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.4f);

                final LivingEntity finalThrowTo = throwTo;
                new BukkitRunnable() {
                    @Override public void run() {
                        if (target.isValid()) {
                            target.damage(7.0, p);
                        }
                        if (finalThrowTo != null && finalThrowTo.isValid()) {
                            finalThrowTo.damage(5.0, p);
                            spawnBloodBurst(finalThrowTo.getLocation().add(0,1,0), 10);
                        }
                    }
                }.runTaskLater(magicPlugin, 10);
            }
        }.runTaskLater(magicPlugin, 10);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);
        addCd(CD_PUPPET, p);
    }

    private void sanguineBurst(Player p) {
        if (onCd(CD_BURST, p, this)) return;
        LivingEntity target = getInSight(p, 14, 0.93);
        if (target == null) { sendActionBar(p, "§cNo target!"); return; }
        if (!costHp(p, 5.0)) return;

        final LivingEntity finalTarget = target;
        final Location tLoc = finalTarget.getLocation().clone().add(0, 1, 0);

        particleLine(p.getEyeLocation(), tLoc, 0.5, C_MID, 2f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 3) {

                    for (int ring = 1; ring <= 3; ring++) {
                        final int r = ring;
                        new BukkitRunnable() {
                            @Override public void run() {
                                particleCircle(tLoc, r * 1.2, C_BRIGHT, 2.5f, 20, 0);
                                particleCircle(tLoc, r * 1.2, C_DARK, 1.5f, 12, 15);
                            }
                        }.runTaskLater(magicPlugin, r * 2L);
                    }

                    if (finalTarget.isValid()) finalTarget.damage(10.0, p);

                    for (Entity e : tLoc.getWorld().getNearbyEntities(tLoc, 4, 4, 4)) {
                        if (!(e instanceof LivingEntity) || e.equals(p) || e.equals(finalTarget)) continue;
                        ((LivingEntity) e).damage(5.0, p);
                        spawnBloodBurst(e.getLocation().add(0,1,0), 8);
                    }

                    tLoc.getWorld().playSound(tLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.4f);
                    tLoc.getWorld().playSound(tLoc, Sound.ENTITY_PLAYER_HURT, 1f, 0.3f);
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        addCd(CD_BURST, p);
    }

    private void crimsonVeil(Player p) {
        if (onCd(CD_VEIL, p, this)) return;
        if (!costHp(p, 4.0)) return;

        veilActive = true;
        veilBlinded.clear();
        final Location center = p.getLocation().clone();

        for (int i = 0; i < 3; i++) {
            final int fi = i;
            new BukkitRunnable() {
                @Override public void run() {
                    particleCircle(center.clone().add(0, 0.05, 0), 5.0, C_MID, 2f, 36, fi * 40);
                    particleCircle(center.clone().add(0, 0.5, 0),  5.0, C_DARK, 1.5f, 24, fi * 40 + 20);
                }
            }.runTaskLater(magicPlugin, i * 3L);
        }

        p.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 0.3f);

        new BukkitRunnable() {
            int seconds = 0;

            @Override public void run() {
                if (seconds >= 5 || !veilActive) {
                    veilActive = false;
                    veilBlinded.clear();
                    cancel();
                    return;
                }

                if (isAuraEnabled(p)) {
                    particleCircle(p.getLocation().clone().add(0, 1, 0), 5.0, C_MID, 2f, 40, seconds * 30);
                    for (int i = 0; i < 10; i++) {
                        double a = rng.nextDouble() * Math.PI * 2;
                        double r = 3.5 + rng.nextDouble() * 1.5;
                        Location drop = p.getLocation().clone().add(Math.cos(a)*r, 2.5, Math.sin(a)*r);
                        particleApi.spawnColoredParticles(drop, C_BRIGHT, 1.5f, 1, 0.1, 0.3, 0.1);
                    }
                }

                for (Entity e : p.getNearbyEntities(5, 5, 5)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    le.damage(2.0, p);
                    if (!veilBlinded.contains(e.getUniqueId())) {
                        applyPotion(le, PotionEffectType.BLINDNESS, 20 * 2, 0);
                        veilBlinded.add(e.getUniqueId());
                    }
                    spawnBloodBurst(e.getLocation().add(0,1,0), 4);
                }

                seconds++;
            }
        }.runTaskTimer(magicPlugin, 0, 20);

        addCd(CD_VEIL, p);
    }

    private void bloodSacrifice(Player p) {
        if (onCd(CD_SACRIFICE, p, this)) return;
        double current = p.getHealth();
        if (current <= 3.0) { sendActionBar(p, "§cNot enough HP to sacrifice!"); return; }

        double sacrifice = Math.max(2.0, Math.min(8.0, current * 0.30));

        if (current - sacrifice < 1.0) sacrifice = current - 1.0;
        if (sacrifice < 1.0) { sendActionBar(p, "§cNot enough HP to sacrifice!"); return; }

        safeSetHealth(p, current - sacrifice);
        final double damage = sacrifice * 2.2;

        final Location origin = p.getLocation().clone().add(0, 1, 0);
        new BukkitRunnable() {
            double rad = 0.3;
            int t = 0;
            @Override public void run() {
                if (t > 14) { cancel(); return; }
                particleCircle(origin, rad, C_DARK,   3f, 20, t * 25);
                particleCircle(origin, rad, C_BRIGHT, 2f, 12, t * 25 + 15);
                rad += 0.55;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        for (Entity e : p.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof LivingEntity) || e.equals(p)) continue;
            ((LivingEntity) e).damage(damage, p);
            spawnBloodBurst(e.getLocation().add(0, 1, 0), 12);
        }

        p.getWorld().playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 1.5f);
        p.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.3f);

        sendActionBar(p, String.format("§c⚠ Sacrificed §f%.1f §cHP — dealt §f%.1f §cdamage!", sacrifice, damage));
        addCd(CD_SACRIFICE, p);
    }

    private void passiveLifesteal(DealDamageExecute ex) {
        if (!(ex.getRawEvent() instanceof EntityDamageByEntityEvent)) return;
        double dmg = ((EntityDamageByEntityEvent) ex.getRawEvent()).getDamage();
        if (dmg <= 0) return;
        safeHeal(getOwner(), dmg * 0.06);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (isAuraEnabled(p)) {

                    for (int i = 0; i < 2; i++) {
                        double a = rng.nextDouble() * Math.PI * 2;
                        Location drop = p.getLocation().clone().add(Math.cos(a) * 0.6, 1.8 + rng.nextDouble() * 0.4, Math.sin(a) * 0.6);
                        particleApi.spawnColoredParticles(drop, i == 0 ? C_MID : C_DARK, 1.5f, 1, 0.05, 0.05, 0.05);
                    }

                    particleCircle(p.getLocation().clone().add(0, 0.05, 0), 0.55, C_DARK, 1f, 8, t * 22);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 18);
        return r;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§cBleeding Scythe";
            case 1: return "§cBlood Puppet";
            case 2: return "§cSanguine Burst";
            case 3: return "§cCrimson Veil";
            case 4: return "§cBlood Sacrifice";
            default: return "§7none";
        }
    }

    private boolean costHp(Player p, double amount) {
        if (p.getHealth() <= amount + 1.0) {
            sendActionBar(p, "§cNot enough HP!");
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BONE_BLOCK_HIT, 0.8f, 0.5f);
            return false;
        }
        safeSetHealth(p, p.getHealth() - amount);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.2, 0), C_BRIGHT, 2f, 6, 0.2, 0.2, 0.2);
        return true;
    }

    private void spawnBloodBurst(Location loc, int count) {
        particleApi.spawnColoredParticles(loc, C_BRIGHT, 2f, count / 2, 0.4, 0.4, 0.4);
        particleApi.spawnColoredParticles(loc, C_DARK,   1.5f, count / 2, 0.3, 0.3, 0.3);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.5f, 0.4f);
    }
}
