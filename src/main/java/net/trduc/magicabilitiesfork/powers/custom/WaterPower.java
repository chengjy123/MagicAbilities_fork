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
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
public class WaterPower extends Power implements IdlePower, Removeable {
    private static final String w_bolt    = "water.bolt";
    private static final String w_whirl   = "water.whirlpool";
    private static final String w_wave    = "water.wave";
    private static final String w_dash    = "water.dash";
    private static final String w_tsunami = "water.tsunami";
    private static final String w_bubble  = "water.bubble";
    private static final String w_counter  = "water.counter";
    private static final String w_hydro    = "water.hydro";
    private static final Color C_OCEAN      = Color.fromRGB(0,   105, 195);
    private static final Color C_DEEP_BLUE  = Color.fromRGB(0,   60,  160);
    private static final Color C_SKY_BLUE   = Color.fromRGB(30,  160, 255);
    private static final Color C_AQUA       = Color.fromRGB(0,   210, 220);
    private static final Color C_TEAL       = Color.fromRGB(0,   175, 155);
    private static final Color C_FOAM       = Color.fromRGB(180, 235, 255);
    private static final Color C_BUBBLE     = Color.fromRGB(200, 240, 255);

    private static final Color[] WATER_COLS = {
            C_OCEAN, C_SKY_BLUE, C_AQUA, C_TEAL, C_FOAM
    };
    private static final Color[] AURA_COLS = {
            C_OCEAN, C_SKY_BLUE, C_AQUA, C_BUBBLE, C_FOAM
    };
    private boolean dashing    = false;
    private boolean hydroActive = false;
    private BukkitRunnable hydroRunnable = null;

