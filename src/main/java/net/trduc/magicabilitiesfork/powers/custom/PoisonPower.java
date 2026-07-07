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

public class PoisonPower extends Power implements IdlePower, Removeable {

    private static final String p_bolt    = "poison.bolt";
    private static final String p_slash   = "poison.slash";
    private static final String p_cloud   = "poison.cloud";
    private static final String p_strike  = "poison.strike";
    private static final String p_barrage = "poison.barrage";
    private static final String p_armor   = "poison.armor";
    private static final String p_shadow  = "poison.shadow";
    private static final String p_deluge  = "poison.deluge";
    private static final String p_counter = "poison.counter";

    private int XP_DELUGE;

    private static final Color C_POISON    = Color.fromRGB(50,  180, 50);
    private static final Color C_VENOM     = Color.fromRGB(30,  220, 80);
    private static final Color C_DARK_GRN  = Color.fromRGB(10,  90,  10);
    private static final Color C_BLACK_TOX = Color.fromRGB(10,  20,  10);
    private static final Color C_ACID      = Color.fromRGB(160, 255, 80);
    private static final Color[] TOX_COLS  = { C_POISON, C_VENOM, C_DARK_GRN };

    private final Map<UUID, Integer> venomStacks = new HashMap<>();
    private static final int MAX_STACKS = 5;

    private boolean armorActive  = false;
    private boolean delugeActive = false;
    private BukkitRunnable armorTask  = null;
    private BukkitRunnable delugeTask = null;

