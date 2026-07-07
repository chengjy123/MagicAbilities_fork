package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
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
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class ShockwavePower extends Power implements IdlePower, Removeable {

    private static final String sw_pulse    = "shockwave.pulse";
    private static final String sw_slam     = "shockwave.slam";
    private static final String sw_blast    = "shockwave.blast";
    private static final String sw_rift     = "shockwave.rift";
    private static final String sw_barrage  = "shockwave.barrage";
    private static final String sw_barrier  = "shockwave.barrier";
    private static final String sw_dash     = "shockwave.dash";
    private static final String sw_counter  = "shockwave.counter";

    private static final Color C_GREY      = Color.fromRGB(180, 180, 185);
    private static final Color C_LIGHT     = Color.fromRGB(230, 235, 245);
    private static final Color C_GOLD      = Color.fromRGB(255, 220, 80);
    private static final Color C_BONE      = Color.fromRGB(245, 240, 225);
    private static final Color[] SW_COLORS = { C_GREY, C_LIGHT, C_BONE };

    private boolean barrierActive = false;
    private BukkitRunnable barrierRunnable = null;

    public ShockwavePower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {

        if (ex instanceof DamagedByExecute) {
            shockwaveCounter((DamagedByExecute) ex);
            return;
        }

        if (!isEnabled()) return;

        if (ex instanceof LeftClickExecute) {
            onLeftClick((LeftClickExecute) ex);
        } else if (ex instanceof RightClickExecute) {
            onRightClick((RightClickExecute) ex);
        } else if (ex instanceof SneakExecute) {
            onSneak((SneakExecute) ex);
        }
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());

        switch (slot) {
            case 0:
                if (onCd(sw_pulse, p, this)) return;
                shockwavePulse(p);
                addCd(sw_pulse, p);
                return;
            case 1:
                if (onCd(sw_slam, p, this)) return;
                seismicSlam(p);
                addCd(sw_slam, p);
                return;
            case 2:
                if (onCd(sw_blast, p, this)) return;
                concussionBlast(p, 1.0, 0);
                addCd(sw_blast, p);
                return;
            case 3:
                if (onCd(sw_rift, p, this)) return;
                sonicRift(p);
                addCd(sw_rift, p);
                return;
            case 4:
                if (onCd(sw_barrage, p, this)) return;
                shockwaveBarrage(p);
                addCd(sw_barrage, p);
                return;
        }
    }

    private void onRightClick(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (barrierActive) return;
        if (onCd(sw_barrier, p, this)) return;
        forceBarrier(p);
        addCd(sw_barrier, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(sw_dash, p, this)) return;
        aftershockDash(p);
        addCd(sw_dash, p);
    }

    private void shockwavePulse(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 1.3f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.8f);

        final double maxRadius = 7.0;
        final Set<Entity> hit = new HashSet<>();
        final Random rng = new Random();

        new BukkitRunnable() {
            double r = 0.5;

            @Override
            public void run() {
                if (r > maxRadius) { cancel(); return; }

                int points = (int)(r * 14);
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location ring = p.getLocation().clone().add(x, 0.15, z);
                    Color c = SW_COLORS[rng.nextInt(SW_COLORS.length)];
                    particleApi.spawnColoredParticles(ring, c, 1.1f, 1, 0.05, 0.05, 0.05);
                    if (rng.nextInt(3) == 0)
                        particleApi.spawnParticles(ring, Particle.CLOUD, 1, 0.1, 0.05, 0.1, 0.01);
                }

                for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), r + 0.6, 1.8, r + 0.6)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(p.getLocation());
                    if (dist > r - 0.5 && dist < r + 0.7) {
                        hit.add(e);
                        ((LivingEntity) e).damage(8.0, p);
                        Vector kb = e.getLocation().subtract(p.getLocation()).toVector()
                                .normalize().multiply(2.2).setY(0.35);
                        e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.4, 0));
                        particleApi.spawnParticles(e.getLocation().add(0, 1, 0),
                                Particle.EXPLOSION_EMITTER, 1, 0.1, 0.1, 0.1, 0);
                        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.6f);
                    }
                }

                r += 0.55;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void seismicSlam(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 0.6f);

        p.setVelocity(new Vector(0, 1.6, 0));

        new BukkitRunnable() {
            int t = 0;
            boolean diving = false;

            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                t++;

                if (t == 12) {
                    diving = true;
                    p.setVelocity(new Vector(0, -2.8, 0));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.5f);
                }

                if (diving) {

                    particleApi.spawnParticles(p.getLocation().add(0, 1, 0),
                            Particle.CLOUD, 10, 0.25, 0.1, 0.25, 0.05);
                    particleApi.spawnColoredParticles(p.getLocation().add(0, 1, 0),
                            C_GREY, 1.2f, 5, 0.2, 0.05, 0.2);

                    if (p.isOnGround() || t > 50) {
                        slamImpact(p);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void slamImpact(Player p) {
        Location loc = p.getLocation();
        p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.8f);
        p.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1f, 0.5f);

        particleApi.spawnParticles(loc, Particle.EXPLOSION_EMITTER, 3, 1.5, 0.2, 1.5, 0.1);
        particleApi.spawnParticles(loc, Particle.CLOUD, 80, 2.0, 0.3, 2.0, 0.15);
        particleApi.spawnColoredParticles(loc, C_LIGHT, 1.5f, 60, 2.0, 0.2, 2.0);
        particleApi.spawnParticles(loc, Particle.BLOCK,
                50, 1.5, 0.1, 1.5, 0.1, Material.STONE.createBlockData());

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5.0, 3.0, 5.0)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            double dmg = Math.max(6, 20 - dist * 2.0);
            ((LivingEntity) e).damage(dmg, p);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 60, 3);
            Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(2.5).setY(0.6);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.6, 0));
        }
    }

    private void concussionBlast(Player p, double damageMult, int yawOffset) {
        ArmorStand proj = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        if (yawOffset != 0) {
            dir = rotateY(dir, yawOffset);
        }
        final Vector finalDir = dir;
        Random rng = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.6f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (proj.isDead() || t > 60) { safeRemove(proj); cancel(); return; }

                proj.teleport(proj.getLocation().add(finalDir.clone().multiply(1.8)));
                Location loc = proj.getLocation();

                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 40 + i * 120);
                    Vector ring = new Vector(Math.cos(a) * 0.4, Math.sin(a) * 0.3, Math.sin(a) * 0.4);
                    particleApi.spawnColoredParticles(loc.clone().add(ring),
                            SW_COLORS[rng.nextInt(SW_COLORS.length)], 1f, 2, 0.05, 0.05, 0.05);
                }
                particleApi.spawnParticles(loc, Particle.CLOUD, 3, 0.1, 0.1, 0.1, 0.02);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.2, 1.2, 1.2)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        concussionExplode(loc, p, damageMult);
                        safeRemove(proj); cancel(); return;
                    }
                }

                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    concussionExplode(loc, p, damageMult);
                    safeRemove(proj); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void concussionExplode(Location loc, Player p, double mult) {
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.1f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.6f);

        particleApi.spawnParticles(loc, Particle.CLOUD, 80, 1.0, 1.0, 1.0, 0.2);
        particleApi.spawnColoredParticles(loc, C_LIGHT, 1.5f, 40, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(loc, C_GOLD, 1.3f, 20, 0.5, 0.5, 0.5);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(loc));
            double dmg = Math.max(5, 16 * mult - dist * 2.0);
            ((LivingEntity) e).damage(dmg, p);

            applyPotion((LivingEntity) e, PotionEffectType.NAUSEA, 80, 1);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 40, 1);
            Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(2.0).setY(0.45);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.45, 0));
        }
    }

    private void sonicRift(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 2f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0).normalize();
        Location start = p.getEyeLocation().clone();
        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step > 20) { cancel(); return; }

                Location cur = start.clone().add(dir.clone().multiply(step * 1.2));

                for (int side = -1; side <= 1; side += 2) {
                    for (int h = 0; h < 3; h++) {
                        double wobble = Math.sin(step * 0.8 + h) * 0.4;
                        Vector perp = new Vector(-dir.getZ(), 0, dir.getX()).multiply(wobble * side);
                        Location pt = cur.clone().add(0, h * 0.5, 0).add(perp);
                        Color c = rng.nextBoolean() ? C_GREY : C_LIGHT;
                        particleApi.spawnColoredParticles(pt, c, 1.2f, 1, 0.04, 0.04, 0.04);
                        if (rng.nextInt(4) == 0)
                            particleApi.spawnParticles(pt, Particle.CLOUD, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 1.5, 2.0, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(14, p);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 50, 2);
                    Vector kb = dir.clone().multiply(1.8).setY(0.3);
                    e.setVelocity(kb);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 0.8f);
                }

                if (!cur.getBlock().isPassable()) { cancel(); return; }
                step++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void shockwaveBarrage(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1.2f);

        int bolts = 5;
        int startAngle = -20;
        for (int i = 0; i < bolts; i++) {
            final int yawOff = startAngle + i * 10;
            new BukkitRunnable() {
                @Override
                public void run() {
                    concussionBlast(p, 0.55, yawOff);
                }
            }.runTaskLater(magicPlugin, i * 3L);
        }
    }

    private void forceBarrier(Player p) {
        barrierActive = true;
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
        applyPotion(p, PotionEffectType.RESISTANCE, 100, 1);

        Random rng = new Random();

        barrierRunnable = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= 80 || !p.isOnline()) {
                    barrierActive = false;
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 2f);
                    removePotion(p, PotionEffectType.RESISTANCE);
                    cancel();
                    return;
                }

                double radius = 2.2;

                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5 + t * 6);
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    Location pt = p.getLocation().clone().add(x, 1.1, z);
                    Color c = SW_COLORS[rng.nextInt(SW_COLORS.length)];
                    particleApi.spawnColoredParticles(pt, c, 1f, 1, 0.05, 0.05, 0.05);

                    double a2 = Math.toRadians(i * 22.5 - t * 5);
                    double x2 = Math.cos(a2) * radius * 0.6;
                    double z2 = Math.sin(a2) * radius * 0.6;
                    Location pt2 = p.getLocation().clone().add(x2, 1.8, z2);
                    particleApi.spawnColoredParticles(pt2, C_LIGHT, 1.1f, 1, 0.04, 0.04, 0.04);
                }

                if (t % 4 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius + 0.3, radius + 0.3, radius + 0.3)) {
                        if (e.equals(p) || e instanceof ArmorStand) continue;
                        if (!(e instanceof LivingEntity)) continue;
                        Vector kb = e.getLocation().subtract(p.getLocation()).toVector()
                                .normalize().multiply(2.8).setY(0.5);
                        e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.5, 0));
                        ((LivingEntity) e).damage(3, p);
                        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.4f, 1.8f);
                    }
                }
                t++;
            }
        };
        barrierRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void aftershockDash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.5f);

        Vector dir = p.getLocation().getDirection().clone().setY(0.15).normalize();
        p.setVelocity(dir.clone().multiply(2.5));

        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 14) { cancel(); return; }

                Location loc = p.getLocation().add(0, 0.8, 0);

                particleApi.spawnParticles(loc, Particle.CLOUD, 15, 0.3, 0.25, 0.3, 0.08);
                particleApi.spawnColoredParticles(loc, C_GREY, 1.2f, 6, 0.25, 0.2, 0.25);
                if (rng.nextInt(3) == 0)
                    particleApi.spawnColoredParticles(loc, C_GOLD, 1.1f, 2, 0.15, 0.1, 0.15);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.6, 1.6, 1.6)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(9, p);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 60, 4);
                    applyPotion((LivingEntity) e, PotionEffectType.NAUSEA, 40, 1);
                    Vector kb = dir.clone().multiply(1.6).setY(0.5);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.5, 0));
                    loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.7f, 0.9f);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void shockwaveCounter(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (addCdFixed_safe(sw_counter, p, 4.0)) return;

        Location loc = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.8f);
        particleApi.spawnParticles(loc, Particle.CLOUD, 30, 0.8, 0.8, 0.8, 0.12);
        particleApi.spawnColoredParticles(loc, C_LIGHT, 1.2f, 20, 0.6, 0.6, 0.6);

        for (Entity e : p.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(1.8).setY(0.4);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.4, 0));
            ((LivingEntity) e).damage(2, p);
        }
    }

    private boolean addCdFixed_safe(String key, Player p, double seconds) {
        if (net.trduc.magicabilitiesfork.cooldowns.CooldownApi.isOnCooldown(key, p)) return true;
        addCdFixed(key, p, seconds);
        return false;
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (isAuraEnabled(p)) {
                    particleApi.spawnColoredParticles(
                            p.getLocation().clone().add(0, 0.08, 0),
                            C_GREY, 0.9f, 1, 0.3, 0.01, 0.3);
                    particleApi.spawnParticles(
                            p.getLocation().clone().add(0, 0.1, 0),
                            Particle.CLOUD, 1, 0.25, 0.01, 0.25, 0.003);
                }
            }
        };
        r.runTaskTimer(magicPlugin, 0, 18);
        return r;
    }

    @Override
    public void remove() {
        if (barrierRunnable != null) {
            barrierRunnable.cancel();
            barrierRunnable = null;
        }
        barrierActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&7Shockwave Pulse";
            case 1: return "&7Seismic Slam";
            case 2: return "&fConcussion Blast";
            case 3: return "&fSonic Rift";
            case 4: return "&7Shockwave Barrage";
            case 5: return "&fForce Barrier";
            case 6: return "&7Aftershock Dash";
            default: return "&7none";
        }
    }
}

