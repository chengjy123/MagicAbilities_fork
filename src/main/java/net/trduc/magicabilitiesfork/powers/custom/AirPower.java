package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
import static net.trduc.magicabilitiesfork.cooldowns.CooldownApi.isOnCooldown;

public class AirPower extends Power implements IdlePower, Removeable {

    private static final String a_shot    = "air.shot";
    private static final String a_chain   = "air.chain";
    private static final String a_vacuum  = "air.vacuum";
    private static final String a_jet     = "air.jet";
    private static final String a_barrage = "air.barrage";
    private static final String a_field   = "air.field";
    private static final String a_step    = "air.step";
    private static final String a_collapse= "air.collapse";
    private static final String a_counter = "air.counter";

    private int XP_COLLAPSE;

    private static final Color C_SKY_BLUE  = Color.fromRGB(80,  160, 255);
    private static final Color C_ICE_BLUE  = Color.fromRGB(140, 200, 255);
    private static final Color C_SILVER    = Color.fromRGB(200, 210, 220);
    private static final Color C_WHITE_ION = Color.fromRGB(230, 240, 255);
    private static final Color C_DEEP_BLUE = Color.fromRGB(30,  80,  200);
    private static final Color C_CYAN      = Color.fromRGB(100, 230, 255);
    private static final Color[] AIR_COLS  = { C_SKY_BLUE, C_ICE_BLUE, C_SILVER, C_WHITE_ION };

    private int airJumps = 0;
    private boolean wasOnGround = true;
    private double lastDeltaY = 0;

    private boolean fieldActive = false;
    private BukkitRunnable fieldTask = null;

    private boolean collapseActive = false;
    private BukkitRunnable collapseTask = null;

