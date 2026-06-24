package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.cooldowns.CooldownApi;
import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilities.MagicAbilities.*;
import static net.trduc.magicabilities.misc.PowerUtils.*;
import static net.trduc.magicabilities.cooldowns.Cooldowns.cooldowns;
import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.players.PowerPlayer.players;

public class WindPower extends Power implements IdlePower, Removeable {

    private static final String w_slash   = "wind.slash";
    private static final String w_cyclone = "wind.cyclone";
    private static final String w_burst   = "wind.burst";
    private static final String w_step    = "wind.step";
    private static final String w_tempest = "wind.tempest";
    private static final String w_leap    = "wind.leap";

    private static final Color C_WHITE      = Color.fromRGB(255, 255, 255);
    private static final Color C_SILVER     = Color.fromRGB(210, 215, 220);
    private static final Color C_SILVER_MID = Color.fromRGB(180, 190, 200);
    private static final Color C_SILVER_DIM = Color.fromRGB(140, 155, 165);
    private static final Color C_ICY_WHITE  = Color.fromRGB(235, 245, 255);
    private static final Color C_PEARL      = Color.fromRGB(250, 250, 248);

    private static final Color[] WIND_COLORS = {
            C_WHITE, C_SILVER, C_ICY_WHITE, C_PEARL, C_SILVER_MID
    };

    private boolean leaping = false;
    private BukkitRunnable leapRunnable = null;

