package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.*;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.cooldowns.Cooldowns.cooldowns;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.misc.GeneralMethods.rotateVector;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class LightningPower extends Power implements IdlePower, Removeable {

    private static final String lightning_strike       = "lightning.strike";
    private static final String lightning_shot         = "lightning.shot";
    private static final String lightning_field        = "lightning.field";
    private static final String lightning_transmission = "lightning.transmission";
    private static final String lightning_passive      = "lightning.passive";
    private static final String lightning_thunderclap  = "lightning.thunderclap";
    private static final String lightning_ball         = "lightning.ball";

    private BukkitRunnable fieldRunnable = null;
    private boolean fieldActive = false;

    private static final Color[] COLORS = {
            Color.fromRGB(0,   100, 220),
            Color.fromRGB(0,   140, 255),
            Color.fromRGB(30,  180, 255),
            Color.fromRGB(80,  200, 255),
            Color.fromRGB(0,   60,  180),
    };

    public LightningPower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute) {
            preventSelfDamage((DamagedExecute) ex);
            return;
        }
        if (ex instanceof DamagedByExecute) {
            electricCounterstrike((DamagedByExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) {
            executeLeftClick((LeftClickExecute) ex);
            return;
        }
        if (ex instanceof DealDamageExecute) {
            executeDealDamage((DealDamageExecute) ex);
        }
    }

    private void executeDealDamage(DealDamageExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 0) return;
        if (onCd(lightning_strike, p, this)) return;

        Entity target = ((EntityDamageByEntityEvent) ex.getRawEvent()).getEntity();
        thunderStrike(p, target);
        addCd(lightning_strike, p);
    }

    private void thunderStrike(Player p, Entity first) {
        flashLightningBeam(p.getEyeLocation(), first.getLocation().clone().add(0, 1, 0), p, 0);

        safeRealLightning(first.getLocation(), p);

        List<Entity> chained = new ArrayList<>();
        chained.add(p);
        chained.add(first);

        new BukkitRunnable() {
            Entity current = first;
            int bounces = 0;

            @Override
            public void run() {
                if (bounces >= 2 || !current.isValid()) { cancel(); return; }

                Entity next = null;
                double best = 5;
                for (Entity e : current.getWorld().getNearbyEntities(current.getLocation(), best, best, best)) {
                    if (chained.contains(e) || !(e instanceof LivingEntity)) continue;
                    double d = e.getLocation().distance(current.getLocation());
                    if (d < best) { best = d; next = e; }
                }
                if (next == null) { cancel(); return; }

                flashLightningBeam(
                    current.getLocation().clone().add(0, 1, 0),
                    next.getLocation().clone().add(0, 1, 0),
                    p, 0);

                next.getWorld().strikeLightningEffect(next.getLocation());
                ((LivingEntity) next).damage(8, p);

                chained.add(next);
                current = next;
                bounces++;
            }
        }.runTaskTimer(magicPlugin, 3L, 3L);
    }

    private void executeLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());

        switch (slot) {
            case 1:
                if (onCd(lightning_shot, p, this)) return;
                if (p.isSneaking()) {
                    lightningShot(p, 6);
                    addCd(lightning_shot, p);
                } else {
                    lightningShot(p, 3);
                    addCd(lightning_shot, p, cooldowns.get(lightning_shot) / (2.0));
                }
                return;

            case 2:
                if (fieldActive) return;
                if (onCd(lightning_field, p, this)) return;
                plasmaField(p);
                return;

            case 3:
                if (onCd(lightning_transmission, p, this)) return;
                voltDash(p);
                addCd(lightning_transmission, p);
                return;

            case 4:
                if (onCd(lightning_thunderclap, p, this)) return;
                thunderclap(p);
                addCd(lightning_thunderclap, p);
                return;

            case 5:
                if (onCd(lightning_ball, p, this)) return;
                ballLightning(p);
                addCd(lightning_ball, p);
                return;
        }
    }

    private void lightningShot(Player p, int segments) {
        Random r = new Random();
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CREEPER_HURT, 1f, 2f);

        new BukkitRunnable() {
            Location current = p.getEyeLocation().clone();
            int seg = 0;
            final Set<Entity> hit = new HashSet<>();

            @Override
            public void run() {
                if (seg >= segments) {
                    current.getWorld().strikeLightningEffect(current);
                    particleApi.spawnParticles(current, Particle.ELECTRIC_SPARK, 30, 0.5, 0.5, 0.5, 1.5);
                    cancel();
                    return;
                }

                int spread = (segments > 4) ? 20 : 40;
                Location dest = current.clone().add(
                        rotateVector(p.getEyeLocation().getDirection().clone().normalize().multiply(3.5),
                                r.nextInt(spread * 2) - spread));

                Color c = COLORS[r.nextInt(COLORS.length)];
                for (Entity e : particleApi.drawColoredLine(current, dest, 1.5, c, 1.2f, 0)) {
                    if (!(e instanceof LivingEntity) || e.equals(p) || hit.contains(e)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(10, p);
                    e.getWorld().strikeLightningEffect(e.getLocation());
                }

                dest.getWorld().strikeLightningEffect(dest);
                particleApi.spawnParticles(dest, Particle.ELECTRIC_SPARK, 8, 0.1, 0.1, 0.1, 0.8);

                current = dest;
                seg++;
            }
        }.runTaskTimer(magicPlugin, 0L, 2L);
    }

    private void plasmaField(Player p) {
        fieldActive = true;
        Random r = new Random();
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1.5f);

        fieldRunnable = new BukkitRunnable() {
            int t = 0;
            int hitCount = 0;

            @Override
            public void run() {
                if (t >= 80 || !p.isOnline()) {
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 2f);
                    addCd(lightning_field, p);
                    fieldActive = false;
                    cancel();
                    return;
                }

                Location center = p.getLocation().clone().add(0, 1.2, 0);

                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 20 + t * 12);
                    drawFieldPoint(center, angle, 3.5, COLORS[i % COLORS.length]);
                }
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30 - t * 18);
                    drawFieldPoint(center, angle, 2.2, COLORS[(i + 2) % COLORS.length]);
                }
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45 + t * 25);
                    drawFieldPoint(center, angle, 1.1, Color.WHITE);
                }

                if (t % 3 == 0) spawnRandomArc(center, r, 3.0, p, 5.0);
                if (t % 10 == 0)
                    p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2f);

                hitCount = 0;
                for (Entity e : p.getWorld().getNearbyEntities(center, 3, 3, 3)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(2.0, p);
                    hitCount++;
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, Math.min(hitCount / 2, 3), false, false));
                t++;
            }
        };
        fieldRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void drawFieldPoint(Location center, double angle, double radius, Color color) {
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        Location lp = center.clone().add(x, 0, z);
        particleApi.spawnColoredParticles(lp, color, 1.1f, 2, 0.04, 0.04, 0.04);
        particleApi.spawnParticles(lp, Particle.ELECTRIC_SPARK, 1, 0.02, 0.02, 0.02, 0.3);
    }

    private void spawnRandomArc(Location center, Random r, double radius, Player p, double damage) {
        Vector v1 = randomSphere(r, radius);
        Vector v2 = randomSphere(r, radius);
        Color c = COLORS[r.nextInt(COLORS.length)];
        for (Entity e : particleApi.drawColoredLine(center.clone().add(v1), center.clone().add(v2), 1.5, c, 1.1f, 0)) {
            if (!(e instanceof LivingEntity) || e.equals(p)) continue;
            ((LivingEntity) e).damage(damage, p);
        }
    }

    private void voltDash(Player p) {
        Location from = p.getEyeLocation().clone();
        Location to = from.clone();

        for (int i = 0; i < 40; i++) {
            to.add(p.getEyeLocation().getDirection().normalize());
            if (!to.getBlock().isPassable() || to.getBlock().isLiquid()) {
                to.subtract(p.getEyeLocation().getDirection().normalize());
                break;
            }
        }

        final Location finalTo = to.clone();
        Random r = new Random();

        new BukkitRunnable() {
            @Override
            public void run() {
                int steps = (int) from.distance(finalTo) * 3;
                Vector step = finalTo.toVector().subtract(from.toVector()).normalize().multiply(1.0 / 3.0);
                Location cursor = from.clone();

                for (int i = 0; i < steps; i++) {
                    Color c = COLORS[r.nextInt(COLORS.length)];
                    particleApi.spawnColoredParticles(cursor, c, 1.3f, 3, 0.08, 0.08, 0.08);
                    particleApi.spawnParticles(cursor, Particle.ELECTRIC_SPARK, 2, 0.05, 0.05, 0.05, 0.4);
                    if (i % 4 == 0) {
                        Vector spark = randomSphere(r, 0.4);
                        particleApi.spawnColoredParticles(cursor.clone().add(spark), Color.WHITE, 1f, 2, 0.03, 0.03, 0.03);
                    }
                    cursor.add(step);
                }
            }
        }.runTask(magicPlugin);

        p.getWorld().playSound(from, Sound.ENTITY_CREEPER_HURT, 1f, 2f);
        p.getWorld().playSound(from, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 2f);
        p.teleport(finalTo.clone().add(0, 1, 0));

        finalTo.getWorld().strikeLightningEffect(finalTo);

        new BukkitRunnable() {
            @Override
            public void run() {
                particleApi.spawnParticles(finalTo, Particle.ELECTRIC_SPARK, 35, 0.8, 0.8, 0.8, 2f);
                particleApi.spawnColoredParticles(finalTo, Color.WHITE, 1.5f, 30, 0.5, 0.5, 0.5);
            }
        }.runTaskLater(magicPlugin, 2L);
    }

    private void thunderclap(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        Random r = new Random();

        p.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.7f);
        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);

        new BukkitRunnable() {
            double radius = 0.5;
            int t = 0;

            @Override
            public void run() {
                if (radius > 7) { cancel(); return; }

                for (int i = 0; i < 24; i++) {
                    double angle = Math.toRadians(i * 15 + t * 5);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location lp = loc.clone().add(x, 0, z);
                    particleApi.spawnColoredParticles(lp, COLORS[r.nextInt(COLORS.length)], 1.2f, 3, 0.06, 0.06, 0.06);
                    particleApi.spawnParticles(lp, Particle.ELECTRIC_SPARK, 2, 0.05, 0.05, 0.05, 0.8);
                }
                radius += 0.55;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + r.nextInt(20));
                    Location strikePos = loc.clone().add(
                            Math.cos(angle) * (3.5 + r.nextDouble()),
                            -1,
                            Math.sin(angle) * (3.5 + r.nextDouble()));
                    safeRealLightning(strikePos, p);
                }
                for (Entity e : p.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5, false, false));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 5, false, false));
                    Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(1.3).setY(0.3);
                    e.setVelocity(kb);
                }
            }
        }.runTaskLater(magicPlugin, 8L);
    }

    private void ballLightning(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CREEPER_HURT, 1f, 2f);

        ArmorStand ball = p.getWorld().spawn(p.getEyeLocation().clone(), ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false);
            en.setSmall(true); en.setMarker(true);
        });

        Vector dir = p.getEyeLocation().getDirection().clone().normalize().multiply(0.35);
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (ball.isDead()) { cancel(); return; }

                if (t >= 80) {
                    ballExplosion(ball.getLocation(), p, r);
                    ball.remove();
                    cancel();
                    return;
                }

                ball.teleport(ball.getLocation().add(dir));
                Location loc = ball.getLocation();

                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30 + t * 15);
                    double x = Math.cos(a) * 0.6;
                    double z = Math.sin(a) * 0.6;
                    particleApi.spawnColoredParticles(loc.clone().add(x, Math.sin(a * 0.5) * 0.4, z),
                            COLORS[r.nextInt(COLORS.length)], 1.3f, 2, 0.04, 0.04, 0.04);
                }
                particleApi.spawnParticles(loc, Particle.ELECTRIC_SPARK, 6, 0.2, 0.2, 0.2, 0.6);

                if (t % 4 == 0) {
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
                        if (e.equals(p) || e.equals(ball) || !(e instanceof LivingEntity)) continue;
                        Color c = COLORS[r.nextInt(COLORS.length)];
                        for (Entity hit : particleApi.drawColoredLine(loc, e.getLocation().clone().add(0, 1, 0), 1.5, c, 1.1f, 0)) {
                            if (!(hit instanceof LivingEntity) || hit.equals(p)) continue;
                            ((LivingEntity) hit).damage(4, p);
                            hit.getWorld().strikeLightningEffect(hit.getLocation());
                        }
                        p.getWorld().playSound(loc, Sound.ENTITY_CREEPER_HURT, 0.3f, 2f);
                    }
                }

                if (t % 15 == 0)
                    p.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2f);

                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    ballExplosion(loc, p, r);
                    ball.remove();
                    cancel();
                    return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void ballExplosion(Location loc, Player p, Random r) {
        particleApi.spawnParticles(loc, Particle.ELECTRIC_SPARK, 250, 2, 2, 2, 3f);
        particleApi.spawnColoredParticles(loc, Color.WHITE, 2f, 20, 1.5, 1.5, 1.5);
        for (int i = 0; i < 5; i++) {
            particleApi.drawColoredLine(loc, loc.clone().add(randomSphere(r, 4)), 1.5, COLORS[r.nextInt(COLORS.length)], 1f, 0);
        }
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
        safeRealLightning(loc, p);
        for (int i = 0; i < 4; i++) {
            final Location sp = loc.clone().add(
                    (r.nextDouble() - 0.5) * 4, 0, (r.nextDouble() - 0.5) * 4);
            new BukkitRunnable() {
                @Override public void run() { safeRealLightning(sp, p); }
            }.runTaskLater(magicPlugin, r.nextInt(6));
        }
    }

    private void electricCounterstrike(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (CooldownApi.isOnCooldown(lightning_passive, p)) return;

        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        Entity damager = event.getDamager();
        if (!(damager instanceof LivingEntity)) return;

        event.setDamage(event.getFinalDamage() * 0.75);
        ((LivingEntity) damager).damage(8, p);
        damager.getWorld().strikeLightningEffect(damager.getLocation());

        Random r = new Random();
        flashLightningBeam(p.getLocation().clone().add(0, 1, 0), damager.getLocation().clone().add(0, 1, 0), p, 0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CREEPER_HURT, 1.2f, 2f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
        particleApi.spawnParticles(p.getLocation().add(0, 1, 0), Particle.ELECTRIC_SPARK, 40, 0.5, 0.5, 0.5, 1.5);

        addCd(lightning_passive, p);
    }

    private void preventSelfDamage(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
        }
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();

        BukkitRunnable runnable = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }

                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 25, 1, false, false));
                Location loc = p.getLocation().clone().add(0, 0.1, 0);
                if (isAuraEnabled(p)) {
                    particleApi.spawnParticles(loc, Particle.ELECTRIC_SPARK, 2, 0.3, 0.02, 0.3, 0.02);
                    particleApi.spawnColoredParticles(loc, COLORS[t % COLORS.length], 0.8f, 1, 0.25, 0.01, 0.25);
                }
                if (r.nextInt(100) < 15) {
                    for (Entity e : p.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(2, p);
                        e.getWorld().strikeLightningEffect(e.getLocation());
                        flashLightningBeam(loc, e.getLocation().clone().add(0, 0.5, 0), p, 0);
                        p.getWorld().playSound(loc, Sound.ENTITY_CREEPER_HURT, 0.4f, 2f);
                    }
                }
                t++;
            }
        };
        runnable.runTaskTimer(magicPlugin, 0, 20);
        return runnable;
    }

    @Override
    public void remove() {
        if (fieldRunnable != null) {
            fieldRunnable.cancel();
            fieldRunnable = null;
        }
        fieldActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&b雷击";
            case 1: return "&b闪电射击";
            case 2: return "&b等离子场";
            case 3: return "&b电压冲刺";
            case 4: return "&b&l雷鸣";
            case 5: return "&3球状闪电";
            default: return "&7none";
        }
    }

    private void safeRealLightning(Location loc, Player owner) {
        loc.getWorld().strikeLightning(loc);
    }

    private void flashLightningBeam(Location from, Location to, Player p, double damage) {
        Random r = new Random();
        int segments = Math.max(3, (int) from.distance(to) * 2);
        Location cursor = from.clone();
        Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / segments);

        for (int i = 0; i < segments; i++) {
            Vector jitter = new Vector((r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4);
            Location next = cursor.clone().add(step).add(jitter);

            Color c = COLORS[r.nextInt(COLORS.length)];
            List<Entity> hit = particleApi.drawColoredLine(cursor, next, 1.5, c, 1.1f, 0);

            if (damage > 0) {
                for (Entity e : hit) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    ((LivingEntity) e).damage(damage, p);
                }
            }
            if (i % 3 == 0)
                particleApi.spawnParticles(next, Particle.ELECTRIC_SPARK, 4, 0.05, 0.05, 0.05, 0.5);

            cursor = next;
        }
    }

    private Vector randomSphere(Random r, double radius) {
        Vector v = new Vector(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
        if (v.lengthSquared() == 0) v.setX(1);
        return v.normalize().multiply(radius * r.nextDouble());
    }
}

