package net.trduc.magicabilities.powers.custom;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilities.cooldowns.CooldownApi;
import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

public class DemonLordPower extends Power implements IdlePower, Removeable {

    private static final String dl_blade     = "demonlord.inferno_blade";
    private static final String dl_summon    = "demonlord.summon";
    private static final String dl_prison    = "demonlord.prison";
    private static final String dl_devour    = "demonlord.devour";
    private static final String dl_step      = "demonlord.shadow_step";
    private static final String dl_judgment  = "demonlord.judgment";
    private static final String dl_fury      = "demonlord.fury";

    private static final Color C_BLOOD      = Color.fromRGB(200,   5,   5);
    private static final Color C_BLOOD_DK   = Color.fromRGB(110,   0,   0);
    private static final Color C_EMBER      = Color.fromRGB(255,  70,   0);
    private static final Color C_EMBER_LITE = Color.fromRGB(255, 160,  30);
    private static final Color C_VOID       = Color.fromRGB( 15,   3,  20);
    private static final Color C_SOUL_FIRE  = Color.fromRGB(  0, 130, 140);
    private static final Color C_SOUL_LITE  = Color.fromRGB( 60, 200, 210);
    private static final Color C_CROWN_GOLD = Color.fromRGB(255, 200,  10);
    private static final Color C_MAGMA      = Color.fromRGB(255, 100,   0);

    private static final Color[] AURA_COLS  = { C_BLOOD, C_BLOOD_DK, C_MAGMA };
    private static final Color[] SOUL_COLS  = { C_SOUL_FIRE, C_SOUL_LITE, C_BLOOD, C_EMBER_LITE };
    private static final Color[] FIRE_COLS  = { C_EMBER, C_EMBER_LITE, C_BLOOD, C_MAGMA };

    private boolean judging    = false;
    private BukkitRunnable judgeTask  = null;
    private BukkitRunnable hudTask    = null;

    private final List<BukkitRunnable> summonedSouls = new ArrayList<>();
    private final Set<WitherSkeleton> summonedSkeletons = new HashSet<>();
    private UUID lastTargetId = null;

    private final Map<UUID, BukkitRunnable> prisonedTargets = new HashMap<>();