    public AirPower(Player owner) {
        super(owner);
        XP_COLLAPSE = magicPlugin.getConfig().getInt("air.xp.collapse", 22);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute)  { passiveCounter((DamagedByExecute) ex); return; }
        if (ex instanceof MoveExecute)       { handleMove((MoveExecute) ex);           return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(a_shot,    p, this)) return; pressureShot(p, 1.0, 0); addCd(a_shot,    p); return;
            case 1: if (onCd(a_chain,   p, this)) return; airSlashChain(p);         addCd(a_chain,   p); return;
            case 2: if (onCd(a_vacuum,  p, this)) return; vacuumSphere(p);          addCd(a_vacuum,  p); return;
            case 3: if (onCd(a_jet,     p, this)) return; jetStream(p);             addCd(a_jet,     p); return;
            case 4: if (onCd(a_barrage, p, this)) return; airBarrage(p);            addCd(a_barrage, p); return;
            case 7:
                if (onCd(a_collapse, p, this)) return;
                if (!checkXp(p, XP_COLLAPSE, this)) return;
                spendXp(p, XP_COLLAPSE);
                atmosphericCollapse(p);
                addCd(a_collapse, p);
                return;
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (fieldActive) return;
        if (onCd(a_field, p, this)) return;
        compressionField(p);
        addCd(a_field, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(a_step, p, this)) return;
        supersonicStep(p);
        addCd(a_step, p);
    }

    private void pressureShot(Player p, double mult, int yawOff) {
        ArmorStand shot = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        if (yawOff != 0) dir = rotateY(dir, yawOff);
        final Vector fDir = dir;
        Random rng = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.9f, 1.8f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (shot.isDead() || t > 65) { safeRemove(shot); cancel(); return; }
                shot.teleport(shot.getLocation().add(fDir.clone().multiply(2.0)));
                Location loc = shot.getLocation();

                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(t * 60 + i * 90);
                    Vector off = new Vector(Math.cos(a) * 0.3, Math.sin(a) * 0.2, Math.sin(a) * 0.3);
                    particleApi.spawnColoredParticles(loc.clone().add(off),
                            AIR_COLS[rng.nextInt(AIR_COLS.length)], 1.1f, 1, 0.03, 0.03, 0.03);
                }
                particleApi.spawnColoredParticles(loc, C_DEEP_BLUE, 1.0f, 1, 0.04, 0.04, 0.04);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        pressureExplosion(loc, p, 3.0, 11 * mult);
                        safeRemove(shot); cancel(); return;
                    }
                }
                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    pressureExplosion(loc, p, 2.0, 7 * mult);
                    safeRemove(shot); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void pressureExplosion(Location loc, Player p, double radius, double damage) {
        loc.getWorld().playSound(loc, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.2f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2.0f);

        particleApi.spawnColoredParticles(loc, C_SKY_BLUE,  1.5f, 60, radius*0.6, radius*0.5, radius*0.6);
        particleApi.spawnColoredParticles(loc, C_WHITE_ION, 1.3f, 30, radius*0.8, radius*0.7, radius*0.8);
        particleApi.spawnColoredParticles(loc, C_CYAN,      1.2f, 20, radius*0.5, radius*0.4, radius*0.5);
        particleApi.spawnParticles(loc, Particle.CLOUD, 25, radius*0.5, radius*0.4, radius*0.5, 0.15);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(loc));
            double dmg  = Math.max(3, damage - dist * 1.5);
            ((LivingEntity) e).damage(dmg, p);
            applyPotion((LivingEntity) e, PotionEffectType.LEVITATION, 30, 1);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS,   25, 0);
            Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(2.2).setY(0.5);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.5, 0));
        }
    }

    private void airSlashChain(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1f, 1.5f);

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            new BukkitRunnable() {
                @Override public void run() {
                    if (!p.isOnline()) return;
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 1.2f + idx * 0.2f);
                    launchSlash(p, idx);
                }
            }.runTaskLater(magicPlugin, i * 7L);
        }
    }

    private void launchSlash(Player p, int idx) {
        ArmorStand slash = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        Random rng = new Random();
        double kbMult = 1.2 + idx * 0.6;
        double dmg    = 7 + idx * 3;
        Set<Entity> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (slash.isDead() || t > 30) { safeRemove(slash); cancel(); return; }
                slash.teleport(slash.getLocation().add(dir.clone().multiply(1.8)));
                Location loc = slash.getLocation();

                for (int s = -2; s <= 2; s++) {
                    Vector perp = new Vector(-dir.getZ(), 0, dir.getX()).multiply(s * 0.3);
                    Location pt = loc.clone().add(perp);
                    particleApi.spawnColoredParticles(pt, AIR_COLS[rng.nextInt(AIR_COLS.length)],
                            1.1f + idx * 0.1f, 2, 0.04, 0.06, 0.04);
                }
                if (t % 2 == 0)
                    particleApi.spawnColoredParticles(loc, C_DEEP_BLUE, 1.0f, 1, 0.05, 0.04, 0.05);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.4, 1.2, 1.4)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(dmg, p);
                    if (idx == 2) applyPotion((LivingEntity) e, PotionEffectType.LEVITATION, 20, 0);
                    Vector kb = dir.clone().multiply(kbMult).setY(0.4 + idx * 0.15);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.4, 0));
                    spawnAirBurst(e.getLocation().clone().add(0, 1, 0), 15);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.4f);
                }
                if (!loc.getBlock().isPassable()) { safeRemove(slash); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void vacuumSphere(Player p) {
        Location center = p.getEyeLocation().clone()
                .add(p.getEyeLocation().getDirection().normalize().multiply(5));

        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.9f, 1.4f);
        p.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 0.7f, 0.5f);

        Random rng = new Random();
        Set<Entity> caught = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 40) {
                    vacuumRelease(center, p, caught);
                    cancel();
                    return;
                }

                double r = 2.5 - t * 0.04;

                particleRing3D(center, Math.max(0.3, r), C_SKY_BLUE,  1.2f, 20, t * 10, 0.4, t);
                particleRing3D(center, Math.max(0.3, r * 0.7), C_ICE_BLUE, 1.1f, 16, -(t * 12), 0.3, t);
                particleCircle(center, Math.max(0.2, r * 0.5), C_CYAN, 1.0f, 12, t * 15);
                particleApi.spawnColoredParticles(center, C_WHITE_ION, 1.3f, 3, 0.1, 0.1, 0.1);

                for (Entity e : center.getWorld().getNearbyEntities(center, 8, 8, 8)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    caught.add(e);
                    Vector pull = center.toVector().subtract(e.getLocation().toVector())
                            .normalize().multiply(0.5);
                    pull.setY(pull.getY() * 0.3);
                    e.setVelocity(e.getVelocity().add(pull));
                    if (t % 8 == 0) ((LivingEntity) e).damage(1.5, p);
                }
                if (t == 35) {
                    p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.5f);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void vacuumRelease(Location center, Player p, Set<Entity> caught) {
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.8f);
        p.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.5f);

        particleApi.spawnColoredParticles(center, C_SKY_BLUE,  2.0f, 120, 3, 3, 3);
        particleApi.spawnColoredParticles(center, C_WHITE_ION, 1.8f, 60,  4, 4, 4);
        particleApi.spawnColoredParticles(center, C_CYAN,      1.5f, 40,  5, 5, 5);
        particleApi.spawnParticles(center, Particle.CLOUD, 60, 3, 3, 3, 0.3);

        for (Entity e : center.getWorld().getNearbyEntities(center, 6, 6, 6)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(center));
            double dmg  = Math.max(8, 22 - dist * 2);
            ((LivingEntity) e).damage(dmg, p);
            applyPotion((LivingEntity) e, PotionEffectType.LEVITATION, 40, 2);
            Vector kb = e.getLocation().subtract(center).toVector().normalize().multiply(3.0).setY(0.8);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.8, 0));
        }
    }

    private void jetStream(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1f, 2.0f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.8f);

        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        spawnAirBurst(p.getLocation().clone().add(0, 1, 0), 20);

        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 18) { cancel(); return; }

                p.setVelocity(dir.clone().multiply(3.5 - t * 0.1));
                p.setFallDistance(0);

                Location loc = p.getLocation().clone().add(0, 1, 0);

                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 70 + i * 120);
                    Vector off = new Vector(Math.cos(a) * 0.5, Math.sin(a) * 0.4, Math.sin(a) * 0.5);
                    particleApi.spawnColoredParticles(loc.clone().add(off),
                            AIR_COLS[rng.nextInt(AIR_COLS.length)], 1.2f, 3, 0.06, 0.05, 0.06);
                }
                particleApi.spawnColoredParticles(loc, C_DEEP_BLUE, 1.1f, 2, 0.08, 0.06, 0.08);
                particleApi.spawnParticles(loc, Particle.CLOUD, 5, 0.3, 0.2, 0.3, 0.06);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.4, 1.4, 1.4)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(12, p);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 40, 2);
                    Vector kb = dir.clone().multiply(1.5).setY(0.3);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.3, 0));
                    spawnAirBurst(e.getLocation().clone().add(0, 1, 0), 12);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void airBarrage(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.5f);
        int shots = 5, start = -20;
        for (int i = 0; i < shots; i++) {
            final int yaw = start + i * 10;
            new BukkitRunnable() {
                @Override public void run() { pressureShot(p, 0.55, yaw); }
            }.runTaskLater(magicPlugin, i * 2L);
        }
    }

    private void compressionField(Player p) {
        fieldActive = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.9f, 0.6f);
        applyPotion(p, PotionEffectType.RESISTANCE, 90, 1);
        sendActionBar(p, "§b§l✦ Compression Field ✦");

        Random rng = new Random();

        fieldTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80 || !p.isOnline()) {
                    fieldActive = false;
                    removePotion(p, PotionEffectType.RESISTANCE);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.6f, 1.8f);
                    cancel();
                    return;
                }

                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30 + t * 11);
                    Location pt = p.getLocation().clone().add(Math.cos(a)*1.1, 0.07, Math.sin(a)*1.1);
                    particleApi.spawnColoredParticles(pt, AIR_COLS[rng.nextInt(AIR_COLS.length)],
                            1.0f, 1, 0.03, 0.02, 0.03);
                }
                particleCircle(p.getLocation().clone().add(0, 0.05, 0),
                        1.1, C_DEEP_BLUE, 1.0f, 8, t * 18);

                if (t % 4 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 2.0, 2.0, 2.0)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        Vector kb = e.getLocation().subtract(p.getLocation()).toVector()
                                .normalize().multiply(2.5).setY(0.5);
                        e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.5, 0));
                        ((LivingEntity) e).damage(3, p);
                        applyPotion((LivingEntity) e, PotionEffectType.LEVITATION, 15, 0);
                        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.4f, 1.6f);
                    }
                }
                t++;
            }
        };
        fieldTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void supersonicStep(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1f, 2.0f);
        Vector dir = p.getEyeLocation().getDirection().clone().setY(0).normalize();

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            new BukkitRunnable() {
                @Override public void run() {
                    if (!p.isOnline()) return;

                    Location from = p.getLocation().clone();
                    Location dest = from.clone().add(dir.clone().multiply(5));

                    for (int s = 5; s >= 1; s--) {
                        Location try_ = from.clone().add(dir.clone().multiply(s));
                        try_.setY(try_.getY() + 0.1);
                        if (try_.getBlock().isPassable()
                                && try_.clone().add(0, 1, 0).getBlock().isPassable()) {
                            dest = try_;
                            break;
                        }
                    }
                    spawnAirBurst(from.clone().add(0, 1, 0), 12);
                    p.teleport(dest);
                    spawnAirBurst(dest.clone().add(0, 1, 0), 12);
                    p.getWorld().playSound(dest, Sound.ENTITY_BREEZE_WIND_BURST, 0.7f, 1.8f);
                    p.setFallDistance(0);

                    for (Entity e : dest.getWorld().getNearbyEntities(dest, 2.0, 2.0, 2.0)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(5, p);
                        applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 25, 1);
                        Vector kb = e.getLocation().subtract(dest).toVector().normalize().multiply(1.5).setY(0.3);
                        e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.3, 0));
                    }
                }
            }.runTaskLater(magicPlugin, idx * 5L);
        }
    }

    private void atmosphericCollapse(Player p) {
        if (collapseActive) return;
        collapseActive = true;

        p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "⬡ ATMOSPHERIC COLLAPSE ⬡");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST,   0.8f, 0.2f);

        Location center = p.getLocation().clone().add(0, 1, 0);
        Random rng = new Random();
        Set<Entity> victims = new HashSet<>();

        collapseTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { collapseActive = false; cancel(); return; }

                if (t < 50) {
                    double intensity = (double) t / 50;
                    double r = 12 - intensity * 4;

                    for (int i = 0; i < 30; i++) {
                        double a = Math.toRadians(rng.nextDouble() * 360);
                        double d = r * (0.5 + rng.nextDouble() * 0.5);
                        double h = rng.nextDouble() * 4 - 1;
                        Location pt = center.clone().add(Math.cos(a)*d, h, Math.sin(a)*d);
                        Vector toCenter = center.toVector().subtract(pt.toVector()).normalize().multiply(0.3);
                        Location mid = pt.clone().add(toCenter);
                        particleApi.spawnColoredParticles(mid,
                                AIR_COLS[rng.nextInt(AIR_COLS.length)], 1.1f, 1, 0.1, 0.1, 0.1);
                    }

                    particleRing3D(center, Math.max(0.3, 3 - intensity * 2.5),
                            C_SKY_BLUE, 1.3f, 20, t * 15, 0.5, t);
                    particleRing3D(center, Math.max(0.2, 2 - intensity * 1.8),
                            C_DEEP_BLUE, 1.2f, 16, -(t * 18), 0.4, t);

                    for (Entity e : center.getWorld().getNearbyEntities(center, 12, 8, 12)) {
                        if (e.equals(p) || e instanceof ArmorStand) continue;
                        if (!(e instanceof LivingEntity)) continue;
                        victims.add(e);
                        double strength = 0.3 + intensity * 0.5;
                        Vector pull = center.toVector().subtract(e.getLocation().toVector())
                                .normalize().multiply(strength);
                        pull.setY(pull.getY() * 0.2);
                        e.setVelocity(pull);
                        applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 6, 4);
                        if (t % 12 == 0) ((LivingEntity) e).damage(1.5, p);
                    }

                    if (t == 40) {
                        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
                        sendActionBar(p, "§b⬡ Collapsing...");
                    }
                }

                else if (t == 50) {
                    collapseRelease(center, p, rng);
                }

                else if (t > 50 && t < 70) {
                    double wave = (t - 50) * 1.2;
                    particleCircle(center, wave,       C_SKY_BLUE,  1.3f, (int)(wave*8), 0);
                    particleCircle(center, wave * 0.8, C_WHITE_ION, 1.1f, (int)(wave*6), 30);
                }

                else if (t >= 70) {
                    collapseActive = false;
                    cancel();
                    return;
                }

                t++;
            }
        };
        collapseTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void collapseRelease(Location center, Player p, Random rng) {
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM,    1f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE,      1f, 0.6f);
        p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.8f);

        particleApi.spawnColoredParticles(center, C_SKY_BLUE,  2.5f, 300, 6, 6, 6);
        particleApi.spawnColoredParticles(center, C_WHITE_ION, 2.0f, 150, 8, 8, 8);
        particleApi.spawnColoredParticles(center, C_DEEP_BLUE, 1.8f, 100, 10,10,10);
        particleApi.spawnColoredParticles(center, C_CYAN,      1.6f, 80,  7, 7, 7);
        particleApi.spawnParticles(center, Particle.CLOUD, 200, 5, 5, 5, 0.5);

        for (Entity e : center.getWorld().getNearbyEntities(center, 14, 8, 14)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(center));
            double dmg  = Math.max(10, 35 - dist * 1.8);
            ((LivingEntity) e).damage(dmg, p);
            applyPotion((LivingEntity) e, PotionEffectType.LEVITATION, 60, 3);
            applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS,  80, 0);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS,   100, 3);

            new BukkitRunnable() {
                @Override public void run() {
                    Vector kb = e.getLocation().subtract(center).toVector()
                            .normalize().multiply(4.0).setY(0.5);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.5, 0));
                }
            }.runTaskLater(magicPlugin, 35L);
        }
    }

    private void handleMove(MoveExecute ex) {
        Player p = ex.getPlayer();
        double fromY = ex.getFrom().getY();
        double toY = ex.getTo().getY();

        if (p.isOnGround()) {
            airJumps = 0;
            wasOnGround = true;
            lastDeltaY = 0;
            p.setFallDistance(0);
            return;
        }

        double deltaY = toY - fromY;
        boolean justJumped = deltaY > 0.05 && lastDeltaY <= 0.02;

        if (justJumped && !wasOnGround && airJumps < 2) {
            airJumps++;
            p.setVelocity(new Vector(p.getVelocity().getX(), 0.55, p.getVelocity().getZ()));
            spawnAirBurst(p.getLocation().clone().add(0, 0.5, 0), 10);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.8f);
        }

        wasOnGround = false;
        lastDeltaY = deltaY;
        p.setFallDistance(0);
    }

    private void passiveCounter(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (isOnCooldown(a_counter, p)) return;
        if (Math.random() > 0.30) return;
        Entity att = ex.getDamager();
        if (!(att instanceof LivingEntity)) return;
        Vector kb = att.getLocation().subtract(p.getLocation()).toVector()
                .normalize().multiply(2.0).setY(0.4);
        att.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.4, 0));
        applyPotion((LivingEntity) att, PotionEffectType.LEVITATION, 15, 0);
        spawnAirBurst(p.getLocation().clone().add(0, 1, 0), 15);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.6f);
        addCdFixed(a_counter, p, 3.0);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                applyPotion(p, PotionEffectType.SPEED, 25, 1);
                p.setFallDistance(0);
                if (isAuraEnabled(p)) {
                    particleCircle(p.getLocation().clone().add(0, 0.06, 0),
                            0.65, C_SKY_BLUE, 0.9f, 5, t * 22);
                    particleApi.spawnColoredParticles(
                            p.getLocation().clone().add(0, 0.05, 0),
                            C_DEEP_BLUE, 0.8f, 1, 0.28, 0.01, 0.28);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 25);
        return r;
    }

    @Override
    public void remove() {
        if (fieldTask    != null) { fieldTask.cancel();    fieldTask    = null; }
        if (collapseTask != null) { collapseTask.cancel(); collapseTask = null; }
        fieldActive    = false;
        collapseActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§bPressure Shot";
            case 1: return "§bAir Slash Chain";
            case 2: return "§bVacuum Sphere";
            case 3: return "§bJet Stream";
            case 4: return "§bAir Barrage";
            case 5: return "§bCompression Field";
            case 6: return "§bSupersonic Step";
            case 7: return "§b§l⬡ ATMOSPHERIC COLLAPSE §3[ULT]";
            default: return "§7none";
        }
    }

    private void spawnAirBurst(Location loc, int count) {
        particleApi.spawnColoredParticles(loc, C_SKY_BLUE,  1.2f, count,     0.35, 0.3, 0.35);
        particleApi.spawnColoredParticles(loc, C_WHITE_ION, 1.0f, count / 2, 0.4,  0.35, 0.4);
        particleApi.spawnParticles(loc, Particle.CLOUD, count / 3, 0.3, 0.25, 0.3, 0.05);
    }
}

