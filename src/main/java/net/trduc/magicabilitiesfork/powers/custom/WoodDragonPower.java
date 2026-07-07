package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

public class WoodDragonPower extends Power implements IdlePower, Removeable {

    private static final String wd_roar   = "wooddragon.roar";
    private static final String wd_blades = "wooddragon.blades";
    private static final String wd_net    = "wooddragon.net";
    private static final String wd_charge = "wooddragon.charge";
    private static final String wd_grove  = "wooddragon.grove";
    private static final String wd_roots  = "wooddragon.roots";

    private static final Color C_FOREST = Color.fromRGB(15,  90,  15);
    private static final Color C_LIME   = Color.fromRGB(40, 160,  30);
    private static final Color C_MOSS   = Color.fromRGB(30, 100,  25);
    private static final Color C_LEAF   = Color.fromRGB(55, 170,  40);
    private static final Color C_BARK   = Color.fromRGB(70,  45,  15);
    private static final Color C_VINE   = Color.fromRGB(20, 120,  40);
    private static final Color C_POLLEN = Color.fromRGB(140,180,  30);

    private static final Color[] WOOD_COLS = { C_FOREST, C_LIME, C_MOSS, C_LEAF, C_VINE };
    private static final Color[] AURA_COLS = { C_FOREST, C_LIME, C_LEAF, C_POLLEN, C_MOSS };

    private static final Set<String> DRAGON_POWERS = new HashSet<>(Arrays.asList(
            "IceDragonPower", "WoodDragonPower"));
    private static final String LONG_WEI_APPLIED = "longwei_applied";
    private static final String LW_META_HP = "lw_hp_base";
    private static final String LW_META_AR = "lw_ar_base";

    private boolean charging = false;
    private final Set<UUID> longWeiTargets = new HashSet<>();
    private final Set<UUID> longWeiInRange = new HashSet<>();
    private final List<Location> netTraps  = new ArrayList<>();

