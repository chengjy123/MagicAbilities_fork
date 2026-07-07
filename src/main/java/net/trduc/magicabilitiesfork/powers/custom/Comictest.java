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

public class Comictest extends Power implements IdlePower, Removeable {

    private static final String cosmic_annihilation = "test.annihilation";
    private static final String comictest     = "test.orb";

    private static final Color C_DEEP_VIOLET   = Color.fromRGB(80,  20,  140);
    private static final Color C_VIOLET        = Color.fromRGB(140, 40,  220);
    private static final Color C_COSMIC_PINK   = Color.fromRGB(255, 90,  180);
    private static final Color C_EMBER_ORANGE  = Color.fromRGB(255, 150, 40);
    private static final Color C_EMBER_GOLD    = Color.fromRGB(255, 210, 90);
    private static final Color C_STARLIGHT     = Color.fromRGB(255, 245, 220);
    private static final Color[] RING_COLORS   = { C_VIOLET, C_COSMIC_PINK, C_EMBER_ORANGE };
    private static final Color[] EMBER_COLORS  = { C_EMBER_GOLD, C_COSMIC_PINK, C_STARLIGHT };

    private static final Color C_ORB_CORE   = Color.fromRGB(20,  8,   35);
    private static final Color C_ORB_HALO   = Color.fromRGB(170, 230, 255);
    private static final Color C_ORB_RING   = Color.fromRGB(140, 170, 255);
    private static final Color C_ORB_CYAN   = Color.fromRGB(90,  230, 235);
    private static final Color C_ORB_PINK   = Color.fromRGB(235, 110, 220);
    private static final Color C_ORB_STREAK = Color.fromRGB(200, 190, 255);

    private static final Color C_ELDRITCH_GREEN  = Color.fromRGB(60,  220, 140);
    private static final Color C_ROYAL_AMETHYST  = Color.fromRGB(120, 30,  180);
    private static final Color C_BLOOD_GARNET    = Color.fromRGB(200, 20,  60);
    private static final Color C_MYSTIC_SILVER   = Color.fromRGB(210, 220, 240);
    private static final Color C_ABYSS_NAVY      = Color.fromRGB(15,  20,  60);
    private static final Color C_ARCANE_TEAL     = Color.fromRGB(40,  200, 190);
    private static final Color[] MYSTIC_COLORS   = { C_ELDRITCH_GREEN, C_ROYAL_AMETHYST, C_BLOOD_GARNET, C_MYSTIC_SILVER, C_ARCANE_TEAL };

    private boolean channeling = false;
    private BukkitRunnable activeTask = null;
    private BukkitRunnable domainTask = null;
    private final List<BukkitRunnable> phase3Tasks = new ArrayList<>();

    public Comictest(Player owner) {
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

        if (ex instanceof RightClickExecute) {
            Player p = ex.getPlayer();
            if (channeling) return;
            if (onCd(comictest, p, this)) return;
            addCd(comictest, p);
            launchComictest(p);
        }
    }

