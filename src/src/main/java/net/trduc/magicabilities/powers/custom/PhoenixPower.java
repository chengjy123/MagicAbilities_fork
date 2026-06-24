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
import org.bukkit.event.entity.PlayerDeathEvent;
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

public class PhoenixPower extends Power implements IdlePower, Removeable {

    private static final String ph_wings   = "phoenix.wings";
    private static final String ph_flame   = "phoenix.flame";
    private static final String ph_storm   = "phoenix.storm";
    private static final String ph_dive    = "phoenix.dive";
    private static final String ph_beam    = "phoenix.beam";
    private static final String ph_ascend  = "phoenix.ascend";
    private static final String ph_tornado = "phoenix.tornado";
    private static final String ph_rebirth = "phoenix.rebirth";

    private static final Color C_DEEP_RED    = Color.fromRGB(220, 30,  0);
    private static final Color C_FLAME_RED   = Color.fromRGB(255, 60,  0);
    private static final Color C_FLAME_ORG   = Color.fromRGB(255, 120, 0);
    private static final Color C_GOLDEN_ORG  = Color.fromRGB(255, 180, 0);
    private static final Color C_GOLD        = Color.fromRGB(255, 210, 20);
    private static final Color C_BRIGHT_GOLD = Color.fromRGB(255, 240, 80);
    private static final Color C_HOT_WHITE   = Color.fromRGB(255, 255, 180);

    private static final Color[] WING_COLORS  = { C_FLAME_RED, C_FLAME_ORG, C_GOLDEN_ORG, C_GOLD };
    private static final Color[] FLAME_COLORS = { C_DEEP_RED, C_FLAME_RED, C_FLAME_ORG, C_GOLDEN_ORG, C_GOLD };
    private static final Color[] AURA_COLORS  = { C_GOLD, C_BRIGHT_GOLD, C_GOLDEN_ORG, C_HOT_WHITE };
    private static final Color[] BURST_COLORS = { C_FLAME_RED, C_FLAME_ORG, C_GOLDEN_ORG, C_GOLD, C_HOT_WHITE };

    private boolean flying        = false;
    private BukkitRunnable flightRunnable = null;
    private static final int FLIGHT_DURATION = 4200;

    private boolean rebirthReady  = true;

    private boolean isDiving      = false;
    private BukkitRunnable wingVisualRunnable = null;

