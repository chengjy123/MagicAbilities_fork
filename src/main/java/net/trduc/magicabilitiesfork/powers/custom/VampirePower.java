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

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
import static net.trduc.magicabilitiesfork.cooldowns.CooldownApi.isOnCooldown;

public class VampirePower extends Power implements IdlePower, Removeable {

    private static final String v_lance   = "vampire.lance";
    private static final String v_dash    = "vampire.dash";
    private static final String v_nova    = "vampire.nova";
    private static final String v_grip    = "vampire.grip";
    private static final String v_swarm   = "vampire.swarm";
    private static final String v_mist    = "vampire.mist";
    private static final String v_leap    = "vampire.leap";
    private static final String v_moon    = "vampire.moon";
    private static final String v_instinct= "vampire.instinct";

    private int XP_MOON;

    private static final Color C_BLOOD      = Color.fromRGB(180, 0,   20);
    private static final Color C_CRIMSON    = Color.fromRGB(220, 20,  60);
    private static final Color C_DARK_RED   = Color.fromRGB(100, 0,   10);
    private static final Color C_BLACK_VOID = Color.fromRGB(30,  0,   0);
    private static final Color C_GOLD_MOON  = Color.fromRGB(220, 160, 20);
    private static final Color[] BLOOD_COLS = { C_BLOOD, C_CRIMSON, C_DARK_RED };

    private boolean mistActive  = false;
    private boolean moonActive  = false;
    private BukkitRunnable mistTask = null;
    private BukkitRunnable moonTask = null;

    public VampirePower(Player owner) {
        super(owner);
        XP_MOON = magicPlugin.getConfig().getInt("vampire.xp.moon", 20);
    }

