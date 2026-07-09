package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import net.trduc.magicabilitiesfork.data.MessagesManager;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class LunarPower extends Power implements IdlePower, Removeable {

    private static final String L_CRESCENT = "lunar.crescent";
    private static final String L_SHADOW   = "lunar.shadow";
    private static final String L_TIDE     = "lunar.tide";
    private static final String L_ECLIPSE  = "lunar.eclipse";
    private static final String L_DOMAIN   = "lunar.domain";

    private static final Color C_WHITE     = Color.fromRGB(230, 240, 255);
    private static final Color C_SILVER    = Color.fromRGB(180, 200, 230);
    private static final Color C_BLUE_PALE = Color.fromRGB(140, 170, 220);
    private static final Color C_PURPLE    = Color.fromRGB(160, 130, 210);
    private static final Color C_GOLD_MOON = Color.fromRGB(220, 200, 100);
    private static final Color[] LUNAR_COLS = {C_WHITE, C_SILVER, C_BLUE_PALE};

    private boolean domainActive = false;
    private boolean shadowActive = false;
    private BukkitRunnable domainTask = null;
    private BukkitRunnable shadowTask = null;
    private Location shadowDecoy = null;
    private final MessagesManager messages = MessagesManager.getInstance();

    public LunarPower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(L_CRESCENT, p, this)) return; lunarCrescent(p); addCd(L_CRESCENT, p); return;
            case 2: if (onCd(L_TIDE,     p, this)) return; lunarTide(p);     addCd(L_TIDE,     p); return;
            case 3: if (onCd(L_ECLIPSE,  p, this)) return; if (lunarEclipse(p)) addCd(L_ECLIPSE, p); return;
            case 4:
                if (domainActive) { sendActionBar(p, "\u00a7b\u2606 Lunar Domain is active!"); return; }
                if (onCd(L_DOMAIN, p, this)) return;
                lunarDomain(p);
                addCd(L_DOMAIN, p);
        }
    }

    private void onRight(RightClickExecute ex) {
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 1) return;
        if (shadowActive) return;
        if (onCd(L_SHADOW, p, this)) return;
        moonShadow(p);
        addCd(L_SHADOW, p);
    }

    private void lunarCrescent(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT,         0.8f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 1.8f);
        launchCrescentBlade(p, 0.0, 0L);
    }

    private void launchCrescentBlade(Player p, double yawOff, long delayTicks) {

        final Location origin = p.getEyeLocation().clone();
        final Vector baseDir  = origin.getDirection().clone().normalize();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                Vector dir = rotateY(baseDir, yawOff);
                ArmorStand blade = spawnProjectileAt(origin.clone());
                Set<Entity> hit = new HashSet<>();
                final Vector finalDir = dir;

                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (blade.isDead() || t > 50) { safeRemove(blade); cancel(); return; }
                        blade.teleport(blade.getLocation().add(finalDir.clone().multiply(1.5)));
                        Location loc = blade.getLocation();

                        Vector flatDir = finalDir.clone().setY(0).normalize();
                        Vector right = rotateY(flatDir, 90).normalize();
                        final double HALF_WIDTH = 1.75;
                        final int POINTS = 11;
                        final double OPEN_DEG = 150.0;
                        for (int i = 0; i < POINTS; i++) {
                            double t2 = (double) i / (POINTS - 1);
                            double arcRad = Math.toRadians(-OPEN_DEG / 2.0 + t2 * OPEN_DEG);

                            double ox = Math.sin(arcRad) * HALF_WIDTH;
                            double oz = (1.0 - Math.cos(arcRad)) * HALF_WIDTH * 0.55;
                            Location outer = loc.clone()
                                    .add(right.clone().multiply(ox))
                                    .add(flatDir.clone().multiply(oz));
                            Color col = LUNAR_COLS[i % LUNAR_COLS.length];
                            float size = (i == POINTS / 2) ? 1.8f : 1.4f;
                            particleApi.spawnColoredParticles(outer, col, size, 1, 0.02, 0.02, 0.02);

                            double ix = Math.sin(arcRad) * HALF_WIDTH * 0.75;
                            double iz = (1.0 - Math.cos(arcRad)) * HALF_WIDTH * 0.55 + 0.3;
                            Location inner = loc.clone()
                                    .add(right.clone().multiply(ix))
                                    .add(flatDir.clone().multiply(iz));
                            particleApi.spawnColoredParticles(inner, C_WHITE, 1.1f, 1, 0.02, 0.02, 0.02);
                        }
                        if (t % 2 == 0)
                            particleApi.spawnColoredParticles(loc, C_SILVER, 1.0f, 2, 0.06, 0.06, 0.06);

                        for (Entity e : loc.getWorld().getNearbyEntities(loc, HALF_WIDTH + 0.3, 1.0, HALF_WIDTH + 0.3)) {
                            if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                            if (!(e instanceof LivingEntity)) continue;
                            hit.add(e);
                            ((LivingEntity) e).damage(11, p);
                            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 40, 1);
                            spawnLunarBurst(e.getLocation().clone().add(0, 1, 0));
                            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 1.4f);
                        }
                        if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                            spawnLunarBurst(loc); safeRemove(blade); cancel(); return;
                        }
                        t++;
                    }
                }.runTaskTimer(magicPlugin, 0, 1);
            }
        }.runTaskLater(magicPlugin, delayTicks);
    }

    private void moonShadow(Player p) {
        shadowActive = true;
        shadowDecoy = p.getLocation().clone();
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT,   0.7f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 1.6f);
        spawnLunarBurst(p.getLocation().clone().add(0, 1, 0));

        Vector dashDir = p.getEyeLocation().getDirection().clone().setY(0.15).normalize();
        p.setVelocity(dashDir.multiply(2.5));

        final Location decoyLoc = shadowDecoy.clone();
        new BukkitRunnable() {
            @Override public void run() {
                spawnLunarBurst(decoyLoc.clone().add(0, 1, 0));
                p.getWorld().playSound(decoyLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
            }
        }.runTaskLater(magicPlugin, 4L);

        shadowTask = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 60 || !p.isOnline()) { clearShadow(); cancel(); return; }
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(t * 15.0 + i * 45.0);
                    Location pt = decoyLoc.clone().add(
                            Math.cos(a) * 0.5, 0.5 + Math.sin(t * 0.15) * 0.3, Math.sin(a) * 0.5);
                    particleApi.spawnColoredParticles(pt, C_BLUE_PALE, 1.1f, 1, 0.03, 0.03, 0.03);
                }
                particleApi.spawnColoredParticles(decoyLoc.clone().add(0, 1, 0), C_WHITE, 1.0f, 1, 0.12, 0.2, 0.12);

                for (Entity e : decoyLoc.getWorld().getNearbyEntities(decoyLoc, 1.5, 2, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(16, p);
                    applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 60, 0);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS,  50, 2);
                    spawnLunarBurst(decoyLoc.clone().add(0, 1, 0));
                    e.getWorld().playSound(decoyLoc, Sound.ENTITY_ENDERMAN_HURT, 0.8f, 1.5f);
                    clearShadow(); cancel(); return;
                }
                t++;
            }
        };
        shadowTask.runTaskTimer(magicPlugin, 5L, 1L);
    }

    private void clearShadow() {
        shadowActive = false;
        shadowDecoy  = null;
        if (shadowTask != null) { try { shadowTask.cancel(); } catch (Exception ignored) {} shadowTask = null; }
    }

    private void lunarTide(Player p) {
        Location center = p.getLocation().clone().add(0, 0.5, 0);
        p.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 0.5f);
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK,  0.6f, 0.6f);
        Set<Entity> tidePulled = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 30) { cancel(); tidePush(p, center, tidePulled); return; }

                int steps = 40;
                for (int i = 0; i < steps; i++) {
                    double a = Math.toRadians(i * (360.0 / steps) + t * 12);
                    double r = 8.0 * (1.0 - t / 35.0);
                    Location pt = center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                    particleApi.spawnColoredParticles(pt, C_SILVER, 1.0f, 1, 0.02, 0.02, 0.02);
                }
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 30 + i * 120);
                    Location pt = center.clone().add(Math.cos(a) * 2.5, 0.3, Math.sin(a) * 2.5);
                    particleApi.spawnColoredParticles(pt, C_WHITE, 1.3f, 1, 0.03, 0.03, 0.03);
                }

                for (Entity e : center.getWorld().getNearbyEntities(center, 8, 8, 8)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    tidePulled.add(e);
                    Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.22);
                    e.setVelocity(e.getVelocity().add(pull));
                    applyPotionSilent((LivingEntity) e, PotionEffectType.SLOWNESS, 5, 1);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void tidePush(Player p, Location center, Set<Entity> pulled) {
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE,      0.8f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.5f);
        particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_WHITE,  2.0f, 120, 3.5, 2.0, 3.5);
        particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_SILVER, 1.5f, 80,  3.0, 1.8, 3.0);

        for (Entity e : center.getWorld().getNearbyEntities(center, 9, 9, 9)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(center));
            ((LivingEntity) e).damage(Math.max(8, 18 - dist * 1.5), p);
            Vector push = e.getLocation().subtract(center).toVector().normalize()
                    .multiply(2.8 + (pulled.contains(e) ? 1.5 : 0)).setY(0.7);
            e.setVelocity(isVecFinite(push) ? push : new Vector(0, 0.7, 0));
            spawnLunarBurst(e.getLocation().clone().add(0, 1, 0));
        }
    }

    private boolean lunarEclipse(Player p) {
        org.bukkit.util.RayTraceResult ray = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                30,
                0.5,
                e -> e instanceof LivingEntity && !e.equals(p) && !(e instanceof ArmorStand)
        );

        if (ray == null || !(ray.getHitEntity() instanceof LivingEntity)) {
            sendActionBar(p, "\u00a77No target in sight for Lunar Eclipse.");
            return false;
        }
        final LivingEntity target = (LivingEntity) ray.getHitEntity();

        sendActionBar(p, "\u00a75\u00a7l\u2726 Lunar Eclipse \u2726");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.4f);
        p.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.7f, 0.3f);

        final int HOVER_HEIGHT = 4;
        final int DESCEND_TICKS = 10;
        final int HOVER_TICKS = 100;
        final int STACK_INTERVAL = 20;

        Location startLoc = target.getEyeLocation().clone().add(0, HOVER_HEIGHT + 6, 0);
        final ArmorStand moon = spawnProjectileAt(startLoc);

        new BukkitRunnable() {
            int t = 0;
            int stack = 0;

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || !p.isOnline() ||
                        target.getLocation().distance(p.getLocation()) > 24) {
                    safeRemove(moon);
                    cancel();
                    return;
                }

                Location aboveHead = target.getEyeLocation().clone().add(0, HOVER_HEIGHT, 0);

                if (t < DESCEND_TICKS) {
                    double prog = (double) (t + 1) / DESCEND_TICKS;
                    Location cur = startLoc.clone().add(
                            0, -(startLoc.getY() - aboveHead.getY()) * prog, 0);
                    moon.teleport(cur);
                    drawMiniMoon(cur, 0, t);
                    t++;
                    return;
                }

                int hoverT = t - DESCEND_TICKS;
                moon.teleport(aboveHead);
                drawMiniMoon(aboveHead, stack, hoverT);

                if (hoverT > 0 && hoverT % STACK_INTERVAL == 0 && stack < MAX_ECLIPSE_STACK) {
                    stack++;
                    p.getWorld().playSound(aboveHead, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.6f, 1.0f + stack * 0.15f);
                }

                if (hoverT % 20 == 0 && hoverT > 0) {
                    int slowAmp = Math.min(3, stack / 2);
                    int weakAmp = Math.min(2, (stack - 1) / 2);
                    double dot  = 2.0 + stack * 1.5;

                    applyPotion(target, PotionEffectType.DARKNESS,  30, 0);
                    applyPotion(target, PotionEffectType.SLOWNESS,  30, slowAmp);
                    if (stack >= 2) applyPotion(target, PotionEffectType.WEAKNESS, 30, weakAmp);
                    target.damage(dot, p);
                    spawnLunarBurst(target.getLocation().clone().add(0, 1, 0));
                }

                if (hoverT >= HOVER_TICKS || (stack >= MAX_ECLIPSE_STACK && hoverT >= HOVER_TICKS - STACK_INTERVAL)) {
                    eclipseBurst(p, target, aboveHead, stack);
                    safeRemove(moon);
                    cancel();
                    return;
                }

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        return true;
    }

    private static final int MAX_ECLIPSE_STACK = 5;

    private void drawMiniMoon(Location center, int stack, int tickAnim) {
        double discR = 0.55 + stack * 0.05;
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(i * 36.0);
            Location pt = center.clone().add(Math.cos(a) * discR, Math.sin(a) * discR * 0.25, 0);
            particleApi.spawnColoredParticles(pt, C_PURPLE, 1.0f, 1, 0.02, 0.02, 0.02);
        }
        particleApi.spawnColoredParticles(center, Color.fromRGB(20, 10, 35), 1.4f + stack * 0.1f, 2, 0.05, 0.05, 0.05);

        int coronaPts = 8 + stack * 2;
        for (int i = 0; i < coronaPts; i++) {
            double a = Math.toRadians(i * (360.0 / coronaPts) + tickAnim * 9.0);
            double r = discR + 0.25;
            Location pt = center.clone().add(Math.cos(a) * r, Math.sin(a) * r * 0.25, Math.sin(a + 1.3) * 0.05);
            Color coronaCol = stack >= MAX_ECLIPSE_STACK ? C_GOLD_MOON : C_WHITE;
            particleApi.spawnColoredParticles(pt, coronaCol, 0.9f, 1, 0.02, 0.02, 0.02);
        }
    }

    private void eclipseBurst(Player p, LivingEntity target, Location at, int stack) {
        double dmg = 10.0 + stack * 6.0;
        target.damage(dmg, p);

        Vector kb = new Vector(0, 0.4 + stack * 0.1, 0);
        target.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.4, 0));

        p.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE,      0.9f, 0.4f);
        p.getWorld().playSound(at, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.3f + stack * 0.1f);

        particleApi.spawnColoredParticles(at, C_PURPLE,    2.0f, 60 + stack * 15, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(at, C_WHITE,     1.6f, 40 + stack * 10, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(at, C_GOLD_MOON, 1.4f, 20 + stack * 8,  0.6, 0.6, 0.6);

        sendActionBar(p, "\u00a75\u00a7l\u2726 Eclipse collapsed (" + stack + "/" + MAX_ECLIPSE_STACK + " stacks)!");
    }

    private void lunarDomain(Player p) {
        domainActive = true;
        Location center = p.getLocation().clone();

        p.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("powers.lunar.celestial_moon")));
        p.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.9f, 0.3f);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_AMBIENT,       0.8f, 0.4f);
        p.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE,       1.0f, 0.5f);

        double[][] rings = {
                { 8,  0.0},
                {20,  2.5},
                { 8,  5.0},
                {24,  7.5},
                {10, 10.0},
                {28, 12.5},
                {10, 15.0},
                {22, 17.5},
                { 8, 20.0},
                {18, 22.5},
                { 6, 25.0},
                {14, 27.5},
                { 5, 30.0},
        };

        int[] orbRings = {1, 3, 5, 7, 9};

        Set<Entity> damaged = new HashSet<>();

        for (int ri = 0; ri < rings.length; ri++) {
            final double[] ring = rings[ri];
            final long revealTick = ri * 6L;
            new BukkitRunnable() {
                @Override public void run() {
                    if (!p.isOnline()) return;
                    double radius = ring[0];
                    double yOff   = ring[1];
                    Location rc = center.clone().add(0, yOff, 0);
                    for (int flash = 0; flash < 4; flash++) {
                        final int f = flash;
                        new BukkitRunnable() {
                            @Override public void run() {
                                int steps = (int)(radius * 10);
                                for (int i = 0; i < steps; i++) {
                                    double a = Math.toRadians(i * (360.0 / steps));
                                    particleApi.spawnColoredParticles(
                                            rc.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                                            f < 2 ? C_WHITE : C_SILVER, 1.6f, 2, 0.02, 0.03, 0.02);
                                }
                                p.getWorld().playSound(rc, Sound.BLOCK_AMETHYST_BLOCK_BREAK,
                                        0.4f, 0.9f + f * 0.15f);
                            }
                        }.runTaskLater(magicPlugin, flash * 4L);
                    }
                }
            }.runTaskLater(magicPlugin, revealTick);
        }

        final int EXPAND_TICKS = rings.length * 6 + 8;

        domainTask = new BukkitRunnable() {
            int t = 0;
            final int DURATION = 20 * 20;

            @Override
            public void run() {
                if (t >= DURATION || !p.isOnline()) { domainEnd(p, center); cancel(); return; }

                for (int ri = 0; ri < rings.length; ri++) {
                    double radius = rings[ri][0];
                    double yOff   = rings[ri][1];
                    Location ringCenter = center.clone().add(0, yOff, 0);

                    int ringSteps = (int)(radius * 7);

                    double spinSpeed = radius < 12 ? 5.0 : (radius < 20 ? 3.0 : 2.0);

                    double spinDir = ri % 2 == 0 ? 1 : -1;
                    for (int layer = 0; layer < 2; layer++) {
                        double rOff = layer == 0 ? 0 : 0.2;
                        for (int i = 0; i < ringSteps; i++) {
                            double spin = spinDir * t * spinSpeed;
                            double a = Math.toRadians(i * (360.0 / ringSteps) + spin);

                            Color col;
                            if (layer == 1) { col = C_SILVER; }
                            else if (radius >= 24) { col = C_GOLD_MOON; }
                            else if (radius < 12)  { col = C_BLUE_PALE; }
                            else                   { col = C_WHITE; }
                            float size = layer == 0 ? 1.4f : 1.0f;
                            particleApi.spawnColoredParticles(
                                    ringCenter.clone().add(Math.cos(a) * (radius + rOff), rOff * 0.3, Math.sin(a) * (radius + rOff)),
                                    col, size, 1, 0.01, 0.01, 0.01);
                        }
                    }

                    boolean hasOrbs = false;
                    for (int or2 : orbRings) { if (or2 == ri) { hasOrbs = true; break; } }
                    if (hasOrbs) {
                        int orbCount = ri <= 4 ? 3 : 2;
                        for (int o = 0; o < orbCount; o++) {
                            double spin = ri % 2 == 0 ? t * 3.5 : -t * 3.5;
                            double orbAngle = Math.toRadians(spin + o * (360.0 / orbCount));
                            Location orb = ringCenter.clone().add(
                                    Math.cos(orbAngle) * radius, 0, Math.sin(orbAngle) * radius);

                            particleApi.spawnColoredParticles(orb, C_GOLD_MOON, 2.0f, 5, 0.0,  0.0,  0.0);
                            particleApi.spawnColoredParticles(orb, C_WHITE,     1.7f, 4, 0.0,  0.0,  0.0);

                            for (int s = 0; s < 8; s++) {
                                double sa = Math.toRadians(s * 45 + t * 25);
                                double sr = 0.5;
                                Location sp = orb.clone().add(
                                        Math.cos(sa) * sr, Math.sin(t * 0.1 + s) * 0.2, Math.sin(sa) * sr);
                                particleApi.spawnColoredParticles(sp, C_SILVER, 1.3f, 1, 0.02, 0.02, 0.02);
                            }

                            if (t % 2 == 0) {
                                particleApi.spawnColoredParticles(orb, C_BLUE_PALE, 1.0f, 3, 0.1, 0.1, 0.1);
                            }

                            for (Entity e : orb.getWorld().getNearbyEntities(orb, 1.4, 1.4, 1.4)) {
                                if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                                if (damaged.contains(e)) continue;
                                damaged.add(e);
                                ((LivingEntity) e).damage(6, p);
                                spawnLunarBurst(e.getLocation().clone().add(0, 1, 0));
                                e.getWorld().playSound(e.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.6f);
                                final Entity fe = e;
                                new BukkitRunnable() {
                                    @Override public void run() { damaged.remove(fe); }
                                }.runTaskLater(magicPlugin, 30L);
                            }
                        }
                    }
                }

                for (int layer = 0; layer < 3; layer++) {
                    double a = Math.toRadians(t * 9 + layer * 120);
                    double h = (t % 30) * 0.55;
                    if (h <= 15.0) {
                        particleApi.spawnColoredParticles(
                                center.clone().add(Math.cos(a) * 1.2, h, Math.sin(a) * 1.2),
                                C_WHITE, 1.6f, 1, 0.02, 0.02, 0.02);
                        if (t % 3 == 0)
                            particleApi.spawnColoredParticles(
                                    center.clone().add(Math.cos(a) * 0.7, h + 0.4, Math.sin(a) * 0.7),
                                    C_GOLD_MOON, 1.3f, 1, 0.02, 0.02, 0.02);
                    }
                }

                if (t % 2 == 0) {
                    int borderPts = 90;
                    for (int b = 0; b < borderPts; b++) {
                        double ba = Math.toRadians(b * (360.0 / borderPts) + t * 1.8);
                        Location bl = center.clone().add(Math.cos(ba) * 30, 0.15, Math.sin(ba) * 30);
                        particleApi.spawnColoredParticles(bl, C_SILVER, 1.5f, 1, 0.01, 0.1, 0.01);
                        if (b % 9 == 0)
                            particleApi.spawnColoredParticles(bl.clone().add(0, 0.6, 0), C_WHITE, 1.8f, 1, 0.01, 0.06, 0.01);
                    }
                }

                for (Entity e : center.getWorld().getNearbyEntities(center, 32, 16, 32)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    applyPotionSilent((LivingEntity) e, PotionEffectType.SLOWNESS, 10, 1);
                    if (e.getLocation().distance(center) > 29) {
                        Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.4);
                        e.setVelocity(e.getVelocity().add(pull));
                    }
                }

                if (t % 40 == 0)
                    p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 1.1f);
                if (t == DURATION - 100)
                    sendActionBar(p, "\u00a7b\u263d Celestial Moon Formation - 5s...");
                t++;
            }
        };
        domainTask.runTaskTimer(magicPlugin, EXPAND_TICKS, 1);
    }

    private void domainEnd(Player p, Location center) {
        domainActive = false;
        if (domainTask != null) { try { domainTask.cancel(); } catch (Exception ignored) {} domainTask = null; }

        sendActionBar(p, "\u00a7b\u263d Moon Collapse!");
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE,  1.0f, 0.3f);
        p.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.9f, 0.4f);

        double[][] rings = {
                { 8,  0.0}, {20,  2.5}, { 8,  5.0}, {24,  7.5},
                {10, 10.0}, {28, 12.5}, {10, 15.0}, {22, 17.5},
                { 8, 20.0}, {18, 22.5}, { 6, 25.0}, {14, 27.5}, { 5, 30.0}
        };

        new BukkitRunnable() {
            int t = 0;
            final int COLLAPSE_TICKS = 40;

            @Override
            public void run() {
                if (t >= COLLAPSE_TICKS) {
                    cancel();
                    moonCollapseExplosion(p, center);
                    return;
                }

                double progress = (double) t / COLLAPSE_TICKS;

                for (int ri = 0; ri < rings.length; ri++) {
                    double startRadius = rings[ri][0];
                    double yOff        = rings[ri][1];

                    double curRadius = startRadius * (1.0 - progress);
                    double curY      = yOff * (1.0 - progress);

                    if (curRadius < 0.5) continue;

                    Location ringCenter = center.clone().add(0, curY, 0);
                    int steps = (int)(curRadius * 7);

                    double spin = ri % 2 == 0 ? t * 6.0 : -t * 6.0;
                    for (int i = 0; i < steps; i++) {
                        double a = Math.toRadians(i * (360.0 / steps) + spin);

                        float size = 1.2f + (float) progress * 0.8f;
                        Color col  = progress > 0.6 ? C_GOLD_MOON : C_WHITE;
                        particleApi.spawnColoredParticles(
                                ringCenter.clone().add(Math.cos(a) * curRadius, 0, Math.sin(a) * curRadius),
                                col, size, 1, 0.01, 0.01, 0.01);
                    }

                    if (t % 3 == 0) {
                        double ta = Math.toRadians(t * 20 + ri * 40);
                        particleApi.spawnColoredParticles(
                                ringCenter.clone().add(Math.cos(ta) * curRadius, 0, Math.sin(ta) * curRadius),
                                C_SILVER, 1.5f, 3, 0.05, 0.1, 0.05);
                    }
                }

                if (t % 8 == 0)
                    p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK,
                            0.6f, 0.5f + (float) progress * 1.0f);

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void moonCollapseExplosion(Player p, Location center) {

        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE,      1.0f, 0.3f);
        p.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.2f);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM,    1.0f, 0.4f);
        p.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER,        0.9f, 0.6f);

        particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_WHITE,     2.8f, 300, 18, 10, 18);
        particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_GOLD_MOON, 2.4f, 200, 14,  8, 14);
        particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_SILVER,    2.0f, 150, 10,  6, 10);

        new BukkitRunnable() {
            double r = 1;
            int wave = 0;
            @Override public void run() {
                if (r > 32) { cancel(); return; }
                int pts = (int)(r * 8);
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(i * (360.0 / pts));
                    particleApi.spawnColoredParticles(
                            center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r),
                            wave % 2 == 0 ? C_WHITE : C_GOLD_MOON, 1.8f, 1, 0.01, 0.05, 0.01);
                }
                r += 2.5;
                wave++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        List<LivingEntity> inRange = new ArrayList<>();
        for (Entity e : center.getWorld().getNearbyEntities(center, 32, 16, 32)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            inRange.add((LivingEntity) e);
        }

        for (LivingEntity e : inRange) {
            double dist = Math.max(1, e.getLocation().distance(center));
            double dmg  = 35.0 * Math.max(0.3, 1.0 - dist / 36.0);
            e.damage(dmg, p);

            Vector radial = e.getLocation().subtract(center).toVector().normalize().multiply(1.5);
            Vector kb = radial.setY(2.8);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 2.8, 0));
            spawnLunarBurst(e.getLocation().clone().add(0, 1, 0));
        }

        sendActionBar(p, "\u00a77\u263e Celestial Moon Formation — Moon Collapse!");
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                boolean night = isNight(p);

                if (night) {
                    applyPotionSilent(p, PotionEffectType.NIGHT_VISION, 400, 0);
                    applyPotionSilent(p, PotionEffectType.SPEED,        30, 0);
                    if (t % 5 == 0) safeHeal(p, 0.5);
                }

                if (isAuraEnabled(p)) {
                    particleCircle(p.getLocation().clone().add(0, 0.07, 0),
                            0.75, night ? C_GOLD_MOON : C_SILVER, 1.0f, 7, t * 18);
                    if (night && t % 2 == 0) {
                        double a = Math.toRadians(t * 25);
                        particleApi.spawnColoredParticles(
                                p.getLocation().clone().add(Math.cos(a) * 0.8, 1.0 + Math.sin(t * 0.1) * 0.3, Math.sin(a) * 0.8),
                                C_WHITE, 1.2f, 1, 0.02, 0.02, 0.02);
                    }
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 20);
        return r;
    }

    @Override
    public void remove() {
        domainActive = false;
        shadowActive = false;
        shadowDecoy  = null;
        if (domainTask != null) { try { domainTask.cancel(); } catch (Exception ignored) {} domainTask = null; }
        if (shadowTask != null) { try { shadowTask.cancel(); } catch (Exception ignored) {} shadowTask = null; }
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&f月牙斩";
            case 1: return "&f月影";
            case 2: return "&f月潮";
            case 3: return "&f月蚀";
            case 4: return "&f&l天月阵";
            default: return "&7none";
        }
    }

    private void spawnLunarBurst(Location loc) {
        particleApi.spawnColoredParticles(loc, C_WHITE,     1.4f, 20, 0.3,  0.3,  0.3);
        particleApi.spawnColoredParticles(loc, C_SILVER,    1.1f, 12, 0.35, 0.3,  0.35);
        particleApi.spawnColoredParticles(loc, C_BLUE_PALE, 0.9f,  8, 0.25, 0.25, 0.25);
    }

    private boolean isNight(Player p) {
        long time = p.getWorld().getTime();
        return !(time < 12300 || time > 23850);
    }
}