    public PhoenixPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DeathExecute)    { rebirth((DeathExecute) ex);          return; }
        if (ex instanceof DamagedExecute)  { preventFireDamage((DamagedExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeftClick((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRightClick((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex);         return; }
        if (ex instanceof MoveExecute)       { onMove((MoveExecute) ex); }
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(ph_wings, p, this)) return; phoenixWings(p); addCd(ph_wings, p); return;
            case 1: if (onCd(ph_flame, p, this)) return; sacredFlame(p);  addCd(ph_flame, p); return;
            case 2: if (onCd(ph_storm, p, this)) return; featherStorm(p); addCd(ph_storm, p); return;
            case 3: if (onCd(ph_dive, p, this)) return; infernoDive(p);  addCd(ph_dive, p);  return;
            case 4: if (onCd(ph_beam, p, this)) return; solarBeam(p);    addCd(ph_beam, p);    return;
            case 6: if (onCd(ph_tornado, p, this)) return; fireTornado(p);  addCd(ph_tornado, p);
        }
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        if (!flying) return;
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 0) return;
        stopFlight(p, true);
        p.sendMessage(ChatColor.YELLOW + "✦ Wings folded.");
    }

    private void onRightClick(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot == 5) {
            if (onCd(ph_ascend, p, this)) return;
            ascension(p);
            addCd(ph_ascend, p);
        }
    }

    private void phoenixWings(Player p) {
        if (flying) { stopFlight(p, true); return; }

        flying = true;
        int totalSec = FLIGHT_DURATION / 20;
        p.sendMessage(ChatColor.GOLD + "✦ Phoenix Wings — " + ChatColor.YELLOW + totalSec + "s");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.45f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE,  1f,  0.6f);

        wingSpawnBurst(p);

        if (p.isOnGround()) {
            p.setVelocity(p.getVelocity().clone().setY(1.2));
        }

        new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline() || !flying) return;
                p.setGliding(true);
                Vector initDir = p.getEyeLocation().getDirection().clone().normalize();
                p.setVelocity(initDir.multiply(2.0));
                startFlightLoop(p);
            }
        }.runTaskLater(magicPlugin, 1L);
    }

    private void startFlightLoop(Player p) {
        int[] ticks = {0};
        flightRunnable = new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline() || ticks[0] >= FLIGHT_DURATION) {
                    stopFlight(p, false); cancel(); return;
                }
                if (!p.isGliding()) p.setGliding(true);

                Vector look    = p.getEyeLocation().getDirection().clone().normalize();
                Vector current = p.getVelocity();
                double pitch  = p.getEyeLocation().getPitch();
                double thrust = 0.85 - (pitch / 180.0);
                thrust = Math.max(0.4, Math.min(1.15, thrust));

                Vector newVel = current.clone().add(look.multiply(thrust));

                double speed = newVel.length();
                if (speed > 3.8) newVel.multiply(3.8 / speed);

                newVel.setX(newVel.getX() * 0.98);
                newVel.setZ(newVel.getZ() * 0.98);

                p.setVelocity(newVel);

                spawnWingTrail(p);
                ticks[0]++;
                int rem = (FLIGHT_DURATION - ticks[0]) / 20;
                if (ticks[0] % 400 == 0 && rem > 0)
                    p.sendMessage(ChatColor.YELLOW + "✦ Wings: " + (rem/60) + "m " + (rem%60) + "s remaining");
                if (rem <= 30 && ticks[0] % 20 == 0 && rem > 0)
                    p.sendMessage(ChatColor.RED + "Wings fading in " + rem + "s!");
            }
        };
        flightRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void stopFlight(Player p, boolean manual) {
        flying = false;
        if (p.isGliding()) p.setGliding(false);
        if (flightRunnable != null) { flightRunnable.cancel(); flightRunnable = null; }
        p.setFallDistance(0);
        Location loc = p.getLocation().clone().add(0, 1, 0);
        for (int i = 0; i < 28; i++) {
            double a = Math.toRadians(i * (360.0/28));
            particleApi.spawnColoredParticles(
                loc.clone().add(Math.cos(a)*1.8, 0, Math.sin(a)*1.8),
                WING_COLORS[i % WING_COLORS.length], 1.4f, 4, 0.1, 0.15, 0.1);
        }
        particleApi.spawnParticles(loc, Particle.FLAME, 40, 1.0, 1.0, 1.0, 0.1);
        p.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.6f);
        if (!manual) p.sendMessage(ChatColor.YELLOW + "Wings faded.");
    }
    private void wingSpawnBurst(Player p) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 15) { cancel(); return; }
                drawWings(p, 1.0 + t * 0.15, true);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void spawnWingTrail(Player p) {
        Random r = new Random();
        Location base = p.getLocation().clone().add(0, 1.2, 0);
        Vector right  = yawRotate(p.getLocation().getDirection().clone().setY(0).normalize(), 90);
        for (int side : new int[]{-1, 1}) {
            for (int j = 0; j < 5; j++) {
                double spread = 0.4 + j * 0.42;
                double droop  = j * j * 0.06;
                Location lp = base.clone().add(
                        right.clone().multiply(side * spread).add(new Vector(0, -droop, 0)));

                Color c = WING_COLORS[r.nextInt(WING_COLORS.length)];
                float size = 1.4f - j * 0.1f;
                particleApi.spawnColoredParticles(lp, c, size, 2, 0.04, 0.05, 0.04);
                if (j == 0)
                    particleApi.spawnColoredParticles(lp, C_HOT_WHITE, 1.0f, 1, 0.02, 0.02, 0.02);
                if (j >= 3 && r.nextInt(3) == 0)
                    particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        Location wake = p.getLocation().clone().add(
                p.getVelocity().clone().normalize().multiply(-0.6)).add(0, 1, 0);
        particleApi.spawnColoredParticles(wake, C_FLAME_ORG, 1.2f, 5, 0.15, 0.15, 0.15);
        particleApi.spawnParticles(wake, Particle.FLAME, 2, 0.1, 0.1, 0.1, 0.04);
    }

    private void drawWings(Player p, double span, boolean burst) {
        Random r = new Random();
        Location base = p.getLocation().clone().add(0, 1.3, 0);
        Vector right  = yawRotate(p.getLocation().getDirection().clone().setY(0).normalize(), 90);

        for (int side : new int[]{-1, 1}) {
            for (int j = 0; j < 7; j++) {
                double spread = j * (span / 6.0);
                double droop  = j * j * 0.04;
                Location lp = base.clone().add(right.clone().multiply(side * spread).add(new Vector(0, -droop, 0)));
                Color c = WING_COLORS[j % WING_COLORS.length];
                particleApi.spawnColoredParticles(lp, c, burst ? 1.5f : 1.2f, burst ? 4 : 2, 0.05, 0.05, 0.05);
                if (burst && r.nextInt(3) == 0)
                    particleApi.spawnParticles(lp, Particle.FLAME, 2, 0.06, 0.06, 0.06, 0.03);
            }
        }
    }
    private long lastMoveCheck = 0;
    private void onMove(MoveExecute ex) {
        long now = System.currentTimeMillis();
        if (now - lastMoveCheck < 200) return;
        lastMoveCheck = now;
        Player p = ex.getPlayer();
        if (flying) p.setFallDistance(0);
    }

    private void sacredFlame(Player p) {
        showWingsIfFlying(p);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.7f, 0.8f);

        ArmorStand orb = spawnAs(p.getEyeLocation().clone());
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (orb.isDead() || t > 80) { safeRemove(orb); cancel(); return; }
                orb.teleport(orb.getLocation().add(dir.clone().multiply(1.4)));
                Location loc = orb.getLocation();
                particleApi.spawnColoredParticles(loc, C_HOT_WHITE, 1.2f, 2, 0.06, 0.06, 0.06);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 + t * 25);
                    Vector rv = new Vector(Math.cos(a), Math.sin(a * 0.7), Math.sin(a)).multiply(0.5);
                    Color c = FLAME_COLORS[i % FLAME_COLORS.length];
                    particleApi.spawnColoredParticles(loc.clone().add(rv), c, 1.4f, 3, 0.04, 0.04, 0.04);
                }
                particleApi.spawnParticles(loc, Particle.FLAME, 4, 0.14, 0.14, 0.14, 0.04);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.3, 1.3, 1.3)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) { sacredExplosion(loc, p); safeRemove(orb); cancel(); return; }
                }
                if (!loc.getBlock().isPassable()) { sacredExplosion(loc, p); safeRemove(orb); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void sacredExplosion(Location loc, Player p) {
        Random r = new Random();
        particleApi.spawnColoredParticles(loc, C_HOT_WHITE,   1.8f, 25,  0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(loc, C_GOLD,        1.6f, 20,  1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc, C_GOLDEN_ORG,  1.5f, 20,  1.3, 1.3, 1.3);
        particleApi.spawnColoredParticles(loc, C_FLAME_ORG,   1.4f, 30,  1.6, 1.6, 1.6);
        particleApi.spawnColoredParticles(loc, C_FLAME_RED,   1.3f, 20,  1.8, 1.8, 1.8);
        particleApi.spawnParticles(loc, Particle.FLAME, 40, 1.4, 1.4, 1.4, 0.4);
        new BukkitRunnable() {
            double rad = 0.4; int t = 0;
            @Override public void run() {
                if (rad > 5.5) { cancel(); return; }
                for (int i = 0; i < 22; i++) {
                    double a = Math.toRadians(i * (360.0 / 22) + t * 8);
                    Location lp = loc.clone().add(Math.cos(a) * rad, 0.08, Math.sin(a) * rad);
                    Color c = t % 3 == 0 ? C_HOT_WHITE : (t % 3 == 1 ? C_GOLD : C_FLAME_ORG);
                    particleApi.spawnColoredParticles(lp, c, 1.3f, 2, 0.04, 0.04, 0.04);
                }
                rad += 0.5; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.1f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.7f, 1.4f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            ((LivingEntity) e).damage(Math.max(10, 28 - dist * 3.5), p);
            e.setFireTicks(120);
            e.setVelocity(e.getLocation().subtract(loc).toVector().normalize().multiply(1.5).setY(0.5));
        }
    }

    private void featherStorm(Player p) {
        showWingsIfFlying(p);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.8f);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), C_HOT_WHITE, 2f, 40, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), C_GOLD,      1.6f, 60, 1.0, 1.0, 1.0);
        particleApi.spawnParticles(p.getLocation().clone().add(0,1,0), Particle.FLAME, 40, 0.8, 0.8, 0.8, 0.3);

        Set<Entity> hit = new HashSet<>();
        Random r = new Random();

        for (int i = 0; i < 10; i++) {
            final double angle  = i * (360.0 / 16);
            final double pitchV = r.nextDouble() * 28 - 14;
            new BukkitRunnable() {
                @Override public void run() {
                    Vector dir = new Vector(
                            Math.cos(Math.toRadians(angle)),
                            Math.sin(Math.toRadians(pitchV)),
                            Math.sin(Math.toRadians(angle))).normalize();
                    shootFeather(p, dir, hit);
                }
            }.runTaskLater(magicPlugin, i / 4L);
        }
    }

    private void shootFeather(Player p, Vector dir, Set<Entity> hit) {
        ArmorStand fth = spawnAs(p.getLocation().clone().add(0, 1.2, 0));
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (fth.isDead() || t > 22) { safeRemove(fth); cancel(); return; }
                fth.teleport(fth.getLocation().add(dir.clone().multiply(1.5)));
                Location loc = fth.getLocation();

                Color c = FLAME_COLORS[Math.min(t / 5, FLAME_COLORS.length - 1)];
                particleApi.spawnColoredParticles(loc, c, 1.2f, 3, 0.05, 0.07, 0.05);
                if (t < 4) {
                    particleApi.spawnColoredParticles(loc, C_HOT_WHITE, 1.0f, 1, 0.03, 0.03, 0.03);
                    particleApi.spawnParticles(loc, Particle.FLAME, 2, 0.04, 0.04, 0.04, 0.02);
                }

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.9, 0.9, 0.9)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(12, p);
                    e.setFireTicks(60);
                    e.setVelocity(e.getLocation().subtract(p.getLocation()).toVector().normalize().multiply(1.6).setY(0.4));
                    particleApi.spawnColoredParticles(loc, C_GOLD, 1.5f, 20, 0.3, 0.3, 0.3);
                    particleApi.spawnParticles(loc, Particle.FLAME, 10, 0.3, 0.3, 0.3, 0.1);
                    safeRemove(fth); cancel(); return;
                }
                if (!loc.getBlock().isPassable()) { safeRemove(fth); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void infernoDive(Player p) {
        showWingsIfFlying(p);
        boolean bonus = flying;
        isDiving = true;

        if (flying) stopFlight(p, true);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT,  1f, 0.3f);
        if (bonus) p.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.BOLD + "AERIAL DIVE — ×1.8 dmg!");

        Vector diveVel = p.getEyeLocation().getDirection().clone().setY(-2.5).normalize().multiply(3.5);
        p.setVelocity(diveVel);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                Location loc = p.getLocation().clone().add(0, 1, 0);
                particleApi.spawnColoredParticles(loc, C_HOT_WHITE,  1.5f, 6, 0.12, 0.06, 0.12);
                particleApi.spawnColoredParticles(loc, C_GOLD,       1.3f, 8, 0.20, 0.06, 0.20);
                particleApi.spawnColoredParticles(loc, C_FLAME_ORG,  1.2f, 8, 0.25, 0.06, 0.25);
                particleApi.spawnColoredParticles(loc, C_FLAME_RED,  1.1f, 6, 0.30, 0.06, 0.30);
                particleApi.spawnParticles(loc, Particle.FLAME, 8, 0.28, 0.06, 0.28, 0.08);

                for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.5, 1.5, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(bonus ? 22 * 1.8 : 22, p);
                    e.setFireTicks(100);
                    e.setVelocity(new Vector(0, 1.2, 0));
                }

                if (p.isOnGround() || t > 80) {
                    isDiving = false;
                    diveImpact(p.getLocation(), p, bonus);
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void diveImpact(Location loc, Player p, boolean bonus) {
        double baseDmg = bonus ? 38 * 1.8 : 38;
        double radius  = bonus ? 9 : 7;

        particleApi.spawnColoredParticles(loc, C_HOT_WHITE,  2.5f, 20,  0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(loc, C_GOLD,       2f,   120, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc, C_GOLDEN_ORG, 1.8f, 150, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(loc, C_FLAME_ORG,  1.6f, 120, 2.0, 2.0, 2.0);
        particleApi.spawnColoredParticles(loc, C_FLAME_RED,  1.4f, 20,  2.5, 2.5, 2.5);
        particleApi.spawnParticles(loc, Particle.FLAME, 150, radius * 0.35, radius * 0.35, radius * 0.35, 0.45);
        Color[] ringColors = { C_HOT_WHITE, C_GOLD, C_FLAME_ORG };
        for (int ring = 0; ring < 3; ring++) {
            final int rIdx = ring;
            final Color rc = ringColors[ring];
            new BukkitRunnable() {
                double rad = 0.5 + rIdx * 0.4; int t = 0;
                @Override public void run() {
                    if (rad > radius + rIdx * 0.5) { cancel(); return; }
                    for (int i = 0; i < 28; i++) {
                        double a = Math.toRadians(i * (360.0 / 28) + t * 12 + rIdx * 60);
                        Location lp = loc.clone().add(Math.cos(a) * rad, 0.12 + rIdx * 0.15, Math.sin(a) * rad);
                        particleApi.spawnColoredParticles(lp, rc, 1.3f, 2, 0.04, 0.04, 0.04);
                    }
                    rad += 0.7; t++;
                }
            }.runTaskLater(magicPlugin, ring * 2L);
        }

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,         1f,  0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,  0.8f, 1.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH,             0.7f, 0.7f);
        p.setFallDistance(0);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            ((LivingEntity) e).damage(Math.max(12, baseDmg - dist * 3.0), p);
            e.setFireTicks(160);
            e.setVelocity(e.getLocation().subtract(loc).toVector().normalize().multiply(2.2).setY(0.8));
        }
    }

    private void solarBeam(Player p) {
        showWingsIfFlying(p);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        new BukkitRunnable() {
            int ct = 40;
            @Override public void run() {
                if (ct <= 0) { cancel(); fireSolarBeam(p); return; }
                double a = Math.toRadians(ct * 22);
                Location eye = p.getEyeLocation().clone();
                for (int i = 0; i < 6; i++) {
                    double ai = a + Math.toRadians(i * 60);
                    Location lp = eye.clone().add(Math.cos(ai) * 0.85, Math.sin(ai * 0.5) * 0.35, Math.sin(ai) * 0.85);
                    Color c = BURST_COLORS[i % BURST_COLORS.length];
                    particleApi.spawnColoredParticles(lp, c, 1.3f, 2, 0.03, 0.03, 0.03);
                }
                if (ct == 20) p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.7f, 1.8f);
                ct--;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void fireSolarBeam(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.8f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 2f);

        Vector dir    = p.getEyeLocation().getDirection().clone().normalize();
        Location cur  = p.getEyeLocation().clone();
        Set<Entity> hit = new HashSet<>();

        for (int step = 0; step < 80; step++) {
            cur.add(dir.clone().multiply(0.5));

            particleApi.spawnColoredParticles(cur, C_HOT_WHITE,  1.5f, 3, 0.06, 0.06, 0.06);
            particleApi.spawnColoredParticles(cur, C_GOLD,       1.3f, 2, 0.12, 0.12, 0.12);
            particleApi.spawnColoredParticles(cur, C_GOLDEN_ORG, 1.1f, 2, 0.18, 0.18, 0.18);
            particleApi.spawnColoredParticles(cur, C_FLAME_ORG,  1.0f, 1, 0.22, 0.22, 0.22);
            if (step % 5 == 0)
                particleApi.spawnParticles(cur, Particle.FLAME, 2, 0.14, 0.14, 0.14, 0.02);

            for (Entity e : cur.getWorld().getNearbyEntities(cur, 1.2, 1.2, 1.2)) {
                if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                if (!(e instanceof LivingEntity)) continue;
                hit.add(e);
                ((LivingEntity) e).damage(32, p);
                e.setFireTicks(140);
                particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_GOLD, 1.8f, 30, 0.4, 0.4, 0.4);
                particleApi.spawnParticles(e.getLocation().clone().add(0,1,0), Particle.FLAME, 15, 0.3, 0.4, 0.3, 0.1);
            }
            if (!cur.getBlock().isPassable()) break;
        }
        particleApi.spawnColoredParticles(cur, C_HOT_WHITE, 2f, 20, 0.4, 0.4, 0.4);
        particleApi.spawnColoredParticles(cur, C_GOLD,      1.6f, 25, 0.6, 0.6, 0.6);
        cur.getWorld().playSound(cur, Sound.ENTITY_BLAZE_DEATH, 0.6f, 1.5f);
    }

    private void ascension(Player p) {
        showWingsIfFlying(p);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE,  1f, 0.6f);

        p.setVelocity(new Vector(p.getVelocity().getX() * 0.3, 3.0, p.getVelocity().getZ() * 0.3));

        Set<Entity> hit = new HashSet<>();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 30 || !p.isOnline()) { cancel(); return; }
                Location loc = p.getLocation().clone().add(0, 0.5, 0);
                particleApi.spawnColoredParticles(loc, C_HOT_WHITE,  1.5f, 5,  0.12, 0.06, 0.12);
                particleApi.spawnColoredParticles(loc, C_GOLD,       1.4f, 8,  0.22, 0.06, 0.22);
                particleApi.spawnColoredParticles(loc, C_GOLDEN_ORG, 1.3f, 8,  0.30, 0.06, 0.30);
                particleApi.spawnColoredParticles(loc, C_FLAME_ORG,  1.2f, 6,  0.36, 0.06, 0.36);
                particleApi.spawnColoredParticles(loc, C_FLAME_RED,  1.1f, 4,  0.42, 0.06, 0.42);
                particleApi.spawnParticles(loc, Particle.FLAME, 10, 0.35, 0.06, 0.35, 0.06);
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30 + t * 18);
                    double rad = 0.8 + t * 0.06;
                    Color c = AURA_COLORS[i % AURA_COLORS.length];
                    particleApi.spawnColoredParticles(loc.clone().add(Math.cos(a)*rad, 0, Math.sin(a)*rad),
                            c, 1.1f, 1, 0.03, 0.03, 0.03);
                }

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(15, p);
                    e.setFireTicks(80);
                    e.setVelocity(e.getLocation().subtract(p.getLocation()).toVector().normalize().multiply(1.2).setY(0.3));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void rebirth(DeathExecute ex) {
        Player p = ex.getPlayer();
        if (!rebirthReady || CooldownApi.isOnCooldown(ph_rebirth, p)) return;

        ((PlayerDeathEvent) ex.getRawEvent()).setDeathMessage(null);
        ((PlayerDeathEvent) ex.getRawEvent()).setDroppedExp(0);
        ((PlayerDeathEvent) ex.getRawEvent()).getDrops().clear();

        rebirthReady = false;
        addCd(ph_rebirth, p);

        new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) return;
                p.spigot().respawn();
                new BukkitRunnable() {
                    @Override public void run() {
                        p.setHealth(6.0);
                        p.setFireTicks(0);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 1, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,   100, 3, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,     200, 1, false, false));

                        Location loc = p.getLocation();
                        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_HOT_WHITE,  2.5f, 80,  0.8, 0.8, 0.8);
                        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_GOLD,       2.2f, 150, 1.5, 1.5, 1.5);
                        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_GOLDEN_ORG, 2f,   150, 2.0, 2.0, 2.0);
                        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_FLAME_ORG,  1.8f, 120, 2.5, 2.5, 2.5);
                        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_FLAME_RED,  1.6f, 80,  3.0, 3.0, 3.0);
                        particleApi.spawnParticles(loc, Particle.FLAME, 200, 2.5, 2.5, 2.5, 0.5);

                        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   1f, 0.5f);
                        loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP,      1f, 0.3f);
                        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP,    1f, 0.7f);
                        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ REBIRTH! " +
                                ChatColor.YELLOW + "Rising from the ashes...");

                        for (Entity e : loc.getWorld().getNearbyEntities(loc, 6, 6, 6)) {
                            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                            ((LivingEntity) e).damage(20, p);
                            e.setFireTicks(140);
                            e.setVelocity(e.getLocation().subtract(loc).toVector().normalize().multiply(2.5).setY(1.0));
                        }
                        p.sendMessage(ChatColor.YELLOW + "Rebirth ready in " +
                                (int)(cooldowns.get(ph_rebirth) / 60) + " min.");
                    }
                }.runTaskLater(magicPlugin, 5L);
            }
        }.runTaskLater(magicPlugin, 1L);
    }

    private void preventFireDamage(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
         || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
         || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            event.setCancelled(true);
        }
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        BukkitRunnable idleR = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (p.getFireTicks() > 0) p.setFireTicks(0);
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 25, 1, false, false));
                if (!flying)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 0, false, false));
                if (isAuraEnabled(p)) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.toRadians(i * 60 + t * 8);
                        Location lp = p.getLocation().clone().add(
                                Math.cos(a) * 1.1, 0.12 + Math.sin(a * 0.5) * 0.05, Math.sin(a) * 1.1);
                        Color c = AURA_COLORS[i % AURA_COLORS.length];
                        particleApi.spawnColoredParticles(lp, c, 1.1f, 1, 0.03, 0.03, 0.03);
                    }

                    if (t % 4 == 0) {
                        Color sc = r.nextInt(3) == 0 ? C_HOT_WHITE : (r.nextBoolean() ? C_GOLD : C_FLAME_ORG);
                        particleApi.spawnColoredParticles(
                                p.getLocation().clone().add((r.nextDouble()-0.5)*0.8, 0.05+r.nextDouble()*0.25, (r.nextDouble()-0.5)*0.8),
                                sc, 0.9f, 1, 0.04, 0.04, 0.04);
                    }
                }

                if (!rebirthReady && !CooldownApi.isOnCooldown(ph_rebirth, p)) {
                    rebirthReady = true;
                    p.sendMessage(ChatColor.GOLD + "✦ Rebirth is ready!");
                }
                t++;
            }
        };
        idleR.runTaskTimer(magicPlugin, 0, 20);
        return idleR;
    }

    @Override
    public void remove() {
        if (flightRunnable != null) { flightRunnable.cancel(); flightRunnable = null; }
        flying   = false;
        isDiving = false;
        Player p = getOwner();
        if (p != null && p.isOnline()) {
            if (p.isGliding()) p.setGliding(false);
            p.setFallDistance(0);
        }
    }
    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&6Phoenix Wings";
            case 1: return "&eSacred Flame";
            case 2: return "&6Feather Storm";
            case 3: return "&cInferno Dive";
            case 4: return "&eSolar Beam";
            case 5: return "&6Ascension";
            case 6: return "&c&lFire Tornado";
            default: return "&7none";
        }
    }

    private void fireTornado(Player p) {
        showWingsIfFlying(p);
        boolean aerial = flying;
        double pullRadius  = aerial ? 6.0 : 4.0;
        int    duration    = aerial ? 100  : 80;

        Location spawnLoc = getRaycastGround(p, 20);
        Random r = new Random();

        p.getWorld().playSound(spawnLoc, Sound.ENTITY_PHANTOM_FLAP,    1f, 0.35f);
        p.getWorld().playSound(spawnLoc, Sound.ITEM_FIRECHARGE_USE,     1f, 0.5f);
        p.getWorld().playSound(spawnLoc, Sound.ENTITY_BLAZE_SHOOT,      0.8f, 0.4f);
        if (aerial) p.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.BOLD + "AERIAL TORNADO — wider range!");
        ArmorStand anchor = spawnAs(spawnLoc.clone());

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= duration || anchor.isDead()) {
                    tornadoEnd(spawnLoc, p);
                    safeRemove(anchor);
                    cancel();
                    return;
                }

                Location center = spawnLoc.clone();
                for (int i = 0; i < 7; i++) {
                    double a    = Math.toRadians(i * 36 + t * 22);
                    double yOff = (i % 10) * 0.18;
                    double rx   = 0.20 + yOff * 0.06;
                    Location lp = center.clone().add(Math.cos(a)*rx, yOff, Math.sin(a)*rx);
                    particleApi.spawnColoredParticles(lp, C_HOT_WHITE, 1.5f, 2, 0.03, 0.03, 0.03);
                }

                for (int i = 0; i < 10; i++) {
                    double a    = Math.toRadians(i * 22.5 + t * 14);
                    double yOff = (i % 8) * 0.22;
                    double rx   = 0.45 + yOff * 0.09;
                    Location lp = center.clone().add(Math.cos(a)*rx, yOff, Math.sin(a)*rx);
                    Color c = (t + i) % 2 == 0 ? C_GOLD : C_GOLDEN_ORG;
                    particleApi.spawnColoredParticles(lp, c, 1.3f, 2, 0.04, 0.04, 0.04);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.04, 0.04, 0.03);
                }

                for (int i = 0; i < 12; i++) {
                    double a    = Math.toRadians(i * 18 - t * 8);
                    double yOff = (i % 10) * 0.20;
                    double rx   = 0.75 + yOff * 0.12;
                    Location lp = center.clone().add(Math.cos(a)*rx, yOff, Math.sin(a)*rx);
                    Color c = (t + i) % 2 == 0 ? C_FLAME_ORG : C_FLAME_RED;
                    particleApi.spawnColoredParticles(lp, c, 1.15f, 1, 0.05, 0.05, 0.05);
                }
                if (t % 5 == 0) {
                    Location top = center.clone().add(
                            (r.nextDouble()-0.5)*0.5, 2.0 + r.nextDouble()*0.4, (r.nextDouble()-0.5)*0.5);
                    particleApi.spawnColoredParticles(top, C_HOT_WHITE,  1.4f, 4, 0.12, 0.08, 0.12);
                    particleApi.spawnColoredParticles(top, C_GOLD,       1.2f, 4, 0.16, 0.08, 0.16);
                    particleApi.spawnParticles(top, Particle.FLAME, 3, 0.1, 0.06, 0.1, 0.05);
                }
                if (t % 3 == 0) {
                    double baseRad = 0.8 + Math.sin(t * 0.3) * 0.3;
                    for (int i = 0; i < 9; i++) {
                        double a = Math.toRadians(i * (360.0/14) + t * 10);
                        Location fp = center.clone().add(Math.cos(a)*baseRad, 0.05, Math.sin(a)*baseRad);
                        particleApi.spawnColoredParticles(fp, FLAME_COLORS[i % FLAME_COLORS.length], 1.0f, 1, 0.04, 0.02, 0.04);
                        particleApi.spawnParticles(fp, Particle.FLAME, 1, 0.04, 0.01, 0.04, 0.03);
                    }
                }
                if (t % 5 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, pullRadius, pullRadius, pullRadius)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        Vector toCenterXZ = center.clone().add(0, 1, 0).toVector()
                                .subtract(e.getLocation().clone().add(0, 1, 0).toVector());
                        double dist = toCenterXZ.length();
                        if (dist < 0.3) continue;

                        double pullStrength = Math.min(0.6, 1.8 / (dist + 0.5));
                        toCenterXZ.normalize().multiply(pullStrength);
                        e.setVelocity(e.getVelocity().add(toCenterXZ));
                        ((LivingEntity) e).damage(4, p);
                        e.setFireTicks(40);
                    }
                }

                if (t % 5 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 1.8, 2.5, 1.8)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        double ex2 = e.getLocation().getX() - center.getX();
                        double ez2 = e.getLocation().getZ() - center.getZ();
                        double len = Math.sqrt(ex2*ex2 + ez2*ez2);
                        if (len < 0.15) continue;
                        double tx = -ez2 / len;
                        double tz =  ex2 / len;
                        Vector swirl = new Vector(tx * 0.55 - ex2/len * 0.18, -0.05, tz * 0.55 - ez2/len * 0.18);
                        e.setVelocity(swirl);
                    }
                }
                if (t % 15 == 0)
                    center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.4f + (float)(t * 0.005));

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void tornadoEnd(Location loc, Player p) {
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_HOT_WHITE,  2.2f, 50, 0.7, 0.7, 0.7);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_GOLD,       1.8f, 80, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_GOLDEN_ORG, 1.6f, 80, 1.3, 1.3, 1.3);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_FLAME_ORG,  1.4f, 50, 1.6, 1.6, 1.6);
        particleApi.spawnParticles(loc, Particle.FLAME, 30, 1.2, 0.8, 1.2, 0.25);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH,      0.9f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,  0.6f, 1.3f);
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            if (e.equals(p)) continue;
            double dist = e.getLocation().distance(loc);
            ((LivingEntity) e).damage(Math.max(4, 12 - dist * 2), p);
        }
    }

    private Location getRaycastGround(Player p, int maxDist) {
        Location cur = p.getEyeLocation().clone();
        Vector dir   = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < maxDist * 2; i++) {
            cur.add(dir.clone().multiply(0.5));
            if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                cur.subtract(dir.clone().multiply(0.5));
                return cur;
            }
        }
        return cur;
    }
    private void showWingsIfFlying(Player p) {
        if (!flying) return;
        spawnWingTrail(p);
        Random rr = new Random();
        for (int side : new int[]{-1, 1}) {
            Vector right = yawRotate(p.getLocation().getDirection().clone().setY(0).normalize(), 90);
            Location base = p.getLocation().clone().add(0, 1.3, 0);
            for (int j = 0; j < 3; j++) {
                double spread = 0.5 + j * 0.5;
                Location lp = base.clone().add(right.clone().multiply(side * spread).add(new Vector(0, -j*0.08, 0)));
                particleApi.spawnColoredParticles(lp, WING_COLORS[rr.nextInt(WING_COLORS.length)], 1.3f, 3, 0.06, 0.06, 0.06);
                particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.04, 0.04, 0.02);
            }
        }
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
    }

    private Vector yawRotate(Vector v, double yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new Vector(v.getX()*Math.cos(r) + v.getZ()*Math.sin(r), v.getY(),
                         -v.getX()*Math.sin(r) + v.getZ()*Math.cos(r));
    }
}