    public WoodDragonPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute)    { handleDamage((DamagedExecute) ex);           return; }
        if (ex instanceof DealDamageExecute) { applyLongWeiArmorBreak((DealDamageExecute) ex); }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeftClick((LeftClickExecute) ex); }
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p   = ex.getPlayer();
        int   slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(wd_roar, p, this)) return; longRoar(p);        addCd(wd_roar, p);   return;
            case 1: if (onCd(wd_blades, p, this)) return; mocKiemVu(p);       addCd(wd_blades, p); return;
            case 2: if (onCd(wd_net, p, this)) return; thienLaDiaVong(p);  addCd(wd_net, p);    return;
            case 3: if (charging)           return;
                    if (onCd(wd_charge, p, this)) return; cuongLamXung(p);    return;
            case 4: if (onCd(wd_grove, p, this)) return; sinhMenhLam(p);     addCd(wd_grove, p);  return;
            case 5: if (onCd(wd_roots, p, this)) return; canVuongPhanNo(p);  addCd(wd_roots, p);
        }
    }

    private void longRoar(Player p) {
        Location loc = p.getLocation().clone().add(0, 0.5, 0);
        Vector   fwd = p.getEyeLocation().getDirection().clone().setY(0).normalize();

        p.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);
        p.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR,       1f, 0.6f);
        p.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ATTACK,     0.7f, 0.4f);
        p.sendMessage(ChatColor.GREEN + "✦ 龍吼 — DRAGON ROAR!");

        Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            double dist = 0.5; int t = 0;
            @Override public void run() {
                if (dist > 8 || t > 12) { cancel(); return; }
                for (int i = -4; i <= 4; i++) {
                    double ang = Math.toRadians(i * 22.5);
                    double vx  = fwd.getX()*Math.cos(ang) - fwd.getZ()*Math.sin(ang);
                    double vz  = fwd.getX()*Math.sin(ang) + fwd.getZ()*Math.cos(ang);
                    Location tip = loc.clone().add(vx*dist, 0, vz*dist);

                    int groundY = tip.getWorld().getHighestBlockYAt(tip);
                    Location ground = tip.clone();
                    ground.setY(groundY + 1);
                    for (double h = 0; h <= 1.5; h += 0.5) {
                        Location lp = ground.clone().add(
                                (Math.random()-0.5)*0.2, h, (Math.random()-0.5)*0.2);
                        particleApi.spawnColoredParticles(lp,
                                WOOD_COLS[(t + i + 5) % WOOD_COLS.length], 1.4f, 2, 0.04, 0.06, 0.04);
                    }
                    if (t % 3 == 0)
                        particleApi.spawnParticles(ground.clone().add(0, 0.5, 0),
                                Particle.CHERRY_LEAVES, 2, 0.15, 0.25, 0.15, 0.07);
                    for (Entity e : ground.getWorld().getNearbyEntities(ground, 1.1, 2.0, 1.1)) {
                        if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                        if (!(e instanceof LivingEntity)) continue;
                        hit.add(e.getUniqueId());
                        ((LivingEntity) e).damage(10, p);
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON,     40, 1, false, true));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   40, 9, false, false));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128, false, false));
                        spawnRootBurst(e.getLocation(), 1.2);
                    }
                }
                if (t % 4 == 0)
                    p.getWorld().playSound(loc, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.5f, 0.7f);
                dist += 0.84; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void mocKiemVu(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_WOOD_BREAK,          0.8f, 1.4f);

        int[] yawOffsets = {-15, 0, 15};
        for (int i = 0; i < 3; i++) {
            final int yaw = yawOffsets[i];
            final int idx = i;
            new BukkitRunnable() {
                @Override public void run() { shootWoodBlade(p, yaw); }
            }.runTaskLater(magicPlugin, idx * 3L);
        }
    }

    private void shootWoodBlade(Player p, int yawDeg) {
        ArmorStand blade = spawnAs(p.getEyeLocation().clone());
        Vector base  = p.getEyeLocation().getDirection().clone().normalize();
        Vector dir   = yawRotate(base, yawDeg);
        Vector right = yawRotate(dir.clone().setY(0).normalize(), 90).normalize();
        Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (blade.isDead() || t > 38) { safeRemove(blade); cancel(); return; }
                blade.teleport(blade.getLocation().add(dir.clone().multiply(1.5)));
                Location loc = blade.getLocation();
                for (double s = -0.65; s <= 0.65; s += 0.33) {
                    Location edge = loc.clone().add(right.clone().multiply(s));
                    particleApi.spawnColoredParticles(edge,
                            WOOD_COLS[(t + Math.abs((int)(s*3))) % WOOD_COLS.length],
                            1.1f, 1, 0.04, 0.04, 0.04);
                }
                particleApi.spawnColoredParticles(loc, C_BARK, 1.3f, 1, 0.05, 0.05, 0.05);
                if (t % 4 == 0)
                    particleApi.spawnParticles(loc, Particle.CHERRY_LEAVES, 1, 0.1, 0.1, 0.1, 0.06);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 0.9, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(12, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0, false, true));
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_LIME, 1.4f, 10, 0.35, 0.4, 0.35);
                    particleApi.spawnParticles(e.getLocation().clone().add(0,1,0),
                            Particle.CHERRY_LEAVES, 6, 0.3, 0.4, 0.3, 0.1);
                    e.getWorld().playSound(e.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.5f, 1.2f);
                }
                if (!loc.getBlock().isPassable()) { safeRemove(blade); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void thienLaDiaVong(Player p) {
        Location center = getRaycastGround(p, 20);
        p.getWorld().playSound(center, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ATTACK,   0.6f, 0.7f);
        p.sendMessage(ChatColor.GREEN + "✦ 天羅地網 — HEAVEN NET!");
        netTraps.add(center);

        Set<UUID> trapped = new HashSet<>();
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60) {
                    netTraps.remove(center);
                    particleApi.spawnColoredParticles(center.clone().add(0,0.1,0),
                            C_MOSS, 1.2f, 15, 5, 0.1, 5);
                    cancel(); return;
                }
                for (int ring = 0; ring < 2; ring++) {
                    double rad = 2.0 + ring * 2.8;
                    for (int i = 0; i < 12; i++) {
                        double a = Math.toRadians(i * 30 + t * (5 + ring * 3));
                        Location lp = center.clone().add(Math.cos(a)*rad, 0.08, Math.sin(a)*rad);
                        particleApi.spawnColoredParticles(lp,
                                WOOD_COLS[(i + ring + t) % WOOD_COLS.length],
                                0.9f + ring*0.07f, 1, 0.04, 0.04, 0.04);
                    }
                }
                for (int branch = 0; branch < 3; branch++) {
                    double a    = Math.toRadians(branch * 120 + t * 4);
                    double bLen = 3.5 + Math.sin(t * 0.2 + branch) * 1.0;
                    for (double s = 0.5; s <= bLen; s += 0.9) {
                        Location lp = center.clone().add(Math.cos(a)*s, 0.08, Math.sin(a)*s);
                        particleApi.spawnColoredParticles(lp, C_BARK, 0.95f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 5 == 0)
                    particleApi.spawnParticles(center.clone().add(
                            (r.nextDouble()-0.5)*10, 0.5+r.nextDouble(), (r.nextDouble()-0.5)*10),
                            Particle.CHERRY_LEAVES, 1, 0.2, 0.2, 0.2, 0.04);

                if (t % 10 == 0)
                    center.getWorld().playSound(center, Sound.BLOCK_ROOTED_DIRT_HIT, 0.3f, 0.8f);
                if (t % 5 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 6, 3, 6)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        if (trapped.contains(e.getUniqueId())) continue;
                        trapped.add(e.getUniqueId());
                        trapEntity((LivingEntity) e, p);
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void trapEntity(LivingEntity target, Player p) {
        Location tl = target.getLocation().clone();
        target.getWorld().playSound(tl, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.8f, 0.6f);
        spawnRootBurst(tl, 1.5);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   65, 9, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 65, 128, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,    65, 0, false, false));
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 3 || target.isDead()) { cancel(); return; }
                target.damage(4, p);
                for (int i = 0; i < 5; i++) {
                    double a = Math.toRadians(i * 72 + t * 40);
                    Location lp = tl.clone().add(0,1,0).add(Math.cos(a)*0.9, 0, Math.sin(a)*0.9);
                    particleApi.spawnColoredParticles(lp, WOOD_COLS[i % WOOD_COLS.length], 1.1f, 1, 0.04, 0.1, 0.04);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 20L, 20L);
    }
    private void cuongLamXung(Player p) {
        charging = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ATTACK,   1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.7f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0.1).normalize();
        p.setVelocity(dir.clone().multiply(2.6));

        Set<UUID> hit = new HashSet<>();
        List<Block> placed = new ArrayList<>();
        Random r = new Random();
        int[] tArr = {0};

        new BukkitRunnable() {
            @Override public void run() {
                int t = tArr[0];
                if (t > 16 || (p.isOnGround() && t > 3)) {
                    chargeImpact(p.getLocation(), p, placed, r);
                    charging = false;
                    addCd(wd_charge, p);
                    cancel(); return;
                }
                Location loc = p.getLocation().clone();
                particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_FOREST, 1.3f, 5, 0.25, 0.15, 0.25);
                particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_BARK,   1.1f, 2, 0.3,  0.1,  0.3);
                particleApi.spawnParticles(loc, Particle.CHERRY_LEAVES, 3, 0.3, 0.2, 0.3, 0.15);
                if (t % 4 == 0)
                    spawnSmallTree(loc.clone().add((r.nextDouble()-0.5)*0.8, 0, (r.nextDouble()-0.5)*0.8), placed);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.3, 1.3, 1.3)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(11, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 30, 0, false, true));
                    e.setVelocity(dir.clone().multiply(1.4).setY(0.5));
                }
                tArr[0]++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void chargeImpact(Location loc, Player p, List<Block> placed, Random r) {
        p.setFallDistance(0);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_FOREST, 1.6f, 25, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_LIME,   1.4f, 18, 2.0, 2.0, 2.0);
        particleApi.spawnParticles(loc, Particle.CHERRY_LEAVES, 12, 2, 1.5, 2, 0.2);
        p.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ATTACK,   1f, 0.6f);
        p.getWorld().playSound(loc, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.5f);
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60);
            spawnThorn(loc.clone().add(Math.cos(a)*2.0, 0, Math.sin(a)*2.0), 3);
        }
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            ((LivingEntity) e).damage(Math.max(5, 15 - dist*2.2), p);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON,   50, 0, false, true));
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
        }
        new BukkitRunnable() {
            @Override public void run() {
                new BukkitRunnable() {
                    @Override public void run() {
                        for (Block b : placed)
                            if (b.getType()==Material.OAK_LOG || b.getType()==Material.OAK_LEAVES
                                    || b.getType()==Material.MOSSY_COBBLESTONE) b.setType(Material.AIR);
                        placed.clear();
                    }
                }.runTask(magicPlugin);
            }
        }.runTaskLater(magicPlugin, 120L);
    }

    private void spawnSmallTree(Location base, List<Block> placed) {
        new BukkitRunnable() {
            @Override public void run() {
                int gy = base.getWorld().getHighestBlockYAt(base);
                Location ground = base.clone(); ground.setY(gy + 1);
                for (int h = 0; h < 3; h++) {
                    Block b = ground.clone().add(0, h, 0).getBlock();
                    if (b.isPassable() && !(b.getState() instanceof org.bukkit.block.Container)) {
                        b.setType(Material.OAK_LOG); placed.add(b);
                    }
                }
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    Block leaf = ground.clone().add(dx, 3, dz).getBlock();
                    if (leaf.isPassable()) { leaf.setType(Material.OAK_LEAVES); placed.add(leaf); }
                }
            }
        }.runTask(magicPlugin);
    }

    private void spawnThorn(Location base, int height) {
        List<Block> thorns = new ArrayList<>();
        new BukkitRunnable() {
            @Override public void run() {
                int gy = base.getWorld().getHighestBlockYAt(base);
                Location ground = base.clone(); ground.setY(gy + 1);
                for (int h = 0; h < height; h++) {
                    Block b = ground.clone().add(0, h, 0).getBlock();
                    if (b.isPassable()) { b.setType(Material.MOSSY_COBBLESTONE); thorns.add(b); }
                }
            }
        }.runTask(magicPlugin);
        new BukkitRunnable() {
            @Override public void run() {
                new BukkitRunnable() {
                    @Override public void run() {
                        for (Block b : thorns)
                            if (b.getType()==Material.MOSSY_COBBLESTONE) b.setType(Material.AIR);
                    }
                }.runTask(magicPlugin);
            }
        }.runTaskLater(magicPlugin, 80L);
    }

    private void sinhMenhLam(Player p) {
        Location center = p.getLocation().clone();
        p.getWorld().playSound(center, Sound.BLOCK_GRASS_PLACE,     1f, 0.6f);
        p.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        p.sendMessage(ChatColor.GREEN + "✦ 生命林 — LIFE FOREST!");

        List<Block> groveTrees = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            spawnSmallTree(center.clone().add(Math.cos(a)*4, 0, Math.sin(a)*4), groveTrees);
        }

        Random r = new Random();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 50) {
                    new BukkitRunnable() {
                        @Override public void run() {
                            for (Block b : groveTrees)
                                if (b.getType()==Material.OAK_LOG || b.getType()==Material.OAK_LEAVES)
                                    b.setType(Material.AIR);
                            groveTrees.clear();
                        }
                    }.runTask(magicPlugin);
                    cancel(); return;
                }
                for (int i = 0; i < 3; i++) {
                    Location lp = center.clone().add(
                            (r.nextDouble()-0.5)*9, r.nextDouble()*3, (r.nextDouble()-0.5)*9);
                    particleApi.spawnColoredParticles(lp, AURA_COLS[i % AURA_COLS.length], 0.9f, 1, 0.1, 0.1, 0.1);
                    if (i % 2 == 0)
                        particleApi.spawnParticles(lp, Particle.CHERRY_LEAVES, 1, 0.1, 0.1, 0.1, 0.04);
                }
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 30 + t * 6);
                    Location ring = center.clone().add(Math.cos(a)*5, 0.1, Math.sin(a)*5);
                    particleApi.spawnColoredParticles(ring, C_LIME, 1f, 1, 0.04, 0.04, 0.04);
                }
                if (t % 5 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 5, 4, 5)) {
                        if (e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 1, false, false));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,   30, 0, false, false));
                    }
                }

                if (t % 10 == 0)
                    center.getWorld().playSound(center, Sound.BLOCK_GRASS_PLACE, 0.3f, 1.2f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void canVuongPhanNo(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL,    1f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR,          1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK,      1f, 0.4f);
        p.sendMessage(ChatColor.GREEN + "✦ 根王憤怒 — " + ChatColor.BOLD + "ROOT KING'S WRATH!");

        Location center = p.getLocation().clone().add(0, 0.5, 0);
        Set<UUID> seized = new HashSet<>();
        Random r = new Random();

        new BukkitRunnable() {
            double rad = 0.5; int t = 0;
            @Override public void run() {
                if (rad > 11) { cancel(); return; }
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5 + t * 8);
                    Location lp = center.clone().add(Math.cos(a)*rad, 0.08, Math.sin(a)*rad);
                    particleApi.spawnColoredParticles(lp, WOOD_COLS[(i + t) % WOOD_COLS.length],
                            1.2f, 1, 0.05, 0.05, 0.05);
                }
                for (int branch = 0; branch < 4; branch++) {
                    double a = Math.toRadians(branch * 90 + t * 5);
                    for (double s = 0; s <= rad; s += 1.2) {
                        Location bp = center.clone().add(
                                Math.cos(a)*s + (r.nextDouble()-0.5)*0.3,
                                0.08,
                                Math.sin(a)*s + (r.nextDouble()-0.5)*0.3);
                        particleApi.spawnColoredParticles(bp, C_BARK, 0.95f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 3 == 0)
                    p.getWorld().playSound(center, Sound.BLOCK_ROOTED_DIRT_HIT,
                            0.4f, 0.6f + r.nextFloat()*0.3f);
                if (t % 3 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, rad+0.5, 3, rad+0.5)) {
                        if (e.equals(p) || e instanceof ArmorStand || seized.contains(e.getUniqueId())) continue;
                        if (!(e instanceof LivingEntity)) continue;
                        double dist = e.getLocation().distance(center);
                        if (dist < rad - 1.5) continue;
                        seized.add(e.getUniqueId());
                        seizeAndSlam((LivingEntity) e, p, r);
                    }
                }
                rad += 0.65 * 3; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 3);
    }

    private void seizeAndSlam(LivingEntity target, Player p, Random r) {
        target.setVelocity(new Vector(0, 0.8, 0));
        spawnRootBurst(target.getLocation(), 1.0);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (target.isDead() || t >= 60) { slamDown(target, p); cancel(); return; }
                if (target.getVelocity().getY() < -0.15)
                    target.setVelocity(target.getVelocity().setY(-0.08));
                if (t % 6 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double a = Math.toRadians(i * 72 + t * 15);
                        Location lp = target.getLocation().clone().add(0,1,0)
                                .add(Math.cos(a)*0.7, 0, Math.sin(a)*0.7);
                        particleApi.spawnColoredParticles(lp, WOOD_COLS[i % WOOD_COLS.length],
                                1.1f, 1, 0.04, 0.08, 0.04);
                    }
                }
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5, 9, false, false));
                t++;
            }
        }.runTaskTimer(magicPlugin, 5L, 1L);
    }

    private void slamDown(LivingEntity target, Player p) {
        if (target.isDead()) return;
        target.setVelocity(new Vector(0, -3.5, 0));
        new BukkitRunnable() {
            int wait = 0;
            @Override public void run() {
                if (target.isDead()) { cancel(); return; }
                if (target.isOnGround() || wait >= 40) {
                    Location sl = target.getLocation();
                    particleApi.spawnColoredParticles(sl.clone().add(0,0.5,0), C_BARK,   1.6f, 18, 1.0, 0.5, 1.0);
                    particleApi.spawnColoredParticles(sl.clone().add(0,0.5,0), C_FOREST, 1.4f, 14, 1.2, 0.5, 1.2);
                    particleApi.spawnParticles(sl, Particle.CHERRY_LEAVES, 14, 1.5, 0.5, 1.5, 0.2);
                    sl.getWorld().playSound(sl, Sound.ENTITY_RAVAGER_ATTACK,    0.8f, 0.7f);
                    sl.getWorld().playSound(sl, Sound.BLOCK_ROOTED_DIRT_BREAK,  0.6f, 0.5f);
                    target.damage(30, p);
                    target.setFallDistance(0);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,       60, 9, false, false));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 4, false, false));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,         40, 2, false, true));
                    for (int i = 0; i < 4; i++) {
                        double a = Math.toRadians(i * 90);
                        spawnThorn(sl.clone().add(Math.cos(a)*1.5, 0, Math.sin(a)*1.5), 2);
                    }
                    cancel();
                }
                wait++;
            }
        }.runTaskTimer(magicPlugin, 5L, 1L);
    }

    private void applyLongWeiArmorBreak(DealDamageExecute ex) {
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        Entity victim = event.getEntity();
        if (victim == null || !longWeiTargets.contains(victim.getUniqueId())) return;
        if (victim.hasMetadata(LONG_WEI_APPLIED)) return;
        victim.setMetadata(LONG_WEI_APPLIED,
                new org.bukkit.metadata.FixedMetadataValue(magicPlugin, true));
        event.setDamage(event.getDamage() * 1.11);
        new BukkitRunnable() {
            @Override public void run() {
                if (victim.isValid()) victim.removeMetadata(LONG_WEI_APPLIED, magicPlugin);
            }
        }.runTaskLater(magicPlugin, 1L);
    }

    private void handleDamage(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.POISON)
            event.setCancelled(true);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p  = ex.getPlayer();
        final Random r  = new Random();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (isAuraEnabled(p)) {
                    for (int i = 0; i < 7; i++) {
                        double a1 = Math.toRadians(i * (360.0/7) + t * 7);
                        double a2 = Math.toRadians(i * (360.0/7) - t * 10);
                        Location lp1 = p.getLocation().clone().add(Math.cos(a1)*1.1, 0.12+Math.sin(a1*0.5)*0.05, Math.sin(a1)*1.1);
                        Location lp2 = p.getLocation().clone().add(Math.cos(a2)*0.65, 0.06+Math.sin(a2*0.5)*0.04, Math.sin(a2)*0.65);
                        particleApi.spawnColoredParticles(lp1, AURA_COLS[i % AURA_COLS.length], 0.95f, 1, 0.03, 0.03, 0.03);
                        particleApi.spawnColoredParticles(lp2, i%2==0 ? C_LIME : C_VINE,        0.85f, 1, 0.03, 0.03, 0.03);
                    }
                    if (t % 3 == 0)
                        particleApi.spawnParticles(p.getLocation().clone().add(
                                (r.nextDouble()-0.5)*1.4, r.nextDouble()*0.3, (r.nextDouble()-0.5)*1.4),
                                Particle.CHERRY_LEAVES, 1, 0.04, 0.04, 0.04, 0.04);
                }

                if (t % 4 == 0) {
                    Block below = p.getLocation().clone().add(0,-1,0).getBlock();
                    if (p.isOnGround() && (below.getType()==Material.GRASS_BLOCK
                            || below.getType()==Material.MOSS_BLOCK
                            || below.getType()==Material.DIRT)) {
                        if (p.getHealth() < getMaxHp(p))
                            p.setHealth(Math.min(getMaxHp(p), p.getHealth() + 1.0));
                    }
                }

                Set<UUID> currentInRange = new HashSet<>();
                for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 6, 6, 6)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    if (e instanceof Player) {
                        try {
                            Power other = players.get((Player) e).getPower();
                            if (other != null && DRAGON_POWERS.contains(other.getClass().getSimpleName())) continue;
                        } catch (Exception ignored) {}
                    }
                    LivingEntity le = (LivingEntity) e;
                    currentInRange.add(e.getUniqueId());
                    longWeiTargets.add(e.getUniqueId());

                    if (!longWeiInRange.contains(e.getUniqueId()))
                        applyLongWei(le);

                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 22, 0, false, false));

                    if (t % 2 == 0 && isAuraEnabled(p))
                        particleApi.spawnColoredParticles(le.getLocation().clone().add(0,1,0),
                                C_VINE, 0.75f, 1, 0.25, 0.4, 0.25);
                }

                Set<UUID> left = new HashSet<>(longWeiInRange);
                left.removeAll(currentInRange);
                for (UUID uid : left) {
                    Entity gone = org.bukkit.Bukkit.getEntity(uid);
                    if (gone instanceof LivingEntity) removeLongWei((LivingEntity) gone);
                    longWeiTargets.remove(uid);
                }
                longWeiInRange.clear();
                longWeiInRange.addAll(currentInRange);
                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 20);
        return task;
    }

    @Override
    public void remove() {
        charging = false;
        for (UUID uid : longWeiInRange) {
            Entity e = org.bukkit.Bukkit.getEntity(uid);
            if (e instanceof LivingEntity) removeLongWei((LivingEntity) e);
        }
        longWeiTargets.clear();
        longWeiInRange.clear();
        netTraps.clear();
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&a龍吼 Dragon Roar";
            case 1: return "&a木劍舞 Wood Sword Dance";
            case 2: return "&a天羅地網 Heaven Net";
            case 3: return "&a狂林衝 Wild Forest Rush";
            case 4: return "&a生命林 Life Forest";
            case 5: return "&2&l根王憤怒 Root King's Wrath";
            default: return "&7none";
        }
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
    }
    private void spawnRootBurst(Location loc, double radius) {
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60);
            for (double h = 0; h <= 1.8; h += 0.45) {
                Location lp = loc.clone().add(Math.cos(a)*radius*0.6, h, Math.sin(a)*radius*0.6)
                        .add((Math.random()-0.5)*0.15, 0, (Math.random()-0.5)*0.15);
                particleApi.spawnColoredParticles(lp, WOOD_COLS[i % WOOD_COLS.length], 1.1f, 1, 0.04, 0.06, 0.04);
            }
        }
        particleApi.spawnParticles(loc.clone().add(0,0.5,0), Particle.CHERRY_LEAVES, 8, 0.5, 0.4, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.5f, 0.8f);
    }

    private Location getRaycastGround(Player p, int maxDist) {
        Location cur = p.getEyeLocation().clone();
        Vector dir   = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < maxDist * 2; i++) {
            cur.add(dir.clone().multiply(0.5));
            if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                cur.subtract(dir.clone().multiply(0.5)); break;
            }
        }
        return cur;
    }

    private Vector yawRotate(Vector v, double deg) {
        double rad = Math.toRadians(deg);
        return new Vector(v.getX()*Math.cos(rad)+v.getZ()*Math.sin(rad),
                v.getY(), -v.getX()*Math.sin(rad)+v.getZ()*Math.cos(rad));
    }

    private void applyLongWei(LivingEntity target) {
        if (!target.hasMetadata(LW_META_HP)) {
            org.bukkit.attribute.AttributeInstance aiHp = target.getAttribute(Attribute.MAX_HEALTH);
            if (aiHp != null) {
                double base = aiHp.getBaseValue();
                target.setMetadata(LW_META_HP, new org.bukkit.metadata.FixedMetadataValue(magicPlugin, base));
                double newVal = Math.max(1.0, base * 0.9);
                aiHp.setBaseValue(newVal);
                if (target.getHealth() > newVal) target.setHealth(newVal);
            }
        }
        if (!target.hasMetadata(LW_META_AR)) {
            org.bukkit.attribute.AttributeInstance aiAr = target.getAttribute(Attribute.ARMOR);
            if (aiAr != null) {
                double base = aiAr.getBaseValue();
                target.setMetadata(LW_META_AR, new org.bukkit.metadata.FixedMetadataValue(magicPlugin, base));
                aiAr.setBaseValue(base * 0.9);
            }
        }
    }

    private void removeLongWei(LivingEntity target) {
        if (target.hasMetadata(LW_META_HP)) {
            org.bukkit.attribute.AttributeInstance aiHp = target.getAttribute(Attribute.MAX_HEALTH);
            if (aiHp != null) {
                double orig = (double) target.getMetadata(LW_META_HP).get(0).value();
                aiHp.setBaseValue(orig);
            }
            target.removeMetadata(LW_META_HP, magicPlugin);
        }
        if (target.hasMetadata(LW_META_AR)) {
            org.bukkit.attribute.AttributeInstance aiAr = target.getAttribute(Attribute.ARMOR);
            if (aiAr != null) {
                double orig = (double) target.getMetadata(LW_META_AR).get(0).value();
                aiAr.setBaseValue(orig);
            }
            target.removeMetadata(LW_META_AR, magicPlugin);
        }
    }
}

