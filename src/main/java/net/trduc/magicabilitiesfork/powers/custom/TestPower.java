package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;

public class TestPower extends Power implements IdlePower, Removeable {

    private static final String cosmic_annihilation = "test.annihilation";


    private static final Color C_DEEP_VIOLET   = Color.fromRGB(80,  20,  140);
    private static final Color C_VIOLET        = Color.fromRGB(140, 40,  220);
    private static final Color C_COSMIC_PINK   = Color.fromRGB(255, 90,  180);
    private static final Color C_EMBER_ORANGE  = Color.fromRGB(255, 150, 40);
    private static final Color C_EMBER_GOLD    = Color.fromRGB(255, 210, 90);
    private static final Color C_STARLIGHT     = Color.fromRGB(255, 245, 220);
    private static final Color[] RING_COLORS   = { C_VIOLET, C_COSMIC_PINK, C_EMBER_ORANGE };
    private static final Color[] EMBER_COLORS  = { C_EMBER_GOLD, C_COSMIC_PINK, C_STARLIGHT };

    private boolean channeling = false;
    private BukkitRunnable activeTask = null;

    public TestPower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (!isEnabled()) return;

        if (ex instanceof LeftClickExecute) {
            Player p = ex.getPlayer();
            if (channeling) return;
            if (onCd(cosmic_annihilation, p, this)) return;
            addCd(cosmic_annihilation, p);
            beginRitual(p);
        }
    }




    private void beginRitual(Player p) {
        channeling = true;

        final Location anchor = p.getLocation().clone();
        final int lockTicks = 30;
        final int channelTicks = 90;

        p.setWalkSpeed(0.0f);
        p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ COSMIC ANNIHILATION " +
                ChatColor.RESET + ChatColor.LIGHT_PURPLE + "— the sky begins to tear open...");
        broadcastWarning(p, anchor, 40);

        p.getWorld().playSound(anchor, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.4f);
        p.getWorld().playSound(anchor, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 0.5f);
        p.getWorld().playSound(anchor, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.4f);

        final Random r = new Random();
        final double maxHeight = 50.0;

        activeTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || t >= channelTicks) {
                    cancel();
                    activeTask = null;
                    p.setWalkSpeed(0.2f);
                    channeling = false;
                    releaseAnnihilation(p, anchor.clone());
                    return;
                }


                if (t < lockTicks) {
                    Location cur = p.getLocation();
                    if (cur.getX() != anchor.getX() || cur.getZ() != anchor.getZ()) {
                        p.teleport(new Location(anchor.getWorld(), anchor.getX(), cur.getY(), anchor.getZ(),
                                cur.getYaw(), cur.getPitch()));
                    }
                }

                double progress = (double) t / channelTicks;
                double curHeight = maxHeight * Math.pow(progress, 0.8);
                int stacks = (int) (6 + progress * 10);


                for (double y = 0; y < curHeight; y += 1.0) {
                    if (r.nextDouble() > 0.55) continue;
                    double wob = Math.sin(t * 0.2 + y * 0.5) * (0.3 + progress * 0.4);
                    Location lp = anchor.clone().add(wob, y, Math.cos(t * 0.2 + y * 0.5) * (0.3 + progress * 0.4));
                    Color c = y < curHeight * 0.3 ? C_EMBER_GOLD : (y < curHeight * 0.7 ? C_COSMIC_PINK : C_VIOLET);
                    particleApi.spawnColoredParticles(lp, c, 1.1f, 1, 0.08, 0.08, 0.08);
                    if (r.nextDouble() < 0.2) particleApi.spawnParticles(lp, Particle.END_ROD, 1, 0.05, 0.05, 0.05, 0.01);
                }


                for (int s = 0; s < stacks; s++) {
                    double ringY = (curHeight / stacks) * s;
                    double ringRadius = 1.2 + 2.6 * Math.sin(Math.PI * (s / (double) Math.max(1, stacks)));
                    double spin = t * (6 - s * 0.15) + s * 50;
                    Color ringColor = RING_COLORS[s % RING_COLORS.length];
                    int ringPoints = 14;
                    for (int i = 0; i < ringPoints; i++) {
                        double a = Math.toRadians(i * (360.0 / ringPoints) + spin);
                        Location lp = anchor.clone().add(Math.cos(a) * ringRadius, ringY, Math.sin(a) * ringRadius);
                        particleApi.spawnColoredParticles(lp, ringColor, 1.0f, 1, 0.02, 0.02, 0.02);
                    }
                }


                double runeY = curHeight * 0.4 + Math.sin(t * 0.05) * curHeight * 0.15;
                double runeSpin = t * 4;
                drawDiagonalLine(anchor, runeY, 6 + progress * 3, runeSpin, C_COSMIC_PINK);
                drawRotatingTriangle(anchor, runeY * 0.7, 4 + progress * 2, -runeSpin * 1.4, C_VIOLET);
                if (progress > 0.4) {
                    drawRotatingDiamond(anchor, curHeight * 0.85, 5 + progress * 2, runeSpin * 0.8, C_EMBER_GOLD);
                }


                for (int i = 0; i < 4; i++) {
                    double emberX = (r.nextDouble() - 0.5) * 14;
                    double emberY = r.nextDouble() * (curHeight + 4);
                    double emberZ = (r.nextDouble() - 0.5) * 14;
                    Location lp = anchor.clone().add(emberX, emberY, emberZ);
                    Color c = EMBER_COLORS[r.nextInt(EMBER_COLORS.length)];
                    particleApi.spawnColoredParticles(lp, c, 0.8f, 1, 0.02, 0.02, 0.02);
                    if (r.nextDouble() < 0.3) particleApi.spawnParticles(lp, Particle.ENCHANT, 1, 0.1, 0.1, 0.1, 0);
                }


                if (t % 10 == 0) {
                    float pitch = 0.5f + (float) progress * 0.9f;
                    p.getWorld().playSound(anchor, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, pitch);
                }
                if (t % 20 == 0) {
                    p.getWorld().playSound(anchor, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 0.6f + (float) progress);
                }
                if (t == channelTicks - 20) {
                    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "THE SKY IS COLLAPSING...");
                    p.getWorld().playSound(anchor, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.7f);
                }

                t++;
            }
        };
        activeTask.runTaskTimer(magicPlugin, 0, 1);
    }




    private void releaseAnnihilation(Player p, Location center) {
        final double maxRadius = 40.0;
        final Random r = new Random();

        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ COSMIC ANNIHILATION!");
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 1f, 0.6f);


        new BukkitRunnable() {
            @Override public void run() {
                particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_STARLIGHT, 3f, 150, 2, 2, 2);
                particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_COSMIC_PINK, 2.5f, 120, 3, 3, 3);
                particleApi.spawnParticles(center.clone().add(0, 1, 0), Particle.EXPLOSION_EMITTER, 2, 0, 0, 0, 0);
                for (double y = 0; y <= 50; y += 1.0) {
                    Location lp = center.clone().add((r.nextDouble() - 0.5) * 0.8, y, (r.nextDouble() - 0.5) * 0.8);
                    particleApi.spawnColoredParticles(lp, C_STARLIGHT, 2f, 3, 0.2, 0.1, 0.2);
                }
            }
        }.runTask(magicPlugin);


        int waveCount = 5;
        for (int w = 0; w < waveCount; w++) {
            final int waveIdx = w;
            new BukkitRunnable() {
                @Override public void run() {
                    runExpandingWave(center, maxRadius, waveIdx, r);
                }
            }.runTaskLater(magicPlugin, w * 6L);
        }


        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : center.getWorld().getNearbyEntities(center, maxRadius, maxRadius, maxRadius)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(center);
                    if (dist > maxRadius) continue;
                    double falloff = 1.0 - (dist / maxRadius);
                    double dmg = 6 + falloff * 34;
                    ((LivingEntity) e).damage(dmg, p);
                    Vector kb = e.getLocation().clone().subtract(center).toVector();
                    if (!isVecFinite(kb) || kb.lengthSquared() < 0.01) kb = new Vector(0.1, 0, 0.1);
                    kb = kb.normalize().multiply(1.5 + falloff * 2.5).setY(0.6 + falloff * 0.8);
                    e.setVelocity(kb);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
                }
            }
        }.runTaskLater(magicPlugin, 8L);


        new BukkitRunnable() {
            @Override public void run() {
                spawnAfterglow(center, maxRadius, r);
            }
        }.runTaskLater(magicPlugin, 20L);
    }

    private void runExpandingWave(Location center, double maxRadius, int waveIdx, Random r) {
        Color waveColor = RING_COLORS[waveIdx % RING_COLORS.length];
        new BukkitRunnable() {
            double radius = 0.5;
            int t = 0;

            @Override
            public void run() {
                if (radius > maxRadius) { cancel(); return; }

                int points = (int) (24 + radius * 2);
                for (int i = 0; i < points; i++) {
                    double a = Math.toRadians(i * (360.0 / points) + t * 6 + waveIdx * 30);
                    Location ground = getGroundBelow(center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
                    particleApi.spawnColoredParticles(ground, waveColor, 1.3f, 1, 0.05, 0.05, 0.05);
                    if (i % 3 == 0) particleApi.spawnParticles(ground, Particle.FLAME, 1, 0.05, 0.05, 0.05, 0.01);

                    if (i % 6 == waveIdx % 6) {
                        for (double y = 0; y < 3; y += 0.5) {
                            particleApi.spawnColoredParticles(ground.clone().add(0, y, 0),
                                    waveColor, 1.0f, 1, 0.02, 0.02, 0.02);
                        }
                    }
                }
                if (t % 4 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.4f - waveIdx * 0.1f);
                }

                radius += 1.6;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void spawnAfterglow(Location center, double maxRadius, Random r) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 100) { cancel(); return; }
                for (int i = 0; i < 10; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double dist = r.nextDouble() * maxRadius;
                    Location lp = getGroundBelow(center.clone().add(Math.cos(a) * dist, 0, Math.sin(a) * dist));
                    if (r.nextDouble() < (1.0 - t / 100.0)) {
                        particleApi.spawnColoredParticles(lp.clone().add(0, 0.1, 0),
                                r.nextBoolean() ? C_EMBER_ORANGE : C_VIOLET, 0.8f, 1, 0.1, 0.05, 0.1);
                    }
                }
                t += 4;
            }
        }.runTaskTimer(magicPlugin, 0, 4);
    }




    private void drawDiagonalLine(Location anchor, double y, double length, double angleDeg, Color color) {
        double rad = Math.toRadians(angleDeg);
        Location center = anchor.clone().add(0, y, 0);
        Location a = center.clone().add(Math.cos(rad) * length, 0, Math.sin(rad) * length);
        Location b = center.clone().add(-Math.cos(rad) * length, 0, -Math.sin(rad) * length);
        particleLine(a, b, 0.6, color, 1.1f);
    }

    private void drawRotatingTriangle(Location anchor, double y, double size, double angleDeg, Color color) {
        Location center = anchor.clone().add(0, y, 0);
        Location[] pts = new Location[3];
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(angleDeg + i * 120);
            pts[i] = center.clone().add(Math.cos(a) * size, 0, Math.sin(a) * size);
        }
        particleLine(pts[0], pts[1], 0.6, color, 1.0f);
        particleLine(pts[1], pts[2], 0.6, color, 1.0f);
        particleLine(pts[2], pts[0], 0.6, color, 1.0f);
    }

    private void drawRotatingDiamond(Location anchor, double y, double size, double angleDeg, Color color) {
        Location center = anchor.clone().add(0, y, 0);
        Location[] pts = new Location[4];
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(angleDeg + i * 90);
            pts[i] = center.clone().add(Math.cos(a) * size, 0, Math.sin(a) * size);
        }
        for (int i = 0; i < 4; i++) {
            particleLine(pts[i], pts[(i + 1) % 4], 0.6, color, 1.0f);
        }
    }

    private void broadcastWarning(Player caster, Location center, double radius) {
        for (Player other : center.getWorld().getPlayers()) {
            if (other.equals(caster)) continue;
            if (other.getLocation().distance(center) > radius * 1.5) continue;
            other.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "⚠ " + ChatColor.LIGHT_PURPLE +
                    caster.getName() + " is channeling something catastrophic nearby...");
        }
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (!isAuraEnabled(p) || channeling) return;
                Location lp = p.getLocation().clone().add(0, 1.1, 0);
                particleApi.spawnColoredParticles(lp, C_VIOLET, 0.7f, 1, 0.3, 0.4, 0.3);
                if (Math.random() < 0.4)
                    particleApi.spawnColoredParticles(lp, C_COSMIC_PINK, 0.6f, 1, 0.25, 0.3, 0.25);
            }
        };
        task.runTaskTimer(magicPlugin, 0, 10);
        return task;
    }

    @Override
    public void remove() {
        channeling = false;
        if (activeTask != null) {
            try { activeTask.cancel(); } catch (Exception ignored) {}
            activeTask = null;
        }
        if (getOwner() != null && getOwner().isOnline()) {
            getOwner().setWalkSpeed(0.2f);
        }
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&5&l宇宙湮灭";
            default: return "&7none";
        }
    }
}