    public DemonLordPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute)   { onDamaged((DamagedExecute) ex);   return; }
        if (ex instanceof DamagedByExecute) { onDamagedBy((DamagedByExecute) ex); return; }
        if (ex instanceof DeathExecute)     {  return; }
        if (ex instanceof DealDamageExecute){ onKillCheck((DealDamageExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) { onLeft((LeftClickExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p    = ex.getPlayer();
        int    slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(dl_blade, p, this)) return; infernoBladeSlash(p);  addCd(dl_blade, p);   return;
            case 1: if (onCd(dl_summon, p, this)) return; summonSoulWarriors(p); addCd(dl_summon, p);  return;
            case 2: if (onCd(dl_prison, p, this)) return; hellPrison(p);         addCd(dl_prison, p);  return;
            case 3: if (onCd(dl_devour, p, this)) return; hellfireEruption(p);    addCd(dl_devour, p);  return;
            case 4: if (onCd(dl_step, p, this)) return; shadowStep(p);         addCd(dl_step, p);    return;
            case 5:
                if (judging) { hud(p, "§4Are judging!"); return; }
                if (onCd(dl_judgment, p, this)) return;
                demonLordJudgment(p);
                addCd(dl_judgment, p);
        }
    }

    private void infernoBladeSlash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT,           1f,   0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,   0.8f, 0.7f);
        int[]  yaws   = {-18, 0, 18};
        double[] pitches = {4, 0, -4};
        for (int b = 0; b < 3; b++) {
            final int yaw   = yaws[b];
            final int pitch = (int) pitches[b];
            final int bi    = b;
            new BukkitRunnable() {
                @Override public void run() {
                    fireInfernoBlade(p, yaw, pitch, bi);
                }
            }.runTaskLater(magicPlugin, b * 3L);
        }
    }
    private void fireInfernoBlade(Player p, int yawDeg, int pitchOffset, int idx) {
        final Location start = p.getEyeLocation().clone();
        Vector base = p.getEyeLocation().getDirection().clone().normalize();
        base = rotateY(base, yawDeg);
        base.setY(base.getY() + Math.toRadians(pitchOffset) * 0.3);
        base.normalize();
        final Vector dir = base.multiply(0.95);
        final Set<UUID> hitSet = new HashSet<>();
        new BukkitRunnable() {
            Location cur = start.clone();
            int t = 0;
            @Override public void run() {
                if (t > 40) { bladeImpact(cur); cancel(); return; }
                cur.add(dir);
                if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                    bladeImpact(cur); cancel(); return;
                }

                particleApi.spawnColoredParticles(cur, C_EMBER_LITE, 1.8f, 3, 0.04, 0.04, 0.04);
                particleApi.spawnColoredParticles(cur, C_BLOOD,      1.3f, 2, 0.07, 0.07, 0.07);
                for (int j = 0; j < 4; j++) {
                    double a = Math.toRadians(j * 90 + t * 40 + idx * 30);
                    Location ring = cur.clone().add(Math.cos(a)*0.25, Math.sin(a*0.5)*0.1, Math.sin(a)*0.25);
                    particleApi.spawnColoredParticles(ring, FIRE_COLS[(t+j) % FIRE_COLS.length],
                            1.1f, 1, 0.03, 0.03, 0.03);
                }
                if (t % 3 == 0)
                    particleApi.spawnParticles(cur, Particle.FLAME, 1, 0.06, 0.06, 0.06, 0.04f);

                Location ground = cur.clone();
                ground.setY(ground.getBlockY());
                if (!ground.getBlock().isPassable())
                    ground.add(0, 1, 0);
                ground.getBlock().getWorld().spawnParticle(Particle.FLAME,
                        ground.getX(), ground.getY(), ground.getZ(), 2, 0.2, 0, 0.2, 0.02);

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 0.8, 0.8, 0.8)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || hitSet.contains(e.getUniqueId())) continue;
                    hitSet.add(e.getUniqueId());
                    ((LivingEntity) e).damage(16.0, p);
                    e.setFireTicks(80);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, true));
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_EMBER, 1.6f, 10, 0.25, 0.25, 0.25);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 1.3f);
                }
                if (t % 6 == 0)
                    p.getWorld().playSound(cur, Sound.ENTITY_BLAZE_AMBIENT, 0.1f, 1.8f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void bladeImpact(Location loc) {
        particleApi.spawnColoredParticles(loc, C_EMBER_LITE, 2.0f, 18, 0.4, 0.4, 0.4);
        particleApi.spawnColoredParticles(loc, C_BLOOD,      1.5f, 14, 0.5, 0.5, 0.5);
        particleApi.spawnParticles(loc, Particle.FLAME, 10, 0.35, 0.35, 0.35, 0.08f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_HURT, 0.4f, 0.7f);
    }

    private void summonSoulWarriors(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT,         0.9f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.7f, 0.5f);

        for (WitherSkeleton sk : summonedSkeletons) {
            if (sk != null && !sk.isDead()) sk.remove();
        }
        summonedSkeletons.clear();

        final List<WitherSkeleton> skeletons = new ArrayList<>();
        final org.bukkit.World world = p.getWorld();
        final Random rng = new Random();

        for (int s = 0; s < 3; s++) {
            double angle = Math.toRadians(s * 120);
            Location spawnLoc = p.getLocation().clone()
                    .add(Math.cos(angle) * 1.8, 0, Math.sin(angle) * 1.8);

            for (int i = 0; i < 10; i++)
                particleApi.spawnColoredParticles(
                        spawnLoc.clone().add(rng.nextDouble()*0.6-0.3, rng.nextDouble()*2, rng.nextDouble()*0.6-0.3),
                        SOUL_COLS[i % SOUL_COLS.length], 1.4f, 1, 0.05, 0.05, 0.05);
            particleApi.spawnParticles(spawnLoc.clone().add(0, 1, 0),
                    Particle.SOUL_FIRE_FLAME, 8, 0.2, 0.4, 0.2, 0.04f);

            WitherSkeleton sk = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
            sk.setCustomName("§5Soul Warrior");
            sk.setCustomNameVisible(true);
            sk.setAI(true);

            skeletons.add(sk);
            summonedSkeletons.add(sk);
        }

        world.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.4f);
        hud(p, "§5☠ 3 Soul Warriors summoned! (10s)");

        org.bukkit.event.Listener targetListener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent e) {
                if (!summonedSkeletons.contains(e.getEntity())) return;

                Entity newTarget = e.getTarget();

                if (newTarget != null && newTarget.equals(p)) {
                    e.setCancelled(true);
                    return;
                }

                if (newTarget instanceof Player && !newTarget.equals(p)) {

                    if (!newTarget.getUniqueId().equals(lastTargetId)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        };
        magicPlugin.getServer().getPluginManager().registerEvents(targetListener, magicPlugin);

        new BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                if (elapsed >= 200 || !p.isOnline()) {

                    for (WitherSkeleton sk : skeletons) {
                        if (!sk.isDead() && sk.isValid()) {
                            particleApi.spawnColoredParticles(sk.getLocation().clone().add(0, 1, 0),
                                    C_SOUL_FIRE, 1.5f, 15, 0.3, 0.5, 0.3);
                            particleApi.spawnParticles(sk.getLocation().clone().add(0, 1, 0),
                                    Particle.SOUL_FIRE_FLAME, 6, 0.2, 0.3, 0.2, 0.04f);
                            world.playSound(sk.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 1.6f);
                            sk.remove();
                        }
                    }
                    skeletons.clear();
                    summonedSkeletons.clear();
                    org.bukkit.event.HandlerList.unregisterAll(targetListener);
                    hud(p, "§7Soul Warriors have vanished.");
                    cancel();
                    return;
                }

                if (lastTargetId != null) {
                    Entity currentTarget = null;
                    for (Entity e : world.getNearbyEntities(p.getLocation(), 30, 30, 30)) {
                        if (e.getUniqueId().equals(lastTargetId)) { currentTarget = e; break; }
                    }
                    if (currentTarget instanceof LivingEntity && !currentTarget.isDead()) {
                        final LivingEntity tgt = (LivingEntity) currentTarget;
                        for (WitherSkeleton sk : skeletons) {
                            if (!sk.isDead() && sk.isValid()) sk.setTarget(tgt);
                        }
                    }
                }

                elapsed += 20;
            }
        }.runTaskTimer(magicPlugin, 20L, 20L);
    }

    private void drawSoul(Location loc, int idx, int t) {
        Color c1 = idx == 0 ? C_SOUL_FIRE : idx == 1 ? C_BLOOD : C_SOUL_LITE;
        particleApi.spawnColoredParticles(loc, c1,          1.4f, 3, 0.06, 0.08, 0.06);
        particleApi.spawnColoredParticles(loc, C_VOID,      0.6f, 1, 0.04, 0.04, 0.04);
        if (t % 4 == 0)
            particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 1, 0.05, 0.08, 0.05, 0.02f);
    }

    private void hellPrison(Player p) {
        final Location center  = p.getLocation().clone().add(0, 0.5, 0);
        final org.bukkit.World world = p.getWorld();
        final Set<UUID> prisonSet = new HashSet<>();

        for (Entity e : world.getNearbyEntities(center, 5, 3, 5)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            prisonSet.add(e.getUniqueId());
        }
        if (prisonSet.isEmpty()) {
            hud(p, "§cThere is no goal in R=5!");
            addCdFixed(dl_prison, p, 2.0);
            return;
        }
        p.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT,            1f, 0.4f);
        p.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE,          0.7f, 0.3f);
        BukkitRunnable prisonRun = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80) {
                    world.playSound(center, Sound.ENTITY_BLAZE_HURT, 1f, 0.5f);
                    for (Entity e : world.getNearbyEntities(center, 5.5, 4, 5.5)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        if (!prisonSet.contains(e.getUniqueId())) continue;
                        ((LivingEntity) e).damage(14.0, p);
                        e.setFireTicks(100);
                    }
                    particleApi.spawnColoredParticles(center.clone().add(0,2,0), C_EMBER, 2.0f, 50, 2.0, 2.0, 2.0);
                    particleApi.spawnParticles(center, Particle.LAVA, 20, 2.0, 1.0, 2.0, 0.1f);
                    cancel(); return;
                }
                for (int i = 0; i < 24; i++) {
                    double a  = Math.toRadians(i * 15 + t * 9);
                    double yh = 0.5 + Math.sin(t * 0.2 + i * 0.26) * 0.5;
                    Location ring = center.clone().add(Math.cos(a)*5.0, yh, Math.sin(a)*5.0);
                    particleApi.spawnColoredParticles(ring, FIRE_COLS[i%FIRE_COLS.length], 1.1f, 1, 0.04, 0.04, 0.04);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(ring, Particle.FLAME, 1, 0.04, 0.04, 0.04, 0.03f);
                }
                if (t % 2 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double a = Math.toRadians(i * 30 + t * 6);
                        Location base = center.clone().add(Math.cos(a)*5.0, 0.05, Math.sin(a)*5.0);
                        particleApi.spawnParticles(base, Particle.FLAME, 2, 0.05, 0.2, 0.05, 0.04f);
                    }
                }
                if (t % 10 == 0) {
                    for (Entity e : world.getNearbyEntities(center, 6, 4, 6)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        if (!prisonSet.contains(e.getUniqueId())) continue;

                        double dist = e.getLocation().distance(center);
                        if (dist > 4.5) {
                            Vector pull = center.clone().subtract(e.getLocation()).toVector();
                            if (pull.lengthSquared() > 0.01)
                                e.setVelocity(pull.normalize().multiply(1.8));
                        }
                        ((LivingEntity) e).damage(4.0, p);
                        e.setFireTicks(30);
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 3, false, false));
                    }
                    world.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 0.6f);
                }

                if (t % 20 == 0)
                    world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.25f, 0.4f);
                t++;
            }
        };
        prisonRun.runTaskTimer(magicPlugin, 0, 1);
    }
    private void hellfireEruption(Player p) {
        final Location center = p.getLocation().clone();
        final org.bukkit.World world = p.getWorld();
        final Random rng = new Random();

        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT,           1f,  0.3f);
        world.playSound(center, Sound.ENTITY_GHAST_SHOOT,           0.8f,0.5f);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE,        0.6f,0.2f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 20) { cancel(); return; }
                for (int k = 0; k < 20; k++) {
                    double ox = (rng.nextDouble()*2-1) * 8;
                    double oz = (rng.nextDouble()*2-1) * 8;
                    Location pt = center.clone().add(ox, rng.nextDouble()*3, oz);
                    particleApi.spawnColoredParticles(pt, FIRE_COLS[rng.nextInt(FIRE_COLS.length)],
                            1.8f, 2, 0.1, 0.2, 0.1);
                    if (t % 5 == 0)
                        particleApi.spawnParticles(pt, Particle.FLAME, 2, 0.1, 0.2, 0.1, 0.06f);
                }

                double rad = t * 0.45;
                for (int j = 0; j < 36; j++) {
                    double ang = Math.toRadians(j * 10);
                    Location ring = center.clone().add(Math.cos(ang)*rad, 0.05, Math.sin(ang)*rad);
                    particleApi.spawnColoredParticles(ring, C_EMBER_LITE, 1.3f, 1, 0.02, 0.01, 0.02);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                world.playSound(center, Sound.ENTITY_WITHER_DEATH,          1f,  0.5f);
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f,0.6f);
                world.playSound(center, Sound.ENTITY_BLAZE_SHOOT,            1f,  0.4f);

                for (int k = 0; k < 30; k++) {
                    double ox = (rng.nextDouble()*2-1) * 8;
                    double oz = (rng.nextDouble()*2-1) * 8;
                    final Location base = center.clone().add(ox, 0, oz);
                    new BukkitRunnable() {
                        int t = 0;
                        @Override
                        public void run() {
                            if (t > 12) { cancel(); return; }
                            Location col = base.clone().add(0, t * 0.5, 0);
                            particleApi.spawnColoredParticles(col, FIRE_COLS[rng.nextInt(FIRE_COLS.length)],
                                    2.2f, 3, 0.15, 0.1, 0.15);
                            particleApi.spawnParticles(col, Particle.FLAME,
                                    3, 0.12, 0.05, 0.12, 0.08f);
                            if (t % 3 == 0)
                                particleApi.spawnColoredParticles(col, C_BLOOD, 1.6f, 2, 0.2, 0.1, 0.2);
                            t++;
                        }
                    }.runTaskTimer(magicPlugin, rng.nextInt(6), 1);
                }

                new BukkitRunnable() {
                    int wave = 0;
                    @Override
                    public void run() {
                        if (wave > 8) { cancel(); return; }
                        double rad = wave * 1.1;
                        for (int j = 0; j < 48; j++) {
                            double ang = Math.toRadians(j * 7.5);
                            Location ring = center.clone().add(Math.cos(ang)*rad, 0.1, Math.sin(ang)*rad);
                            particleApi.spawnColoredParticles(ring, C_EMBER, 2.0f, 2, 0.02, 0.1, 0.02);
                            if (j % 8 == 0)
                                particleApi.spawnParticles(ring, Particle.FLAME, 1, 0.05, 0.1, 0.05, 0.05f);
                        }
                        wave++;
                    }
                }.runTaskTimer(magicPlugin, 0, 2);

                for (Entity e : world.getNearbyEntities(center, 8, 5, 8)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;
                    double dist = e.getLocation().distance(center);

                    double dmg = 35 - (dist / 8.0) * 17;
                    le.setNoDamageTicks(0);
                    le.damage(dmg, p);
                    le.setFireTicks(100);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));

                    Vector away = e.getLocation().subtract(center).toVector();
                    if (away.lengthSquared() > 0.01)
                        e.setVelocity(away.normalize().multiply(1.0).setY(1.2));
                    particleApi.spawnColoredParticles(le.getLocation().clone().add(0, 1, 0),
                            C_EMBER_LITE, 2.0f, 12, 0.3, 0.4, 0.3);
                    world.playSound(le.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.6f, 0.8f);
                }

                particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_EMBER_LITE, 2.5f, 50, 2.0, 2.0, 2.0);
                particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_BLOOD,      2.0f, 40, 2.5, 2.5, 2.5);
                particleApi.spawnParticles(center, Particle.FLAME, 30, 2.0, 1.5, 2.0, 0.1f);
            }
        }.runTaskLater(magicPlugin, 20L);
    }
    private void spawnDeathSoul(Location from, Location to) {
        new BukkitRunnable() {
            @Override public void run() {
                int steps = Math.max(5, (int) from.distance(to) * 3);
                Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur = from.clone();
                for (int i = 0; i < steps; i++) {
                    particleApi.spawnColoredParticles(cur, SOUL_COLS[i % SOUL_COLS.length], 1.2f, 2, 0.05, 0.08, 0.05);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(cur, Particle.SOUL_FIRE_FLAME, 1, 0.04, 0.04, 0.04, 0.02f);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);
    }

    private void shadowStep(Player p) {
        LivingEntity target = findNearestEnemy(p, p.getLocation().clone().add(0,1,0), 14.0);
        if (target == null) {
            hud(p, "§cThere are no enemies in R=14!");
            addCdFixed(dl_step, p, 2.0);
            return;
        }

        Location behind = target.getLocation().clone();
        Vector   back   = target.getLocation().getDirection().normalize().multiply(-1.8);
        behind.add(back.getX(), 0, back.getZ());
        behind.setYaw(target.getLocation().getYaw() + 180);

        Location fromLoc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(fromLoc, C_VOID,  0.5f, 20, 0.4, 0.8, 0.4);
        particleApi.spawnColoredParticles(fromLoc, C_BLOOD, 1.2f, 15, 0.3, 0.6, 0.3);
        particleApi.spawnParticles(fromLoc, Particle.SOUL_FIRE_FLAME, 8, 0.3, 0.5, 0.3, 0.04f);
        p.getWorld().playSound(fromLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.4f);

        p.teleport(behind);

        Location toLoc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(toLoc, C_EMBER,      2.0f, 25, 0.5, 0.8, 0.5);
        particleApi.spawnColoredParticles(toLoc, C_EMBER_LITE, 1.6f, 20, 0.4, 0.7, 0.4);
        particleApi.spawnParticles(toLoc, Particle.FLAME, 15, 0.4, 0.6, 0.4, 0.07f);
        p.getWorld().playSound(toLoc, Sound.ENTITY_BLAZE_HURT,       0.9f, 0.5f);
        p.getWorld().playSound(toLoc, Sound.ENTITY_WARDEN_SONIC_BOOM,0.5f, 1.5f);

        final LivingEntity ft = target;
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : ft.getWorld().getNearbyEntities(p.getLocation(), 2.5, 2, 2.5)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(18.0, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  30, 3, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, true));
                    Vector away = e.getLocation().subtract(p.getLocation()).toVector();
                    if (away.lengthSquared() > 0.01)
                        e.setVelocity(away.normalize().multiply(1.2).setY(0.5));
                }
            }
        }.runTaskLater(magicPlugin, 1L);
    }

    private void demonLordJudgment(Player p) {
        judging = true;
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 70, 255, false, false));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.2f);
        p.sendMessage(org.bukkit.ChatColor.DARK_RED + "" + org.bukkit.ChatColor.BOLD + "🔱 JUDGMENT OF HELL — Invoking...");
        hud(p, "§4🔱 Charging...");
        judgeTask = new BukkitRunnable() {
            int ct = 60;
            @Override public void run() {
                if (!p.isOnline()) {
                    judging = false; judgeTask = null; cancel(); return;
                }
                if (ct <= 0) {
                    judging = false; judgeTask = null;
                    cancel();
                    releaseJudgment(p);
                    return;
                }

                Location loc = p.getLocation().clone().add(0, 1, 0);
                Random rng = new Random();
                double r = 3.0 - (60 - ct) * 0.045;
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(i * 36 + (60-ct) * 14);
                    Location lp = loc.clone().add(Math.cos(a)*Math.max(0.2,r), rng.nextDouble()*2.5, Math.sin(a)*Math.max(0.2,r));
                    particleApi.spawnColoredParticles(lp, i%2==0 ? C_EMBER : C_BLOOD, 1.4f, 2, 0.05, 0.05, 0.05);
                    particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.04, 0.04, 0.05f);
                }

                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 + (60-ct) * 8);
                    Location crown = p.getLocation().clone().add(0, 2.5, 0)
                            .add(Math.cos(a)*0.6, 0, Math.sin(a)*0.6);
                    particleApi.spawnColoredParticles(crown, C_CROWN_GOLD, 1.5f, 1, 0.03, 0.03, 0.03);
                }
                if (ct % 10 == 0) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.3f + (60-ct)*0.015f);

                    for (Entity e : p.getWorld().getNearbyEntities(loc, 15, 8, 15)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15, 0, false, false));
                    }
                }
                ct--;
            }
        };
        judgeTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void releaseJudgment(Player p) {
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH,  1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
        p.sendMessage(org.bukkit.ChatColor.DARK_RED + "🔱 " + org.bukkit.ChatColor.BOLD + "JUDGMENT OF HELL!");
        final Location epicenter = p.getLocation().clone().add(0, 1, 0);
        final org.bukkit.World world = p.getWorld();

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : world.getNearbyEntities(epicenter, 15, 8, 15)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            targets.add((LivingEntity) e);
        }

        for (int i = 0; i < targets.size(); i++) {
            final LivingEntity ft = targets.get(i);
            new BukkitRunnable() {
                @Override public void run() {
                    if (!ft.isValid() || ft.isDead()) return;
                    Location tLoc = ft.getLocation().clone();

                    new BukkitRunnable() {
                        double y = 12.0;
                        @Override public void run() {
                            if (y < 0) { cancel(); return; }
                            Location col = tLoc.clone().add(0, y, 0);
                            particleApi.spawnColoredParticles(col, C_EMBER,  1.8f, 4, 0.15, 0.1, 0.15);
                            particleApi.spawnColoredParticles(col, C_BLOOD,  1.4f, 3, 0.2, 0.1, 0.2);
                            particleApi.spawnParticles(col, Particle.FLAME, 3, 0.12, 0.05, 0.12, 0.06f);
                            y -= 0.9;
                        }
                    }.runTaskTimer(magicPlugin, 0, 1);

                    new BukkitRunnable() {
                        @Override public void run() {
                            if (!ft.isValid() || ft.isDead()) return;
                            double dmg = 25.0 + ft.getMaxHealth() * 0.30;
                            ft.damage(dmg, p);
                            ft.setFireTicks(120);
                            ft.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   80, 2, false, true));
                            ft.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3, false, true));
                            ft.setVelocity(new Vector(0, 1.5, 0));

                            tLoc.getWorld().strikeLightningEffect(tLoc);
                            particleApi.spawnColoredParticles(tLoc.clone().add(0,0.5,0), C_EMBER_LITE, 2.2f, 25, 0.6, 0.5, 0.6);
                            particleApi.spawnColoredParticles(tLoc.clone().add(0,0.5,0), C_BLOOD, 1.8f, 20, 0.8, 0.6, 0.8);
                            particleApi.spawnParticles(tLoc, Particle.LAVA, 8, 0.5, 0.3, 0.5, 0.1f);
                            tLoc.getWorld().playSound(tLoc, Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);
                            tLoc.getWorld().playSound(tLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.7f);

                            hud(p, "§c🔱 +" + (int)dmg + " dmg");
                        }
                    }.runTaskLater(magicPlugin, 14L);
                }
            }.runTaskLater(magicPlugin, i * 5L);
        }

        particleApi.spawnColoredParticles(epicenter, C_EMBER_LITE, 2.5f, 60, 2.0, 2.0, 2.0);
        particleApi.spawnColoredParticles(epicenter, C_BLOOD,      2.0f, 80, 3.0, 3.0, 3.0);
        particleApi.spawnParticles(epicenter, Particle.LAVA, 30, 3.0, 1.5, 3.0, 0.15f);
    }

    private void onDamaged(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA) {
            event.setCancelled(true);
            return;
        }

        Player p = ex.getPlayer();
        double hpAfter = p.getHealth() - event.getFinalDamage();
        if (hpAfter > 0 && hpAfter / getMaxHp(p) <= 0.30) {
            if (!CooldownApi.isOnCooldown(dl_fury, p)) {
                triggerFury(p);
                addCdFixed(dl_fury, p, cooldowns.containsKey(dl_fury) ? cooldowns.get(dl_fury) : 15.0);
            }
        }
    }

    private void triggerFury(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        final org.bukkit.World world = p.getWorld();

        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,    80, 2, false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       80, 1, false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));

        world.playSound(loc, Sound.ENTITY_WITHER_HURT,  0.9f, 0.6f);
        world.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.7f, 1.2f);
        hud(p, "§4☠ DEMON LORD FURY!");

        particleApi.spawnColoredParticles(loc, C_BLOOD,  2.0f, 40, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(loc, C_EMBER,  1.8f, 50, 2.0, 2.0, 2.0);
        particleApi.spawnParticles(loc, Particle.FLAME, 20, 1.5, 1.0, 1.5, 0.08f);

        for (Entity e : world.getNearbyEntities(loc, 4, 3, 4)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            ((LivingEntity) e).damage(12.0, p);
            e.setFireTicks(80);
            Vector away = e.getLocation().subtract(loc).toVector();
            if (away.lengthSquared() > 0.01)
                e.setVelocity(away.normalize().multiply(1.6).setY(0.6));
        }
    }

    private void onKillCheck(DealDamageExecute ex) {
        Player p = ex.getPlayer();
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) event.getEntity();

        if (!target.equals(p) && !summonedSkeletons.contains(target))
            lastTargetId = target.getUniqueId();

        double hpAfter = target.getHealth() - event.getFinalDamage();
        if (hpAfter <= 0 && !target.equals(p)) {
            p.setHealth(Math.min(getMaxHp(p), p.getHealth() + 4.0));
            spawnDeathSoul(target.getLocation().clone().add(0,1,0), p.getLocation().clone().add(0,1,0));
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.5f);

            if (target.getUniqueId().equals(lastTargetId)) lastTargetId = null;
        }
    }

    private void onDamagedBy(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        Entity attacker = event.getDamager();
        if (!(attacker instanceof LivingEntity) || attacker.equals(p)) return;

        attacker.setFireTicks(40);
        ((LivingEntity) attacker).damage(4.0, p);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p   = ex.getPlayer();
        final Random rng = new Random();

        hudTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                p.setFireTicks(0);

                if (isAuraEnabled(p)) {
                    Location base   = p.getLocation().clone();
                    Location ground = base.clone().add(0, 0.08, 0);

                    for (int i = 0; i < 12; i++) {
                        double a = Math.toRadians(i * 30 + t * 7);
                        Color  c = AURA_COLS[i % AURA_COLS.length];
                        particleApi.spawnColoredParticles(
                                ground.clone().add(Math.cos(a)*1.2, 0, Math.sin(a)*1.2),
                                c, 1.05f, 1, 0.03, 0.01, 0.03);
                        if (i % 6 == 0)
                            particleApi.spawnParticles(
                                    ground.clone().add(Math.cos(a)*1.2, 0, Math.sin(a)*1.2),
                                    Particle.FLAME, 1, 0.03, 0.01, 0.03, 0.01f);
                    }

                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45 - t * 10);
                        particleApi.spawnColoredParticles(
                                ground.clone().add(Math.cos(a)*0.7, 0, Math.sin(a)*0.7),
                                i%2==0 ? C_SOUL_FIRE : C_VOID, 0.9f, 1, 0.03, 0.01, 0.03);
                    }

                    if (t % 4 == 0)
                        particleApi.spawnParticles(
                                ground.clone().add((rng.nextDouble()-0.5)*1.5, 0, (rng.nextDouble()-0.5)*1.5),
                                Particle.SOUL_FIRE_FLAME, 1, 0.04, 0.01, 0.04, 0.02f);

                    if (t % 5 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double a = Math.toRadians(i * 45 + t * 4);
                            Location fp = ground.clone().add(Math.cos(a)*0.95, 0, Math.sin(a)*0.95);
                            particleApi.spawnParticles(fp, Particle.FLAME, 1, 0.04, 0.01, 0.04, 0.02f);
                            particleApi.spawnColoredParticles(fp, C_BLOOD_DK, 0.8f, 1, 0.05, 0.01, 0.05);
                        }
                    }

                    if (t % 5 == 0) {
                        for (int i = 0; i < 4; i++) {
                            double a = Math.toRadians(i * 90 + t * 5);
                            Location crown = base.clone().add(
                                    Math.cos(a)*0.35, 2.3, Math.sin(a)*0.35);
                            particleApi.spawnColoredParticles(crown, C_CROWN_GOLD, 1.0f, 1, 0.02, 0.02, 0.02);
                        }
                    }

                    if (t % 100 == 0)
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.15f, 0.55f);
                }
                if (t % 10 == 0) hud(p, null);
                t++;
            }
        };
        hudTask.runTaskTimer(magicPlugin, 0, 1);
        return hudTask;
    }

    @Override
    public void remove() {
        judging = false;
        if (judgeTask != null) {
            BukkitRunnable jt = judgeTask; judgeTask = null;
            try { jt.cancel(); } catch (Exception ignored) {}
        }
        if (hudTask != null) {
            BukkitRunnable ht = hudTask; hudTask = null;
            try { ht.cancel(); } catch (Exception ignored) {}
        }
        for (BukkitRunnable r : summonedSouls) { try { r.cancel(); } catch (Exception ignored) {} }
        summonedSouls.clear();
        lastTargetId = null;
        for (WitherSkeleton sk : summonedSkeletons) {
            if (sk != null && !sk.isDead()) sk.remove();
        }
        summonedSkeletons.clear();
        for (BukkitRunnable r : prisonedTargets.values()) { try { r.cancel(); } catch (Exception ignored) {} }
        prisonedTargets.clear();
        Player owner = getOwner();
        if (owner != null && owner.isOnline()) {
            owner.removePotionEffect(PotionEffectType.RESISTANCE);
            owner.removePotionEffect(PotionEffectType.STRENGTH);
            owner.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        }
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§c⚔ Hell's Tongue";
            case 1: return "§5☠ Soul Warrior Summoning";
            case 2: return "§4🔥 Hell's Loose Confinement";
            case 3: return "§c🌋 Hellfire Eruption";
            case 4: return "§8⚡ Hell's Shadow Step";
            case 5: return "§4§l🔱 Hell's Judgment";
            default: return "§7none";
        }
    }
    private void hud(Player p, String msg) {
        String state = judging ? " §4[JUDGMENT]"
                : summonedSouls.size() > 0 ? " §5[" + summonedSouls.size() + " soul]" : "";
        String m   = msg != null ? " §r§f" + msg : "";
    }

    private LivingEntity findNearestEnemy(Player p, Location from, double radius) {
        LivingEntity best = null; double bestDist = radius;
        for (Entity e : from.getWorld().getNearbyEntities(from, radius, radius, radius)) {
            if (e.equals(p) || !(e instanceof LivingEntity) || e instanceof Player) continue;
            double d = e.getLocation().distance(from);
            if (d < bestDist) { bestDist = d; best = (LivingEntity) e; }
        }
        return best;
    }

}
