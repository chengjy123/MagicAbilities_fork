package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.DamagedExecute;
import net.trduc.magicabilitiesfork.powers.executions.Execute;
import net.trduc.magicabilitiesfork.powers.executions.IdleExecute;
import net.trduc.magicabilitiesfork.powers.executions.LeftClickExecute;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import net.trduc.magicabilitiesfork.data.MessagesManager;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.*;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class MeteorLordPower extends Power implements IdlePower, Removeable {
    private static final String mt_shot     = "meteor.shot";
    private static final String mt_rain     = "meteor.rain";
    private static final String mt_slam     = "meteor.slam";
    private static final String mt_gravity  = "meteor.gravity";
    private static final String mt_armor    = "meteor.armor";
    private static final String mt_extinct  = "meteor.extinction";
    private static final String mt_scorch   = "meteor.scorch";

    private static final Color C_CORE_WHITE = Color.fromRGB(255, 250, 230);
    private static final Color C_HOT_YELLOW = Color.fromRGB(255, 220,  50);
    private static final Color C_METEOR_ORG = Color.fromRGB(255, 120,  20);
    private static final Color C_METEOR_RED = Color.fromRGB(220,  45,  10);
    private static final Color C_SMOKE_GRAY = Color.fromRGB(100,  90,  85);
    private static final Color C_SMOKE_DARK = Color.fromRGB( 55,  50,  48);
    private static final Color C_EMBER      = Color.fromRGB(255, 160,  30);

    private static final Color[] METEOR_TRAIL = {
            C_CORE_WHITE, C_HOT_YELLOW, C_METEOR_ORG, C_METEOR_RED
    };
    private static final Color[] SMOKE_TRAIL  = {
            C_SMOKE_GRAY, C_SMOKE_DARK
    };

    private boolean armorActive = false;
    private BukkitRunnable armorTask = null;
    private final MessagesManager messages = MessagesManager.getInstance();

    public MeteorLordPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute) {
            onDamaged((DamagedExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeft((LeftClickExecute) ex);
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(mt_shot, p, this)) return; meteorShot(p);    addCd(mt_shot, p);    return;
            case 1: if (onCd(mt_rain, p, this)) return; meteorRain(p);    addCd(mt_rain, p);    return;
            case 2: if (onCd(mt_slam, p, this)) return; impactSlam(p);    addCd(mt_slam, p);    return;
            case 3: if (onCd(mt_gravity, p, this)) return; gravityPull(p);   addCd(mt_gravity, p); return;
            case 4:
                if (armorActive) { p.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("powers.meteor_lord.armor_active"))); return; }
                if (onCd(mt_armor, p, this)) return;
                meteorArmor(p);
                addCd(mt_armor, p);
                return;
            case 5: if (onCd(mt_extinct, p, this)) return; extinctionEvent(p); addCd(mt_extinct, p);
        }
    }

    private void meteorShot(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT,     1f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 0.5f);

        final Location start = p.getEyeLocation().clone();
        final Vector   dir   = p.getEyeLocation().getDirection().clone().normalize().multiply(1.1);
        final Random   rng   = new Random();

        new BukkitRunnable() {
            Location cur = start.clone();
            int t = 0;

            @Override public void run() {
                if (t > 32) { meteorImpact(cur, p, 8, 4, 2.5f); cancel(); return; }
                cur.add(dir);

                if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                    meteorImpact(cur, p, 16, 8, 2.5f);
                    cancel(); return;
                }

                particleApi.spawnColoredParticles(cur, C_CORE_WHITE, 1.8f, 4, 0.06, 0.06, 0.06);
                for (int i = 0; i < 3; i++) {
                    Vector offset = new Vector(
                            (rng.nextDouble()-0.5)*0.25,
                            (rng.nextDouble()-0.5)*0.25,
                            (rng.nextDouble()-0.5)*0.25);
                    particleApi.spawnColoredParticles(
                            cur.clone().add(offset),
                            METEOR_TRAIL[(t + i) % METEOR_TRAIL.length], 1.3f, 2, 0.08, 0.08, 0.08);
                }

                particleApi.spawnParticles(cur, Particle.FLAME, 3, 0.1, 0.1, 0.1, 0.06);
                Location smokePos = cur.clone().subtract(dir.clone().normalize().multiply(0.5));
                particleApi.spawnColoredParticles(smokePos,
                        SMOKE_TRAIL[t % 2], 0.9f, 2, 0.15, 0.15, 0.15);
                particleApi.spawnParticles(smokePos, Particle.CAMPFIRE_COSY_SMOKE,
                        1, 0.08, 0.08, 0.08, 0.01);

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 0.8, 0.8, 0.8)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    meteorImpact(cur, p, 16, 8, 2.5f);
                    cancel(); return;
                }

                if (t % 6 == 0)
                    p.getWorld().playSound(cur, Sound.ENTITY_BLAZE_HURT, 0.2f, 0.7f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void meteorRain(Player p) {
        Location center = getRaycast(p, 30);
        Random rng = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN,  0.6f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT,   1f,   0.4f);

        for (int i = 0; i < 5; i++) {
            final Location impact = center.clone().add(
                    (rng.nextDouble()-0.5) * 9,
                    0,
                    (rng.nextDouble()-0.5) * 9);
            adjustGround(impact);
            final int delay = 5 + rng.nextInt(30);

            new BukkitRunnable() {
                int wt = 0;
                @Override public void run() {
                    if (wt >= delay) { cancel(); return; }
                    double pulse = 0.85 + Math.sin(wt * 0.5) * 0.15;
                    for (int j = 0; j < 12; j++) {
                        double a = Math.toRadians(j * 30 + wt * 8);
                        Location lp = impact.clone().add(
                                Math.cos(a)*pulse*1.6, 0.04, Math.sin(a)*pulse*1.6);
                        particleApi.spawnColoredParticles(lp,
                                wt % 2 == 0 ? C_METEOR_RED : C_METEOR_ORG, 1.0f, 1, 0.05, 0.01, 0.05);
                        particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.01, 0.04, 0.03);
                    }
                    wt++;
                }
            }.runTaskTimer(magicPlugin, 0, 2);

            new BukkitRunnable() {
                @Override public void run() { dropMeteor(impact, p); }
            }.runTaskLater(magicPlugin, delay);
        }
    }

    private void dropMeteor(Location ground, Player p) {
        Location skyPos = ground.clone().add(
                (Math.random()-0.5)*1.5, 25, (Math.random()-0.5)*1.5);
        Random rng = new Random();

        new BukkitRunnable() {
            Location cur = skyPos.clone();
            final Vector step = ground.toVector().subtract(skyPos.toVector()).normalize().multiply(1.2);
            int t = 0;
            final int maxT = (int)(skyPos.distance(ground) / 1.2) + 3;

            @Override public void run() {
                if (t >= maxT || !cur.getBlock().isPassable()) {
                    meteorImpact(ground, p, 18, 9, 2.2f);
                    cancel(); return;
                }
                cur.add(step);

                particleApi.spawnColoredParticles(cur, C_CORE_WHITE, 1.7f, 3, 0.1, 0.1, 0.1);
                for (int i = 0; i < 4; i++) {
                    Vector off = new Vector((rng.nextDouble()-0.5)*0.4, (rng.nextDouble()-0.5)*0.4, (rng.nextDouble()-0.5)*0.4);
                    particleApi.spawnColoredParticles(cur.clone().add(off),
                            METEOR_TRAIL[rng.nextInt(METEOR_TRAIL.length)], 1.2f, 2, 0.1, 0.1, 0.1);
                }
                particleApi.spawnParticles(cur, Particle.FLAME,                2, 0.12, 0.12, 0.12, 0.05);
                particleApi.spawnParticles(cur, Particle.CAMPFIRE_COSY_SMOKE,  1, 0.12, 0.12, 0.12, 0.02);

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 1.0, 1.0, 1.0)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    meteorImpact(cur, p, 18, 9, 2.2f);
                    cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        ground.getWorld().playSound(ground, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.55f);
    }

    private void impactSlam(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

        Location target = getRaycast(p, 12);

        p.setVelocity(new Vector(0, 1.2, 0));

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 12) { cancel(); return; }
                for (int j = 0; j < 12; j++) {
                    double a = Math.toRadians(j * 30 + t * 10);
                    double r = 1.2 + Math.sin(t * 0.6) * 0.2;
                    Location lp = target.clone().add(Math.cos(a)*r, 0.05, Math.sin(a)*r);
                    particleApi.spawnColoredParticles(lp, t%2==0 ? C_METEOR_RED : C_HOT_YELLOW,
                            1.1f, 1, 0.04, 0.01, 0.04);
                    particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.01, 0.04, 0.04);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                new BukkitRunnable() {
                    final Location from = p.getLocation().clone().add(0,1,0);
                    @Override public void run() {
                        Random rng = new Random();
                        int steps = (int) from.distance(target) * 3;
                        Vector step = target.toVector().subtract(from.toVector()).multiply(1.0 / Math.max(1, steps));
                        Location cur = from.clone();
                        for (int i = 0; i < steps; i++) {
                            particleApi.spawnColoredParticles(cur, C_CORE_WHITE, 1.6f, 3, 0.08, 0.06, 0.08);
                            particleApi.spawnColoredParticles(cur,
                                    METEOR_TRAIL[i % METEOR_TRAIL.length], 1.2f, 2, 0.1, 0.1, 0.1);
                            particleApi.spawnParticles(cur, Particle.FLAME, 2, 0.08, 0.06, 0.08, 0.06);
                            if (rng.nextBoolean())
                                particleApi.spawnColoredParticles(cur.clone().add(
                                        (rng.nextDouble()-0.5)*0.5, 0, (rng.nextDouble()-0.5)*0.5),
                                        C_SMOKE_GRAY, 0.8f, 1, 0.12, 0.12, 0.12);
                            cur.add(step);
                        }
                    }
                }.runTask(magicPlugin);

                p.teleport(target.clone().add(0, 1, 0));
                p.setVelocity(new Vector(0, -0.5, 0));
                meteorImpact(target, p, 28, 16, 3.5f);
            }
        }.runTaskLater(magicPlugin, 12L);
    }

    private void gravityPull(Player p) {
        Location center = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.7f, 0.6f);

        new BukkitRunnable() {
            double rad = 6.0; int t = 0;
            @Override public void run() {
                if (rad <= 0.3) { cancel(); return; }
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 18 + t * 7);
                    Location lp = center.clone().add(Math.cos(a)*rad, 0, Math.sin(a)*rad);
                    particleApi.spawnColoredParticles(lp,
                            t%3==0 ? C_METEOR_ORG : t%3==1 ? C_HOT_YELLOW : C_METEOR_RED,
                            1.2f, 1, 0.05, 0.05, 0.05);
                    particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.04, 0.04, 0.02);
                }
                if (t % 4 == 0) {
                    double a = Math.toRadians(t * 37);
                    Location outer = center.clone().add(Math.cos(a)*rad, 0, Math.sin(a)*rad);
                    particleApi.drawColoredLine(outer, center, 1.5, C_CORE_WHITE, 1.0f, 0);
                }
                rad -= 0.4; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        new BukkitRunnable() {
            @Override public void run() {
                Random rng = new Random();
                for (Entity e : center.getWorld().getNearbyEntities(center, 6, 6, 6)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(center);
                    if (dist < 0.5) continue;

                    Vector pull = center.toVector().subtract(e.getLocation().toVector());
                    double strength = Math.min(0.9, 3.0 / (dist + 0.8));
                    e.setVelocity(pull.normalize().multiply(strength).setY(0.15));

                    ((LivingEntity) e).damage(10, p);
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));

                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_METEOR_ORG, 1.2f, 8, 0.2, 0.2, 0.2);
                }
                particleApi.spawnColoredParticles(center, C_CORE_WHITE,  1.8f, 25, 0.4, 0.4, 0.4);
                particleApi.spawnColoredParticles(center, C_HOT_YELLOW,  1.4f, 20, 0.6, 0.6, 0.6);
                particleApi.spawnParticles(center, Particle.LAVA, 8, 0.5, 0.3, 0.5, 0.1);
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
            }
        }.runTaskLater(magicPlugin, 6L);
    }

    private void meteorArmor(Player p) {
        armorActive = true;
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE,    1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE,  0.8f, 0.7f);

        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 0, false, false));

        if (armorTask != null) { try { armorTask.cancel(); } catch (Exception ignored) {} }
        armorTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 120 || !p.isOnline()) {
                    armorActive = false; armorTask = null;
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 0.8f, 1.2f);
                    cancel(); return;
                }

                Location center = p.getLocation().clone().add(0, 1, 0);

                for (int orbit = 0; orbit < 3; orbit++) {
                    double a = Math.toRadians(t * 14 + orbit * 120);
                    double yOff = Math.sin(t * 0.1 + orbit) * 0.5;
                    double rx = 0.9 + orbit * 0.15;
                    Location lp = center.clone().add(Math.cos(a)*rx, yOff, Math.sin(a)*rx);

                    particleApi.spawnColoredParticles(lp,
                            orbit==0 ? C_CORE_WHITE : orbit==1 ? C_METEOR_ORG : C_METEOR_RED,
                            1.3f, 2, 0.05, 0.05, 0.05);
                    particleApi.spawnParticles(lp, Particle.FLAME,  1, 0.04, 0.04, 0.04, 0.04);
                    if (t % 3 == 0)
                        particleApi.spawnParticles(lp, Particle.LAVA, 1, 0.04, 0.04, 0.04, 0.02);
                }
                if (t % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45 + t * 6);
                        Location fp = p.getLocation().clone()
                                .add(Math.cos(a)*0.9, 0.05, Math.sin(a)*0.9);
                        particleApi.spawnParticles(fp, Particle.FLAME, 1, 0.04, 0.01, 0.04, 0.04);
                        particleApi.spawnColoredParticles(fp, C_EMBER, 0.9f, 1, 0.05, 0.01, 0.05);
                    }
                }

                if (t % 10 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.8, 1.8, 1.8)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(6, p);
                        e.setFireTicks(40);
                        particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                                C_METEOR_RED, 1.3f, 8, 0.2, 0.2, 0.2);
                        p.getWorld().playSound(center, Sound.ENTITY_BLAZE_HURT, 0.4f, 1.5f);
                    }
                }

                if (t % 20 == 0)
                    p.getWorld().playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 0.8f);
                t++;
            }
        };
        armorTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void extinctionEvent(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 0.4f);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("powers.meteor_lord.extinction_charging")));

        new BukkitRunnable() {
            int ct = 50;
            @Override public void run() {
                if (ct <= 0) { cancel(); releaseExtinction(p); return; }
                Random rng = new Random();
                Location loc = p.getLocation().clone().add(0,1,0);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 + (50-ct) * 8);
                    double r = 3.0 - (50-ct) * 0.05;
                    Location lp = loc.clone().add(Math.cos(a)*r, rng.nextDouble()*2-1, Math.sin(a)*r);
                    particleApi.spawnColoredParticles(lp,
                            i%2==0 ? C_METEOR_ORG : C_CORE_WHITE, 1.3f, 2, 0.05, 0.05, 0.05);
                    particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.05, 0.05, 0.05, 0.05);
                }
                if (ct == 25) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.3f);
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("powers.meteor_lord.extinction_incoming")));
                }
                ct--;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void releaseExtinction(Player p) {
        Location center = getRaycast(p, 35);
        Random rng = new Random();

        p.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN,  1f,  0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT,   1f,  0.3f);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("powers.meteor_lord.extinction_done")));
        particleApi.spawnColoredParticles(center.clone().add(0,1,0), C_CORE_WHITE,  2.5f, 60, 2, 2, 2);
        particleApi.spawnColoredParticles(center.clone().add(0,1,0), C_METEOR_ORG,  2.0f, 80, 3, 3, 3);
        particleApi.spawnParticles(center, Particle.LAVA, 20, 2, 1, 2, 0.15);

        for (int i = 0; i < 12; i++) {
            final Location impact = center.clone().add(
                    (rng.nextDouble()-0.5) * 22,
                    0,
                    (rng.nextDouble()-0.5) * 22);
            adjustGround(impact);
            final int delay = rng.nextInt(30) + 3;
            new BukkitRunnable() {
                int wt = 0;
                @Override public void run() {
                    if (wt >= delay) { cancel(); return; }
                    for (int j = 0; j < 14; j++) {
                        double a = Math.toRadians(j * (360.0/14) + wt * 10);
                        double r = 1.8 + Math.sin(wt * 0.5) * 0.3;
                        Location lp = impact.clone().add(Math.cos(a)*r, 0.04, Math.sin(a)*r);
                        particleApi.spawnColoredParticles(lp, wt%2==0 ? C_METEOR_RED : C_HOT_YELLOW,
                                1.1f, 1, 0.05, 0.01, 0.05);
                        particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.05, 0.01, 0.05, 0.04);
                    }
                    wt++;
                }
            }.runTaskTimer(magicPlugin, 0, 2);
            new BukkitRunnable() {
                @Override public void run() {
                    Location sky = impact.clone().add((rng.nextDouble()-0.5)*2, 30, (rng.nextDouble()-0.5)*2);
                    dropBigMeteor(sky, impact, p);
                }
            }.runTaskLater(magicPlugin, delay);
        }
    }

    private void dropBigMeteor(Location sky, Location ground, Player p) {
        Random rng = new Random();
        new BukkitRunnable() {
            Location cur = sky.clone();
            final Vector step = ground.toVector().subtract(sky.toVector()).normalize().multiply(1.4);
            int t = 0;
            final int maxT = (int)(sky.distance(ground) / 1.4) + 2;

            @Override public void run() {
                if (t >= maxT || !cur.getBlock().isPassable()) {
                    meteorImpact(ground, p, 22, 11, 3.2f);
                    for (Entity e : ground.getWorld().getNearbyEntities(ground, 3.2, 3.2, 3.2)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        e.setFireTicks(100);
                    }
                    cancel(); return;
                }
                cur.add(step);

                particleApi.spawnColoredParticles(cur, C_CORE_WHITE, 2.0f, 5, 0.12, 0.12, 0.12);
                for (int i = 0; i < 5; i++) {
                    Vector off = new Vector((rng.nextDouble()-0.5)*0.6, (rng.nextDouble()-0.5)*0.6, (rng.nextDouble()-0.5)*0.6);
                    particleApi.spawnColoredParticles(cur.clone().add(off),
                            METEOR_TRAIL[rng.nextInt(METEOR_TRAIL.length)], 1.4f, 2, 0.12, 0.12, 0.12);
                }
                particleApi.spawnParticles(cur, Particle.FLAME, 4, 0.15, 0.15, 0.15, 0.07);
                particleApi.spawnParticles(cur, Particle.CAMPFIRE_COSY_SMOKE, 2, 0.15, 0.15, 0.15, 0.02);
                particleApi.spawnColoredParticles(
                        cur.clone().subtract(step.clone().normalize().multiply(0.8)),
                        SMOKE_TRAIL[rng.nextInt(2)], 0.9f, 3, 0.18, 0.18, 0.18);

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 1.1, 1.1, 1.1)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    meteorImpact(cur, p, 22, 11, 3.2f);
                    cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        ground.getWorld().playSound(ground, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.4f);
    }

    private void onDamaged(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        Player p = ex.getPlayer();

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getDamage() >= 3) {
            event.setCancelled(true);
            Location loc = p.getLocation().clone();
            double impactDmg = Math.min(event.getDamage() * 1.5, 18);
            double radius    = Math.min(event.getDamage() * 0.25, 3.0);
            particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_CORE_WHITE,  1.6f, 20, (float)radius*0.4f, 0.3f, (float)radius*0.4f);
            particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_METEOR_ORG,  1.3f, 25, (float)radius*0.6f, 0.4f, (float)radius*0.6f);
            particleApi.spawnParticles(loc, Particle.LAVA,  6, radius*0.5, 0.2, radius*0.5, 0.1);
            particleApi.spawnParticles(loc, Particle.FLAME, 8, radius*0.5, 0.3, radius*0.5, 0.06);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
            for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                ((LivingEntity) e).damage(impactDmg, p);
                e.setFireTicks(30);
            }
        }
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random rng = new Random();

        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                p.setFireTicks(0);

                Location center = p.getLocation().clone().add(0, 1, 0);

                if (isAuraEnabled(p)) {
                    for (int i = 0; i < 12; i++) {
                        double a = Math.toRadians(i * 30 + t * 6);
                        double yOsc = Math.sin(t * 0.08 + i * 0.5) * 0.3;
                        Location lp = center.clone().add(Math.cos(a)*1.15, yOsc, Math.sin(a)*1.15);
                        Color c = (t+i)%3==0 ? C_METEOR_ORG : (t+i)%3==1 ? C_EMBER : C_METEOR_RED;
                        particleApi.spawnColoredParticles(lp, c, 1.1f, 1, 0.04, 0.04, 0.04);
                        if (i % 3 == 0)
                            particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.04, 0.04, 0.02);
                    }
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45 - t * 8);
                        Location lp = center.clone().add(Math.cos(a)*0.7, 0.4+Math.sin(a*0.5)*0.2, Math.sin(a)*0.7);
                        particleApi.spawnColoredParticles(lp, i%2==0 ? C_CORE_WHITE : C_HOT_YELLOW,
                                0.9f, 1, 0.03, 0.03, 0.03);
                    }
                    if (t % 4 == 0) {
                        for (int i = 0; i < 6; i++) {
                            double a = Math.toRadians(i * 60 + t * 5);
                            Location fp = p.getLocation().clone().add(Math.cos(a)*0.9, 0.05, Math.sin(a)*0.9);
                            particleApi.spawnParticles(fp, Particle.LAVA, 1, 0.04, 0.01, 0.04, 0.02);
                            particleApi.spawnColoredParticles(fp, C_EMBER, 0.85f, 1, 0.05, 0.01, 0.05);
                        }
                    }
                    if (t % 3 == 0)
                        particleApi.spawnParticles(
                            center.clone().add(
                                (rng.nextDouble()-0.5)*1.3, rng.nextDouble()*1.6-0.2, (rng.nextDouble()-0.5)*1.3),
                            Particle.FLAME, 1, 0.05, 0.05, 0.05, 0.03);
                }
                if (t % 40 == 0 && !CooldownApi.isOnCooldown(mt_scorch, p)) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.5, 1.5, 1.5)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        e.setFireTicks(40);
                        ((LivingEntity) e).damage(1, p);
                        addCd(mt_scorch, p);
                    }
                }
                if (t % 80 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.2f, 0.7f);

                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 20);
        return task;
    }

    @Override
    public void remove() {
        if (armorTask != null) { try { armorTask.cancel(); } catch (Exception ignored) {} armorTask = null; }
        armorActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&6流星射击";
            case 1: return "&c流星雨";
            case 2: return "&6冲击猛击";
            case 3: return "&e重力牵引";
            case 4: return "&6流星护甲";
            case 5: return "&4&l灭绝事件";
            default: return "&7none";
        }
    }

    private void meteorImpact(Location loc, Player p,
                               double coreDmg, double rimDmg, float radius) {
        particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_CORE_WHITE,  2.2f, 40, radius*0.4f, 0.5f, radius*0.4f);
        particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_HOT_YELLOW,  1.8f, 50, radius*0.6f, 0.6f, radius*0.6f);
        particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_METEOR_ORG,  1.5f, 50, radius*0.8f, 0.7f, radius*0.8f);
        particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_METEOR_RED,  1.2f, 35, radius*1.0f, 0.8f, radius*1.0f);
        particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_SMOKE_GRAY,  0.8f, 30, radius*1.1f, 1.0f, radius*1.1f);
        particleApi.spawnParticles(loc, Particle.LAVA,                10, radius*0.6, 0.3, radius*0.6, 0.12);
        particleApi.spawnParticles(loc, Particle.CAMPFIRE_COSY_SMOKE,  8, radius*0.8, 0.6, radius*0.8, 0.02);
        particleApi.spawnParticles(loc, Particle.FLAME,               12, radius*0.5, 0.4, radius*0.5, 0.08);
        new BukkitRunnable() {
            double r = 0.2; int t = 0;
            @Override public void run() {
                if (r > radius + 1) { cancel(); return; }
                int points = Math.max(12, (int)(r * 8));
                for (int i = 0; i < points; i++) {
                    double a = Math.toRadians(i * (360.0/points) + t * 5);
                    Location rp = loc.clone().add(0, 0.08, 0)
                            .add(Math.cos(a)*r, 0, Math.sin(a)*r);
                    Color c = t < 4 ? C_CORE_WHITE : t < 8 ? C_METEOR_ORG : C_SMOKE_GRAY;
                    particleApi.spawnColoredParticles(rp, c, 1.1f, 1, 0.04, 0.04, 0.04);
                }
                r += 0.42; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,  1f,  0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_HURT,       0.8f, 0.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK,       0.6f, 0.5f);
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            double dmg = dist <= 1.5 ? coreDmg : rimDmg;
            ((LivingEntity) e).damage(dmg, p);
            e.setFireTicks(40);
            Vector kb = e.getLocation().subtract(loc).toVector();
            if (kb.lengthSquared() < 0.01)
                kb = new Vector((Math.random()-0.5)*0.8, 0.3, (Math.random()-0.5)*0.8);
            e.setVelocity(kb.normalize().multiply(1.2).setY(0.5));
        }
    }

    private Location getRaycast(Player p, int maxDist) {
        Location cur = p.getEyeLocation().clone();
        Vector dir   = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < maxDist * 2; i++) {
            cur.add(dir.clone().multiply(0.5));
            if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                cur.subtract(dir.clone().multiply(0.5)); break;
            }
        }
        return cur;
    }

    private void adjustGround(Location loc) {
        for (int i = 0; i < 7; i++) {
            if (!loc.clone().subtract(0,1,0).getBlock().isPassable()) break;
            loc.subtract(0,1,0);
        }
        for (int i = 0; i < 7; i++) {
            if (loc.getBlock().isPassable()) break;
            loc.add(0,1,0);
        }
    }

}

