package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
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

public class DemonPower extends Power implements IdlePower, Removeable {

    private static final String dm_soulbolt = "demon.soulbolt";
    private static final String dm_hellfire = "demon.hellfire";
    private static final String dm_cleave   = "demon.cleave";
    private static final String dm_drain    = "demon.drain";
    private static final String dm_charge   = "demon.charge";
    private static final String dm_grasp    = "demon.grasp";
    private static final String dm_counter  = "demon.counter";

    private static final Color C_BLOOD_RED   = Color.fromRGB(180,  10,  10);
    private static final Color C_DEEP_RED    = Color.fromRGB(110,   0,   0);
    private static final Color C_CRIMSON     = Color.fromRGB(210,  30,  55);
    private static final Color C_PURPLE_DARK = Color.fromRGB( 70,   0, 145);
    private static final Color C_PURPLE_MID  = Color.fromRGB(130,  15, 210);
    private static final Color C_PURPLE_LT   = Color.fromRGB(185,  75, 255);
    private static final Color C_SOUL_CYAN   = Color.fromRGB(  0, 165, 175);
    private static final Color C_VOID_BLACK  = Color.fromRGB( 18,   4,  28);
    private static final Color C_FIRE_ORANGE = Color.fromRGB(255,  95,   0);
    private static final Color C_FIRE_YELLOW = Color.fromRGB(255, 205,  25);

    private static final Color[] AURA_COLORS = { C_BLOOD_RED, C_CRIMSON, C_PURPLE_MID, C_PURPLE_LT, C_DEEP_RED };
    private static final Color[] SOUL_COLORS  = { C_PURPLE_MID, C_PURPLE_LT, C_SOUL_CYAN, C_PURPLE_DARK };
    private static final Color[] HELL_COLORS  = { C_BLOOD_RED, C_CRIMSON, C_FIRE_ORANGE, C_FIRE_YELLOW };

    private boolean draining = false;
    private BukkitRunnable drainRunnable = null;
    private boolean charging = false;