    private void launchComictest(Player p) {
        final Location chargeOrigin = p.getEyeLocation().clone();
        p.getWorld().playSound(chargeOrigin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
        p.getWorld().playSound(chargeOrigin, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.5f, 1.6f);
        sendActionBar(p, "§5§l✦ CHARGING... ✦");

        final int chargeTicks = 6;
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline() || t >= chargeTicks) {
                    cancel();
                    if (p.isOnline()) {
                        p.sendTitle("", "§d§l✦ LAUNCH ✦", 0, 3, 2);
                        fireComictestOrb(p);
                    }
                    return;
                }
                Location hand = p.getEyeLocation().add(p.getEyeLocation().getDirection().clone().multiply(0.8));
                double pull = (double) (chargeTicks - t) / chargeTicks;
                particleApi.spawnColoredParticles(hand, C_ORB_HALO, 1.2f, 2, 0.25 * pull, 0.25 * pull, 0.25 * pull);
                particleApi.spawnColoredParticles(hand, C_MYSTIC_SILVER, 0.9f, 1, 0.35 * pull, 0.35 * pull, 0.35 * pull);
                sendActionBar(p, t % 2 == 0 ? "§5§l⬡ CHARGING ⬡" : "§d§l◈ CHARGING ◈");
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void fireComictestOrb(Player p) {
        final Location origin = p.getEyeLocation().clone();
        final Vector dir = origin.getDirection().clone().normalize();

        p.getWorld().playSound(origin, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.6f);
        p.getWorld().playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 2.0f);

        final double ORB_RADIUS   = 3.0;
        final double MAX_RANGE    = 25.0;
        final double BASE_SPEED   = 0.35;
        final double WOBBLE_AMP   = 0.55;
        final double WOBBLE_FREQ  = 0.22;
        final double HOMING_RANGE = 8.0;
        final double HOMING_CONE  = 45.0;
        final double HOMING_TURN  = 0.05;
        final Set<Entity> ignore = new HashSet<>();
        ignore.add(p);

        new BukkitRunnable() {
            final Location loc = origin.clone();
            final Vector curDir = dir.clone();
            final List<Location> trail = new ArrayList<>();
            double traveled = 0;
            int tick = 0;

            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }

                LivingEntity target = findHomingTarget(loc, curDir, HOMING_RANGE, HOMING_CONE, ignore);
                if (target != null) {
                    Vector toTarget = target.getEyeLocation().toVector().subtract(loc.toVector()).normalize();
                    Vector blended = curDir.clone().multiply(1.0 - HOMING_TURN).add(toTarget.multiply(HOMING_TURN));
                    if (isVecFinite(blended) && blended.lengthSquared() > 0.001) {
                        blended.normalize();
                        curDir.setX(blended.getX()).setY(blended.getY()).setZ(blended.getZ());
                    }
                }

                double rangeProgress = traveled / MAX_RANGE;
                double speedMult;
                if (tick < 8) {
                    speedMult = 0.5 + (tick / 8.0) * 0.5;
                } else if (rangeProgress > 0.75) {
                    speedMult = 1.0 + (rangeProgress - 0.75) * 2.4;
                } else {
                    speedMult = 1.0;
                }
                double curSpeed = BASE_SPEED * speedMult;

                double breathe = 1.0 + Math.sin(tick * 0.3) * 0.12;
                double curRadius = ORB_RADIUS * breathe;

                Vector up = Math.abs(curDir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
                Vector right = curDir.clone().crossProduct(up).normalize();
                Vector axisUp = right.clone().crossProduct(curDir).normalize();
                double wobbleAngle = tick * WOBBLE_FREQ;
                Vector wobbleOffset = right.clone().multiply(Math.cos(wobbleAngle) * WOBBLE_AMP)
                        .add(axisUp.clone().multiply(Math.sin(wobbleAngle) * WOBBLE_AMP));
                Location visualLoc = loc.clone().add(wobbleOffset);

                drawComictestAt(visualLoc, curRadius, tick, curDir);
                drawComictestTrail(trail, visualLoc, curDir, tick);
                drawOrbitingMoon(visualLoc, curRadius, tick, curDir);
                drawSecondaryMoon(visualLoc, curRadius, tick, curDir);
                drawChromaticAura(visualLoc, curRadius, tick, curDir);

                if (tick % 10 == 0 && tick > 0) {
                    for (Player pl : getNearbyPlayers(visualLoc, 20)) {
                        sendActionBar(pl, "§d§l✦ incoming... ✦");
                    }
                }

                double nearMissRadius = ORB_RADIUS * 1.8;
                for (Entity e : loc.getWorld().getNearbyEntities(loc, nearMissRadius, nearMissRadius, nearMissRadius)) {
                    if (ignore.contains(e) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    double d = e.getLocation().distance(loc);
                    if (d <= ORB_RADIUS || d > nearMissRadius) continue;
                    if (tick % 4 != 0) continue;
                    LivingEntity le = (LivingEntity) e;
                    particleApi.spawnColoredParticles(le.getEyeLocation(), C_STARLIGHT, 1.0f, 6, 0.15, 0.15, 0.15);
                    if (le instanceof Player) {
                        Player warnedPlayer = (Player) le;
                        sendActionBar(warnedPlayer, "§e§l⚠ near miss! ⚠");
                        warnedPlayer.playSound(warnedPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.8f);
                    }
                }

                for (Entity e : loc.getWorld().getNearbyEntities(loc, ORB_RADIUS, ORB_RADIUS, ORB_RADIUS)) {
                    if (ignore.contains(e) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    cancel();
                    explodeComictest(p, visualLoc.clone());
                    return;
                }

                if (traveled >= MAX_RANGE) {
                    cancel();
                    explodeComictest(p, visualLoc.clone());
                    return;
                }

                loc.add(curDir.clone().multiply(curSpeed));
                traveled += curSpeed;
                tick++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private LivingEntity findHomingTarget(Location loc, Vector dir, double range, double coneDeg, Set<Entity> ignore) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double coneCos = Math.cos(Math.toRadians(coneDeg));
        for (Entity e : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (ignore.contains(e) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            Vector toE = e.getLocation().toVector().subtract(loc.toVector());
            double dist = toE.length();
            if (dist < 0.1 || dist > range) continue;
            Vector toEn = toE.clone().normalize();
            if (toEn.dot(dir) < coneCos) continue;
            if (dist < bestDist) { bestDist = dist; best = (LivingEntity) e; }
        }
        return best;
    }

    private void drawOrbitingMoon(Location center, double radius, int tick, Vector dir) {
        Vector up = Math.abs(dir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector axisUp = right.clone().crossProduct(dir).normalize();

        double moonOrbitRadius = radius * 1.5;
        double a = Math.toRadians(tick * 14);
        Location moonCenter = center.clone()
                .add(right.clone().multiply(Math.cos(a) * moonOrbitRadius))
                .add(axisUp.clone().multiply(Math.sin(a) * moonOrbitRadius));

        int shellPts = 6;
        for (int j = 0; j < shellPts; j++) {
            double sa = Math.toRadians(j * 60 + tick * 16);
            Location sp = moonCenter.clone().add(Math.cos(sa) * 0.25, Math.sin(sa) * 0.25, 0);
            particleApi.spawnColoredParticles(sp, C_MYSTIC_SILVER, 0.8f, 1, 0.02, 0.02, 0.02);
        }
        particleApi.spawnColoredParticles(moonCenter, C_STARLIGHT, 1.0f, 1, 0.03, 0.03, 0.03);
    }

    private void drawSecondaryMoon(Location center, double radius, int tick, Vector dir) {
        Vector up = Math.abs(dir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector axisUp = right.clone().crossProduct(dir).normalize();

        double tilt = Math.toRadians(40);
        Vector planeA = right.clone();
        Vector planeB = axisUp.clone().multiply(Math.cos(tilt)).add(dir.clone().multiply(Math.sin(tilt)));

        double moonOrbitRadius = radius * 2.1;
        double a = Math.toRadians(-tick * 9 + 180);
        Location moonCenter = center.clone()
                .add(planeA.clone().multiply(Math.cos(a) * moonOrbitRadius))
                .add(planeB.clone().multiply(Math.sin(a) * moonOrbitRadius));

        int shellPts = 5;
        for (int j = 0; j < shellPts; j++) {
            double sa = Math.toRadians(j * 72 - tick * 10);
            Location sp = moonCenter.clone().add(Math.cos(sa) * 0.35, Math.sin(sa) * 0.35, 0);
            particleApi.spawnColoredParticles(sp, C_EMBER_GOLD, 0.9f, 1, 0.02, 0.02, 0.02);
        }
        particleApi.spawnColoredParticles(moonCenter, C_ORB_PINK, 1.1f, 1, 0.03, 0.03, 0.03);
    }

    private void drawChromaticAura(Location center, double radius, int tick, Vector dir) {
        Vector up = Math.abs(dir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector axisUp = right.clone().crossProduct(dir).normalize();

        double auraRadius = radius * 1.15;
        int pts = 16;
        for (int i = 0; i < pts; i++) {
            double hue = ((tick * 6 + i * (360.0 / pts)) % 360) / 360.0;
            float rgbF = 1f;
            int rgb = java.awt.Color.HSBtoRGB((float) hue, 0.85f, 1f);
            Color c = Color.fromRGB(rgb & 0xFFFFFF);
            double a = Math.toRadians(i * (360.0 / pts) + tick * 8);
            Location pt = center.clone()
                    .add(right.clone().multiply(Math.cos(a) * auraRadius))
                    .add(axisUp.clone().multiply(Math.sin(a) * auraRadius));
            particleApi.spawnColoredParticles(pt, c, 0.7f, 1, 0.015, 0.015, 0.015);
        }
    }

    private void drawComictestTrail(List<Location> trail, Location current, Vector dir, int tick) {
        trail.add(current.clone());
        if (trail.size() > 9) trail.remove(0);

        for (int i = 0; i < trail.size(); i++) {
            double fade = (double) i / trail.size();
            if (fade < 0.1) continue;
            Location tp = trail.get(i);
            float size = (float) (0.8 + fade * 0.9);
            particleApi.spawnColoredParticles(tp, C_ORB_HALO, size, 2, 0.1, 0.1, 0.1);
            particleApi.spawnColoredParticles(tp, C_VIOLET, size * 0.85f, 1, 0.12, 0.12, 0.12);
        }

        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            double spreadX = (r.nextDouble() - 0.5) * 1.6;
            double spreadY = (r.nextDouble() - 0.5) * 1.6;
            double spreadZ = (r.nextDouble() - 0.5) * 1.6;
            Location dust = current.clone().add(spreadX, spreadY, spreadZ);
            Color c = r.nextBoolean() ? C_ORB_CYAN : C_ORB_PINK;
            particleApi.spawnColoredParticles(dust, c, 0.8f, 1, 0.03, 0.03, 0.03);
            if (r.nextDouble() < 0.4) particleApi.spawnParticles(dust, Particle.END_ROD, 1, 0.02, 0.02, 0.02, 0.005);
        }

        if (tick % 3 == 0) {
            Vector up = Math.abs(dir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
            Vector right = dir.clone().crossProduct(up).normalize();
            Vector axisUp = right.clone().crossProduct(dir).normalize();
            for (int s = 0; s < 4; s++) {
                double sparkAng = r.nextDouble() * Math.PI * 2;
                double sparkLen = 1.4 + r.nextDouble() * 1.2;
                Location sparkEnd = current.clone()
                        .add(right.clone().multiply(Math.cos(sparkAng) * sparkLen))
                        .add(axisUp.clone().multiply(Math.sin(sparkAng) * sparkLen));
                particleLine(current, sparkEnd, 0.45, C_ORB_STREAK, 1.1f);
            }
        }

        if (r.nextDouble() < 0.12) {
            double gx = (r.nextDouble() - 0.5) * 2.0;
            double gy = (r.nextDouble() - 0.5) * 2.0;
            double gz = (r.nextDouble() - 0.5) * 2.0;
            Location glint = current.clone().add(gx, gy, gz);
            particleApi.spawnColoredParticles(glint, C_STARLIGHT, 1.6f, 3, 0.04, 0.04, 0.04);
            particleApi.spawnParticles(glint, Particle.END_ROD, 2, 0.02, 0.02, 0.02, 0.01);
        }
    }

    private void drawComictestAt(Location loc, double radius, int tick, Vector dir) {

        particleApi.spawnColoredParticles(loc, C_ORB_CORE, 2.6f, 8, radius * 0.2, radius * 0.2, radius * 0.2);
        particleApi.spawnColoredParticles(loc, C_VIOLET,   2.0f, 6, radius * 0.35, radius * 0.35, radius * 0.35);
        particleApi.spawnColoredParticles(loc, C_DEEP_VIOLET, 1.8f, 5, radius * 0.45, radius * 0.45, radius * 0.45);
        particleApi.spawnColoredParticles(loc, C_ROYAL_AMETHYST, 1.6f, 4, radius * 0.4, radius * 0.4, radius * 0.4);
        if (tick % 5 == 0) {
            particleApi.spawnColoredParticles(loc, C_ELDRITCH_GREEN, 1.4f, 2, radius * 0.5, radius * 0.5, radius * 0.5);
        }

        double pulse = Math.sin(tick * 0.25) * 0.08;
        double glowRadius = radius * 0.62 * (1.0 + pulse);

        drawGlowShell(loc, glowRadius, tick);
        drawSpiralSystem(loc, radius, tick, dir);
        drawDistortionStreaks(loc, radius, tick, dir);
        drawGalaxyDisc(loc, radius, tick, dir);

        int ringPoints = 24;
        for (int i = 0; i < ringPoints; i++) {
            double a = Math.toRadians(i * (360.0 / ringPoints) + tick * 4);
            Location ringPt = loc.clone().add(
                    Math.cos(a) * radius * 0.55,
                    Math.sin(a) * radius * 0.18,
                    Math.sin(a) * radius * 0.55);
            Color ringColor = i % 6 == 0 ? C_MYSTIC_SILVER : C_ORB_RING;
            particleApi.spawnColoredParticles(ringPt, ringColor, 1.4f, 2, 0.03, 0.03, 0.03);
        }
    }

    private void drawSpiralSystem(Location center, double radius, int tick, Vector dir) {

        Vector up = Math.abs(dir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector axisUp = right.clone().crossProduct(dir).normalize();

        int armPoints = 16;
        double spiralLen = radius * 1.8;

        for (int i = 0; i < armPoints; i++) {
            double t2 = (double) i / armPoints;
            double ang = Math.toRadians(tick * 10 + t2 * 360 * 2.2);
            double r = (radius * 0.35) + Math.sin(t2 * Math.PI) * (radius * 0.5);
            double along = (t2 - 0.5) * spiralLen;
            Location pt = center.clone()
                    .add(right.clone().multiply(Math.cos(ang) * r))
                    .add(axisUp.clone().multiply(Math.sin(ang) * r))
                    .add(dir.clone().multiply(along));
            particleApi.spawnColoredParticles(pt, C_ORB_CYAN, 1.3f, 2, 0.03, 0.03, 0.03);
        }

        for (int i = 0; i < armPoints; i++) {
            double t2 = (double) i / armPoints;
            double ang = Math.toRadians(-tick * 10 + t2 * 360 * 2.2 + 90);
            double r = (radius * 0.35) + Math.sin(t2 * Math.PI) * (radius * 0.5);
            double along = (t2 - 0.5) * spiralLen;
            Location pt = center.clone()
                    .add(right.clone().multiply(Math.cos(ang) * r))
                    .add(axisUp.clone().multiply(Math.sin(ang) * r))
                    .add(dir.clone().multiply(along));
            particleApi.spawnColoredParticles(pt, C_ORB_PINK, 1.3f, 2, 0.03, 0.03, 0.03);
        }
    }

    private void drawDistortionStreaks(Location center, double radius, int tick, Vector dir) {
        Vector up = Math.abs(dir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector axisUp = right.clone().crossProduct(dir).normalize();

        double[] tiltDeg   = { -25, 35, 10 };
        double[] spinSpeed = { 3.0, -2.4, 1.6 };
        Color[] streakColors = { C_ROYAL_AMETHYST, C_ARCANE_TEAL, C_MYSTIC_SILVER };
        double streakRadius = radius * 1.35;
        int arcPoints = 22;
        double arcSpan = 230;

        for (int s = 0; s < tiltDeg.length; s++) {
            double tilt = Math.toRadians(tiltDeg[s]);

            Vector tiltedRight = right.clone().multiply(Math.cos(tilt)).add(dir.clone().multiply(Math.sin(tilt)));
            Vector tiltedUp    = axisUp.clone();

            double spin = tick * spinSpeed[s];
            for (int i = 0; i < arcPoints; i++) {
                double t2 = (double) i / (arcPoints - 1);
                double ang = Math.toRadians(spin + t2 * arcSpan);
                double r = streakRadius * (0.85 + 0.15 * Math.sin(t2 * Math.PI * 2));
                Location pt = center.clone()
                        .add(tiltedRight.clone().multiply(Math.cos(ang) * r))
                        .add(tiltedUp.clone().multiply(Math.sin(ang) * r).multiply(0.5));
                float size = (i == arcPoints / 2) ? 1.6f : 1.1f;
                Color c = i == arcPoints / 2 ? C_ORB_STREAK : streakColors[s];
                particleApi.spawnColoredParticles(pt, c, size, 2, 0.02, 0.02, 0.02);
            }
        }
    }

    private void drawGalaxyDisc(Location center, double radius, int tick, Vector dir) {
        Vector up = Math.abs(dir.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector axisUp = right.clone().crossProduct(dir).normalize();

        double tilt = Math.toRadians(22);
        Vector planeA = right.clone();
        Vector planeB = axisUp.clone().multiply(Math.cos(tilt)).add(dir.clone().multiply(Math.sin(tilt)));

        int arms = 4;
        int pointsPerArm = 10;
        double discRadius = radius * 1.6;
        double spin = tick * 5;
        Color[] armColors = MYSTIC_COLORS;

        for (int a = 0; a < arms; a++) {
            double armOffset = a * (360.0 / arms);
            Color armColor = armColors[a % armColors.length];
            for (int i = 0; i < pointsPerArm; i++) {
                double t2 = (double) i / pointsPerArm;
                double r = discRadius * (0.25 + t2 * 0.95);
                double ang = Math.toRadians(armOffset + spin + t2 * 140);
                Location pt = center.clone()
                        .add(planeA.clone().multiply(Math.cos(ang) * r))
                        .add(planeB.clone().multiply(Math.sin(ang) * r));
                float size = (float) (0.7 + (1.0 - t2) * 0.6);
                particleApi.spawnColoredParticles(pt, armColor, size, 1, 0.02, 0.02, 0.02);

                if (i % 2 == 0) {
                    Location cloud = pt.clone().add(
                            (Math.random() - 0.5) * 0.4,
                            (Math.random() - 0.5) * 0.4,
                            (Math.random() - 0.5) * 0.4);
                    particleApi.spawnParticles(cloud, Particle.CLOUD, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
        }

        if (tick % 2 == 0) {
            Random sr = new Random();
            int starCount = 6;
            for (int s = 0; s < starCount; s++) {
                double ang = sr.nextDouble() * Math.PI * 2;
                double r = sr.nextDouble() * discRadius;
                Location star = center.clone()
                        .add(planeA.clone().multiply(Math.cos(ang) * r))
                        .add(planeB.clone().multiply(Math.sin(ang) * r));
                Color starColor = sr.nextBoolean() ? C_STARLIGHT : C_MYSTIC_SILVER;
                float starSize = (float) (0.4 + sr.nextDouble() * 0.6);
                particleApi.spawnColoredParticles(star, starColor, starSize, 1, 0.01, 0.01, 0.01);
                if (sr.nextDouble() < 0.3) {
                    particleApi.spawnParticles(star, Particle.END_ROD, 1, 0.01, 0.01, 0.01, 0.0);
                }
            }
        }
    }

    private void drawGlowShell(Location center, double radius, int tick) {
        int points = 18;
        double spin = tick * 6;

        for (int i = 0; i < points; i++) {
            double a = Math.toRadians(i * (360.0 / points) + spin);
            double cos = Math.cos(a);
            double sin = Math.sin(a);

            spawnGlowPoint(center, cos * radius, sin * radius, 0);

            spawnGlowPoint(center, cos * radius, 0, sin * radius);

            spawnGlowPoint(center, 0, cos * radius, sin * radius);
        }
    }

    private void spawnGlowPoint(Location center, double x, double y, double z) {
        Location inner = center.clone().add(x, y, z);

        particleApi.spawnColoredParticles(inner, C_ORB_HALO, 1.6f, 2, 0.04, 0.04, 0.04);

        Location mid = center.clone().add(x * 1.05, y * 1.05, z * 1.05);
        particleApi.spawnColoredParticles(mid, C_ORB_CYAN, 1.1f, 1, 0.04, 0.04, 0.04);

        Location outer = center.clone().add(x * 1.12, y * 1.12, z * 1.12);
        particleApi.spawnColoredParticles(outer, C_ORB_RING, 1.1f, 1, 0.05, 0.05, 0.05);
    }

    private void explodeComictest(Player p, Location center) {
        final double EXPLODE_RADIUS = 9.0;
        final double MAX_DAMAGE     = 28.0;

        drawImplosionBuildup(center);

        new BukkitRunnable() {
            @Override
            public void run() {
                detonateComictest(p, center, EXPLODE_RADIUS, MAX_DAMAGE);
            }
        }.runTaskLater(magicPlugin, 4L);
    }

    private void drawImplosionBuildup(Location center) {
        Random r = new Random();
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 0.6f);
        List<Player> nearby = getNearbyPlayers(center, 40);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 4) { cancel(); return; }
                double pull = 2.5 - t * 0.5;
                for (int i = 0; i < 24; i++) {
                    double theta = r.nextDouble() * Math.PI * 2;
                    double phi = Math.acos(2 * r.nextDouble() - 1);
                    Vector dir = new Vector(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta));
                    Location pt = center.clone().add(dir.multiply(pull));
                    Color c = i % 2 == 0 ? C_ORB_HALO : C_ROYAL_AMETHYST;
                    particleApi.spawnColoredParticles(pt, c, 1.3f, 1, 0.02, 0.02, 0.02);
                }
                center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f - t * 0.15f);

                for (Player pl : nearby) {
                    pl.sendTitle(t % 2 == 0 ? "§4§l█" : "§5§l█", "", 0, 2, 0);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void detonateComictest(Player p, Location center, double EXPLODE_RADIUS, double MAX_DAMAGE) {

        center.getWorld().strikeLightningEffect(center);
        center.getWorld().strikeLightningEffect(center.clone().add(1.5, 0, 0));
        center.getWorld().strikeLightningEffect(center.clone().add(-1.5, 0, 0));

        List<Player> nearby = getNearbyPlayers(center, 40);
        for (Player pl : nearby) {
            pl.sendTitle("§f§l✦ COSMIC IMPACT ✦", "§7§o— the galaxy trembles —", 0, 8, 4);
        }

        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.55f);
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.1f);
        p.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 0.9f);
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.9f, 1.5f);
        p.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.7f, 1.8f);

        particleApi.spawnColoredParticles(center, C_ORB_HALO, 3.6f, 280, 0.9, 0.9, 0.9);
        particleApi.spawnColoredParticles(center, C_VIOLET,   3.2f, 220, 1.1, 1.1, 1.1);
        particleApi.spawnColoredParticles(center, C_ORB_CORE, 2.8f, 150, 0.7, 0.7, 0.7);
        particleApi.spawnColoredParticles(center, C_ORB_CYAN, 2.8f, 120, 0.95, 0.95, 0.95);
        particleApi.spawnColoredParticles(center, C_ORB_PINK, 2.8f, 120, 0.95, 0.95, 0.95);
        particleApi.spawnColoredParticles(center, C_STARLIGHT, 2.6f, 100, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(center, C_ROYAL_AMETHYST, 2.8f, 130, 0.9, 0.9, 0.9);
        particleApi.spawnColoredParticles(center, C_ELDRITCH_GREEN, 2.4f, 80, 0.85, 0.85, 0.85);
        particleApi.spawnColoredParticles(center, C_BLOOD_GARNET, 2.4f, 75, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(center, C_ARCANE_TEAL, 2.4f, 65, 0.85, 0.85, 0.85);
        particleApi.spawnParticles(center, Particle.EXPLOSION_EMITTER, 5, 0.4, 0.4, 0.4, 0);
        particleApi.spawnParticles(center, Particle.END_ROD, 45, 0.7, 0.7, 0.7, 0.07);

        drawComictestExplosionRays(center);
        drawGalaxySupernovaBurst(center);
        runComictestDome(center, 14.0);

        for (Entity e : center.getWorld().getNearbyEntities(center, EXPLODE_RADIUS, EXPLODE_RADIUS, EXPLODE_RADIUS)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(center);
            if (dist > EXPLODE_RADIUS) continue;
            double falloff = 1.0 - (dist / EXPLODE_RADIUS);
            double dmg = MAX_DAMAGE * Math.max(0.2, falloff);
            LivingEntity le = (LivingEntity) e;
            le.damage(dmg, p);

            Vector kb = e.getLocation().clone().subtract(center).toVector();
            if (!isVecFinite(kb) || kb.lengthSquared() < 0.01) kb = new Vector(0.1, 0, 0.1);
            kb = kb.normalize().multiply(1.4 + falloff * 1.8).setY(0.6 + falloff * 0.7);
            e.setVelocity(kb);

            int slowAmp = falloff > 0.6 ? 3 : 2;
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, slowAmp, false, true));
            if (falloff > 0.4) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true));
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                particleApi.spawnColoredParticles(center, C_STARLIGHT, 2.4f, 70, 0.5, 0.5, 0.5);
                particleApi.spawnColoredParticles(center, C_ROYAL_AMETHYST, 2.2f, 60, 0.55, 0.55, 0.55);
                particleApi.spawnParticles(center, Particle.EXPLOSION_EMITTER, 1, 0.15, 0.15, 0.15, 0);
                for (Entity e : center.getWorld().getNearbyEntities(center, EXPLODE_RADIUS * 0.6, EXPLODE_RADIUS * 0.6, EXPLODE_RADIUS * 0.6)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(MAX_DAMAGE * 0.25, p);
                }
            }
        }.runTaskLater(magicPlugin, 10L);

        runComictestWave(center, 26.0);
        runComictestAfterglow(center, 26.0);

        new BukkitRunnable() {
            @Override public void run() {
                runSingularityPull(p, center.clone(), () -> {
                    runComictestCollapseExplosion(p, center.clone());
                    drawGalaxyCoreRemnant(center.clone());
                    new BukkitRunnable() {
                        @Override public void run() { runComictestDomainExpansion(p, center.clone(), 50.0); }
                    }.runTaskLater(magicPlugin, 18L);
                });
            }
        }.runTaskLater(magicPlugin, 22L);
    }

    private void runSingularityPull(Player p, Location center, Runnable onFinish) {
        final double PULL_RADIUS  = 22.0;
        final double PULL_FORCE   = 0.55;
        final double DAMAGE_TICK  = 2.0;
        final int DURATION        = 60;

        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.4f, 0.35f);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.3f);
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.4f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= DURATION) {
                    cancel();
                    onFinish.run();
                    return;
                }

                double progress = (double) t / DURATION;

                int ringPts = 30;
                for (int i = 0; i < ringPts; i++) {
                    double a = Math.toRadians(i * (360.0 / ringPts) - t * 12);
                    double r = PULL_RADIUS * (1.0 - progress * 0.4);
                    Location rp = center.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                    Color c = i % 3 == 0 ? C_ORB_HALO : i % 3 == 1 ? C_ROYAL_AMETHYST : C_STARLIGHT;
                    particleApi.spawnColoredParticles(rp, c, 1.4f, 1, 0.02, 0.02, 0.02);
                }

                int corePts = (int) (8 + progress * 12);
                for (int i = 0; i < corePts; i++) {
                    double ca = Math.toRadians(i * (360.0 / corePts) + t * 22);
                    double cr = 0.5 + Math.sin(t * 0.3 + i) * 0.2;
                    Location cp = center.clone().add(Math.cos(ca) * cr, Math.sin(ca * 0.5) * 0.3, Math.sin(ca) * cr);
                    particleApi.spawnColoredParticles(cp, C_ORB_CORE, 0.9f, 1, 0.04, 0.04, 0.04);
                    if (i % 3 == 0) particleApi.spawnParticles(cp, Particle.END_ROD, 1, 0.01, 0.01, 0.01, 0.0);
                }

                if (t % 3 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double sa = Math.toRadians(i * 60 + t * 8);
                        double sr = PULL_RADIUS * (0.3 + Math.random() * 0.6);
                        Location sp = center.clone().add(Math.cos(sa) * sr, (Math.random() - 0.5) * 4, Math.sin(sa) * sr);
                        Color sc = i % 2 == 0 ? C_ORB_CYAN : C_ORB_PINK;
                        particleApi.spawnColoredParticles(sp, sc, 1.1f, 1, 0.03, 0.03, 0.03);
                    }
                }