    @Override
    public void executePower(Execute ex) {

        if (ex instanceof DealDamageExecute) {
            passiveLifesteal((DealDamageExecute) ex);
            return;
        }

        if (ex instanceof DamagedByExecute) {
            passiveInstinct((DamagedByExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(v_lance, p, this)) return; bloodLance(p, 1.0, 0); addCd(v_lance, p); return;
            case 1: if (onCd(v_dash,  p, this)) return; crimsonDash(p);         addCd(v_dash,  p); return;
            case 2: if (onCd(v_nova,  p, this)) return; bloodNova(p);           addCd(v_nova,  p); return;
            case 3: if (onCd(v_grip,  p, this)) return; deathGrip(p);           addCd(v_grip,  p); return;
            case 4: if (onCd(v_swarm, p, this)) return; batSwarm(p);            addCd(v_swarm, p); return;
            case 7:
                if (onCd(v_moon, p, this)) return;
                if (!checkXp(p, XP_MOON, this)) return;
                spendXp(p, XP_MOON);
                bloodMoon(p);
                addCd(v_moon, p);
                return;
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (mistActive) return;
        if (onCd(v_mist, p, this)) return;
        mistForm(p);
        addCd(v_mist, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(v_leap, p, this)) return;
        shadowLeap(p);
        addCd(v_leap, p);
    }

    private void bloodLance(Player p, double mult, int yawOff) {
        ArmorStand lance = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        if (yawOff != 0) dir = rotateY(dir, yawOff);
        final Vector fDir = dir;
        Random rng = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 0.4f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (lance.isDead() || t > 70) { safeRemove(lance); cancel(); return; }
                lance.teleport(lance.getLocation().add(fDir.clone().multiply(1.6)));
                Location loc = lance.getLocation();

                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 55 + i * 120);
                    Vector off = new Vector(Math.cos(a) * 0.25, Math.sin(a) * 0.2, Math.sin(a) * 0.25);
                    particleApi.spawnColoredParticles(loc.clone().add(off),
                            BLOOD_COLS[rng.nextInt(BLOOD_COLS.length)], 1.1f, 2, 0.04, 0.04, 0.04);
                }
                if (t % 2 == 0)
                    particleApi.spawnColoredParticles(loc, C_BLACK_VOID, 1.0f, 1, 0.06, 0.06, 0.06);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        double dmg = 12 * mult;
                        ((LivingEntity) e).damage(dmg, p);
                        double heal = dmg * 0.6;
                        safeHeal(p, heal);
                        spawnBloodBurst(loc, 30);
                        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.7f, 0.6f);
                        safeRemove(lance); cancel(); return;
                    }
                }
                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    spawnBloodBurst(loc, 15);
                    safeRemove(lance); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void crimsonDash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT,  0.6f, 0.4f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0.1).normalize();
        p.setVelocity(dir.clone().multiply(2.8));

        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();
        spawnBloodBurst(p.getLocation().clone().add(0, 1, 0), 20);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 14) { cancel(); return; }
                Location loc = p.getLocation().clone().add(0, 1, 0);

                particleApi.spawnColoredParticles(loc, C_CRIMSON,    1.2f, 5, 0.25, 0.2, 0.25);
                particleApi.spawnColoredParticles(loc, C_BLACK_VOID, 1.0f, 3, 0.2, 0.15, 0.2);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    double dmg = 11;
                    ((LivingEntity) e).damage(dmg, p);
                    safeHeal(p, dmg * 0.5);
                    spawnBloodBurst(e.getLocation().clone().add(0, 1, 0), 20);
                    Vector kb = dir.clone().multiply(1.4).setY(0.35);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.35, 0));
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.6f);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void bloodNova(Player p) {
        Location center = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_HURT, 1f, 0.4f);
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 0.3f);

        Random rng = new Random();
        final Set<Entity> hit = new HashSet<>();

        new BukkitRunnable() {
            double r = 0.4;
            @Override public void run() {
                if (r > 7.0) { cancel(); return; }
                int pts = (int)(r * 12);
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(360.0 / pts * i);
                    Location pt = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    Color c = BLOOD_COLS[rng.nextInt(BLOOD_COLS.length)];
                    particleApi.spawnColoredParticles(pt, c, 1.1f, 1, 0.05, 0.05, 0.05);
                    if (rng.nextInt(3) == 0)
                        particleApi.spawnColoredParticles(pt, C_BLACK_VOID, 0.9f, 1, 0.04, 0.04, 0.04);
                }

                for (Entity e : center.getWorld().getNearbyEntities(center, r + 0.6, 2, r + 0.6)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(center);
                    if (dist > r - 0.5 && dist < r + 0.7) {
                        hit.add(e);
                        double dmg = 10;
                        ((LivingEntity) e).damage(dmg, p);
                        safeHeal(p, dmg * 0.4);
                        Vector kb = e.getLocation().subtract(center).toVector().normalize().multiply(1.8).setY(0.3);
                        e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.3, 0));
                        spawnBloodBurst(e.getLocation().clone().add(0, 1, 0), 15);
                    }
                }
                r += 0.5;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void deathGrip(Player p) {
        LivingEntity target = getNearestTarget(p, 14);
        if (target == null) {
            sendActionBar(p, "§cNo target in range!");
            return;
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_HURT, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 0.5f);

        Location from = target.getLocation().clone().add(0, 1, 0);
        Location to   = p.getLocation().clone().add(0, 1, 0);
        Random rng = new Random();
        new BukkitRunnable() {
            double prog = 0;
            @Override public void run() {
                if (prog > 1) { cancel(); return; }
                Location pt = from.clone().add(to.toVector().subtract(from.toVector()).multiply(prog));
                particleApi.spawnColoredParticles(pt, C_CRIMSON, 1.2f, 3, 0.1, 0.1, 0.1);
                particleApi.spawnColoredParticles(pt, C_BLACK_VOID, 0.9f, 1, 0.08, 0.08, 0.08);
                prog += 0.12;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override public void run() {
                Vector pull = p.getLocation().toVector().subtract(target.getLocation().toVector())
                        .normalize().multiply(2.5).setY(0.5);
                target.setVelocity(isVecFinite(pull) ? pull : new Vector(0, 0.5, 0));

                new BukkitRunnable() {
                    @Override public void run() {
                        target.damage(14, p);
                        safeHeal(p, 6);
                        applyPotion(target, PotionEffectType.SLOWNESS, 40, 5);
                        spawnBloodBurst(target.getLocation().clone().add(0, 1, 0), 30);
                        p.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9f, 0.5f);
                        p.getWorld().playSound(target.getLocation(), Sound.ENTITY_BAT_HURT, 0.7f, 0.6f);
                    }
                }.runTaskLater(magicPlugin, 8L);
            }
        }.runTaskLater(magicPlugin, 6L);
    }

    private void batSwarm(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_LOOP,    0.8f, 0.6f);

        int bats = 8;
        Random rng = new Random();

        for (int b = 0; b < bats; b++) {
            double angle = Math.toRadians(b * (360.0 / bats));
            Vector batDir = new Vector(Math.cos(angle), rng.nextDouble() * 0.3 - 0.1, Math.sin(angle))
                    .normalize().multiply(1.5);
            ArmorStand bat = spawnProjectile(p);
            final Vector fDir = batDir;
            final Set<Entity> batHit = new HashSet<>();

            new BukkitRunnable() {
                int t = 0;
                @Override public void run() {
                    if (bat.isDead() || t > 35) { safeRemove(bat); cancel(); return; }
                    bat.teleport(bat.getLocation().add(fDir));

                    Location loc = bat.getLocation();

                    double wing = Math.sin(t * 0.8) * 0.3;
                    particleApi.spawnColoredParticles(loc.clone().add(wing, 0, 0), C_BLACK_VOID, 0.9f, 2, 0.05, 0.05, 0.05);
                    particleApi.spawnColoredParticles(loc.clone().add(-wing, 0, 0), C_BLACK_VOID, 0.9f, 2, 0.05, 0.05, 0.05);
                    particleApi.spawnColoredParticles(loc, C_CRIMSON, 1.0f, 1, 0.04, 0.04, 0.04);

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                        if (e.equals(p) || e instanceof ArmorStand || batHit.contains(e)) continue;
                        if (!(e instanceof LivingEntity)) continue;
                        batHit.add(e);
                        ((LivingEntity) e).damage(7, p);
                        safeHeal(p, 3);
                        applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 30, 0);
                        spawnBloodBurst(e.getLocation().clone().add(0, 1, 0), 12);
                        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_BAT_HURT, 0.5f, 1.2f);
                        safeRemove(bat); cancel(); return;
                    }
                    t++;
                }
            }.runTaskTimer(magicPlugin, b * 2L, 1L);
        }
    }

    private void mistForm(Player p) {
        mistActive = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.6f, 0.3f);

        applyPotion(p, PotionEffectType.INVISIBILITY, 80, 0);
        applyPotion(p, PotionEffectType.RESISTANCE,   80, 4);
        applyPotion(p, PotionEffectType.SPEED,        80, 2);
        sendActionBar(p, "§4§l✦ Mist Form ✦");

        Random rng = new Random();

        mistTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60 || !p.isOnline()) {
                    exitMistForm(p);
                    cancel();
                    return;
                }
                Location loc = p.getLocation().clone().add(0, 1, 0);

                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(rng.nextDouble() * 360);
                    double d = rng.nextDouble() * 1.0;
                    double h = rng.nextDouble() * 2.0;
                    Location pt = p.getLocation().clone().add(Math.cos(a)*d, h, Math.sin(a)*d);
                    particleApi.spawnColoredParticles(pt, BLOOD_COLS[rng.nextInt(BLOOD_COLS.length)],
                            0.9f, 1, 0.1, 0.05, 0.1);
                    if (rng.nextInt(4) == 0)
                        particleApi.spawnColoredParticles(pt, C_BLACK_VOID, 0.8f, 1, 0.08, 0.05, 0.08);
                }
                t++;
            }
        };
        mistTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void exitMistForm(Player p) {
        mistActive = false;
        if (mistTask != null) { mistTask.cancel(); mistTask = null; }
        removePotion(p, PotionEffectType.INVISIBILITY);
        removePotion(p, PotionEffectType.RESISTANCE);
        removePotion(p, PotionEffectType.SPEED);
        spawnBloodBurst(p.getLocation().clone().add(0, 1, 0), 40);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8f, 1.2f);

        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 2.5, 2.5, 2.5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            ((LivingEntity) e).damage(8, p);
            safeHeal(p, 4);
            applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 30, 0);
        }
    }

    private void shadowLeap(Player p) {
        LivingEntity target = getInSight(p, 18, 0.8);
        if (target == null) {
            sendActionBar(p, "§cNo target in sight!");
            return;
        }
        Location from = p.getLocation().clone();
        Vector behind = target.getLocation().getDirection().normalize().multiply(1.8);
        Location dest  = target.getLocation().clone().subtract(behind);
        dest.setDirection(target.getLocation().toVector().subtract(dest.toVector()));
        dest.setY(dest.getY() + 0.1);

        spawnBloodBurst(from.clone().add(0, 1, 0), 20);
        p.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
        p.teleport(dest);
        spawnBloodBurst(dest.clone().add(0, 1, 0), 20);
        p.getWorld().playSound(dest, Sound.ENTITY_BAT_HURT, 0.8f, 0.5f);

        double dmg = isNight(p) ? 20 : 14;
        target.damage(dmg, p);
        safeHeal(p, dmg * 0.5);
        applyPotion(target, PotionEffectType.SLOWNESS, 50, 3);
        spawnBloodBurst(target.getLocation().clone().add(0, 1, 0), 35);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9f, 0.5f);
    }

    private void bloodMoon(Player p) {
        if (moonActive) return;
        moonActive = true;

        p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "🌑 BLOOD MOON 🌑");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_DEATH, 0.8f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF,  1f,   0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.3f);

        for (int i = 0; i < 3; i++) {
            final int fi = i;
            new BukkitRunnable() {
                @Override public void run() {
                    spawnBloodBurst(p.getLocation().clone().add(0, 1 + fi, 0), 40);
                }
            }.runTaskLater(magicPlugin, fi * 4L);
        }

        applyPotion(p, PotionEffectType.STRENGTH,     220, 2);
        applyPotion(p, PotionEffectType.SPEED,        220, 2);
        applyPotion(p, PotionEffectType.RESISTANCE,   220, 0);
        applyPotion(p, PotionEffectType.NIGHT_VISION, 420, 0);

        Random rng = new Random();

        moonTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 200 || !p.isOnline()) {
                    moonEnd(p);
                    cancel();
                    return;
                }

                Location center = p.getLocation().clone().add(0, 1, 0);
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(i * 36 + t * 8);
                    Location pt = center.clone().add(Math.cos(a)*1.1, Math.sin(a*0.4)*0.2, Math.sin(a)*1.1);
                    particleApi.spawnColoredParticles(pt, BLOOD_COLS[rng.nextInt(BLOOD_COLS.length)], 1.1f, 1, 0.03, 0.03, 0.03);
                    if (i % 3 == 0)
                        particleApi.spawnColoredParticles(pt, C_BLACK_VOID, 0.9f, 1, 0.04, 0.04, 0.04);
                }

                particleCircle(p.getLocation().clone().add(0, 0.08, 0),
                        0.8, C_BLOOD, 1.0f, 8, t * 20);

                if (t % 40 == 0 && t > 0) {
                    p.getWorld().playSound(center, Sound.ENTITY_BAT_HURT, 0.6f, 0.4f);
                    for (Entity e : center.getWorld().getNearbyEntities(center, 6, 4, 6)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        double dmg = 6;
                        ((LivingEntity) e).damage(dmg, p);
                        safeHeal(p, dmg * 0.8);
                        spawnBloodBurst(e.getLocation().clone().add(0, 1, 0), 15);
                        Vector pull = center.toVector().subtract(e.getLocation().toVector())
                                .normalize().multiply(0.8);
                        e.setVelocity(e.getVelocity().add(pull));
                    }
                }

                if (t == 100) sendActionBar(p, "§4✦ Blood Moon — 5s remaining");
                t++;
            }
        };
        moonTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void moonEnd(Player p) {
        moonActive = false;
        if (moonTask != null) { moonTask.cancel(); moonTask = null; }

        removePotion(p, PotionEffectType.STRENGTH);
        removePotion(p, PotionEffectType.SPEED);
        removePotion(p, PotionEffectType.RESISTANCE);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8f, 0.4f);
        sendActionBar(p, "§7Blood Moon faded...");

        Location center = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(center, C_CRIMSON,    2.0f, 150, 3, 3, 3);
        particleApi.spawnColoredParticles(center, C_BLOOD,      1.8f, 80,  2.5, 2.5, 2.5);
        particleApi.spawnColoredParticles(center, C_BLACK_VOID, 1.5f, 60,  3.5, 3.5, 3.5);

        for (Entity e : center.getWorld().getNearbyEntities(center, 7, 5, 7)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(center));
            double dmg  = Math.max(8, 28 - dist * 2.5);
            ((LivingEntity) e).damage(dmg, p);
            safeHeal(p, dmg * 0.6);
            Vector kb = e.getLocation().subtract(center).toVector().normalize().multiply(2.5).setY(0.7);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.7, 0));
        }
    }

    private void passiveLifesteal(DealDamageExecute ex) {
        Player p = ex.getPlayer();
        double dmg = ex.getDamage();
        double healPct = moonActive ? 0.8 : (isNight(p) ? 0.15 : 0.08);
        safeHeal(p, dmg * healPct);
    }

    private void passiveInstinct(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (p.getHealth() > 8) return;
        if (isOnCooldown(v_instinct, p)) return;
        LivingEntity nearest = getNearestTarget(p, 8);
        if (nearest == null) return;
        nearest.damage(10, p);
        safeHeal(p, 8);
        spawnBloodBurst(nearest.getLocation().clone().add(0, 1, 0), 20);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_HURT, 0.7f, 0.5f);
        addCdFixed(v_instinct, p, 8.0);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                boolean night = isNight(p);

                if (night) {
                    applyPotion(p, PotionEffectType.NIGHT_VISION, 25, 0);
                    applyPotion(p, PotionEffectType.SPEED,        25, 1);
                    applyPotion(p, PotionEffectType.STRENGTH,     25, 0);
                } else {

                    if (p.getLocation().getBlock().getLightFromSky() > 12) {
                        applyPotion(p, PotionEffectType.WEAKNESS,        25, 0);
                        applyPotion(p, PotionEffectType.MINING_FATIGUE,  25, 0);
                    }
                }

                if (isAuraEnabled(p)) {
                    particleCircle(p.getLocation().clone().add(0, 0.06, 0),
                            0.6, night ? C_CRIMSON : C_DARK_RED, 0.9f, 5, t * 24);
                    particleApi.spawnColoredParticles(
                            p.getLocation().clone().add(0, 0.05, 0),
                            C_BLACK_VOID, 0.8f, 1, 0.25, 0.01, 0.25);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 25);
        return r;
    }

    @Override
    public void remove() {
        if (mistTask != null) { mistTask.cancel(); mistTask = null; }
        if (moonTask != null) { moonTask.cancel(); moonTask = null; }
        mistActive = false;
        moonActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§4血矛";
            case 1: return "§4深红冲刺";
            case 2: return "§4血新星";
            case 3: return "§4死亡之握";
            case 4: return "§4蝙蝠群";
            case 5: return "§4迷雾形态";
            case 6: return "§4暗影跳跃";
            case 7: return "§4§l🌑 血月 §c[ULT]";
            default: return "§7none";
        }
    }

    private void spawnBloodBurst(Location loc, int count) {
        Random rng = new Random();
        particleApi.spawnColoredParticles(loc, C_CRIMSON,  1.3f, count,     0.35, 0.3, 0.35);
        particleApi.spawnColoredParticles(loc, C_BLOOD,    1.1f, count / 2, 0.4,  0.35, 0.4);
        particleApi.spawnColoredParticles(loc, C_BLACK_VOID, 0.9f, count / 3, 0.3, 0.25, 0.3);
    }

    private boolean isNight(Player p) {
        long t = p.getWorld().getTime();
        return !(t < 12300 || t > 23850);
    }
}