    public DemonPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) {
            if (draining && drainRunnable != null) {
                drainRunnable.cancel();
                drainRunnable = null;
                draining = false;
                ((DamagedByExecute) ex).getPlayer().sendMessage(
                        ChatColor.DARK_RED + "Soul Drain cancelled!");
                addCd(dm_drain, ((DamagedByExecute) ex).getPlayer(), 0.5);
            }
            shadowCounter((DamagedByExecute) ex);
            return;
        }
        if (ex instanceof DamagedExecute) {
            preventEnvFire((DamagedExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(dm_soulbolt, p, this)) return; soulBolt(p);    addCd(dm_soulbolt, p); return;
            case 1: if (onCd(dm_hellfire, p, this)) return; hellfireRain(p); addCd(dm_hellfire, p); return;
            case 2: if (onCd(dm_cleave, p, this)) return; shadowCleave(p); addCd(dm_cleave, p);   return;
            case 3: if (draining) { p.sendMessage(ChatColor.DARK_PURPLE + "Channeling Soul Drain!"); return; }
                    if (onCd(dm_drain, p, this)) return; soulDrain(p); return;
            case 4: if (charging) return;
                    if (onCd(dm_charge, p, this)) return; demonCharge(p); return;
            case 5: if (onCd(dm_grasp, p, this)) return; voidGrasp(p); addCd(dm_grasp, p);
        }
    }

    private void soulBolt(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.7f, 1.8f);

        final Location start = p.getEyeLocation().clone();
        final Vector dir = p.getEyeLocation().getDirection().clone().normalize().multiply(0.85);
        final Random r = new Random();

        new BukkitRunnable() {
            Location cur = start.clone();
            int t = 0;
            boolean hit = false;

            @Override
            public void run() {
                if (t > 37 || hit) { cancel(); return; }

                cur.add(dir);

                if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                    soulPop(cur);
                    cancel();
                    return;
                }

                particleApi.spawnParticles(cur, Particle.SOUL_FIRE_FLAME, 2, 0.1, 0.1, 0.1, 0.02);
                particleApi.spawnColoredParticles(cur, SOUL_COLORS[t % SOUL_COLORS.length], 1.3f, 3, 0.09, 0.09, 0.09);
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(i * 120 + t * 30);
                    Location ring = cur.clone().add(Math.cos(a)*0.3, Math.sin(a*0.5)*0.15, Math.sin(a)*0.3);
                    particleApi.spawnColoredParticles(ring, C_CRIMSON, 0.9f, 1, 0.03, 0.03, 0.03);
                }

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 0.75, 0.75, 0.75)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    hit = true;
                    ((LivingEntity) e).damage(12, p);
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                    soulPop(e.getLocation().clone().add(0, 1, 0));
                    cancel();
                    return;
                }

                if (t % 9 == 0)
                    p.getWorld().playSound(cur, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.1f, 1.6f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void soulPop(Location loc) {
        particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 14, 0.35, 0.35, 0.35, 0.04);
        particleApi.spawnColoredParticles(loc, C_PURPLE_LT, 1.2f, 12, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_SOUL_CYAN, 1.0f, 8, 0.4, 0.4, 0.4);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, 0.35f, 1.9f);
    }

    private void hellfireRain(Player p) {
        Location center = getRaycastTarget(p, 28);
        Random r = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT,  1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.8f);

        for (int i = 0; i < 5; i++) {
            final Location base = center.clone().add(
                    (r.nextDouble() - 0.5) * 7, 0, (r.nextDouble() - 0.5) * 7);
            adjustToGround(base);
            final int delay = 5 + r.nextInt(25);

            new BukkitRunnable() {
                int wt = 0;
                @Override public void run() {
                    if (wt >= delay) { cancel(); return; }
                    double pulse = 0.9 + Math.sin(wt * 0.45) * 0.2;
                    for (int j = 0; j < 10; j++) {
                        double a = Math.toRadians(j * 36 + wt * 10);
                        Location lp = base.clone().add(Math.cos(a)*pulse*1.3, 0.05, Math.sin(a)*pulse*1.3);
                        particleApi.spawnColoredParticles(lp, wt % 2 == 0 ? C_BLOOD_RED : C_FIRE_ORANGE, 0.9f, 1, 0.04, 0.01, 0.04);
                        particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.01, 0.04, 0.02);
                    }
                    wt++;
                }
            }.runTaskTimer(magicPlugin, 0, 1);

            new BukkitRunnable() {
                @Override public void run() { hellPillar(base, p); }
            }.runTaskLater(magicPlugin, delay);
        }
    }

    private void hellPillar(Location base, Player p) {
        Random r = new Random();
        new BukkitRunnable() {
            @Override public void run() {
                for (double y = 0; y <= 10; y += 0.55) {
                    Location lp = base.clone().add((r.nextDouble()-0.5)*0.6, y, (r.nextDouble()-0.5)*0.6);
                    particleApi.spawnColoredParticles(lp, HELL_COLORS[r.nextInt(HELL_COLORS.length)], 1.35f, 2, 0.08, 0.04, 0.08);
                    particleApi.spawnParticles(lp, Particle.FLAME, 2, 0.07, 0.04, 0.07, 0.04);
                }
            }
        }.runTask(magicPlugin);

        base.getWorld().playSound(base, Sound.ENTITY_BLAZE_HURT, 0.8f, 0.65f);

        new BukkitRunnable() {
            @Override public void run() {
                particleApi.spawnColoredParticles(base.clone().add(0,0.5,0), C_CRIMSON, 1.6f, 25, 0.6, 0.4, 0.6);
                particleApi.spawnParticles(base, Particle.LAVA, 6, 0.4, 0.2, 0.4, 0.08);
                for (Entity e : base.getWorld().getNearbyEntities(base, 1.8, 2.5, 1.8)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(14, p);
                    e.setFireTicks(60);
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, true));
                }
            }
        }.runTaskLater(magicPlugin, 2L);
    }

    private void shadowCleave(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        Random r = new Random();

        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.8f);
        p.getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT,    0.7f, 1.5f);

        new BukkitRunnable() {
            double radius = 0.2;
            int t = 0;
            @Override public void run() {
                if (radius > 4.5) { cancel(); return; }
                for (int i = 0; i < 24; i++) {
                    double a = Math.toRadians(i * 15 + t * 9);
                    Location lp = loc.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius);
                    Color c = t % 3 == 0 ? C_BLOOD_RED : t % 3 == 1 ? C_PURPLE_MID : C_CRIMSON;
                    particleApi.spawnColoredParticles(lp, c, 1.2f, 2, 0.05, 0.05, 0.05);
                    if (i % 3 == 0)
                        particleApi.spawnParticles(lp, Particle.SOUL, 1, 0.04, 0.04, 0.04, 0.03);
                }
                if (t % 2 == 0) {
                    for (int j = 0; j < 5; j++) {
                        double a = Math.toRadians(j * 72 + t * 18);
                        Location blade = loc.clone().add(Math.cos(a)*radius*0.65, 0, Math.sin(a)*radius*0.65);
                        particleApi.spawnColoredParticles(blade, C_VOID_BLACK, 0.6f, 1, 0.04, 0.18, 0.04);
                        particleApi.spawnColoredParticles(blade, C_PURPLE_LT,  0.9f, 1, 0.04, 0.08, 0.04);
                    }
                }
                radius += 0.5;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : p.getWorld().getNearbyEntities(loc, 3.5, 2.5, 3.5)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(loc);
                    double dmg = Math.max(6, 14 - dist * 2.2);
                    ((LivingEntity) e).damage(dmg, p);
                    Vector kb = e.getLocation().subtract(loc).toVector();
                    if (kb.lengthSquared() < 0.01) kb = new Vector(r.nextDouble()-0.5, 0, r.nextDouble()-0.5);
                    kb.normalize().multiply(1.4).setY(0.35);
                    e.setVelocity(kb);
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, true));
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_VOID_BLACK, 0.7f, 5, 0.25, 0.35, 0.25);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_CRIMSON,    1.1f, 4, 0.2,  0.25, 0.2);
                }
            }
        }.runTaskLater(magicPlugin, 5L);
    }

    private void soulDrain(Player p) {
        draining = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.9f, 0.85f);

        drainRunnable = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 50 || !p.isOnline()) {
                    addCd(dm_drain, p);
                    draining = false;
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.6f);
                    cancel();
                    return;
                }

                Location pLoc = p.getLocation().clone().add(0, 1, 0);
                double healed = 0;

                if (t % 5 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(pLoc, 3.5, 3.5, 3.5)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(3, p);
                        healed += 1.5;
                    }
                    if (healed > 0 && p.getHealth() < getMaxHp(p))
                        p.setHealth(Math.min(getMaxHp(p), p.getHealth() + healed));
                }

                if (t % 2 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(pLoc, 3.5, 3.5, 3.5)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        Vector toP = pLoc.toVector().subtract(e.getLocation().clone().add(0,1,0).toVector()).normalize();
                        Location ts = e.getLocation().clone().add(0, 1, 0);
                        double dist = ts.distance(pLoc);
                        for (int s = 1; s <= 5; s++) {
                            Location mid = ts.clone().add(toP.clone().multiply(dist * (s / 5.0)));
                            particleApi.spawnColoredParticles(mid, SOUL_COLORS[s % SOUL_COLORS.length], 0.85f, 1, 0.05, 0.05, 0.05);
                            if (s == 3)
                                particleApi.spawnParticles(mid, Particle.SOUL_FIRE_FLAME, 1, 0.04, 0.04, 0.04, 0.02);
                        }
                    }
                }
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 + t * 14);
                    Location ring = pLoc.clone().add(Math.cos(a)*0.85, Math.sin(a*0.3)*0.25, Math.sin(a)*0.85);
                    particleApi.spawnColoredParticles(ring, SOUL_COLORS[i % SOUL_COLORS.length], 1.05f, 1, 0.03, 0.03, 0.03);
                }

                if (t % 12 == 0)
                    p.getWorld().playSound(pLoc, Sound.ENTITY_WITHER_HURT, 0.25f, 1.7f);
                t++;
            }
        };
        drainRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void demonCharge(Player p) {
        charging = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.6f, 1.6f);

        final Vector dir = p.getEyeLocation().getDirection().clone().setY(0.12).normalize();
        final Set<UUID> onPathHit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 8) {
                    chargeBlast(p.getLocation(), p);
                    charging = false;
                    addCd(dm_charge, p);
                    cancel();
                    return;
                }

                Location next = p.getLocation().clone().add(dir.clone().multiply(1.05));
                if (!next.getBlock().isPassable() || next.getBlock().isLiquid()) {
                    chargeBlast(p.getLocation(), p);
                    charging = false;
                    addCd(dm_charge, p);
                    cancel();
                    return;
                }
                p.setVelocity(dir.clone().multiply(1.55));

                Random r = new Random();
                Location tl = p.getLocation().clone();
                for (int i = 0; i < 6; i++) {
                    Location lp = tl.clone().add((r.nextDouble()-0.5)*0.5, r.nextDouble()*1.9, (r.nextDouble()-0.5)*0.5);
                    particleApi.spawnColoredParticles(lp, t%2==0 ? C_BLOOD_RED : C_PURPLE_MID, 1.15f, 1, 0.06, 0.06, 0.06);
                    particleApi.spawnParticles(lp, Particle.SOUL, 1, 0.06, 0.06, 0.06, 0.02);
                }

                for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.2, 1.7, 1.2)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || onPathHit.contains(e.getUniqueId())) continue;
                    onPathHit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(8, p);
                    e.setVelocity(dir.clone().multiply(1.8).setY(0.45));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void chargeBlast(Location loc, Player p) {
        Random r = new Random();
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_BLOOD_RED,   1.8f, 55, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_PURPLE_LT,   1.5f, 40, 1.2, 1.2, 1.2);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_VOID_BLACK,   0.7f, 25, 1.5, 1.5, 1.5);
        particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 18, 0.8, 0.4, 0.8, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.9f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   0.7f, 1.5f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            double dmg = Math.max(6, 14 - dist * 2.5);
            ((LivingEntity) e).damage(dmg, p);
            Vector kb = e.getLocation().subtract(loc).toVector();
            if (kb.lengthSquared() < 0.01) kb = new Vector(r.nextDouble()-0.5, 0.2, r.nextDouble()-0.5);
            kb.normalize().multiply(1.3).setY(0.45);
            e.setVelocity(kb);
            ((LivingEntity) e).addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, false));
        }
    }

    private void voidGrasp(Player p) {
        LivingEntity target = getNearestTarget(p, 7);
        if (target == null) {
            p.sendMessage(ChatColor.DARK_RED + "No target in sight!");
            return;
        }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 0.55f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT,    0.6f, 1.4f);

        final Location fromLoc = target.getLocation().clone();
        Location pullTo = p.getEyeLocation().clone()
                .add(p.getEyeLocation().getDirection().normalize().multiply(2.5));
        pullTo.setY(p.getLocation().getY());

        new BukkitRunnable() {
            @Override public void run() {
                int steps = Math.max(4, (int) fromLoc.distance(pullTo) * 3);
                Vector step = pullTo.toVector().subtract(fromLoc.toVector()).multiply(1.0 / steps);
                Location cur = fromLoc.clone().add(0, 1, 0);
                for (int i = 0; i < steps; i++) {
                    particleApi.spawnColoredParticles(cur, SOUL_COLORS[i % SOUL_COLORS.length], 1.15f, 2, 0.05, 0.05, 0.05);
                    if (i % 3 == 0)
                        particleApi.spawnParticles(cur, Particle.SOUL_FIRE_FLAME, 1, 0.04, 0.04, 0.04, 0.02);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);

        target.teleport(pullTo);
        target.setVelocity(new Vector(0, 0.1, 0));
        ((LivingEntity) target).damage(12, p);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2, false, false));

        particleApi.spawnParticles(pullTo.clone().add(0,1,0), Particle.SOUL_FIRE_FLAME, 18, 0.35, 0.35, 0.35, 0.04);
        particleApi.spawnColoredParticles(pullTo.clone().add(0,1,0), C_PURPLE_LT, 1.4f, 15, 0.35, 0.35, 0.35);
        particleApi.spawnColoredParticles(pullTo.clone().add(0,1,0), C_VOID_BLACK, 0.6f, 10, 0.45, 0.45, 0.45);
        p.getWorld().playSound(pullTo, Sound.ENTITY_ENDERMAN_HURT, 0.7f, 0.55f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 30 || target.isDead()) { cancel(); return; }
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 + t * 12);
                    Location ring = target.getLocation().clone().add(0, 1, 0)
                            .add(Math.cos(a)*0.75, Math.sin(a*0.4)*0.25, Math.sin(a)*0.75);
                    particleApi.spawnColoredParticles(ring, SOUL_COLORS[i % SOUL_COLORS.length], 0.85f, 1, 0.03, 0.03, 0.03);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void shadowCounter(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (CooldownApi.isOnCooldown(dm_counter, p)) return;
        Location loc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(loc, C_VOID_BLACK,  0.65f, 22, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(loc, C_CRIMSON,     1.2f,  18, 0.45, 0.45, 0.45);
        particleApi.spawnColoredParticles(loc, C_PURPLE_MID,  1.1f,  14, 0.55, 0.55, 0.55);
        particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 10, 0.45, 0.45, 0.45, 0.04);
        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.7f);

        for (Entity e : p.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            ((LivingEntity) e).damage(5, p);
            ((LivingEntity) e).addPotionEffect(
                    new PotionEffect(PotionEffectType.BLINDNESS, 10, 0, false, true));
        }
        addCd(dm_counter, p);
    }
    private void preventEnvFire(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA) {
            event.setCancelled(true);
        }

    }
    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                p.setFireTicks(0);
                if (isAuraEnabled(p)) {
                    Location base   = p.getLocation().clone();
                    Location center = base.clone().add(0, 1.1, 0);
                    for (int i = 0; i < 7; i++) {
                        double a = Math.toRadians(i * 22.5 + t * 6);
                        Location lp = center.clone().add(Math.cos(a)*1.15, Math.sin(a*0.4)*0.22, Math.sin(a)*1.15);
                        particleApi.spawnColoredParticles(lp, AURA_COLORS[i % AURA_COLORS.length], 1.05f, 1, 0.03, 0.03, 0.03);
                        if (i % 2 == 0)
                            particleApi.spawnParticles(center.clone().add(Math.cos(a)*1.15, r.nextDouble()*0.25, Math.sin(a)*1.15),
                                    Particle.SOUL, 1, 0.04, 0.04, 0.04, 0.03);
                    }
                    for (int i = 0; i < 10; i++) {
                        double a = Math.toRadians(i * 36 - t * 9);
                        Location lp = center.clone().add(Math.cos(a)*0.7, 0.5 + Math.sin(a*0.6)*0.18, Math.sin(a)*0.7);
                        particleApi.spawnColoredParticles(lp, i%2==0 ? C_PURPLE_DARK : C_DEEP_RED, 0.85f, 1, 0.03, 0.03, 0.03);
                    }
                    if (t % 3 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double a = Math.toRadians(i * 45 + t * 4);
                            Location foot = base.clone().add(Math.cos(a)*0.95, 0.05, Math.sin(a)*0.95);
                            particleApi.spawnParticles(foot, Particle.SOUL_FIRE_FLAME, 1, 0.04, 0.01, 0.04, 0.01);
                            particleApi.spawnColoredParticles(foot, C_BLOOD_RED, 0.75f, 1, 0.05, 0.01, 0.05);
                        }
                    }
                    if (t % 4 == 0) {
                        Location rp = center.clone().add(
                                (r.nextDouble()-0.5)*1.4, r.nextDouble()*1.7-0.25, (r.nextDouble()-0.5)*1.4);
                        particleApi.spawnParticles(rp, Particle.SOUL_FIRE_FLAME, 1, 0.05, 0.05, 0.05, 0.02);
                    }
                    if (t % 80 == 0)
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.18f, 0.65f);
                }
                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 20);
        return task;
    }
    @Override
    public void remove() {
        if (drainRunnable != null) { drainRunnable.cancel(); drainRunnable = null; }
        draining = false;
        charging = false;
    }
    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&5Soul Bolt";
            case 1: return "&4Hellfire Rain";
            case 2: return "&8Shadow Cleave";
            case 3: return "&dSoul Drain";
            case 4: return "&cDemon Charge";
            case 5: return "&5Void Grasp";
            default: return "&7none";
        }
    }
}