    public PoisonPower(Player owner) {
        super(owner);
        XP_DELUGE = magicPlugin.getConfig().getInt("poison.xp.deluge", 22);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DealDamageExecute) { passiveStack((DealDamageExecute) ex); return; }
        if (ex instanceof DamagedByExecute)    { passiveCounter((DamagedByExecute) ex);  return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(p_bolt,    p, this)) return; venomBolt(p, 1.0, 0); addCd(p_bolt,    p); return;
            case 1: if (onCd(p_slash,   p, this)) return; toxicSlash(p);         addCd(p_slash,   p); return;
            case 2: if (onCd(p_cloud,   p, this)) return; poisonCloud(p);        addCd(p_cloud,   p); return;
            case 3: if (onCd(p_strike,  p, this)) return; venomStrike(p);        addCd(p_strike,  p); return;
            case 4: if (onCd(p_barrage, p, this)) return; toxicBarrage(p);       addCd(p_barrage, p); return;
            case 7:
                if (onCd(p_deluge, p, this)) return;
                if (!checkXp(p, XP_DELUGE, this)) return;
                spendXp(p, XP_DELUGE);
                toxicDeluge(p);
                addCd(p_deluge, p);
                return;
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (armorActive) return;
        if (onCd(p_armor, p, this)) return;
        acidArmor(p);
        addCd(p_armor, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(p_shadow, p, this)) return;
        shadowVenom(p);
        addCd(p_shadow, p);
    }

    private void venomBolt(Player p, double mult, int yawOff) {
        ArmorStand bolt = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        if (yawOff != 0) dir = rotateY(dir, yawOff);
        final Vector fDir = dir;
        Random rng = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 0.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (bolt.isDead() || t > 75) { safeRemove(bolt); cancel(); return; }
                bolt.teleport(bolt.getLocation().add(fDir.clone().multiply(1.7)));
                Location loc = bolt.getLocation();

                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 50 + i * 120);
                    Vector off = new Vector(Math.cos(a) * 0.25, Math.sin(a) * 0.2, Math.sin(a) * 0.25);
                    particleApi.spawnColoredParticles(loc.clone().add(off),
                            TOX_COLS[rng.nextInt(TOX_COLS.length)], 1.0f, 2, 0.04, 0.04, 0.04);
                }
                if (t % 2 == 0)
                    particleApi.spawnColoredParticles(loc, C_BLACK_TOX, 0.9f, 1, 0.05, 0.05, 0.05);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) e;
                        le.damage(9 * mult, p);
                        applyPotion(le, PotionEffectType.POISON,   100, 1);
                        applyPotion(le, PotionEffectType.SLOWNESS,  40, 0);
                        addVenomStack(p, le);
                        spawnToxBurst(loc, 25);
                        safeRemove(bolt); cancel(); return;
                    }
                }
                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    spawnToxBurst(loc, 15);
                    safeRemove(bolt); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void toxicSlash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.5f, 0.4f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0.1).normalize();
        p.setVelocity(dir.clone().multiply(2.5));
        spawnToxBurst(p.getLocation().clone().add(0, 1, 0), 15);

        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 14) { cancel(); return; }
                Location loc = p.getLocation().clone().add(0, 1, 0);

                particleApi.spawnColoredParticles(loc, C_VENOM,     1.1f, 5, 0.25, 0.2, 0.25);
                particleApi.spawnColoredParticles(loc, C_BLACK_TOX, 0.9f, 2, 0.2,  0.15, 0.2);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    LivingEntity le = (LivingEntity) e;
                    le.damage(8, p);
                    applyPotion(le, PotionEffectType.POISON,  80, 1);
                    applyPotion(le, PotionEffectType.SLOWNESS, 30, 1);
                    addVenomStack(p, le);
                    spawnToxBurst(e.getLocation().clone().add(0, 1, 0), 15);
                    Vector kb = dir.clone().multiply(1.3).setY(0.3);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.3, 0));
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 0.7f);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void poisonCloud(Player p) {
        Location center = p.getLocation().clone().add(
                p.getLocation().getDirection().clone().setY(0).normalize().multiply(3));
        center.setY(p.getLocation().getY());

        p.getWorld().playSound(center, Sound.ENTITY_BREEZE_WHIRL, 0.8f, 0.4f);
        p.getWorld().playSound(center, Sound.BLOCK_SLIME_BLOCK_PLACE, 0.7f, 0.5f);

        Random rng = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 120) { cancel(); return; }

                for (int i = 0; i < 14; i++) {
                    double a = Math.toRadians(rng.nextDouble() * 360);
                    double d = rng.nextDouble() * 2.5;
                    double h = rng.nextDouble() * 2.5;
                    Location pt = center.clone().add(Math.cos(a)*d, h, Math.sin(a)*d);
                    Color c = TOX_COLS[rng.nextInt(TOX_COLS.length)];
                    particleApi.spawnColoredParticles(pt, c, 1.0f, 1, 0.12, 0.08, 0.12);
                    if (rng.nextInt(4) == 0)
                        particleApi.spawnColoredParticles(pt, C_BLACK_TOX, 0.8f, 1, 0.1, 0.06, 0.1);
                }

                particleCircle(center.clone().add(0, 0.1, 0), 2.5, C_DARK_GRN, 1.0f, 12, t * 10);

                if (t % 8 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 2.6, 2.6, 2.6)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;
                        applyPotion(le, PotionEffectType.POISON,   60, 2);
                        applyPotion(le, PotionEffectType.SLOWNESS,  40, 1);
                        le.damage(1.5, p);
                        if (t % 24 == 0) addVenomStack(p, le);
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void venomStrike(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SLIME_BLOCK_HIT, 0.8f, 0.5f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0).normalize();
        Location start = p.getEyeLocation().clone();
        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                if (step > 18) { cancel(); return; }
                Location cur = start.clone().add(dir.clone().multiply(step * 1.1));

                for (int h = 0; h < 4; h++) {
                    double wobble = Math.sin(step * 0.9 + h) * 0.35;
                    Vector perp = new Vector(-dir.getZ(), 0, dir.getX()).multiply(wobble);
                    Location pt = cur.clone().add(0, h * 0.35 - 0.2, 0).add(perp);
                    Color c = TOX_COLS[rng.nextInt(TOX_COLS.length)];
                    particleApi.spawnColoredParticles(pt, c, 1.1f, 1, 0.05, 0.05, 0.05);
                    if (rng.nextInt(3) == 0)
                        particleApi.spawnColoredParticles(pt, C_BLACK_TOX, 0.9f, 1, 0.04, 0.04, 0.04);
                }

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 1.3, 1.8, 1.3)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    LivingEntity le = (LivingEntity) e;
                    le.damage(14, p);
                    applyPotion(le, PotionEffectType.POISON,   60, 3);
                    applyPotion(le, PotionEffectType.SLOWNESS,  60, 2);
                    applyPotion(le, PotionEffectType.WEAKNESS,  50, 1);
                    addVenomStack(p, le);
                    addVenomStack(p, le);
                    spawnToxBurst(e.getLocation().clone().add(0, 1, 0), 20);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.5f);
                }
                if (!cur.getBlock().isPassable()) { cancel(); return; }
                step++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void toxicBarrage(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.5f);
        int bolts = 5, start = -20;
        for (int i = 0; i < bolts; i++) {
            final int yaw = start + i * 10;
            new BukkitRunnable() {
                @Override public void run() { venomBolt(p, 0.55, yaw); }
            }.runTaskLater(magicPlugin, i * 3L);
        }
    }

    private void acidArmor(Player p) {
        armorActive = true;
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 1f, 0.5f);
        applyPotion(p, PotionEffectType.RESISTANCE, 90, 0);
        sendActionBar(p, "§2§l✦ Acid Armor ✦");

        Random rng = new Random();

        armorTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80 || !p.isOnline()) {
                    armorActive = false;
                    removePotion(p, PotionEffectType.RESISTANCE);
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SLIME_BLOCK_BREAK, 0.7f, 1.2f);
                    cancel();
                    return;
                }

                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(i * 36 + t * 9);
                    Location pt = p.getLocation().clone().add(Math.cos(a)*1.0, 0.08, Math.sin(a)*1.0);
                    Color c = rng.nextBoolean() ? C_ACID : C_VENOM;
                    particleApi.spawnColoredParticles(pt, c, 1.1f, 1, 0.03, 0.02, 0.03);
                }
                particleCircle(p.getLocation().clone().add(0, 0.06, 0),
                        1.0, C_DARK_GRN, 1.0f, 8, t * 18);

                if (t % 5 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.8, 1.8, 1.8)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;
                        le.damage(4, p);
                        applyPotion(le, PotionEffectType.POISON,   80, 1);
                        applyPotion(le, PotionEffectType.SLOWNESS,  40, 1);
                        addVenomStack(p, le);
                        e.getWorld().playSound(e.getLocation(), Sound.BLOCK_SLIME_BLOCK_HIT, 0.5f, 0.8f);
                    }
                }
                t++;
            }
        };
        armorTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void shadowVenom(Player p) {
        LivingEntity target = getInSight(p, 18, 0.8);
        if (target == null) {
            sendActionBar(p, "§cNo target!");
            return;
        }
        Location from = p.getLocation().clone();
        Vector behind = target.getLocation().getDirection().normalize().multiply(1.8);
        Location dest  = target.getLocation().clone().subtract(behind);
        dest.setDirection(target.getLocation().toVector().subtract(dest.toVector()));
        dest.setY(dest.getY() + 0.1);

        spawnToxBurst(from.clone().add(0, 1, 0), 20);
        p.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
        p.teleport(dest);
        spawnToxBurst(dest.clone().add(0, 1, 0), 20);
        p.getWorld().playSound(dest, Sound.BLOCK_SLIME_BLOCK_PLACE, 0.7f, 0.4f);

        target.damage(12, p);
        applyPotion(target, PotionEffectType.POISON,   120, 4);
        applyPotion(target, PotionEffectType.WEAKNESS,  80, 1);
        applyPotion(target, PotionEffectType.SLOWNESS,  60, 2);

        addVenomStack(p, target);
        addVenomStack(p, target);
        addVenomStack(p, target);
        spawnToxBurst(target.getLocation().clone().add(0, 1, 0), 35);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9f, 0.5f);
    }

    private void toxicDeluge(Player p) {
        if (delugeActive) return;
        delugeActive = true;

        p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "☠ TOXIC DELUGE ☠");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 1f, 0.3f);

        Random rng = new Random();
        Location origin = p.getLocation().clone();

        delugeTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { delugeActive = false; cancel(); return; }

                if (t < 30) {
                    double grow = (double) t / 30;
                    double r = 4 + grow * 16;

                    for (int i = 0; i < 20; i++) {
                        double a = Math.toRadians(rng.nextDouble() * 360);
                        double d = rng.nextDouble() * r;
                        Location cp = origin.clone().add(Math.cos(a)*d, 4 + rng.nextDouble()*2, Math.sin(a)*d);
                        particleApi.spawnColoredParticles(cp, TOX_COLS[rng.nextInt(TOX_COLS.length)],
                                1.1f, 2, 0.4, 0.2, 0.4);
                    }
                    if (t == 29) {
                        p.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.4f);
                    }
                }

                else if (t < 120) {

                    for (int i = 0; i < 25; i++) {
                        double a = Math.toRadians(rng.nextDouble() * 360);
                        double d = rng.nextDouble() * 20;
                        Location cp = origin.clone().add(Math.cos(a)*d, 4 + rng.nextDouble()*1.5, Math.sin(a)*d);
                        particleApi.spawnColoredParticles(cp, TOX_COLS[rng.nextInt(TOX_COLS.length)],
                                1.1f, 1, 0.35, 0.15, 0.35);
                    }

                    if (t % 3 == 0) {
                        int drops = 4 + rng.nextInt(4);
                        for (int d = 0; d < drops; d++) {
                            double a = Math.toRadians(rng.nextDouble() * 360);
                            double dist = rng.nextDouble() * 18;
                            Location dropStart = origin.clone().add(
                                    Math.cos(a)*dist, 5, Math.sin(a)*dist);
                            adjustToGround(dropStart);
                            Location groundLoc = dropStart.clone();

                            new BukkitRunnable() {
                                double y = 5;
                                @Override public void run() {
                                    if (y <= 0) {

                                        Location impact = groundLoc.clone().add(
                                                Math.cos(rng.nextDouble()*Math.PI*2)*dist, 0,
                                                Math.sin(rng.nextDouble()*Math.PI*2)*dist);
                                        impact.setY(groundLoc.getY());
                                        particleApi.spawnColoredParticles(impact,
                                                C_VENOM, 1.2f, 8, 0.3, 0.2, 0.3);
                                        particleApi.spawnColoredParticles(impact,
                                                C_BLACK_TOX, 0.9f, 3, 0.2, 0.15, 0.2);

                                        for (Entity e : impact.getWorld().getNearbyEntities(impact, 2, 2, 2)) {
                                            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                                            LivingEntity le = (LivingEntity) e;
                                            le.damage(4, p);
                                            applyPotion(le, PotionEffectType.POISON,  80, 2);
                                            applyPotion(le, PotionEffectType.SLOWNESS, 30, 1);
                                            addVenomStack(p, le);
                                        }
                                        cancel();
                                        return;
                                    }
                                    Location falling = groundLoc.clone().add(0, y, 0);
                                    particleApi.spawnColoredParticles(falling,
                                            C_POISON, 1.0f, 2, 0.05, 0.05, 0.05);
                                    y -= 0.7;
                                }
                            }.runTaskTimer(magicPlugin, 0, 1);
                        }
                    }

                    if (t % 15 == 0)
                        p.getWorld().playSound(origin, Sound.BLOCK_SLIME_BLOCK_HIT, 0.5f, 0.4f);
                }

                else {
                    delugeFinale(p, origin, rng);
                    delugeActive = false;
                    cancel();
                    return;
                }
                t++;
            }
        };
        delugeTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void delugeFinale(Player p, Location origin, Random rng) {
        p.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.4f);
        p.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.4f);

        particleApi.spawnColoredParticles(origin.clone().add(0, 2, 0), C_VENOM,     2.0f, 200, 5, 4, 5);
        particleApi.spawnColoredParticles(origin.clone().add(0, 2, 0), C_DARK_GRN,  1.8f, 100, 6, 5, 6);
        particleApi.spawnColoredParticles(origin.clone().add(0, 2, 0), C_BLACK_TOX, 1.5f, 80,  7, 6, 7);

        for (Entity e : origin.getWorld().getNearbyEntities(origin, 20, 6, 20)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) e;
            double dist = Math.max(0.5, e.getLocation().distance(origin));
            double dmg  = Math.max(6, 20 - dist * 0.8);
            le.damage(dmg, p);
            applyPotion(le, PotionEffectType.POISON,          160, 3);
            applyPotion(le, PotionEffectType.SLOWNESS,        120, 2);
            applyPotion(le, PotionEffectType.WEAKNESS,        100, 1);
            applyPotion(le, PotionEffectType.MINING_FATIGUE,   80, 1);

            venomStacks.put(e.getUniqueId(), MAX_STACKS - 1);
            addVenomStack(p, le);
            spawnToxBurst(e.getLocation().clone().add(0, 1, 0), 20);
        }

        new BukkitRunnable() {
            double r = 0.5;
            @Override public void run() {
                if (r > 22) { cancel(); return; }
                particleCircle(origin.clone().add(0, 0.2, 0), r, C_VENOM, 1.1f, (int)(r*8), 0);
                r += 1.0;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void addVenomStack(Player p, LivingEntity target) {
        UUID uid = target.getUniqueId();
        int stacks = venomStacks.getOrDefault(uid, 0) + 1;

        if (stacks >= MAX_STACKS) {

            venomStacks.put(uid, 0);
            target.damage(18, p);
            applyPotion(target, PotionEffectType.POISON,   60, 4);
            applyPotion(target, PotionEffectType.SLOWNESS,  60, 3);
            spawnStackBurst(target.getLocation().clone().add(0, 1, 0));
            target.getWorld().playSound(target.getLocation(),
                    Sound.ENTITY_GENERIC_HURT, 0.8f, 0.4f);
            sendActionBar(p, "§2§l☠ VENOM BURST!");
        } else {
            venomStacks.put(uid, stacks);

            particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 2.2, 0),
                    C_ACID, 1.1f, stacks * 2, 0.15, 0.05, 0.15);
        }

        new BukkitRunnable() {
            @Override public void run() {
                int cur = venomStacks.getOrDefault(uid, 0);
                if (cur > 0) venomStacks.put(uid, cur - 1);
            }
        }.runTaskLater(magicPlugin, 160L);
    }

    private void spawnStackBurst(Location loc) {
        particleApi.spawnColoredParticles(loc, C_ACID,      1.5f, 40, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(loc, C_VENOM,     1.3f, 30, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(loc, C_BLACK_TOX, 1.0f, 20, 0.7, 0.7, 0.7);
    }

    private void passiveCounter(DamagedByExecute ex) {
        Player p = ex.getPlayer();

        if (!armorActive) return;
        if (isOnCooldown(p_counter, p)) return;
        if (!(ex.getDamager() instanceof LivingEntity)) return;
        LivingEntity att = (LivingEntity) ex.getDamager();
        applyPotion(att, PotionEffectType.POISON,   60, 2);
        applyPotion(att, PotionEffectType.SLOWNESS,  40, 1);
        att.damage(3, p);
        spawnToxBurst(att.getLocation().clone().add(0, 1, 0), 12);
        addCdFixed(p_counter, p, 1.5);
    }

    private void passiveStack(DealDamageExecute ex) {
        Player p = ex.getPlayer();
        if (!(ex.getTarget() instanceof LivingEntity)) return;
        LivingEntity le = (LivingEntity) ex.getTarget();
        if (le instanceof ArmorStand) return;
        addVenomStack(p, le);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (isAuraEnabled(p)) {
                    particleCircle(p.getLocation().clone().add(0, 0.06, 0),
                            0.65, C_POISON, 0.9f, 5, t * 23);
                    particleApi.spawnColoredParticles(
                            p.getLocation().clone().add(0, 0.05, 0),
                            C_BLACK_TOX, 0.8f, 1, 0.28, 0.01, 0.28);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 28);
        return r;
    }

    @Override
    public void remove() {
        if (armorTask  != null) { armorTask.cancel();  armorTask  = null; }
        if (delugeTask != null) { delugeTask.cancel(); delugeTask = null; }
        armorActive  = false;
        delugeActive = false;
        venomStacks.clear();
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§2Venom Bolt";
            case 1: return "§2Toxic Slash";
            case 2: return "§2Poison Cloud";
            case 3: return "§2Venom Strike";
            case 4: return "§2Toxic Barrage";
            case 5: return "§2Acid Armor";
            case 6: return "§2Shadow Venom";
            case 7: return "§2§l☠ TOXIC DELUGE §a[ULT]";
            default: return "§7none";
        }
    }

    private void spawnToxBurst(Location loc, int count) {
        Random rng = new Random();
        particleApi.spawnColoredParticles(loc, C_VENOM,     1.2f, count,     0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_POISON,    1.0f, count / 2, 0.35, 0.35, 0.35);
        particleApi.spawnColoredParticles(loc, C_BLACK_TOX, 0.8f, count / 3, 0.4, 0.4, 0.4);
    }
}