    public WaterPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) { aquaCounter((DamagedByExecute) ex); return; }
        if (ex instanceof DamagedExecute)   { handleDamage((DamagedExecute) ex);  return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(w_bolt, p, this)) return; tidalBolt(p);    addCd(w_bolt, p);    return;
            case 1: if (onCd(w_whirl, p, this)) return; whirlpool(p);    addCd(w_whirl, p);   return;
            case 2: if (onCd(w_wave, p, this)) return; waveCrash(p);    addCd(w_wave, p);    return;
            case 3: if (dashing) return;
                    if (onCd(w_dash, p, this)) return; aquaDash(p);                                                                       return;
            case 4: if (onCd(w_tsunami, p, this)) return; tsunami(p);      addCd(w_tsunami, p); return;
            case 5: if (onCd(w_bubble, p, this)) return; bubblePrison(p); addCd(w_bubble, p); return;
            case 6: if (hydroActive) return;
                    if (onCd(w_hydro, p, this))    return; hydroForm(p);    addCd(w_hydro, p);
        }
    }

    private void tidalBolt(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.9f, 1.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW,    0.5f, 1.8f);

        ArmorStand bolt = spawnAs(p.getEyeLocation().clone());
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            boolean hit = false;
            @Override public void run() {
                if (bolt.isDead() || t > 50 || hit) { safeRemove(bolt); cancel(); return; }

                bolt.teleport(bolt.getLocation().add(dir.clone().multiply(1.5)));
                Location loc = bolt.getLocation();
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(i * 120 + t * 28);
                    Location lp = loc.clone().add(
                            Math.cos(a)*0.28, Math.sin(a*0.6)*0.14, Math.sin(a)*0.28);
                    particleApi.spawnColoredParticles(lp, WATER_COLS[i % WATER_COLS.length], 1.1f, 2, 0.04, 0.04, 0.04);
                }
                particleApi.spawnParticles(loc, Particle.DRIPPING_WATER, 2, 0.1, 0.1, 0.1, 0.02);
                particleApi.spawnColoredParticles(loc, C_FOAM, 0.9f, 1, 0.06, 0.06, 0.06);

                if (loc.getBlock().isLiquid()) { t++; return; }
                if (!loc.getBlock().isPassable()) {
                    waterSplash(loc, null); safeRemove(bolt); cancel(); return;
                }

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.9, 0.9, 0.9)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    hit = true;
                    ((LivingEntity) e).damage(11, p);
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
                    waterSplash(loc, e.getLocation().clone().add(0, 1, 0));
                    safeRemove(bolt); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void waterSplash(Location loc, Location altLoc) {
        Location sl = altLoc != null ? altLoc : loc;
        particleApi.spawnParticles(sl, Particle.SPLASH, 30, 0.4, 0.3, 0.4, 0.3);
        particleApi.spawnColoredParticles(sl, C_AQUA, 1.3f, 15, 0.35, 0.3, 0.35);
        particleApi.spawnColoredParticles(sl, C_FOAM, 1.0f, 10, 0.4,  0.3, 0.4);
        sl.getWorld().playSound(sl, Sound.ENTITY_GENERIC_SPLASH, 0.7f, 1.4f);
    }

    private void whirlpool(Player p) {
        Location center = getRaycastGround(p, 20);
        Random r = new Random();

        p.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 1.2f);
        p.sendMessage(ChatColor.AQUA + "✦ Whirlpool!");

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80 || !p.isOnline()) {
                    particleApi.spawnParticles(center, Particle.SPLASH, 30, 2, 0.5, 2, 0.4);
                    particleApi.spawnColoredParticles(center.clone().add(0,0.5,0), C_AQUA, 1.5f, 40, 2, 1, 2);
                    cancel(); return;
                }
                for (int layer = 0; layer < 4; layer++) {
                    double rad  = 1.0 + layer * 0.8;
                    double yOff = layer * 0.3;
                    int dir     = layer % 2 == 0 ? 1 : -1;
                    for (int i = 0; i < 11; i++) {
                        double a = Math.toRadians(i * 20 + t * (14 + layer * 4) * dir);
                        Location lp = center.clone().add(Math.cos(a)*rad, yOff, Math.sin(a)*rad);
                        particleApi.spawnColoredParticles(lp,
                                WATER_COLS[(i + layer + t) % WATER_COLS.length],
                                0.95f + layer * 0.08f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 3 == 0)
                    particleApi.spawnParticles(center.clone().add(
                            (r.nextDouble()-0.5)*3, r.nextDouble()*1.8, (r.nextDouble()-0.5)*3),
                            Particle.DRIPPING_WATER, 2, 0.2, 0.2, 0.2, 0.04);
                if (t % 20 == 0)
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 0.4f, 0.7f + r.nextFloat()*0.3f);
                for (Entity e : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.3);
                    pull.setY(Math.max(pull.getY(), 0.04));
                    e.setVelocity(pull);
                    double ex = e.getLocation().getX() - center.getX();
                    double ez = e.getLocation().getZ() - center.getZ();
                    double len = Math.sqrt(ex*ex + ez*ez);
                    if (len > 0.2) {
                        double nx = -ez / len;
                        double nz =  ex / len;
                        e.setVelocity(e.getVelocity().add(new Vector(nx*0.22, 0, nz*0.22)));
                    }
                    if (t % 20 == 0) {
                        ((LivingEntity) e).damage(3, p);
                        ((LivingEntity) e).addPotionEffect(
                                new PotionEffect(PotionEffectType.SLOWNESS, 25, 2, false, false));
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void waveCrash(Player p) {
        Location center = p.getLocation().clone().add(0, 0.5, 0);
        Vector fwd = p.getEyeLocation().getDirection().clone().setY(0).normalize();
        Random r = new Random();

        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH,        1f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.6f, 0.8f);
        new BukkitRunnable() {
            double rad = 0.3; int t = 0;
            @Override public void run() {
                if (rad > 5.5) { cancel(); return; }
                for (int i = -9; i <= 9; i++) {
                    double angle = Math.toRadians(i * 10);
                    double vx = fwd.getX()*Math.cos(angle) - fwd.getZ()*Math.sin(angle);
                    double vz = fwd.getX()*Math.sin(angle) + fwd.getZ()*Math.cos(angle);
                    for (double y : new double[]{0.2, 1.2}) {
                        Location lp = center.clone().add(vx*rad, y, vz*rad);
                        particleApi.spawnColoredParticles(lp,
                                t%3==0 ? C_FOAM : t%3==1 ? C_AQUA : C_SKY_BLUE,
                                1.1f, 2, 0.05, 0.08, 0.05);
                        if (t % 4 == 0)
                            particleApi.spawnParticles(lp, Particle.SPLASH, 1, 0.05, 0.05, 0.05, 0.1);
                    }
                }
                rad += 0.55; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : center.getWorld().getNearbyEntities(center, 5, 3, 5)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    Vector toE = e.getLocation().subtract(center).toVector().setY(0).normalize();
                    if (fwd.dot(toE) < 0) continue;
                    double dist = e.getLocation().distance(center);
                    double dmg = Math.max(5, 13 - dist * 1.6);
                    ((LivingEntity) e).damage(dmg, p);
                    double power = Math.max(0.8, 2.4 - dist * 0.3);
                    e.setVelocity(fwd.clone().multiply(power).setY(0.55));
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false));
                    waterSplash(e.getLocation().clone().add(0, 1, 0), null);
                }
            }
        }.runTaskLater(magicPlugin, 5L);
    }

    private void aquaDash(Player p) {
        dashing = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1f, 1.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW,    0.6f, 1.6f);

        Vector dir  = p.getEyeLocation().getDirection().clone().setY(0.12).normalize();
        Location from = p.getLocation().clone();
        Location to   = from.clone();

        for (int i = 0; i < 24; i++) {
            to.add(dir.clone().multiply(0.5));
            if (!to.getBlock().isPassable() && !to.getBlock().isLiquid()) {
                to.subtract(dir.clone().multiply(0.5)); break;
            }
        }
        drawWaterTrail(from.clone().add(0,1,0), to.clone().add(0,1,0));
        p.teleport(to.clone().add(0, 0.5, 0));
        particleApi.spawnParticles(to.clone().add(0,1,0), Particle.SPLASH, 40, 0.6, 0.6, 0.6, 0.3);
        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_AQUA, 1.5f, 25, 0.5, 0.5, 0.5);
        p.getWorld().playSound(to, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.4f);
        int numTraps = 5;
        for (int i = 1; i <= numTraps; i++) {
            double frac = (double) i / (numTraps + 1);
            Location tl = from.clone().add(to.toVector().subtract(from.toVector()).multiply(frac)).add(0, 0.3, 0);
            spawnWaterZone(tl, p);
        }

        addCd(w_dash, p);
        new BukkitRunnable() { @Override public void run() { dashing = false; } }.runTaskLater(magicPlugin, 5L);
    }

    private void spawnWaterZone(Location loc, Player p) {
        Random r = new Random();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60) { cancel(); return; }
                if (t % 6 == 0) {
                    particleApi.spawnColoredParticles(loc, WATER_COLS[r.nextInt(WATER_COLS.length)],
                            0.95f, 3, 0.28, 0.3, 0.28);
                    particleApi.spawnParticles(loc, Particle.DRIPPING_WATER, 1, 0.2, 0.2, 0.2, 0.04);
                }
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.1, 1.4, 1.1)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 25, 1, false, false));
                    ((LivingEntity) e).damage(2, p);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void tsunami(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH,         1f, 0.3f);
        p.sendMessage(ChatColor.AQUA + "✦ " + ChatColor.BOLD + "TSUNAMI!");

        Vector fwd    = p.getEyeLocation().getDirection().clone().setY(0).normalize();
        Vector right  = yawRotate(fwd.clone(), 90);
        Location start = p.getLocation().clone().add(0, 0.5, 0);
        Set<UUID> hit = new HashSet<>();
        Random r = new Random();

        new BukkitRunnable() {
            double dist = 1.5;
            int t = 0;
            @Override public void run() {
                if (dist > 21 || t > 50) { cancel(); return; }

                Location waveFront = start.clone().add(fwd.clone().multiply(dist));
                for (double h = 0; h <= 3.0; h += 0.4) {
                    for (double side = -3.0; side <= 3.0; side += 0.45) {
                        Location lp = waveFront.clone().add(right.clone().multiply(side)).add(0, h, 0);
                        Color c = h < 1.0 ? C_DEEP_BLUE : h < 2.0 ? C_OCEAN : h < 2.7 ? C_AQUA : C_FOAM;
                        particleApi.spawnColoredParticles(lp, c, 1.1f, 1, 0.05, 0.06, 0.05);
                    }
                }
                if (t % 2 == 0) {
                    for (double side = -3.0; side <= 3.0; side += 0.8) {
                        Location top = waveFront.clone().add(right.clone().multiply(side)).add(0, 3.1, 0);
                        particleApi.spawnParticles(top, Particle.SPLASH, 2, 0.1, 0.1, 0.1, 0.2);
                    }
                }
                if (t % 8 == 0)
                    p.getWorld().playSound(waveFront, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 0.5f + r.nextFloat()*0.3f);
                for (Entity e : waveFront.getWorld().getNearbyEntities(waveFront, 3.5, 3, 3.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    e.setVelocity(fwd.clone().multiply(1.6).setY(0.35));
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 20, 2, false, false));
                    if (!hit.contains(e.getUniqueId())) {
                        hit.add(e.getUniqueId());
                        ((LivingEntity) e).damage(18, p);
                        waterSplash(e.getLocation().clone().add(0, 1, 0), null);
                    }
                }

                dist += 0.45;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void bubblePrison(Player p) {
        LivingEntity target = getNearestTarget(p, 7);
        if (target == null) {
            p.sendMessage(ChatColor.AQUA + "No target in sight 7 block!"); return;
        }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.9f, 1.8f);
        p.sendMessage(ChatColor.AQUA + "✦ Bubble Prison — " + target.getType().name());

        final Location prisionCenter = target.getLocation().clone().add(0, 1.5, 0);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 65, 5, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 65, 1, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,  65, 0, false, false));
        ((LivingEntity) target).damage(8, p);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60 || target.isDead() || !target.isValid()) { cancel(); return; }

                for (int lat = -3; lat <= 3; lat++) {
                    double y    = lat * 0.35;
                    double rad  = Math.sqrt(Math.max(0, 1.8*1.8 - y*y));
                    int pts     = Math.max(6, (int)(rad * 10));
                    for (int i = 0; i < pts; i++) {
                        double a = Math.toRadians(i * (360.0/pts) + t * 5);
                        Location lp = prisionCenter.clone().add(Math.cos(a)*rad, y, Math.sin(a)*rad);
                        Color c = (t + lat + i) % 4 == 0 ? C_FOAM :
                                  (t + lat + i) % 4 == 1 ? C_BUBBLE :
                                  (t + lat + i) % 4 == 2 ? C_AQUA : C_SKY_BLUE;
                        particleApi.spawnColoredParticles(lp, c, 0.9f, 1, 0.03, 0.03, 0.03);
                    }
                }
                if (t % 5 == 0)
                    particleApi.spawnParticles(prisionCenter, Particle.DRIPPING_WATER, 2, 0.4, 0.4, 0.4, 0.04);

                if (!target.isOnGround()) {
                    Vector vel = target.getVelocity();
                    if (vel.getY() < -0.1) target.setVelocity(vel.setY(-0.05));
                }

                if (target.getLocation().distance(prisionCenter) > 2.2) {
                    Vector pull = prisionCenter.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.5);
                    target.setVelocity(pull);
                }

                if (t % 20 == 0)
                    target.getWorld().playSound(prisionCenter, Sound.ENTITY_GENERIC_SPLASH, 0.3f, 1.8f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void hydroForm(Player p) {
        hydroActive = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH,        1f,   0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.7f, 1.4f);
        p.sendMessage(ChatColor.AQUA + "✦ " + ChatColor.BOLD + "Hydro Form!");
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         85, 2, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,85, 0, false, false));
        new BukkitRunnable() {
            @Override public void run() {
                Location loc = p.getLocation().clone().add(0, 1, 0);
                for (int i = 0; i < 24; i++) {
                    double a = Math.toRadians(i * 15);
                    for (double h = 0; h <= 2.0; h += 0.5) {
                        Location lp = loc.clone().add(Math.cos(a)*0.6, h, Math.sin(a)*0.6);
                        particleApi.spawnColoredParticles(lp, WATER_COLS[(i+3)%WATER_COLS.length], 1.1f, 2, 0.05, 0.05, 0.05);
                    }
                }
                particleApi.spawnParticles(loc, Particle.SPLASH, 25, 0.8, 0.8, 0.8, 0.4);
                particleApi.spawnParticles(loc, Particle.DRIPPING_WATER, 20, 0.6, 0.8, 0.6, 0.05);
            }
        }.runTask(magicPlugin);

        final Random rng = new Random();

        hydroRunnable = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80 || !p.isOnline()) {
                    endHydroForm(p);
                    cancel();
                    return;
                }

                Location loc = p.getLocation().clone().add(0, 1, 0);
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30 + t * 9);
                    double h = (i % 6) * 0.32;
                    double rx = 0.35 + h * 0.04;
                    Location lp = p.getLocation().clone().add(Math.cos(a)*rx, h, Math.sin(a)*rx);
                    particleApi.spawnColoredParticles(lp, AURA_COLS[i % AURA_COLS.length], 0.95f, 1, 0.04, 0.04, 0.04);
                }
                if (t % 3 == 0) {
                    for (int i = 0; i < 3; i++) {
                        Location drop = p.getLocation().clone().add(
                                (rng.nextDouble()-0.5)*0.6,
                                rng.nextDouble()*2.2,
                                (rng.nextDouble()-0.5)*0.6);
                        particleApi.spawnParticles(drop, Particle.DRIPPING_WATER, 1, 0.05, 0.05, 0.05, 0.04);
                        particleApi.spawnColoredParticles(drop, C_BUBBLE, 0.8f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 6 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45 - t * 12);
                        Location bp = loc.clone().add(Math.cos(a)*0.9, rng.nextDouble()*0.4-0.2, Math.sin(a)*0.9);
                        particleApi.spawnColoredParticles(bp, C_FOAM, 1.0f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 20 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.2f, 1.8f);
                if (t % 4 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.5, 1.5, 1.5)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(2, p);
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  25, 2, false, true));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 0, false, true));
                        particleApi.spawnParticles(e.getLocation().clone().add(0,1,0),
                                Particle.SPLASH, 10, 0.3, 0.2, 0.3, 0.15);
                        particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                                C_AQUA, 1.2f, 6, 0.25, 0.25, 0.25);
                    }
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 8, 2, false, false));

                t++;
            }
        };
        hydroRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void endHydroForm(Player p) {
        hydroActive = false;
        if (hydroRunnable != null) { hydroRunnable.cancel(); hydroRunnable = null; }
        Location loc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnParticles(loc, Particle.SPLASH, 30, 1.0, 1.0, 1.0, 0.4);
        particleApi.spawnColoredParticles(loc, C_AQUA,   1.5f, 20, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(loc, C_FOAM,   1.2f, 30, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc, C_OCEAN,  1.3f, 25, 1.2, 1.2, 1.2);
        particleApi.spawnParticles(loc, Particle.DRIPPING_WATER, 15, 0.8, 0.5, 0.8, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH,         0.9f, 0.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.5f, 1.6f);
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 3, 3, 3)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            ((LivingEntity) e).damage(5, p);
            Vector kb = e.getLocation().subtract(p.getLocation()).toVector();
            if (kb.lengthSquared() < 0.01) kb = new Vector(1, 0, 0);
            e.setVelocity(kb.normalize().multiply(1.0).setY(0.35));
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 1, false, false));
        }

        p.sendMessage(ChatColor.AQUA + "Hydro Form ended.");
    }
    private void aquaCounter(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (hydroActive) {
            ((org.bukkit.event.entity.EntityDamageByEntityEvent) ex.getRawEvent()).setCancelled(true);
            particleApi.spawnParticles(p.getLocation().clone().add(0,1,0), Particle.SPLASH, 20, 0.4, 0.4, 0.4, 0.2);
            particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), C_FOAM, 1.2f, 10, 0.3, 0.3, 0.3);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.5f, 1.6f);
            return;
        }
        if (CooldownApi.isOnCooldown(w_counter, p)) return;

        Entity damager = ((EntityDamageByEntityEvent) ex.getRawEvent()).getDamager();
        if (!(damager instanceof LivingEntity)) return;

        Location loc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnParticles(loc, Particle.SPLASH, 35, 0.6, 0.5, 0.6, 0.3);
        particleApi.spawnColoredParticles(loc, C_AQUA,  1.4f, 20, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(loc, C_FOAM,  1.1f, 15, 0.6, 0.5, 0.6);
        p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.5f);

        ((LivingEntity) damager).damage(6, p);
        ((LivingEntity) damager).addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false));
        Vector kb = damager.getLocation().subtract(p.getLocation()).toVector().normalize().multiply(1.3).setY(0.3);
        damager.setVelocity(kb);

        addCd(w_counter, p);
    }

    private void handleDamage(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING)
            event.setCancelled(true);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (p.getFireTicks() > 0) p.setFireTicks(0);
                if (p.isInWater()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         25, 1, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,          25, 2, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,25, 0, false, false));
                }

                for (int i = 0; i < 10; i++) {
                    double a1 = Math.toRadians(i * 36 + t * 8);
                    double a2 = Math.toRadians(i * 36 - t * 11);
                    Location lp1 = p.getLocation().clone().add(Math.cos(a1)*1.05, 0.12 + Math.sin(a1*0.5)*0.06, Math.sin(a1)*1.05);
                    Location lp2 = p.getLocation().clone().add(Math.cos(a2)*0.65, 0.06 + Math.sin(a2*0.5)*0.05, Math.sin(a2)*0.65);
                    if (isAuraEnabled(p)) {
                        particleApi.spawnColoredParticles(lp1, AURA_COLS[i % AURA_COLS.length], 0.9f, 1, 0.03, 0.03, 0.03);
                        particleApi.spawnColoredParticles(lp2, i%2==0 ? C_AQUA : C_BUBBLE,      0.8f, 1, 0.03, 0.03, 0.03);
                    }
                }
                if (t % 4 == 0 && isAuraEnabled(p))
                    particleApi.spawnParticles(p.getLocation().clone().add(
                            (r.nextDouble()-0.5)*1.4, r.nextDouble()*0.3, (r.nextDouble()-0.5)*1.4),
                            Particle.DRIPPING_WATER, 1, 0.04, 0.04, 0.04, 0.04);

                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 20);
        return task;
    }

    @Override
    public void remove() {
        dashing = false;
        if (hydroRunnable != null) { hydroRunnable.cancel(); hydroRunnable = null; }
        hydroActive = false;
    }
    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&b潮汐箭";
            case 1: return "&b漩涡";
            case 2: return "&b海浪冲击";
            case 3: return "&b水之冲刺";
            case 4: return "&b&l海啸";
            case 5: return "&b气泡牢笼";
            case 6: return "&b&l水之形态";
            default: return "&7none";
        }
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
    }

    private void drawWaterTrail(Location from, Location to) {
        new BukkitRunnable() {
            @Override public void run() {
                if (from.getWorld() == null) return;
                double dist = from.distance(to);
                if (dist < 0.1) return;
                int steps = (int)(dist * 4);
                if (steps == 0) return;
                Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur = from.clone();
                Random r = new Random();
                for (int i = 0; i < steps; i++) {
                    particleApi.spawnColoredParticles(cur, WATER_COLS[r.nextInt(WATER_COLS.length)], 1.0f, 2, 0.06, 0.06, 0.06);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(cur, Particle.SPLASH, 1, 0.05, 0.05, 0.05, 0.08);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);
    }

    private Location getRaycastGround(Player p, int maxDist) {
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

    private Vector yawRotate(Vector v, double deg) {
        double r = Math.toRadians(deg);
        return new Vector(v.getX()*Math.cos(r)+v.getZ()*Math.sin(r), v.getY(),
                         -v.getX()*Math.sin(r)+v.getZ()*Math.cos(r));
    }
}