    public WindPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute) {
            handleDamage((DamagedExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeftClick((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRightClick((RightClickExecute) ex); return; }
        if (ex instanceof MoveExecute)       { onMove((MoveExecute) ex); }
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(w_slash, p, this)) return; galeSlash(p);  addCd(w_slash, p);   return;
            case 1: if (onCd(w_cyclone, p, this)) return; cyclone(p);    addCd(w_cyclone, p); return;
            case 2: if (onCd(w_burst, p, this)) return; windBurst(p);  addCd(w_burst, p);   return;
            case 3: if (onCd(w_step, p, this)) return; galeStep(p);   addCd(w_step, p);    return;
            case 4: if (onCd(w_tempest, p, this)) return; tempest(p);    addCd(w_tempest, p);
        }
    }

    private void onRightClick(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot == 5) {
            if (onCd(w_leap, p, this)) return;
            skyLeap(p);
            addCd(w_leap, p);
        }
    }
    private void galeSlash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW,  0.6f, 1.5f);

        ArmorStand blade = spawnAs(p.getEyeLocation().clone());
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        Random r = new Random();
        Set<Entity> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (blade.isDead() || t > 40) { safeRemove(blade); cancel(); return; }

                blade.teleport(blade.getLocation().add(dir.clone().multiply(1.6)));
                Location loc = blade.getLocation();
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 35 + i * 120);
                    Vector side = new Vector(Math.cos(a)*0.5, Math.sin(a)*0.3, Math.sin(a)*0.5);
                    particleApi.spawnColoredParticles(loc.clone().add(side),
                            WIND_COLORS[r.nextInt(WIND_COLORS.length)], 1.1f, 2, 0.05, 0.05, 0.05);
                }
                particleApi.spawnParticles(loc, Particle.CLOUD, 3, 0.12, 0.08, 0.12, 0.02);
                particleApi.spawnColoredParticles(loc, C_WHITE, 1.3f, 2, 0.08, 0.06, 0.08);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.1, 1.1, 1.1)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(14, p);
                    e.setVelocity(dir.clone().multiply(1.8).setY(0.4));
                    particleApi.spawnParticles(loc, Particle.CLOUD, 20, 0.4, 0.4, 0.4, 0.15);
                    particleApi.spawnColoredParticles(loc, C_WHITE, 1.5f, 15, 0.4, 0.4, 0.4);
                    loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_HURT, 0.5f, 1.6f);
                }
                if (!loc.getBlock().isPassable()) { safeRemove(blade); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void cyclone(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 0.6f);
        p.sendMessage(ChatColor.WHITE + "✦ Cyclone!");

        Set<Entity> sucked = new HashSet<>();
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60 || !p.isOnline()) {
                    for (Entity e : sucked) {
                        if (e.isValid() && !e.isDead()) {
                            e.setVelocity(new Vector(
                                    (r.nextDouble()-0.5)*1.2,
                                    2.8,
                                    (r.nextDouble()-0.5)*1.2));
                            ((LivingEntity) e).damage(12, p);
                        }
                    }
                    particleApi.spawnParticles(p.getLocation().clone().add(0,1,0),
                            Particle.CLOUD, 80, 2, 2, 2, 0.3);
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0),
                            C_WHITE, 1.8f, 60, 2, 2, 2);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.4f);
                    cancel();
                    return;
                }

                Location center = p.getLocation().clone().add(0, 1, 0);
                for (int ring = 0; ring < 3; ring++) {
                    double ringRad = 2.5 + ring * 0.4;
                    double yOff    = ring * 0.6;
                    for (int i = 0; i < 16; i++) {
                        double a = Math.toRadians(i * 22.5 - t * (12 + ring * 4));
                        Location lp = center.clone().add(
                                Math.cos(a) * ringRad, yOff, Math.sin(a) * ringRad);
                        particleApi.spawnColoredParticles(lp,
                                WIND_COLORS[(t + i + ring) % WIND_COLORS.length],
                                1f + ring * 0.1f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 4 == 0)
                    particleApi.spawnParticles(center, Particle.CLOUD, 4, 1.8, 0.8, 1.8, 0.05);

                for (Entity e : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    sucked.add(e);
                    Vector pull = center.toVector().subtract(e.getLocation().toVector())
                            .normalize().multiply(0.35);
                    pull.setY(Math.max(pull.getY(), 0.05));
                    e.setVelocity(pull);
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 10, 2, false, false));
                }

                if (t % 20 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.7f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void windBurst(Player p) {
        Location center = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2f);
        new BukkitRunnable() {
            double rad = 0.5; int t = 0;
            @Override public void run() {
                if (rad > 5.5) { cancel(); return; }
                for (int i = 0; i < 14; i++) {
                    double a = Math.toRadians(i * 15 + t * 7);
                    Location lp = center.clone().add(
                            Math.cos(a) * rad, 0.1, Math.sin(a) * rad);
                    particleApi.spawnColoredParticles(lp,
                            t % 2 == 0 ? C_WHITE : C_SILVER, 1.2f, 2, 0.05, 0.05, 0.05);
                    if (t % 3 == 0)
                        particleApi.spawnParticles(lp, Particle.CLOUD, 1, 0.04, 0.04, 0.04, 0.04);
                }
                rad += 0.55; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
        particleApi.spawnParticles(center, Particle.CLOUD, 30, 1.5, 1.5, 1.5, 0.25);
        particleApi.spawnColoredParticles(center, C_WHITE, 1.8f, 25, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(center, C_SILVER, 1.4f, 20, 1.5, 1.5, 1.5);

        for (Entity e : center.getWorld().getNearbyEntities(center, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(center);
            double power = Math.max(0.5, 2.5 - dist * 0.35);
            Vector kb = e.getLocation().subtract(center).toVector().normalize().multiply(power).setY(0.5);
            e.setVelocity(kb);
            ((LivingEntity) e).damage(6, p);
            ((LivingEntity) e).addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false));
        }
    }

    private void galeStep(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.6f, 2f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0.15).normalize();
        Location from = p.getLocation().clone();
        Location to   = from.clone();

        for (int i = 0; i < 14; i++) {
            to.add(dir.clone().multiply(0.5));
            if (!to.getBlock().isPassable()) { to.subtract(dir.clone().multiply(0.5)); break; }
        }

        drawWindTrail(from.clone().add(0,1,0), to.clone().add(0,1,0));

        p.teleport(to.clone().add(0, 0.5, 0));
        particleApi.spawnParticles(to.clone().add(0,1,0), Particle.CLOUD, 30, 0.6, 0.6, 0.6, 0.2);
        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_WHITE, 1.5f, 25, 0.5, 0.5, 0.5);
        p.getWorld().playSound(to, Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.6f);
        int numPoints = 6;
        for (int i = 1; i <= numPoints; i++) {
            double frac = (double) i / (numPoints + 1);
            Location trailLoc = from.clone().add(
                    to.toVector().subtract(from.toVector()).multiply(frac)).add(0, 0.5, 0);
            spawnWindTrailZone(trailLoc, p);
        }
    }

    private void spawnWindTrailZone(Location loc, Player p) {
        Random r = new Random();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60) { cancel(); return; }
                if (t % 5 == 0) {
                    particleApi.spawnColoredParticles(loc,
                            WIND_COLORS[r.nextInt(WIND_COLORS.length)], 1f, 3, 0.3, 0.4, 0.3);
                    particleApi.spawnParticles(loc, Particle.CLOUD, 2, 0.25, 0.3, 0.25, 0.05);
                }
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.2, 1.5, 1.2)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(3, p);
                    e.setVelocity(e.getVelocity().add(new Vector(
                            (r.nextDouble()-0.5)*0.8, 0.3, (r.nextDouble()-0.5)*0.8)));
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, false));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void tempest(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 0.4f);
        p.sendMessage(ChatColor.WHITE + "✦ Tempest summoned!");

        Location center = getRaycastGround(p, 20);
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 120 || !p.isOnline()) {
                    p.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 1.5f);
                    cancel();
                    return;
                }
                for (int layer = 0; layer < 4; layer++) {
                    double rad  = 3.5 + layer * 0.8;
                    double yOff = layer * 0.5;
                    int dir     = layer % 2 == 0 ? 1 : -1;
                    for (int i = 0; i < 18; i++) {
                        double a = Math.toRadians(i * 20 + t * (10 + layer * 3) * dir);
                        Location lp = center.clone().add(
                                Math.cos(a) * rad, yOff, Math.sin(a) * rad);
                        particleApi.spawnColoredParticles(lp,
                                WIND_COLORS[(i + layer + t) % WIND_COLORS.length],
                                0.9f + layer * 0.1f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 3 == 0)
                    particleApi.spawnParticles(
                            center.clone().add((r.nextDouble()-0.5)*5, r.nextDouble()*2.5, (r.nextDouble()-0.5)*5),
                            Particle.CLOUD, 3, 0.3, 0.3, 0.3, 0.08);
                if (t % 25 == 0)
                    center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.4f, 0.5f + r.nextFloat()*0.4f);
                for (Entity e : center.getWorld().getNearbyEntities(center, 6, 4, 6)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;
                    double ex = e.getLocation().getX() - center.getX();
                    double ez = e.getLocation().getZ() - center.getZ();
                    double len = Math.sqrt(ex*ex + ez*ez);
                    if (len > 0.1) {
                        double nx = -ez / len;
                        double nz =  ex / len;
                        e.setVelocity(new Vector(nx * 0.4, 0.08, nz * 0.4));
                    }
                    if (t % 15 == 0) le.damage(4, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, true));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  20, 2, false, false));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void skyLeap(Player p) {
        if (leaping) return;
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW,  1f, 1.2f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 0.8f);
        p.setVelocity(new Vector(
                p.getVelocity().getX() * 0.3,
                2.8,
                p.getVelocity().getZ() * 0.3));

        leaping = true;
        particleApi.spawnParticles(p.getLocation().clone().add(0,0.5,0), Particle.CLOUD, 30, 0.8, 0.3, 0.8, 0.15);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0,0.5,0), C_WHITE, 1.5f, 25, 0.6, 0.3, 0.6);

        leapRunnable = new BukkitRunnable() {
            int t = 0;
            boolean gliding = false;

            @Override public void run() {
                if (!p.isOnline() || t > 120) { stopLeap(p); cancel(); return; }

                Location loc = p.getLocation().clone().add(0, 0.5, 0);

                particleApi.spawnColoredParticles(loc, WIND_COLORS[t % WIND_COLORS.length],
                        1.1f, 3, 0.3, 0.1, 0.3);
                if (t % 4 == 0)
                    particleApi.spawnParticles(loc, Particle.CLOUD, 2, 0.25, 0.1, 0.25, 0.04);

                if (p.getVelocity().getY() <= 0.05 && !gliding && !p.isOnGround()) {
                    gliding = true;
                    p.sendMessage(ChatColor.GRAY + "Gliding...");
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.5f, 1.4f);
                }

                if (gliding && !p.isOnGround()) {
                    Vector vel = p.getVelocity();
                    if (vel.getY() < -0.18) {
                        p.setVelocity(vel.setY(-0.18));
                    }
                    Vector look = p.getEyeLocation().getDirection().clone().setY(0).normalize();
                    p.setVelocity(p.getVelocity().add(look.multiply(0.04)));
                }
                if (p.isOnGround() && t > 5) {
                    particleApi.spawnParticles(p.getLocation(), Particle.CLOUD, 25, 1.0, 0.2, 1.0, 0.15);
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0,0.5,0),
                            C_WHITE, 1.4f, 20, 0.8, 0.3, 0.8);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.6f, 1.2f);
                    stopLeap(p); cancel();
                    return;
                }
                t++;
            }
        };
        leapRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void stopLeap(Player p) {
        leaping = false;
        if (leapRunnable != null) { leapRunnable.cancel(); leapRunnable = null; }
    }

    private void handleDamage(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    private long lastMoveMs = 0;
    private void onMove(MoveExecute ex) {
        long now = System.currentTimeMillis();
        if (now - lastMoveMs < 200) return;
        lastMoveMs = now;
        Player p = ex.getPlayer();
        if (leaping && p.isOnGround()) stopLeap(p);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 1, false, false));
                p.setFallDistance(0);
                for (int i = 0; i < 8; i++) {
                    double a1 = Math.toRadians(i * 45 + t * 10);
                    double a2 = Math.toRadians(i * 45 - t * 14);
                    Location lp1 = p.getLocation().clone().add(
                            Math.cos(a1)*1.0, 0.1 + Math.sin(a1*0.5)*0.05, Math.sin(a1)*1.0);
                    Location lp2 = p.getLocation().clone().add(
                            Math.cos(a2)*0.6, 0.08 + Math.sin(a2*0.5)*0.04, Math.sin(a2)*0.6);
                    if (isAuraEnabled(p)) {
                        particleApi.spawnColoredParticles(lp1,
                                WIND_COLORS[i % WIND_COLORS.length], 0.9f, 1, 0.03, 0.03, 0.03);
                        particleApi.spawnColoredParticles(lp2,
                                i%2==0 ? C_SILVER : C_ICY_WHITE, 0.8f, 1, 0.03, 0.03, 0.03);
                    }
                }
                if (t % 3 == 0 && isAuraEnabled(p))
                    particleApi.spawnParticles(
                            p.getLocation().clone().add(
                                    (r.nextDouble()-0.5)*1.2, r.nextDouble()*0.25, (r.nextDouble()-0.5)*1.2),
                            Particle.CLOUD, 1, 0.04, 0.04, 0.04, 0.06);

                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 20);
        return task;
    }

    @Override
    public void remove() {
        if (leapRunnable != null) { leapRunnable.cancel(); leapRunnable = null; }
        leaping = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&fGale Slash";
            case 1: return "&fCyclone";
            case 2: return "&fWind Burst";
            case 3: return "&fGale Step";
            case 4: return "&f&lTempest";
            case 5: return "&fSky Leap";
            default: return "&7none";
        }
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
    }

    private void drawWindTrail(Location from, Location to) {
        new BukkitRunnable() {
            @Override public void run() {
                if (from.getWorld() == null) return;
                double dist = from.distance(to);
                if (dist < 0.1) return;
                int steps = (int)(dist * 4);
                Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur = from.clone();
                Random r = new Random();
                for (int i = 0; i < steps; i++) {
                    particleApi.spawnColoredParticles(cur,
                            WIND_COLORS[r.nextInt(WIND_COLORS.length)], 1f, 2, 0.06, 0.06, 0.06);
                    if (i % 3 == 0)
                        particleApi.spawnParticles(cur, Particle.CLOUD, 1, 0.05, 0.05, 0.05, 0.04);
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
                cur.subtract(dir.clone().multiply(0.5));
                break;
            }
        }
        return cur;
    }
}
