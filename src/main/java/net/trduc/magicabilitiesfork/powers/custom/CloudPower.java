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
import static net.trduc.magicabilitiesfork.cooldowns.CooldownApi.isOnCooldown;

public class CloudPower extends Power implements IdlePower, Removeable {

    private static final String c_bolt    = "cloud.bolt";
    private static final String c_ascend  = "cloud.ascend";
    private static final String c_fog     = "cloud.fog";
    private static final String c_slam    = "cloud.slam";
    private static final String c_barrage = "cloud.barrage";
    private static final String c_step    = "cloud.step";
    private static final String c_dash    = "cloud.dash";
    private static final String c_storm   = "cloud.storm";
    private static final String c_mist    = "cloud.mist";

    private int XP_STORM;

    private static final Color C_WHITE      = Color.fromRGB(255, 255, 255);
    private static final Color C_SKY        = Color.fromRGB(200, 230, 255);
    private static final Color C_CLOUD_GREY = Color.fromRGB(210, 215, 220);
    private static final Color C_ICY        = Color.fromRGB(180, 220, 240);
    private static final Color C_STORM_GREY = Color.fromRGB(130, 140, 150);
    private static final Color C_LIGHTNING  = Color.fromRGB(255, 245, 160);
    private static final Color[] CLOUD_COLS = { C_WHITE, C_SKY, C_CLOUD_GREY, C_ICY };

    private boolean ascending    = false;
    private boolean stormActive  = false;
    private BukkitRunnable hoverTask  = null;
    private BukkitRunnable stormTask  = null;