                if (t % 20 == 0) {
                    int secLeft = (DURATION - t) / 20;
                    String bar = "§5§l⬡⬡⬡⬡⬡⬡⬡⬡⬡⬡".substring(0, Math.max(2, (int)((1.0 - progress) * 22)));
                    for (Player pl : getNearbyPlayers(center, 40)) {
                        pl.sendTitle("§4§l☯ SINGULARITY ☯", "§c§l" + bar, 0, 22, 4);
                    }
                }
                if (t % 10 == 5) {
                    for (Player pl : getNearbyPlayers(center, 40)) {
                        pl.sendTitle("", "", 0, 2, 0);
                    }
                }

                if (t % 10 == 0) {
                    float vol = 0.5f + (float) progress * 0.8f;
                    float pitch = 0.3f + (float) progress * 0.25f;
                    center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, vol, pitch);
                }

                for (Entity e : center.getWorld().getNearbyEntities(center, PULL_RADIUS, PULL_RADIUS, PULL_RADIUS)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;

                    Vector toCenter = center.clone().subtract(e.getLocation()).toVector();
                    double dist = toCenter.length();
                    if (dist < 0.5) continue;
                    Vector pull = toCenter.normalize().multiply(PULL_FORCE + progress * 0.35);

                    pull.setY(pull.getY() * 0.4 + 0.05);
                    e.setVelocity(pull);

                    le.damage(DAMAGE_TICK, p);
                }

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void runComictestCollapseExplosion(Player p, Location center) {
        final double COLLAPSE_RADIUS = 13.0;
        final double COLLAPSE_DAMAGE = 45.0;

        for (int i = 0; i < 5; i++) {
            double rad = Math.toRadians(i * 72);
            center.getWorld().strikeLightningEffect(center.clone().add(Math.cos(rad) * 3, 0, Math.sin(rad) * 3));
        }

        List<Player> nearby = getNearbyPlayers(center, 50);
        for (Player pl : nearby) {
            pl.sendTitle("§c§l⚡ COLLAPSE ⚡", "§4§l— singularity released —", 0, 12, 5);
        }
        new BukkitRunnable() {
            @Override public void run() {
                center.getWorld().strikeLightningEffect(center);
                for (Player pl : nearby) pl.sendTitle("§f§l!", "", 0, 3, 2);
            }
        }.runTaskLater(magicPlugin, 6L);

        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.4f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.65f);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.8f);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 1.0f, 1.2f);
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.6f);
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 0.7f);
        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);

        particleApi.spawnColoredParticles(center, C_STARLIGHT,       4.0f, 400, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(center, C_ORB_HALO,        3.8f, 350, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(center, C_VIOLET,          3.4f, 280, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(center, C_ORB_CORE,        3.0f, 200, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(center, C_ROYAL_AMETHYST,  3.2f, 220, 0.7, 0.7, 0.7);
        particleApi.spawnColoredParticles(center, C_ORB_CYAN,        3.0f, 160, 0.7, 0.7, 0.7);
        particleApi.spawnColoredParticles(center, C_ORB_PINK,        3.0f, 160, 0.7, 0.7, 0.7);
        particleApi.spawnColoredParticles(center, C_BLOOD_GARNET,    2.6f, 100, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(center, C_ARCANE_TEAL,     2.6f, 90,  0.65, 0.65, 0.65);
        particleApi.spawnColoredParticles(center, C_ELDRITCH_GREEN,  2.6f, 90,  0.65, 0.65, 0.65);
        particleApi.spawnParticles(center, Particle.EXPLOSION_EMITTER, 8, 0.5, 0.5, 0.5, 0);
        particleApi.spawnParticles(center, Particle.END_ROD, 80, 0.8, 0.8, 0.8, 0.12);

        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            Vector rayDir = new Vector(Math.cos(a), 0, Math.sin(a));
            particleLine(center, center.clone().add(rayDir.multiply(COLLAPSE_RADIUS)), 0.25, MYSTIC_COLORS[i % MYSTIC_COLORS.length], 1.8f);
        }

        particleLine(center, center.clone().add(0, COLLAPSE_RADIUS * 0.8, 0), 0.3, C_STARLIGHT, 2.0f);
        particleLine(center, center.clone().add(0, -COLLAPSE_RADIUS * 0.5, 0), 0.3, C_ORB_HALO, 2.0f);

        for (int wave = 0; wave < 3; wave++) {
            final int w = wave;
            new BukkitRunnable() {
                double r = 0.5;
                int t = 0;
                @Override public void run() {
                    if (r > COLLAPSE_RADIUS) { cancel(); return; }
                    int pts = (int) (24 + r * 4);
                    for (int i = 0; i < pts; i++) {
                        double a = Math.toRadians(i * (360.0 / pts));
                        for (int layer = -1; layer <= 1; layer++) {
                            Location lp = center.clone().add(Math.cos(a) * r, layer * r * 0.25, Math.sin(a) * r);
                            Color c = w == 0 ? C_STARLIGHT : w == 1 ? C_ROYAL_AMETHYST : C_ORB_CYAN;
                            particleApi.spawnColoredParticles(lp, c, 1.6f, 1, 0.02, 0.02, 0.02);
                        }
                    }
                    r += 1.6;
                    t++;
                }
            }.runTaskTimer(magicPlugin, w * 4L, 1);
        }

        List<LivingEntity> victims = new ArrayList<>();
        for (Entity e : center.getWorld().getNearbyEntities(center, COLLAPSE_RADIUS, COLLAPSE_RADIUS, COLLAPSE_RADIUS)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(center);
            if (dist > COLLAPSE_RADIUS) continue;
            double falloff = 1.0 - (dist / COLLAPSE_RADIUS);
            double dmg = COLLAPSE_DAMAGE * Math.max(0.3, falloff);
            ((LivingEntity) e).damage(dmg, p);

            Vector kb = e.getLocation().subtract(center).toVector();
            if (!isVecFinite(kb) || kb.lengthSquared() < 0.01) kb = new Vector(0.1, 0, 0.1);
            kb = kb.normalize().multiply(2.2 + falloff * 2.8).setY(0.9 + falloff * 1.1);
            e.setVelocity(kb);

            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 140, 4, false, true));
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
            victims.add((LivingEntity) e);
        }

        if (victims.size() >= 2) {
            center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.3f);
            for (int i = 0; i < victims.size(); i++) {
                LivingEntity a = victims.get(i);
                LivingEntity b = victims.get((i + 1) % victims.size());
                particleLine(a.getEyeLocation(), b.getEyeLocation(), 0.35, C_STARLIGHT, 1.5f);
                b.damage(4.0, p);
            }
        }

        Random meteorRng = new Random();
        for (LivingEntity v : victims) {
            spawnMeteor(v.getLocation().clone(), center, 30.0, p);
        }
        int extraMeteors = 4;
        for (int i = 0; i < extraMeteors; i++) {
            double ang = meteorRng.nextDouble() * Math.PI * 2;
            double r = meteorRng.nextDouble() * COLLAPSE_RADIUS * 0.9;
            Location groundPt = center.clone().add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
            spawnMeteor(groundPt, center, 30.0, p);
        }
    }

    private void spawnMeteor(Location targetLoc, Location blastCenter, double startHeight, Player caster) {
        Random r = new Random();
        double driftX = (r.nextDouble() - 0.5) * 3.0;
        double driftZ = (r.nextDouble() - 0.5) * 3.0;
        Location start = targetLoc.clone().add(driftX, startHeight, driftZ);
        Vector fallDir = targetLoc.clone().subtract(start).toVector().normalize();
        World world = targetLoc.getWorld();

        world.playSound(targetLoc, Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.4f);

        new BukkitRunnable() {
            Location loc = start.clone();
            @Override
            public void run() {
                if (loc.distance(targetLoc) < 1.0) {
                    cancel();

                    world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.1f);
                    world.playSound(targetLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.6f);
                    particleApi.spawnColoredParticles(targetLoc, C_EMBER_GOLD, 2.2f, 60, 0.6, 0.4, 0.6);
                    particleApi.spawnColoredParticles(targetLoc, C_BLOOD_GARNET, 1.8f, 40, 0.5, 0.3, 0.5);
                    particleApi.spawnParticles(targetLoc, Particle.EXPLOSION_EMITTER, 1, 0.1, 0.1, 0.1, 0);
                    particleApi.spawnParticles(targetLoc, Particle.LAVA, 12, 0.4, 0.2, 0.4, 0.02);

                    double impactRadius = 3.0;
                    for (Entity e : world.getNearbyEntities(targetLoc, impactRadius, impactRadius, impactRadius)) {
                        if (e.equals(caster) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        double dist = e.getLocation().distance(targetLoc);
                        if (dist > impactRadius) continue;
                        ((LivingEntity) e).damage(8.0 * (1.0 - dist / impactRadius) + 2.0, caster);
                        Vector kb = e.getLocation().subtract(targetLoc).toVector();
                        if (isVecFinite(kb) && kb.lengthSquared() > 0.01) {
                            e.setVelocity(kb.normalize().multiply(0.8).setY(0.5));
                        }
                    }
                    return;
                }

                particleApi.spawnColoredParticles(loc, C_EMBER_GOLD, 1.4f, 2, 0.08, 0.08, 0.08);
                particleApi.spawnParticles(loc, Particle.SMOKE, 2, 0.06, 0.06, 0.06, 0.01);
                particleApi.spawnParticles(loc, Particle.LAVA, 1, 0.05, 0.05, 0.05, 0.0);

                loc.add(fallDir.clone().multiply(1.6));
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void drawGalaxyCoreRemnant(Location center) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 18) { cancel(); return; }
                double fade = 1.0 - (double) t / 18;
                int pts = 10;
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(i * 36 + t * 15);
                    double r = 1.8 * fade;
                    Location pt = center.clone().add(Math.cos(a) * r, Math.sin(t * 0.2) * 0.3, Math.sin(a) * r);
                    particleApi.spawnColoredParticles(pt, C_ROYAL_AMETHYST, (float) (1.2 * fade), 1, 0.02, 0.02, 0.02);
                }
                particleApi.spawnColoredParticles(center, C_STARLIGHT, (float) (1.5 * fade), 2, 0.05, 0.05, 0.05);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void runComictestDomainExpansion(Player p, Location center, double maxRadius) {
        p.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.3f);

        particleApi.spawnColoredParticles(center, C_STARLIGHT, 3.2f, 110, 0.4, 0.4, 0.4);
        particleApi.spawnColoredParticles(center, C_ORB_HALO,  2.8f, 90, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(center, C_ORB_CYAN,  2.2f, 60, 0.45, 0.45, 0.45);
        particleApi.spawnParticles(center, Particle.EXPLOSION_EMITTER, 2, 0.2, 0.2, 0.2, 0);

        drawComictestJet(center, maxRadius * 1.4);

        BukkitRunnable expandTask = new BukkitRunnable() {
            double radius = 1.0;
            int t = 0;

            @Override
            public void run() {
                if (radius > maxRadius) {
                    cancel();
                    phase3Tasks.remove(this);
                    bloomComictestDomain(p, center);
                    return;
                }

                int points = (int) (28 + radius * 2.2);
                double spin = t * 6;
                double tiltRad = Math.toRadians(18);
                for (int i = 0; i < points; i++) {
                    double a = Math.toRadians(i * (360.0 / points) + spin);
                    double wobble = Math.sin(a * 3 + t * 0.3) * radius * 0.08;
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    double y = Math.sin(a) * radius * Math.sin(tiltRad) + wobble;

                    Location disk = center.clone().add(x, y, z);
                    Color c = i % 3 == 0 ? C_ORB_HALO : (i % 3 == 1 ? C_ORB_CYAN : C_STARLIGHT);
                    particleApi.spawnColoredParticles(disk, c, 1.4f, 2, 0.05, 0.05, 0.05);

                    Location diskUpper = disk.clone().add(0, 0.6, 0);
                    particleApi.spawnColoredParticles(diskUpper, c, 0.9f, 1, 0.03, 0.03, 0.03);
                    Location diskLower = disk.clone().add(0, -0.6, 0);
                    particleApi.spawnColoredParticles(diskLower, c, 0.9f, 1, 0.03, 0.03, 0.03);
                }

                if (t % 4 == 0) {
                    drawComictestJet(center, maxRadius * (0.5 + radius / maxRadius));
                    p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.6f);
                }

                radius += 2.5;
                t++;
            }
        };
        phase3Tasks.add(expandTask);
        expandTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void drawComictestJet(Location center, double length) {

        particleLine(center.clone(), center.clone().add(0, length, 0), 0.25, C_STARLIGHT, 1.8f);
        particleLine(center.clone(), center.clone().add(0, -length, 0), 0.25, C_STARLIGHT, 1.8f);

        double[][] offsets = { {0.18, 0}, {-0.18, 0}, {0, 0.18}, {0, -0.18} };
        for (double[] off : offsets) {
            Location top = center.clone().add(off[0], 0, off[1]);
            particleLine(top, top.clone().add(0, length, 0), 0.5, C_ORB_HALO, 1.1f);
            particleLine(top, top.clone().add(0, -length, 0), 0.5, C_ORB_HALO, 1.1f);
        }
    }

    private void drawComictestExplosionRays(Location center) {
        Random r = new Random();
        int rayCount = 44;
        for (int i = 0; i < rayCount; i++) {

            double theta = r.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * r.nextDouble() - 1);
            Vector rayDir = new Vector(
                    Math.sin(phi) * Math.cos(theta),
                    Math.cos(phi),
                    Math.sin(phi) * Math.sin(theta));
            double rayLen = 5.0 + r.nextDouble() * 4.0;
            Location rayEnd = center.clone().add(rayDir.multiply(rayLen));
            Color c;
            int colorPick = i % 7;
            if (colorPick == 0) c = C_ORB_HALO;
            else if (colorPick == 1) c = C_ORB_CYAN;
            else if (colorPick == 2) c = C_ORB_PINK;
            else if (colorPick == 3) c = C_ROYAL_AMETHYST;
            else if (colorPick == 4) c = C_ELDRITCH_GREEN;
            else if (colorPick == 5) c = C_ARCANE_TEAL;
            else c = C_BLOOD_GARNET;
            particleLine(center, rayEnd, 0.3, c, 1.3f);
        }
    }

    private void drawGalaxySupernovaBurst(Location center) {
        Random r = new Random();
        int planeCount = 3;
        double[] planeTilt = { 0, 35, -35 };
        Color[] burstColors = { C_STARLIGHT, C_ORB_CYAN, C_ORB_PINK };

        for (int pl = 0; pl < planeCount; pl++) {
            Vector base1 = new Vector(1, 0, 0);
            Vector base2 = new Vector(0, 0, 1);
            double tiltRad = Math.toRadians(planeTilt[pl]);
            Vector planeA = base1.clone();
            Vector planeB = base2.clone().multiply(Math.cos(tiltRad)).add(new Vector(0, 1, 0).multiply(Math.sin(tiltRad)));
            Color armColor = burstColors[pl % burstColors.length];

            new BukkitRunnable() {
                double radius = 0.5;
                int t = 0;

                @Override
                public void run() {
                    if (radius > 9.0) { cancel(); return; }
                    int arms = 2;
                    for (int a = 0; a < arms; a++) {
                        double baseAng = a * 180 + t * 14;
                        for (int seg = 0; seg < 3; seg++) {
                            double segR = radius - seg * 0.6;
                            if (segR <= 0) continue;
                            double ang = Math.toRadians(baseAng + segR * 18);
                            Location pt = center.clone()
                                    .add(planeA.clone().multiply(Math.cos(ang) * segR))
                                    .add(planeB.clone().multiply(Math.sin(ang) * segR));
                            particleApi.spawnColoredParticles(pt, armColor, 1.4f, 2, 0.04, 0.04, 0.04);
                        }
                    }
                    radius += 1.3;
                    t++;
                }
            }.runTaskTimer(magicPlugin, pl * 2L, 1);
        }

        for (int i = 0; i < 16; i++) {
            double theta = r.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * r.nextDouble() - 1);
            Vector dir = new Vector(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta));
            double len = 3.0 + r.nextDouble() * 5.0;
            Location end = center.clone().add(dir.multiply(len));
            particleLine(center, end, 0.4, C_MYSTIC_SILVER, 0.9f);
        }
    }

    private void runComictestDome(Location center, double maxRadius) {
        new BukkitRunnable() {
            double radius = 0.5;
            int t = 0;

            @Override
            public void run() {
                if (radius > maxRadius) { cancel(); return; }

                int meridianPoints = 14;
                int slices = 18;
                for (int s = 0; s < slices; s++) {
                    double az = Math.toRadians(s * (360.0 / slices) + t * 8);
                    for (int m = 0; m <= meridianPoints; m++) {
                        double polar = Math.toRadians(m * (90.0 / meridianPoints));
                        double x = Math.sin(polar) * Math.cos(az) * radius;
                        double y = Math.cos(polar) * radius;
                        double z = Math.sin(polar) * Math.sin(az) * radius;
                        Location pt = center.clone().add(x, y, z);
                        particleApi.spawnColoredParticles(pt, C_ORB_RING, 1.3f, 2, 0.04, 0.04, 0.04);
                    }
                }

                radius += 0.7;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void runComictestWave(Location center, double maxRadius) {
        new BukkitRunnable() {
            double radius = 0.5;
            int t = 0;

            @Override
            public void run() {
                if (radius > maxRadius) { cancel(); return; }

                int points = (int) (28 + radius * 3);
                for (int i = 0; i < points; i++) {
                    double a = Math.toRadians(i * (360.0 / points) + t * 5);
                    Location ground = getGroundBelow(center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
                    particleApi.spawnColoredParticles(ground, C_ORB_RING, 1.5f, 2, 0.05, 0.05, 0.05);
                    particleApi.spawnColoredParticles(ground, C_ORB_CYAN, 1.1f, 1, 0.03, 0.03, 0.03);
                }
                if (t % 3 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.35f, 1.5f);
                }

                radius += 1.4;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void runComictestAfterglow(Location center, double maxRadius) {
        Random r = new Random();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80) { cancel(); return; }
                int amount = (int) (16 * (1.0 - t / 80.0));
                for (int i = 0; i < amount; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double dist = r.nextDouble() * maxRadius;
                    Location lp = getGroundBelow(center.clone().add(Math.cos(a) * dist, 0, Math.sin(a) * dist));
                    particleApi.spawnColoredParticles(lp.clone().add(0, 0.1, 0),
                            r.nextBoolean() ? C_ORB_PINK : C_ORB_STREAK, 1.1f, 2, 0.12, 0.06, 0.12);
                    if (r.nextDouble() < 0.2) {
                        particleApi.spawnParticles(lp.clone().add(0, 0.2, 0), Particle.END_ROD, 1, 0.05, 0.1, 0.05, 0.005);
                    }
                }
                t += 4;
            }
        }.runTaskTimer(magicPlugin, 0, 4);
    }

    private void bloomComictestDomain(Player p, Location center) {
        final double DOMAIN_RADIUS = 50.0;
        final int DOMAIN_TICKS     = 600;
        final double DPS           = 2.5;

        p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ A cosmic domain tears open — r=50!");
        p.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        p.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);

        domainTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= DOMAIN_TICKS) {
                    cancel();
                    domainTask = null;
                    runComictestCollapse(p, center.clone());
                    return;
                }

                double globalSpin = t * 2.2;
                drawGalaxySpiral(center, DOMAIN_RADIUS, t, globalSpin);
                drawDomainTwinkles(center, DOMAIN_RADIUS, t, globalSpin);
                drawDomainCenterPulse(center, t, globalSpin);
                drawDomainSatellites(center, DOMAIN_RADIUS, t, globalSpin);
                drawDomainOrbitingAsteroids(center, DOMAIN_RADIUS, t, globalSpin);
                if (t % 2 == 0) drawDomainPlanets(center, DOMAIN_RADIUS, t, globalSpin);

                if (t % 20 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, DOMAIN_RADIUS, DOMAIN_RADIUS, DOMAIN_RADIUS)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        double dist = e.getLocation().distance(center);
                        if (dist > DOMAIN_RADIUS) continue;
                        ((LivingEntity) e).damage(DPS, p);
                    }
                    p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f);
                }

                t++;
            }
        };
        domainTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void runComictestCollapse(Player p, Location center) {
        final int COLLAPSE_TICKS = 50;
        final double PULL_RADIUS = 50.0;

        p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ The domain collapses inward...");
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.4f);
        p.getWorld().playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 0.5f);

        BukkitRunnable collapseTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= COLLAPSE_TICKS) {
                    cancel();
                    phase3Tasks.remove(this);
                    p.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.6f);
                    runComictestShockwaves(p, center.clone(), 5, 50.0, 5.0);
                    return;
                }

                double progress = (double) t / COLLAPSE_TICKS;
                double pullStrength = 0.15 + progress * 0.5;

                for (Entity e : center.getWorld().getNearbyEntities(center, PULL_RADIUS, PULL_RADIUS, PULL_RADIUS)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    Vector toCenter = center.clone().subtract(e.getLocation()).toVector();
                    if (!isVecFinite(toCenter) || toCenter.lengthSquared() < 0.4) continue;
                    Vector pull = toCenter.normalize().multiply(pullStrength);
                    e.setVelocity(e.getVelocity().add(pull));
                }

                drawCollapseVortex(center, 16.0 * (1.0 - progress * 0.5), t);

                t++;
            }
        };
        phase3Tasks.add(collapseTask);
        collapseTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void drawCollapseVortex(Location center, double maxRadius, int t) {
        int arms = 8;
        int pointsPerArm = 18;
        double spin = t * 14;

        for (int arm = 0; arm < arms; arm++) {
            double armOffset = arm * (360.0 / arms);
            for (int i = 0; i < pointsPerArm; i++) {
                double progress = (double) i / (pointsPerArm - 1);

                double inwardShift = (t % 14) / 14.0;
                double effectiveProgress = Math.max(0, progress - inwardShift * 0.3);
                double r = maxRadius * effectiveProgress;
                if (r < 0.3) continue;

                double angle = Math.toRadians(effectiveProgress * 360 + armOffset + spin);
                double height = (1.0 - effectiveProgress) * 2.0 - 1.0;
                Location pt = center.clone().add(Math.cos(angle) * r, height, Math.sin(angle) * r);

                Color c = i % 4 == 0 ? C_ABYSS_NAVY : (i % 4 == 1 ? C_ORB_CYAN : (i % 4 == 2 ? C_VIOLET : C_BLOOD_GARNET));
                float size = (float) (1.0 + (1.0 - effectiveProgress) * 0.8);
                particleApi.spawnColoredParticles(pt, c, size, 2, 0.06, 0.06, 0.06);
            }
        }

        particleApi.spawnColoredParticles(center, C_ORB_HALO, 2.0f, 6, 0.25, 0.25, 0.25);
        particleApi.spawnColoredParticles(center, C_STARLIGHT, 1.6f, 4, 0.2, 0.2, 0.2);
        particleApi.spawnColoredParticles(center, C_ROYAL_AMETHYST, 1.6f, 3, 0.2, 0.2, 0.2);
    }

    private void runComictestShockwaves(Player p, Location center, int waveCount, double maxRadius, double damagePerWave) {
        for (int w = 0; w < waveCount; w++) {
            final int waveIndex = w;
            BukkitRunnable delayTask = new BukkitRunnable() {
                @Override public void run() {
                    phase3Tasks.remove(this);
                    p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.6f + waveIndex * 0.12f);
                    p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f + waveIndex * 0.1f);
                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Shockwave " + (waveIndex + 1) + "/" + waveCount + "!");
                    runSingleShockwave(p, center, maxRadius, damagePerWave, waveIndex, waveCount);
                }
            };
            phase3Tasks.add(delayTask);
            delayTask.runTaskLater(magicPlugin, w * 24L);
        }
    }

    private void runSingleShockwave(Player p, Location center, double maxRadius, double damage, int waveIndex, int waveCount) {
        double intensity = 1.0 + (double) waveIndex / Math.max(1, waveCount - 1) * 0.8;
        Color coreColor;
        if (waveIndex >= waveCount - 1) coreColor = C_STARLIGHT;
        else {
            Color[] waveColorCycle = { C_ORB_HALO, C_EMBER_GOLD, C_ROYAL_AMETHYST, C_ELDRITCH_GREEN, C_BLOOD_GARNET };
            coreColor = waveColorCycle[waveIndex % waveColorCycle.length];
        }

        drawComictestJet(center, maxRadius * 0.9 * intensity * 0.7);
        particleApi.spawnColoredParticles(center, C_STARLIGHT, (float) (2.8 * intensity), (int) (90 * intensity), 0.4, 0.4, 0.4);
        particleApi.spawnColoredParticles(center, C_EMBER_GOLD, (float) (2.2 * intensity), (int) (60 * intensity), 0.35, 0.35, 0.35);
        particleApi.spawnColoredParticles(center, coreColor, (float) (3.0 * intensity), (int) (50 * intensity), 0.3, 0.3, 0.3);
        particleApi.spawnParticles(center, Particle.EXPLOSION_EMITTER, waveIndex >= waveCount - 1 ? 3 : 1, 0.2, 0.2, 0.2, 0);

        drawShockwaveLightning(center, maxRadius * 0.5, coreColor, (int) (10 + waveIndex * 2));

        BukkitRunnable waveTask = new BukkitRunnable() {
            double radius = 1.0;
            int t = 0;

            @Override
            public void run() {
                if (radius > maxRadius) {
                    cancel();
                    phase3Tasks.remove(this);

                    for (Entity e : center.getWorld().getNearbyEntities(center, maxRadius, maxRadius, maxRadius)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        double dist = e.getLocation().distance(center);
                        if (dist > maxRadius) continue;
                        ((LivingEntity) e).damage(damage, p);
                    }
                    return;
                }

                int points = (int) ((28 + radius * 2.2) * Math.min(1.3, intensity));
                double spin = t * 5;
                double tiltRad = Math.toRadians(18);
                for (int i = 0; i < points; i++) {
                    double a = Math.toRadians(i * (360.0 / points) + spin);
                    double wobble = Math.sin(a * 3 + t * 0.3) * radius * 0.08;
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    double y = Math.sin(a) * radius * Math.sin(tiltRad) + wobble;

                    Location disk = center.clone().add(x, y, z);
                    Color c = i % 2 == 0 ? coreColor : C_EMBER_GOLD;
                    particleApi.spawnColoredParticles(disk, c, (float) (1.5 * intensity), 2, 0.06, 0.06, 0.06);

                    Location diskUpper = disk.clone().add(0, 0.7, 0);
                    particleApi.spawnColoredParticles(diskUpper, C_STARLIGHT, 1.0f, 1, 0.04, 0.04, 0.04);
                    Location diskLower = disk.clone().add(0, -0.7, 0);
                    particleApi.spawnColoredParticles(diskLower, C_STARLIGHT, 1.0f, 1, 0.04, 0.04, 0.04);

                    if (i % 3 == 0) {
                        Location haloPt = center.clone().add(x * 1.08, y * 1.08, z * 1.08);
                        particleApi.spawnColoredParticles(haloPt, C_ORB_RING, 0.8f, 1, 0.05, 0.05, 0.05);
                    }
                }

                if (t % 3 == 0) {
                    drawComictestJet(center, maxRadius * (0.4 + 0.6 * radius / maxRadius) * intensity * 0.7);
                }

                if (t % 6 == 0) {
                    drawShockwaveLightning(center, radius, coreColor, 6);
                }

                radius += 2.2;
                t++;
            }
        };
        phase3Tasks.add(waveTask);
        waveTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void drawShockwaveLightning(Location center, double length, Color color, int boltCount) {
        Random r = new Random();
        for (int i = 0; i < boltCount; i++) {
            double theta = r.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * r.nextDouble() - 1);
            Vector boltDir = new Vector(
                    Math.sin(phi) * Math.cos(theta),
                    Math.cos(phi) * 0.6,
                    Math.sin(phi) * Math.sin(theta));
            Location boltEnd = center.clone().add(boltDir.multiply(length));

            Location mid1 = center.clone().add(boltDir.clone().multiply(length * 0.33))
                    .add((r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.6);
            Location mid2 = center.clone().add(boltDir.clone().multiply(length * 0.66))
                    .add((r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.6);
            particleLine(center, mid1, 0.3, color, 1.1f);
            particleLine(mid1, mid2, 0.3, color, 1.0f);
            particleLine(mid2, boltEnd, 0.3, C_STARLIGHT, 0.9f);
        }
    }

    private void drawGalaxySpiral(Location center, double maxRadius, int t, double globalSpin) {
        final int ARMS = 4;
        final int POINTS_PER_ARM = 24;
        final int HEIGHT_LAYERS = 9;
        final double B = 0.18;

        Color[] armColors = { C_ORB_CYAN, C_ROYAL_AMETHYST, C_ARCANE_TEAL, C_BLOOD_GARNET };

        if (t % 2 == 0) {
            int borderPoints = 72;
            for (int i = 0; i < borderPoints; i++) {
                double a = Math.toRadians(i * (360.0 / borderPoints) + globalSpin);
                Location edge = center.clone().add(
                        Math.cos(a) * maxRadius, 0, Math.sin(a) * maxRadius);
                Color edgeColor = i % 9 == 0 ? C_MYSTIC_SILVER : C_ORB_STREAK;
                particleApi.spawnColoredParticles(edge, edgeColor, 1.7f, 2, 0.12, 0.12, 0.12);
            }
        }

        for (int layer = 0; layer < HEIGHT_LAYERS; layer++) {

            double layerT = (double) layer / (HEIGHT_LAYERS - 1) - 0.5;
            double y = layerT * maxRadius * 1.2;

            double layerRadiusScale = Math.sqrt(Math.max(0.05, 1.0 - Math.pow(Math.abs(layerT) * 2, 2)));
            double layerMaxR = maxRadius * layerRadiusScale;
            if (layerMaxR < 2) continue;

            for (int arm = 0; arm < ARMS; arm++) {
                double armOffset = arm * (360.0 / ARMS);
                Color armColor = armColors[arm];
                for (int i = 0; i < POINTS_PER_ARM; i++) {
                    double progress = (double) i / (POINTS_PER_ARM - 1);
                    double theta = progress * 4.0;
                    double r = layerMaxR * (Math.exp(B * theta) - 1) / (Math.exp(B * 4.0) - 1);
                    if (r < 0.5) continue;

                    double angleDeg = Math.toDegrees(theta) + armOffset + globalSpin;
                    double a = Math.toRadians(angleDeg);

                    Location pt = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);

                    boolean bright = progress < 0.35;
                    float size = bright ? 1.6f : 1.1f;
                    int count = bright ? 2 : 1;
                    Color c = bright ? (i % 4 == 0 ? C_STARLIGHT : C_ORB_HALO) : armColor;

                    particleApi.spawnColoredParticles(pt, c, size, count, 0.06, 0.06, 0.06);
                }
            }
        }
    }

    private void drawDomainTwinkles(Location center, double maxRadius, int t, double globalSpin) {
        if (t % 3 != 0) return;
        int twinkleCount = 22;
        for (int i = 0; i < twinkleCount; i++) {

            double seedTheta = (i * 137.5) % 360;
            double seedPhi = ((i * 71.0) % 180);
            double theta = Math.toRadians(seedTheta + globalSpin);
            double phi = Math.toRadians(seedPhi);
            double dist = maxRadius * (0.25 + (i % 5) * 0.15);
            Location pt = center.clone().add(
                    Math.sin(phi) * Math.cos(theta) * dist,
                    Math.cos(phi) * dist * 0.5,
                    Math.sin(phi) * Math.sin(theta) * dist);
            float size = 0.9f + (i % 3) * 0.25f;
            Color twinkleColor = i % 5 == 0 ? MYSTIC_COLORS[i % MYSTIC_COLORS.length] : C_STARLIGHT;
            particleApi.spawnColoredParticles(pt, twinkleColor, size, 1, 0.02, 0.02, 0.02);
            if (i % 4 == 0) particleApi.spawnParticles(pt, Particle.END_ROD, 1, 0.01, 0.01, 0.01, 0.005);
        }
    }

    private void drawDomainCenterPulse(Location center, int t, double globalSpin) {
        double cycle = (t % 40) / 40.0;
        double pulseRadius = 1.5 + Math.sin(cycle * Math.PI) * 2.5;
        int points = 32;
        for (int i = 0; i < points; i++) {
            double a = Math.toRadians(i * (360.0 / points) + globalSpin);
            Color pulseColor = i % 8 == 0 ? C_ROYAL_AMETHYST : C_ORB_HALO;
            for (int plane = 0; plane < 3; plane++) {
                Location pt;
                if (plane == 0) pt = center.clone().add(Math.cos(a) * pulseRadius, 0, Math.sin(a) * pulseRadius);
                else if (plane == 1) pt = center.clone().add(Math.cos(a) * pulseRadius, Math.sin(a) * pulseRadius, 0);
                else pt = center.clone().add(0, Math.cos(a) * pulseRadius, Math.sin(a) * pulseRadius);
                particleApi.spawnColoredParticles(pt, pulseColor, 1.4f, 1, 0.03, 0.03, 0.03);
            }
        }
    }

    private void drawDomainSatellites(Location center, double maxRadius, int t, double globalSpin) {
        int satelliteCount = 6;
        Color[] satelliteColors = { C_ORB_CYAN, C_ORB_PINK, C_ELDRITCH_GREEN, C_ROYAL_AMETHYST, C_ARCANE_TEAL, C_BLOOD_GARNET };
        for (int s = 0; s < satelliteCount; s++) {
            double orbitRadius = maxRadius * (0.4 + 0.5 * (s / (double) satelliteCount));
            double phase = s * 70;
            double height = Math.sin(t * 0.05 + s) * maxRadius * 0.25;
            double a = Math.toRadians(globalSpin + phase);
            Location satCenter = center.clone().add(Math.cos(a) * orbitRadius, height, Math.sin(a) * orbitRadius);

            Color c = satelliteColors[s % satelliteColors.length];
            particleApi.spawnColoredParticles(satCenter, c, 1.6f, 4, 0.18, 0.18, 0.18);

            for (int trailStep = 1; trailStep <= 3; trailStep++) {
                double trailA = Math.toRadians(globalSpin - trailStep * 5 + phase);
                Location trailPt = center.clone().add(Math.cos(trailA) * orbitRadius, height, Math.sin(trailA) * orbitRadius);
                float trailSize = (float) (1.1 - trailStep * 0.2);
                particleApi.spawnColoredParticles(trailPt, c, trailSize, 1, 0.06, 0.06, 0.06);
            }
        }
    }

    private void drawDomainOrbitingAsteroids(Location center, double maxRadius, int t, double globalSpin) {
        int asteroidCount = 10;
        Color[] asteroidColors = {
                C_MYSTIC_SILVER, C_ORB_CYAN, C_ROYAL_AMETHYST, C_ELDRITCH_GREEN, C_BLOOD_GARNET,
                C_ORB_PINK, C_ARCANE_TEAL, C_EMBER_GOLD, C_STARLIGHT, C_DEEP_VIOLET
        };

        double[] sizeScale = { 1.8, 0.7, 1.3, 1.0, 2.2, 0.6, 1.5, 0.9, 1.1, 1.7 };

        double[] radiusOffset = { 0, -2.5, 1.5, -1.0, 3.0, -3.5, 0.5, 2.0, -1.5, 1.0 };

        double[] tiltDeg = { 12, -8, 16, 5, -14, 10, -6, 18, 3, -11 };

        for (int i = 0; i < asteroidCount; i++) {
            double phase = i * (360.0 / asteroidCount);
            double orbitRadius = maxRadius + radiusOffset[i];
            double orbitTilt = Math.toRadians(tiltDeg[i]);

            double speedFactor = 0.7 + (i % 4) * 0.08;
            double a = Math.toRadians(globalSpin * speedFactor + phase);
            double x = Math.cos(a) * orbitRadius;
            double z = Math.sin(a) * orbitRadius;
            double y = Math.sin(a) * orbitRadius * Math.sin(orbitTilt);
            Location asteroidCenter = center.clone().add(x, y, z);

            Color c = asteroidColors[i % asteroidColors.length];
            double asteroidSize = sizeScale[i];

            int shellPoints = asteroidSize > 1.4 ? 14 : 8;
            for (int j = 0; j < shellPoints; j++) {
                double sa = Math.toRadians(j * (360.0 / shellPoints) + t * 8);
                Location p1 = asteroidCenter.clone().add(Math.cos(sa) * asteroidSize, Math.sin(sa) * asteroidSize, 0);
                Location p2 = asteroidCenter.clone().add(Math.cos(sa) * asteroidSize, 0, Math.sin(sa) * asteroidSize);
                float pointSize = (float) (1.0 + asteroidSize * 0.25);
                particleApi.spawnColoredParticles(p1, c, pointSize, 2, 0.04, 0.04, 0.04);
                particleApi.spawnColoredParticles(p2, c, pointSize, 2, 0.04, 0.04, 0.04);
            }

            particleApi.spawnColoredParticles(asteroidCenter, c, (float) (1.4 + asteroidSize * 0.5), (int) (2 + asteroidSize), 0.06, 0.06, 0.06);

            double trailA = Math.toRadians(globalSpin * speedFactor - 8 + phase);
            Location trailPt = center.clone().add(
                    Math.cos(trailA) * orbitRadius,
                    Math.sin(trailA) * orbitRadius * Math.sin(orbitTilt),
                    Math.sin(trailA) * orbitRadius);
            particleApi.spawnColoredParticles(trailPt, c, (float) (0.7 + asteroidSize * 0.2), 1, 0.05, 0.05, 0.05);
        }
    }

    private void drawDomainPlanets(Location center, double maxRadius, int t, double globalSpin) {
        int planetCount = 4;
        double[] orbitFrac  = { 0.32, 0.52, 0.72, 0.92 };
        Color[] planetColors = { C_EMBER_GOLD, C_ORB_CYAN, C_ROYAL_AMETHYST, C_BLOOD_GARNET };
        double[] planetSize  = { 1.6, 2.4, 3.0, 1.3 };
        double[] speedFactor = { 0.62, 0.5, 0.4, 0.34 };
        double[] phaseDeg    = { 0, 95, 200, 300 };
        double[] tiltDeg     = { 9, 16, -12, 6 };
        boolean[] hasRing    = { false, true, true, false };

        for (int i = 0; i < planetCount; i++) {
            double orbitRadius = maxRadius * orbitFrac[i];
            double tilt = Math.toRadians(tiltDeg[i]);
            double a = Math.toRadians(globalSpin * speedFactor[i] + phaseDeg[i]);
            double x = Math.cos(a) * orbitRadius;
            double z = Math.sin(a) * orbitRadius;
            double y = Math.sin(a) * orbitRadius * Math.sin(tilt);
            Location pc = center.clone().add(x, y, z);
            Color c = planetColors[i];
            double size = planetSize[i];

            int latLayers = 4;
            int ptsPerLat = 9;
            for (int l = 0; l <= latLayers; l++) {
                double polar = Math.toRadians(l * (180.0 / latLayers));
                for (int j = 0; j < ptsPerLat; j++) {
                    double az = Math.toRadians(j * (360.0 / ptsPerLat) + t * 7);
                    double px = Math.sin(polar) * Math.cos(az) * size;
                    double py = Math.cos(polar) * size;
                    double pz = Math.sin(polar) * Math.sin(az) * size;
                    particleApi.spawnColoredParticles(pc.clone().add(px, py, pz), c, 1.2f, 1, 0.02, 0.02, 0.02);
                }
            }

            particleApi.spawnColoredParticles(pc, c, (float) (1.3 + size * 0.3), (int) (2 + size), 0.05, 0.05, 0.05);

            if (hasRing[i]) {
                Vector ringNormal = new Vector(Math.sin(tilt), Math.cos(tilt), 0);
                Vector ringA = ringNormal.clone().crossProduct(new Vector(0, 1, 0));
                if (ringA.lengthSquared() < 0.001) ringA = new Vector(1, 0, 0);
                ringA.normalize();
                Vector ringB = ringNormal.clone().crossProduct(ringA).normalize();
                int ringPts = 26;
                double ringR = size * 1.8;
                for (int j = 0; j < ringPts; j++) {
                    double ra = Math.toRadians(j * (360.0 / ringPts) + t * 5);
                    Location rp = pc.clone()
                            .add(ringA.clone().multiply(Math.cos(ra) * ringR))
                            .add(ringB.clone().multiply(Math.sin(ra) * ringR));
                    Color ringColor = j % 4 == 0 ? C_MYSTIC_SILVER : c;
                    particleApi.spawnColoredParticles(rp, ringColor, 0.9f, 1, 0.02, 0.02, 0.02);
                }
            }

            double trailA = Math.toRadians(globalSpin * speedFactor[i] - 6 + phaseDeg[i]);
            Location trailPt = center.clone().add(
                    Math.cos(trailA) * orbitRadius,
                    Math.sin(trailA) * orbitRadius * Math.sin(tilt),
                    Math.sin(trailA) * orbitRadius);
            particleApi.spawnColoredParticles(trailPt, c, (float) (0.7 + size * 0.15), 1, 0.05, 0.05, 0.05);
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

                int ritualPlanetCount = 3 + (int) (progress * 5);
                drawRitualOrbitingPlanets(anchor, curHeight, progress, t, ritualPlanetCount);

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

        drawShatteringPlanetFragments(center, maxRadius);

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

    private void drawShatteringPlanetFragments(Location center, double maxRadius) {
        Random r = new Random();
        int fragmentCount = 7;
        Color[] fragColors = MYSTIC_COLORS;

        for (int f = 0; f < fragmentCount; f++) {
            double theta = r.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * r.nextDouble() - 1);
            Vector flyDir = new Vector(Math.sin(phi) * Math.cos(theta), Math.cos(phi) * 0.5, Math.sin(phi) * Math.sin(theta));
            double speed = 0.4 + r.nextDouble() * 0.3;
            double size = 0.8 + r.nextDouble() * 1.4;
            Color c = fragColors[f % fragColors.length];

            new BukkitRunnable() {
                Location loc = center.clone();
                int t = 0;
                @Override
                public void run() {
                    if (t >= 26 || loc.distance(center) > maxRadius * 0.8) { cancel(); return; }
                    int shellPts = 5;
                    for (int j = 0; j < shellPts; j++) {
                        double sa = Math.toRadians(j * 72 + t * 20);
                        Location sp = loc.clone().add(Math.cos(sa) * size * 0.3, Math.sin(sa) * size * 0.3, 0);
                        particleApi.spawnColoredParticles(sp, c, (float) size, 1, 0.02, 0.02, 0.02);
                    }
                    particleApi.spawnColoredParticles(loc, c, (float) (size * 1.1), 2, 0.04, 0.04, 0.04);
                    loc.add(flyDir.clone().multiply(speed));
                    t++;
                }
            }.runTaskTimer(magicPlugin, 0, 1);
        }
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

    private void drawRitualOrbitingPlanets(Location anchor, double curHeight, double progress, int t, int planetCount) {
        Color[] planetColors = MYSTIC_COLORS;
        for (int i = 0; i < planetCount; i++) {
            double orbitRadius = 2.2 + i * 1.6;
            double speed = 9 - i * 0.7;
            double phase = i * 47;
            double a = Math.toRadians(t * speed + phase);
            double y = curHeight * (0.15 + (i % 4) * 0.22);
            Location planetCenter = anchor.clone().add(Math.cos(a) * orbitRadius, y, Math.sin(a) * orbitRadius);
            Color c = planetColors[i % planetColors.length];
            float size = 0.9f + (i % 3) * 0.35f;

            int shellPts = 6;
            for (int j = 0; j < shellPts; j++) {
                double sa = Math.toRadians(j * 60 + t * 12);
                Location sp = planetCenter.clone().add(Math.cos(sa) * size * 0.3, Math.sin(sa) * size * 0.3, 0);
                particleApi.spawnColoredParticles(sp, c, size * 0.8f, 1, 0.02, 0.02, 0.02);
            }
            particleApi.spawnColoredParticles(planetCenter, c, size * 1.2f, 2, 0.04, 0.04, 0.04);

            double trailA = Math.toRadians(t * speed - 10 + phase);
            Location trailPt = anchor.clone().add(Math.cos(trailA) * orbitRadius, y, Math.sin(trailA) * orbitRadius);
            particleApi.spawnColoredParticles(trailPt, c, size * 0.6f, 1, 0.03, 0.03, 0.03);
        }
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
        if (domainTask != null) {
            try { domainTask.cancel(); } catch (Exception ignored) {}
            domainTask = null;
        }
        for (BukkitRunnable task : phase3Tasks) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        phase3Tasks.clear();
        if (getOwner() != null && getOwner().isOnline()) {
            getOwner().setWalkSpeed(0.2f);
        }
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&5&lCosmic Annihilation";
            case 1: return "&b&lComictest";
            default: return "&7none";
        }
    }

    private List<Player> getNearbyPlayers(Location center, double radius) {
        List<Player> result = new ArrayList<>();
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof Player) result.add((Player) e);
        }
        return result;
    }
}
