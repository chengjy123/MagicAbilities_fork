package net.trduc.magicabilitiesfork.misc;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.Power;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.cooldowns.Cooldowns.cooldowns;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
public final class PowerUtils {

    private PowerUtils() {}

    public static boolean isAuraEnabled(Player p) {
        if (!players.containsKey(p)) return true;
        return players.get(p).isAuraEnabled();
    }
    public static int calcTotalXp(Player p) {
        int   lvl  = p.getLevel();
        float prog = p.getExp();
        return xpToReachLevel(lvl) + Math.round(prog * xpForLevel(lvl));
    }
    public static boolean checkXp(Player p, int cost, Power power) {
        if (cost <= 0) return true;
        if (calcTotalXp(p) >= cost) return true;
        sendActionBar(p, "§cNot enough XP! Need " + cost);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.5f);
        return false;
    }
    public static void spendXp(Player p, int cost) {
        if (cost <= 0) return;
        int newTotal = Math.max(0, calcTotalXp(p) - cost);
        p.setLevel(0);
        p.setExp(0f);
        p.setTotalExperience(0);
        if (newTotal > 0) p.giveExp(newTotal);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.6f);
    }
    public static void giveXp(Player p, int amount) {
        if (amount > 0) p.giveExp(amount);
    }

    public static int xpToReachLevel(int level) {
        if (level <= 0)  return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int)(2.5 * level * level - 40.5 * level + 360);
        return (int)(4.5 * level * level - 162.5 * level + 2220);
    }

    public static int xpForLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }
    public static LivingEntity getNearestTarget(Player p, double radius) {
        LivingEntity best = null;
        double bestDist = radius;
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            double d = e.getLocation().distance(p.getLocation());
            if (d < bestDist) { bestDist = d; best = (LivingEntity) e; }
        }
        return best;
    }
    public static LivingEntity getNearestEnemy(Player p, double radius) {
        LivingEntity best = null;
        double bestDist = radius;
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
            if (e.equals(p) || !(e instanceof LivingEntity) || e instanceof Player) continue;
            double d = e.getLocation().distance(p.getLocation());
            if (d < bestDist) { bestDist = d; best = (LivingEntity) e; }
        }
        return best;
    }
    public static LivingEntity getInSight(Player p, double radius, double dotMin) {
        LivingEntity best      = null;
        double       bestScore = radius + 1;
        Vector       look      = p.getEyeLocation().getDirection().normalize();
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            Vector toE = e.getLocation().clone().add(0, 1, 0).subtract(p.getEyeLocation()).toVector();
            if (toE.lengthSquared() < 0.01) continue;
            double dot = look.dot(toE.normalize());
            if (dot < dotMin) continue;
            double score = e.getLocation().distance(p.getLocation()) / dot;
            if (score < bestScore) { bestScore = score; best = (LivingEntity) e; }
        }
        return best;
    }
    public static LivingEntity findTargetFromLoc(Player owner, Location from, double radius) {
        LivingEntity best = null;
        double bestDist = radius;
        for (Entity e : from.getWorld().getNearbyEntities(from, radius, radius, radius)) {
            if (e.equals(owner) || !(e instanceof LivingEntity) || e instanceof Player) continue;
            double d = e.getLocation().distance(from);
            if (d < bestDist) { bestDist = d; best = (LivingEntity) e; }
        }
        return best;
    }
    public static List<LivingEntity> getNearbyTargets(Player p, double radius) {
        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            list.add((LivingEntity) e);
        }
        return list;
    }
    public static boolean onCd(String key, Player p, Power power) {
        if (CooldownApi.isOnCooldown(key, p)) {
            power.onCooldownInfo(CooldownApi.getCooldownForPlayerLong(key, p));
            return true;
        }
        return false;
    }
    public static void addCd(String key, Player p) {
        CooldownApi.addCooldown(key, p, cooldowns.get(key));
    }
    public static void addCd(String key, Player p, double multiplier) {
        CooldownApi.addCooldown(key, p, cooldowns.get(key) * multiplier);
    }
    public static void addCdFixed(String key, Player p, double seconds) {
        CooldownApi.addCooldown(key, p, seconds);
    }
    public static Vector rotateY(Vector v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        return new Vector(
                v.getX() * cos + v.getZ() * sin,
                v.getY(),
                -v.getX() * sin + v.getZ() * cos
        );
    }
    public static Vector pitchRotate(Vector v, double degrees) {
        Vector axis = new Vector(-v.getZ(), 0, v.getX());
        if (axis.lengthSquared() < 0.0001) axis = new Vector(1, 0, 0);
        axis.normalize();
        double rad = Math.toRadians(degrees), cos = Math.cos(rad), sin = Math.sin(rad);
        return v.clone().multiply(cos)
                .add(axis.clone().crossProduct(v).multiply(sin))
                .add(axis.clone().multiply(axis.dot(v) * (1 - cos)))
                .normalize();
    }
    public static boolean isVecFinite(Vector v) {
        return !Double.isNaN(v.getX())      && !Double.isNaN(v.getY())      && !Double.isNaN(v.getZ())
            && !Double.isInfinite(v.getX()) && !Double.isInfinite(v.getY()) && !Double.isInfinite(v.getZ());
    }
    public static Location orbitPoint(Location center, double radius, double angleDeg, double yOffset) {
        double rad = Math.toRadians(angleDeg);
        return center.clone().add(Math.cos(rad) * radius, yOffset, Math.sin(rad) * radius);
    }
    public static Vector knockbackVector(Location from, Entity target, double strength, double yForce) {
        Vector kb = target.getLocation().subtract(from).toVector();
        if (!isVecFinite(kb) || kb.lengthSquared() < 0.01) {
            Random rng = new Random();
            kb = new Vector(rng.nextDouble() - 0.5, 0.1, rng.nextDouble() - 0.5);
        }
        return kb.normalize().multiply(strength).setY(yForce);
    }

    public static Location getRaycastTarget(Player p, int maxDist) {
        Location cur = p.getEyeLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < maxDist * 2; i++) {
            cur.add(dir.clone().multiply(0.5));
            if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                cur.subtract(dir.clone().multiply(0.5));
                break;
            }
        }
        return cur;
    }
    public static void adjustToGround(Location loc) {
        for (int i = 0; i < 10; i++) {
            if (!loc.clone().subtract(0, 1, 0).getBlock().isPassable()) break;
            loc.subtract(0, 1, 0);
        }
        for (int i = 0; i < 10; i++) {
            if (loc.getBlock().isPassable()) break;
            loc.add(0, 1, 0);
        }
    }
    public static Location getGroundBelow(Location loc) {
        Location g = loc.clone();
        while (g.getBlock().isPassable() && !g.getBlock().isLiquid() && g.getY() > -64)
            g.add(0, -1, 0);
        return g.add(0, 1, 0);
    }
    public static void safeHeal(Player p, double amount) {
        if (amount <= 0) return;
        p.setHealth(Math.min(getMaxHp(p), p.getHealth() + amount));
    }
    public static double getMaxHp(Player p) {
        AttributeInstance ai = p.getAttribute(Attribute.MAX_HEALTH);
        return ai != null ? ai.getValue() : 20.0;
    }
    public static void safeSetHealth(LivingEntity e, double hp) {
        AttributeInstance ai = e.getAttribute(Attribute.MAX_HEALTH);
        double max = ai != null ? ai.getValue() : 20.0;
        e.setHealth(Math.max(0.01, Math.min(max, hp)));
    }
    public static ArmorStand spawnProjectile(Player p) {
        return p.getWorld().spawn(p.getEyeLocation().clone(), ArmorStand.class, en -> {
            en.setVisible(false);
            en.setGravity(false);
            en.setSmall(true);
            en.setMarker(true);
        });
    }
    public static ArmorStand spawnProjectileAt(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false);
            en.setGravity(false);
            en.setSmall(true);
            en.setMarker(true);
        });
    }
    public static void safeRemove(ArmorStand as) {
        if (as != null && !as.isDead()) as.remove();
    }
    public static void spawnFireRing(Location center, int radius, long delayTicks) {
        final int r = Math.max(1, radius);
        new BukkitRunnable() {
            @Override public void run() {
                Location ground = getGroundBelow(center);
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (x * x + z * z > r * r) continue;
                        Block b = ground.clone().add(x, 0, z).getBlock();
                        if (b.isPassable() && !b.isLiquid() && b.getType() != Material.FIRE)
                            b.setType(Material.FIRE);
                    }
                }
                new BukkitRunnable() {
                    @Override public void run() {
                        for (int x = -r; x <= r; x++)
                            for (int z = -r; z <= r; z++) {
                                if (x * x + z * z > r * r) continue;
                                Block b = ground.clone().add(x, 0, z).getBlock();
                                if (b.getType() == Material.FIRE) b.setType(Material.AIR);
                            }
                    }
                }.runTaskLater(magicPlugin, delayTicks);
            }
        }.runTask(magicPlugin);
    }
    public static void restoreBlocks(HashMap<Block, Material> blocks, long delayTicks) {
        new BukkitRunnable() {
            @Override public void run() {
                for (Block b : blocks.keySet()) b.setType(blocks.get(b));
                blocks.clear();
            }
        }.runTaskLater(magicPlugin, delayTicks);
    }
    public static BukkitRunnable cancelTask(BukkitRunnable task) {
        if (task != null) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        return null;
    }
    public static void particleCircle(Location center, double radius,
                                       Color color, float size, int steps, double rotate) {
        for (int i = 0; i < steps; i++) {
            double angle = Math.toRadians(i * (360.0 / steps) + rotate);
            Location lp = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            particleApi.spawnColoredParticles(lp, color, size, 1, 0, 0, 0);
        }
    }
    public static void particleRing3D(Location center, double radius, Color color, float size,
                                       int steps, double rotate, double yWave, int tick) {
        for (int i = 0; i < steps; i++) {
            double angle = Math.toRadians(i * (360.0 / steps) + rotate);
            double y     = yWave > 0 ? Math.sin(tick * 0.07 + i * 0.6) * yWave : 0;
            Location lp  = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            particleApi.spawnColoredParticles(lp, color, size, 1, 0.03, 0.03, 0.03);
        }
    }
    public static void particleLine(Location from, Location to, double step,
                                     Color color, float size) {
        Vector dir  = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        if (dist < 0.01) return;
        Vector unit = dir.normalize().multiply(step);
        Location cur = from.clone();
        int steps = (int)(dist / step);
        for (int i = 0; i <= steps; i++) {
            particleApi.spawnColoredParticles(cur, color, size, 1, 0.04, 0.04, 0.04);
            cur.add(unit);
        }
    }
    public static void applyPotion(LivingEntity e, PotionEffectType type, int ticks, int amplifier) {
        e.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, true));
    }
    public static void applyPotionSilent(LivingEntity e, PotionEffectType type, int ticks, int amplifier) {
        e.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, false));
    }
    public static void removePotion(LivingEntity e, PotionEffectType type) {
        if (e.hasPotionEffect(type)) e.removePotionEffect(type);
    }
    public static void sendActionBar(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
    public static double cfgDouble(String path, double defaultVal) {
        return magicPlugin.getConfig().getDouble(path, defaultVal);
    }
    public static int cfgInt(String path, int defaultVal) {
        return magicPlugin.getConfig().getInt(path, defaultVal);
    }
    public static boolean cfgBool(String path, boolean defaultVal) {
        return magicPlugin.getConfig().getBoolean(path, defaultVal);
    }
}