    public CloudPower(Player owner) {
        super(owner);
        XP_STORM = magicPlugin.getConfig().getInt("cloud.xp.storm", 25);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) { passiveMist((DamagedByExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(c_bolt,    p, this)) return; mistBolt(p, 1.0, 0);  addCd(c_bolt,    p); return;
            case 1: if (onCd(c_ascend,  p, this)) return; cloudAscend(p);        addCd(c_ascend,  p); return;
            case 2: if (onCd(c_fog,     p, this)) return; fogWall(p);            addCd(c_fog,     p); return;
            case 3: if (onCd(c_slam,    p, this)) return; cumulusSlam(p);        addCd(c_slam,    p); return;
            case 4: if (onCd(c_barrage, p, this)) return; cloudBarrage(p);       addCd(c_barrage, p); return;
            case 7:
                if (onCd(c_storm, p, this)) return;
                if (!checkXp(p, XP_STORM, this)) return;
                spendXp(p, XP_STORM);
                stormCalling(p);
                addCd(c_storm, p);
                return;
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (onCd(c_step, p, this)) return;
        skyStep(p);
        addCd(c_step, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(c_dash, p, this)) return;
        nimbusDash(p);
        addCd(c_dash, p);
    }

    private void mistBolt(Player p, double dmgMult, int yawOff) {
        ArmorStand bolt = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        if (yawOff != 0) dir = rotateY(dir, yawOff);
        final Vector fDir = dir;
        Random rng = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.3f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (bolt.isDead() || t > 80) { safeRemove(bolt); cancel(); return; }
                bolt.teleport(bolt.getLocation().add(fDir.clone().multiply(1.4)));
                Location loc = bolt.getLocation();

                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(t * 50 + i * 90);
                    Vector off = new Vector(Math.cos(a) * 0.3, Math.sin(a) * 0.2, Math.sin(a) * 0.3);
                    particleApi.spawnColoredParticles(loc.clone().add(off),
                            CLOUD_COLS[rng.nextInt(CLOUD_COLS.length)], 1.1f, 2, 0.08, 0.08, 0.08);
                }
                particleApi.spawnParticles(loc, Particle.CLOUD, 3, 0.15, 0.1, 0.15, 0.01);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        mistExplosion(loc, p, 3.0, 10 * dmgMult);
                        safeRemove(bolt); cancel(); return;
                    }
                }
                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    mistExplosion(loc, p, 2.0, 7 * dmgMult);
                    safeRemove(bolt); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void mistExplosion(Location loc, Player p, double radius, double damage) {
        loc.getWorld().playSound(loc, Sound.ENTITY_BREEZE_WHIRL, 0.9f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_POWDER_SNOW_BREAK, 0.7f, 0.7f);

        particleApi.spawnParticles(loc, Particle.CLOUD,    80, radius * 0.6, radius * 0.5, radius * 0.6, 0.12);
        particleApi.spawnColoredParticles(loc, C_WHITE,    1.4f, 40, radius * 0.5, radius * 0.4, radius * 0.5);
        particleApi.spawnColoredParticles(loc, C_SKY,      1.2f, 20, radius * 0.4, radius * 0.3, radius * 0.4);
        particleApi.spawnColoredParticles(loc, C_ICY,      1.0f, 15, radius * 0.6, radius * 0.5, radius * 0.6);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(loc));
            double dmg  = Math.max(3, damage - dist * 1.5);
            ((LivingEntity) e).damage(dmg, p);
            applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 60, 0);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS,  50, 1);
            Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(1.4).setY(0.3);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.3, 0));
        }
    }

    private void cloudAscend(Player p) {
        if (ascending) {

            stopAscend(p);
            return;
        }
        ascending = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.6f, 0.6f);

        spawnCloudBurst(p.getLocation().clone().add(0, 1, 0), 30);
        p.setVelocity(new Vector(p.getVelocity().getX(), 1.5, p.getVelocity().getZ()));

        Random rng = new Random();

        hoverTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline() || !ascending) { cancel(); return; }
                if (t > 200) { stopAscend(p); cancel(); return; }

                Vector vel = p.getVelocity();
                if (vel.getY() < -0.1) {
                    p.setVelocity(new Vector(vel.getX(), -0.08, vel.getZ()));
                }
                p.setFallDistance(0);

                applyPotion(p, PotionEffectType.SPEED, 25, 1);
                applyPotion(p, PotionEffectType.JUMP_BOOST, 25, 2);

                if (t % 4 == 0) {
                    Location under = p.getLocation().clone();
                    for (int i = 0; i < 6; i++) {
                        double a = Math.toRadians(i * 60 + t * 15);
                        particleApi.spawnColoredParticles(
                                under.clone().add(Math.cos(a) * 0.6, 0.05, Math.sin(a) * 0.6),
                                CLOUD_COLS[rng.nextInt(CLOUD_COLS.length)], 1.0f, 1, 0.05, 0.01, 0.05);
                    }
                    particleApi.spawnParticles(under, Particle.CLOUD, 3, 0.4, 0.02, 0.4, 0.02);
                }

                if (p.isOnGround() && t > 10) { stopAscend(p); cancel(); return; }
                t++;
            }
        };
        hoverTask.runTaskTimer(magicPlugin, 5L, 1L);
    }

    private void stopAscend(Player p) {
        ascending = false;
        if (hoverTask != null) { hoverTask.cancel(); hoverTask = null; }
        p.setFallDistance(0);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_BREAK, 0.8f, 1.2f);
        spawnCloudBurst(p.getLocation().clone().add(0, 0.5, 0), 20);
    }

    private void fogWall(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WHIRL, 1f, 0.5f);

        Vector fwd  = p.getLocation().getDirection().clone().setY(0).normalize();
        Vector perp = new Vector(-fwd.getZ(), 0, fwd.getX());
        Location base = p.getLocation().clone().add(fwd.clone().multiply(3));

        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 120) { cancel(); return; }

                for (float w = -2.5f; w <= 2.5f; w += 0.5f) {
                    for (float h = 0f; h <= 3f; h += 0.5f) {
                        if (rng.nextInt(3) != 0) continue;
                        Location pt = base.clone().add(perp.clone().multiply(w)).add(0, h, 0);
                        Color c = CLOUD_COLS[rng.nextInt(CLOUD_COLS.length)];
                        particleApi.spawnColoredParticles(pt, c, 1.1f, 1, 0.1, 0.1, 0.1);
                        if (rng.nextInt(4) == 0)
                            particleApi.spawnParticles(pt, Particle.CLOUD, 1, 0.1, 0.1, 0.1, 0.005);
                    }
                }

                if (t % 5 == 0) {
                    for (Entity e : base.getWorld().getNearbyEntities(base, 2.8, 2.0, 2.8)) {
                        if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                        if (!(e instanceof LivingEntity)) continue;

                        Vector toE = e.getLocation().toVector().subtract(base.toVector());
                        double along = toE.dot(fwd);
                        double side  = Math.abs(toE.dot(perp));
                        if (Math.abs(along) < 1.2 && side < 2.6) {
                            hit.add(e);
                            applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 80, 0);
                            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS,  60, 2);
                            ((LivingEntity) e).damage(5, p);
                            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5f, 0.7f);

                            new BukkitRunnable() {
                                @Override public void run() { hit.remove(e.getUniqueId()); }
                            }.runTaskLater(magicPlugin, 40L);
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void cumulusSlam(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 0.7f);
        p.setVelocity(new Vector(0, 1.8, 0));

        new BukkitRunnable() {
            int t = 0;
            boolean diving = false;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                t++;
                if (t == 14) {
                    diving = true;
                    p.setVelocity(new Vector(0, -3.2, 0));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1f, 0.5f);
                }
                if (diving) {

                    particleApi.spawnParticles(p.getLocation().add(0, 1, 0),
                            Particle.CLOUD, 12, 0.3, 0.1, 0.3, 0.04);
                    particleApi.spawnColoredParticles(p.getLocation().add(0, 1, 0),
                            C_WHITE, 1.2f, 4, 0.2, 0.05, 0.2);
                    if (p.isOnGround() || t > 55) {
                        cumulusImpact(p);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void cumulusImpact(Player p) {
        Location loc = p.getLocation();
        p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.6f);
        p.getWorld().playSound(loc, Sound.ENTITY_BREEZE_WHIRL, 1f, 0.5f);
        p.getWorld().playSound(loc, Sound.BLOCK_POWDER_SNOW_BREAK, 1f, 0.4f);

        particleApi.spawnParticles(loc, Particle.CLOUD, 150, 2.5, 0.5, 2.5, 0.18);
        particleApi.spawnColoredParticles(loc, C_WHITE,      1.6f, 80, 2.0, 0.3, 2.0);
        particleApi.spawnColoredParticles(loc, C_SKY,        1.3f, 40, 2.5, 0.4, 2.5);
        particleApi.spawnColoredParticles(loc, C_CLOUD_GREY, 1.1f, 30, 3.0, 0.5, 3.0);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5.5, 3.5, 5.5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(loc));
            double dmg  = Math.max(8, 22 - dist * 2.2);
            ((LivingEntity) e).damage(dmg, p);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 60, 3);
            applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 40, 0);
            Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(2.5).setY(0.6);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.6, 0));
        }
    }

    private void cloudBarrage(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.2f);
        int bolts = 5, start = -20;
        for (int i = 0; i < bolts; i++) {
            final int yaw = start + i * 10;
            final long delay = i * 3L;
            new BukkitRunnable() {
                @Override public void run() { mistBolt(p, 0.55, yaw); }
            }.runTaskLater(magicPlugin, delay);
        }
    }

    private void skyStep(Player p) {
        Location from = p.getLocation().clone();
        Location dest = from.clone().add(0, 8, 0);

        for (int y = 8; y >= 1; y--) {
            Location try_ = from.clone().add(0, y, 0);
            if (try_.getBlock().isPassable() && try_.clone().add(0, 1, 0).getBlock().isPassable()) {
                dest = try_;
                break;
            }
        }

        spawnCloudBurst(from.clone().add(0, 1, 0), 25);
        p.getWorld().playSound(from, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.6f);
        p.teleport(dest);
        spawnCloudBurst(dest.clone().add(0, 1, 0), 25);
        p.getWorld().playSound(dest, Sound.BLOCK_POWDER_SNOW_BREAK, 0.8f, 1.3f);
        p.setFallDistance(0);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ > 25 || p.isOnGround()) { cancel(); return; }
                p.setFallDistance(0);
                Vector v = p.getVelocity();
                if (v.getY() < -0.05) p.setVelocity(new Vector(v.getX(), -0.05, v.getZ()));
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void nimbusDash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.4f);
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();

        dir.setY(Math.max(dir.getY(), 0.0));
        p.setVelocity(dir.multiply(2.6));
        p.setFallDistance(0);

        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 16) { cancel(); return; }
                p.setFallDistance(0);
                Location loc = p.getLocation().add(0, 1, 0);

                particleApi.spawnParticles(loc, Particle.CLOUD, 18, 0.35, 0.2, 0.35, 0.06);
                particleApi.spawnColoredParticles(loc, CLOUD_COLS[rng.nextInt(CLOUD_COLS.length)],
                        1.2f, 6, 0.3, 0.15, 0.3);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(9, p);
                    applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 40, 0);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS,  40, 2);
                    Vector kb = dir.clone().multiply(1.6).setY(0.4);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.4, 0));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void stormCalling(Player p) {
        if (stormActive) return;
        stormActive = true;

        p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "⛈ 召唤风暴 ⛈");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE,   0.7f, 0.3f);

        Random rng = new Random();

        stormTask = new BukkitRunnable() {
            int t = 0;

            @Override public void run() {
                if (!p.isOnline()) { stormActive = false; cancel(); return; }

                Location center = p.getLocation().clone().add(0, 4, 0);

                if (t < 40) {
                    double grow = (double) t / 40;
                    double r = 4 + grow * 16;

                    for (int i = 0; i < 20; i++) {
                        double a = Math.toRadians(rng.nextDouble() * 360);
                        double d = rng.nextDouble() * r;
                        Location cp = center.clone().add(Math.cos(a) * d, rng.nextDouble() * 2 - 1, Math.sin(a) * d);
                        particleApi.spawnColoredParticles(cp, C_STORM_GREY, 1.3f, 2, 0.5, 0.2, 0.5);
                        particleApi.spawnParticles(cp, Particle.CLOUD, 2, 0.4, 0.2, 0.4, 0.02);
                    }

                    if (t % 12 == 0) strikeLightning(p, center, 10, rng, false);
                }

                else if (t < 120) {

                    for (int i = 0; i < 35; i++) {
                        double a = Math.toRadians(rng.nextDouble() * 360);
                        double d = rng.nextDouble() * 20;
                        Location cp = center.clone().add(Math.cos(a) * d, rng.nextDouble() * 3 - 1.5, Math.sin(a) * d);
                        particleApi.spawnColoredParticles(cp, C_STORM_GREY, 1.4f, 2, 0.6, 0.3, 0.6);
                        particleApi.spawnParticles(cp, Particle.CLOUD, 3, 0.5, 0.25, 0.5, 0.03);
                    }

                    if (t % 7 == 0) strikeLightning(p, center, 20, rng, true);

                    if (t % 10 == 0) {
                        for (Entity e : center.getWorld().getNearbyEntities(center, 20, 5, 20)) {
                            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                            ((LivingEntity) e).damage(2.5, p);
                            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 25, 1);
                        }
                    }
                }

                else {
                    stormFinale(p, center, rng);
                    stormActive = false;
                    cancel();
                    return;
                }

                t++;
            }
        };
        stormTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void strikeLightning(Player p, Location center, double radius, Random rng, boolean damage) {
        double a = Math.toRadians(rng.nextDouble() * 360);
        double d = rng.nextDouble() * radius;
        Location target = center.clone().add(Math.cos(a) * d, -3, Math.sin(a) * d);
        adjustToGround(target);

        target.getWorld().strikeLightningEffect(target);
        target.getWorld().playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f + rng.nextFloat() * 0.4f);

        particleApi.spawnColoredParticles(target.clone().add(0, 1, 0), C_LIGHTNING, 1.5f, 20, 0.15, 0.6, 0.15);

        if (damage) {
            for (Entity e : target.getWorld().getNearbyEntities(target, 2.5, 2.5, 2.5)) {
                if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                ((LivingEntity) e).damage(6, p);
                applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 30, 1);
            }
        }
    }

    private void stormFinale(Player p, Location center, Random rng) {
        p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.3f);
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE,         0.8f, 0.5f);

        for (int i = 0; i < 5; i++) {
            final int fi = i;
            new BukkitRunnable() {
                @Override public void run() { strikeLightning(p, center, 15, rng, true); }
            }.runTaskLater(magicPlugin, fi * 4L);
        }

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 20) { cancel(); return; }
                for (int i = 0; i < 25; i++) {
                    double a = Math.toRadians(rng.nextDouble() * 360);
                    double d = rng.nextDouble() * 20;
                    Location cp = center.clone().add(Math.cos(a) * d, rng.nextDouble() * 3 - 1, Math.sin(a) * d);
                    particleApi.spawnColoredParticles(cp, C_WHITE, 1.2f, 2, 0.5, 0.3, 0.5);
                    particleApi.spawnParticles(cp, Particle.CLOUD, 2, 0.5, 0.2, 0.5, 0.04);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void passiveMist(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (isOnCooldown(c_mist, p)) return;
        Location loc = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(loc, Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.6f);
        particleApi.spawnParticles(loc, Particle.CLOUD, 40, 0.8, 0.6, 0.8, 0.1);
        particleApi.spawnColoredParticles(loc, C_WHITE, 1.2f, 20, 0.6, 0.5, 0.6);

        if (ex.getDamager() instanceof LivingEntity) {
            LivingEntity att = (LivingEntity) ex.getDamager();
            applyPotion(att, PotionEffectType.BLINDNESS, 40, 0);
            applyPotion(att, PotionEffectType.SLOWNESS,  30, 1);
        }
        addCdFixed(c_mist, p, 5.0);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                p.setFallDistance(0);
                if (isAuraEnabled(p)) {

                    particleCircle(p.getLocation().clone().add(0, 0.07, 0),
                            0.65, C_SKY, 0.9f, 5, t * 22);
                    particleApi.spawnColoredParticles(
                            p.getLocation().clone().add(0, 0.05, 0),
                            C_WHITE, 0.8f, 1, 0.3, 0.01, 0.3);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 28);
        return r;
    }

    @Override
    public void remove() {
        if (hoverTask  != null) { hoverTask.cancel();  hoverTask  = null; }
        if (stormTask  != null) { stormTask.cancel();  stormTask  = null; }
        ascending   = false;
        stormActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§f迷雾箭";
            case 1: return "§f云升";
            case 2: return "§f雾墙";
            case 3: return "§f积雨云冲击";
            case 4: return "§f云层弹幕";
            case 5: return "§b天空步";
            case 6: return "§f雨云冲刺";
            case 7: return "§b§l召唤风暴 §e[ULT]";
            default: return "§7none";
        }
    }

    private void spawnCloudBurst(Location loc, int count) {
        Random rng = new Random();
        particleApi.spawnParticles(loc, Particle.CLOUD, count, 0.5, 0.3, 0.5, 0.1);
        particleApi.spawnColoredParticles(loc, C_WHITE, 1.3f, count / 2, 0.4, 0.25, 0.4);
        particleApi.spawnColoredParticles(loc, C_SKY,   1.1f, count / 3, 0.5, 0.3, 0.5);
    }
}

