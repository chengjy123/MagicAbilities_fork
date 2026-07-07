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

public class IceDragonPower extends Power implements IdlePower, Removeable {

    private static final String id_roar     = "icedragon.roar";
    private static final String id_slash    = "icedragon.slash";
    private static final String id_prison   = "icedragon.prison";
    private static final String id_charge   = "icedragon.charge";
    private static final String id_blizzard = "icedragon.blizzard";
    private static final String id_heaven   = "icedragon.heaven";

    private static final Color C_ICE_WHITE  = Color.fromRGB(220, 245, 255);
    private static final Color C_ICE_BLUE   = Color.fromRGB(120, 200, 255);
    private static final Color C_ICE_DEEP   = Color.fromRGB(50,  140, 220);
    private static final Color C_ICE_CRYSTAL= Color.fromRGB(180, 230, 255);
    private static final Color C_FROST      = Color.fromRGB(200, 235, 255);
    private static final Color C_COLD_TEAL  = Color.fromRGB(80,  210, 220);
    private static final Color C_SILVER     = Color.fromRGB(210, 220, 230);

    private static final Color[] ICE_COLORS = {
            C_ICE_WHITE, C_ICE_BLUE, C_ICE_CRYSTAL, C_FROST, C_COLD_TEAL
    };
    private static final Color[] AURA_COLORS = {
            C_ICE_WHITE, C_ICE_BLUE, C_ICE_DEEP, C_ICE_CRYSTAL, C_SILVER
    };

    private static final Set<String> DRAGON_POWERS = new HashSet<>(Arrays.asList(
            "IceDragonPower",
            "WoodDragonPower"
    ));

    private boolean charging = false;
    private final Set<UUID> longWeiTargets  = new HashSet<>();
    private final Set<UUID> longWeiInRange  = new HashSet<>();

    public IceDragonPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute)  { handleDamage((DamagedExecute) ex);         return; }
        if (ex instanceof DamagedByExecute) { longWeiArmorReduction((DamagedByExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof DealDamageExecute) { applyLongWeiArmorBreak((DealDamageExecute) ex); }
        if (ex instanceof LeftClickExecute)  onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(id_roar, p, this)) return; longRoar(p);      addCd(id_roar, p);     return;
            case 1: if (onCd(id_slash, p, this)) return; iceSlash(p);      addCd(id_slash, p);    return;
            case 2: if (onCd(id_prison, p, this)) return; hanNguyetTran(p); addCd(id_prison, p);   return;
            case 3: if (charging) return;
                    if (onCd(id_charge, p, this)) return; bangHaXung(p);                                                                          return;
            case 4: if (onCd(id_blizzard, p, this)) return; tuyetVu(p);       addCd(id_blizzard, p); return;
            case 5: if (onCd(id_heaven, p, this)) return; thienHan(p);      addCd(id_heaven, p);
        }
    }

    private void longRoar(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        Vector fwd   = p.getEyeLocation().getDirection().clone().setY(0).normalize();

        p.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL,    1f, 0.6f);
        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM,     0.8f, 0.5f);
        p.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT,0.6f, 0.4f);
        p.sendMessage(ChatColor.AQUA + "✦ 龍吼 — DRAGON ROAR!");

        new BukkitRunnable() {
            double rad = 0.3; int t = 0;
            @Override public void run() {
                if (rad > 7) { cancel(); return; }
                for (int i = -9; i <= 9; i++) {
                    double ang = Math.toRadians(i * 10);
                    double vx  = fwd.getX()*Math.cos(ang) - fwd.getZ()*Math.sin(ang);
                    double vz  = fwd.getX()*Math.sin(ang) + fwd.getZ()*Math.cos(ang);
                    for (double h : new double[]{0.2, 0.9, 1.6}) {
                        Location lp = loc.clone().add(vx*rad, h, vz*rad);
                        particleApi.spawnColoredParticles(lp,
                                t%3==0 ? C_ICE_WHITE : t%3==1 ? C_ICE_CRYSTAL : C_FROST,
                                1.2f, 2, 0.06, 0.06, 0.06);
                        if (t % 3 == 0)
                            particleApi.spawnParticles(lp, Particle.SNOWFLAKE, 1, 0.05, 0.05, 0.05, 0.04);
                    }
                }
                for (int i = -9; i <= 9; i++) {
                    double ang = Math.toRadians(i * 10);
                    double vx  = fwd.getX()*Math.cos(ang) - fwd.getZ()*Math.sin(ang);
                    double vz  = fwd.getX()*Math.sin(ang) + fwd.getZ()*Math.cos(ang);
                    particleApi.spawnParticles(
                            loc.clone().add(vx*rad, 0.5, vz*rad),
                            Particle.CLOUD, 1, 0.05, 0.1, 0.05, 0.02);
                }
                rad += 0.55; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : p.getWorld().getNearbyEntities(loc, 7, 3, 7)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    Vector toE = e.getLocation().subtract(loc).toVector().setY(0);
                    if (toE.lengthSquared() > 0 && fwd.dot(toE.normalize()) < -0.1) continue;
                    double dist = e.getLocation().distance(loc);
                    double dmg  = Math.max(4, 10 - dist * 1.0);
                    ((LivingEntity) e).damage(dmg, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,         20, 4, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,       20, 9, false, false));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20, 9, false, false));
                    Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(1.5).setY(0.3);
                    e.setVelocity(kb);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_ICE_WHITE,   1.5f, 20, 0.4, 0.4, 0.4);
                    particleApi.spawnParticles(e.getLocation().clone().add(0,1,0), Particle.SNOWFLAKE, 15, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }.runTaskLater(magicPlugin, 4L);
    }

    private void iceSlash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,          0.6f, 1.8f);

        ArmorStand blade = spawnAs(p.getEyeLocation().clone());
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        Vector right = yawRotate(dir.clone().setY(0).normalize(), 90).setY(0).normalize();
        Random r = new Random();
        Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (blade.isDead() || t > 35) { safeRemove(blade); cancel(); return; }

                blade.teleport(blade.getLocation().add(dir.clone().multiply(1.7)));
                Location loc = blade.getLocation();

                for (double s = -0.8; s <= 0.8; s += 0.18) {
                    Location edge = loc.clone().add(right.clone().multiply(s));
                    particleApi.spawnColoredParticles(edge, C_ICE_WHITE,   1.4f, 2, 0.04, 0.04, 0.04);
                    particleApi.spawnColoredParticles(edge, C_ICE_CRYSTAL, 1.1f, 1, 0.03, 0.03, 0.03);
                }
                particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.5f, 3, 0.05, 0.05, 0.05);
                if (t % 2 == 0)
                    particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 2, 0.15, 0.1, 0.15, 0.05);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.1, 0.9, 1.1)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(14, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,       40, 9, false, false));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 4, false, false));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,     40, -10, false, false));
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_ICE_WHITE,  1.6f, 25, 0.4, 0.5, 0.4);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_ICE_BLUE,   1.3f, 15, 0.5, 0.5, 0.5);
                    particleApi.spawnParticles(e.getLocation().clone().add(0,1,0), Particle.SNOWFLAKE, 20, 0.5, 0.6, 0.5, 0.1);
                    e.getWorld().playSound(e.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 0.7f);
                }

                if (!loc.getBlock().isPassable()) { safeRemove(blade); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void hanNguyetTran(Player p) {
        LivingEntity target = getNearestTarget(p, 7);
        if (target == null) {
            p.sendMessage(ChatColor.AQUA + "There are no enemies within 7 blocks!"); return;
        }

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,   0.8f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.2f);
        p.sendMessage(ChatColor.AQUA + "✦ 寒月陣 — COLD MOON FORMATION!");

        Location center = target.getLocation().clone();
        List<Block> iceBlocks = new ArrayList<>();
        double radius = 1.6;
        Random r = new Random();

        new BukkitRunnable() {
            @Override public void run() {
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60);
                    int bx = (int) Math.round(center.getX() + Math.cos(a) * radius);
                    int bz = (int) Math.round(center.getZ() + Math.sin(a) * radius);
                    for (int by = center.getBlockY(); by >= center.getBlockY() - 3; by--) {
                        Block ground = center.getWorld().getBlockAt(bx, by, bz);
                        if (!ground.isPassable())
                        {
                            for (int h = 1; h <= 3; h++) {
                                Block col = center.getWorld().getBlockAt(bx, by + h, bz);
                                if (col.isPassable()) {
                                    col.setType(h <= 2 ? Material.PACKED_ICE : Material.BLUE_ICE);
                                    iceBlocks.add(col);
                                }
                            }
                            break;
                        }
                    }
                }
                particleApi.spawnColoredParticles(center.clone().add(0,1.5,0), C_ICE_WHITE,  1.8f, 40, 1.8, 1.0, 1.8);
                particleApi.spawnColoredParticles(center.clone().add(0,1.5,0), C_ICE_BLUE,   1.5f, 30, 2.0, 1.0, 2.0);
                particleApi.spawnParticles(center, Particle.SNOWFLAKE, 40, 2.0, 1.0, 2.0, 0.15);
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 0.4f);
            }
        }.runTask(magicPlugin);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 65, 2, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,  65, 0, false, false));

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 3 || target.isDead()) { cancel(); breakTrap(center, iceBlocks, p, r); return; }
                target.damage(5, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 + t * 30);
                    Location lp = center.clone().add(0,1,0).add(Math.cos(a)*radius, 0, Math.sin(a)*radius);
                    particleApi.spawnColoredParticles(lp, ICE_COLORS[i % ICE_COLORS.length], 1.1f, 2, 0.05, 0.1, 0.05);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 20L, 20L);
    }

    private void breakTrap(Location center, List<Block> iceBlocks, Player p, Random r) {
        new BukkitRunnable() {
            @Override public void run() {
                for (Block b : iceBlocks) if (b.getType() == Material.PACKED_ICE || b.getType() == Material.BLUE_ICE) b.setType(Material.AIR);
            }
        }.runTask(magicPlugin);
        particleApi.spawnColoredParticles(center.clone().add(0,1.5,0), C_ICE_WHITE,  1.6f, 40, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(center.clone().add(0,1.5,0), C_ICE_CRYSTAL,1.4f, 30, 1.8, 1.5, 1.8);
        particleApi.spawnParticles(center, Particle.SNOWFLAKE, 35, 1.8, 1.0, 1.8, 0.2);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.9f, 0.7f);
    }

    private void bangHaXung(Player p) {
        charging = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,          0.8f, 0.8f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0.15).normalize();
        p.setVelocity(dir.clone().multiply(2.8));

        Map<Block, Material> changed = new HashMap<>();
        Set<UUID> hit = new HashSet<>();
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 16 || p.isOnGround() && t > 3) {
                    chargeImpact(p.getLocation(), p, changed, r);
                    charging = false;
                    addCd(id_charge, p);
                    cancel(); return;
                }

                Location loc = p.getLocation().clone();

                particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_ICE_WHITE,   1.4f, 8,  0.25, 0.1, 0.25);
                particleApi.spawnColoredParticles(loc.clone().add(0,0.5,0), C_ICE_CRYSTAL, 1.2f, 5,  0.3,  0.1, 0.3);
                particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 5, 0.3, 0.1, 0.3, 0.1);

                Block below = loc.clone().add(0, -0.5, 0).getBlock();
                if (!changed.containsKey(below) && below.getType() == Material.GRASS_BLOCK) {
                    changed.put(below, Material.GRASS_BLOCK);
                    below.setType(Material.FROSTED_ICE);
                }

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.4, 1.4, 1.4)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(12, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 3, false, false));
                    e.setVelocity(dir.clone().multiply(1.5).setY(0.6));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void chargeImpact(Location loc, Player p, Map<Block, Material> changed, Random r) {
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_ICE_WHITE,   2f,   80, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_ICE_BLUE,    1.7f, 60, 2.0, 2.0, 2.0);
        particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 30, 2.0, 1.0, 2.0, 0.2);
        p.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
        p.setFallDistance(0);

        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            Location sloc = loc.clone().add(Math.cos(a)*1.8, 0, Math.sin(a)*1.8);
            spawnIceSpike(sloc, 3);
        }

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            ((LivingEntity) e).damage(Math.max(6, 16 - dist * 2.5), p);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
        }

        new BukkitRunnable() {
            @Override public void run() {
                for (Block b : changed.keySet()) if (b.getType() == Material.FROSTED_ICE) b.setType(changed.get(b));
            }
        }.runTaskLater(magicPlugin, 100L);
    }

    private void spawnIceSpike(Location base, int height) {
        List<Block> spike = new ArrayList<>();
        new BukkitRunnable() {
            @Override public void run() {
                Location cur = base.clone();
                while (cur.getBlock().isPassable() && cur.getY() > 0) cur.add(0,-1,0);
                cur.add(0,1,0);
                for (int h = 0; h < height; h++) {
                    Block b = cur.clone().add(0, h, 0).getBlock();
                    if (b.isPassable()) { b.setType(Material.PACKED_ICE); spike.add(b); }
                }
                particleApi.spawnColoredParticles(cur.clone().add(0,height/2.0,0), C_ICE_CRYSTAL, 1.3f, 5, 0.1, 0.3, 0.1);
            }
        }.runTask(magicPlugin);

        new BukkitRunnable() {
            @Override public void run() {
                new BukkitRunnable() {
                    @Override public void run() {
                        for (Block b : spike) if (b.getType() == Material.PACKED_ICE) b.setType(Material.AIR);
                    }
                }.runTask(magicPlugin);
            }
        }.runTaskLater(magicPlugin, 60L);
    }

    private void tuyetVu(Player p) {
        Location center = getRaycastGround(p, 20);
        Random r = new Random();

        p.getWorld().playSound(center, Sound.WEATHER_RAIN,          1f, 0.6f);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.5f);
        p.sendMessage(ChatColor.AQUA + "✦ 雪雨 — SNOW RAIN!");

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 100) {
                    particleApi.spawnColoredParticles(center.clone().add(0,1,0), C_ICE_WHITE, 1.5f, 30, 2, 2, 2);
                    cancel(); return;
                }

                for (int layer = 0; layer < 4; layer++) {
                    double rad   = 2.0 + layer * 0.8;
                    double yOff  = layer * 0.5;
                    int    dir2  = layer % 2 == 0 ? 1 : -1;
                    for (int i = 0; i < 10; i++) {
                        double a = Math.toRadians(i * 22.5 + t * (11 + layer*3) * dir2);
                        Location lp = center.clone().add(Math.cos(a)*rad, yOff, Math.sin(a)*rad);
                        particleApi.spawnColoredParticles(lp,
                                ICE_COLORS[(i + layer + t) % ICE_COLORS.length],
                                0.9f + layer*0.08f, 1, 0.04, 0.04, 0.04);
                    }
                }
                if (t % 2 == 0) {
                    for (int i = 0; i < 4; i++) {
                        Location snow = center.clone().add(
                                (r.nextDouble()-0.5)*8, 2 + r.nextDouble()*0.25, (r.nextDouble()-0.5)*8);
                        particleApi.spawnParticles(snow, Particle.SNOWFLAKE, 2, 0.3, 0.3, 0.3, 0.1);
                        particleApi.spawnColoredParticles(snow, C_ICE_WHITE, 0.85f, 1, 0.1, 0.1, 0.1);
                    }
                }

                if (t % 25 == 0)
                    center.getWorld().playSound(center, Sound.WEATHER_RAIN, 0.3f, 0.7f + r.nextFloat()*0.3f);

                if (t % 20 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 5, 4, 5)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(3, p);
                    }
                }
                for (Entity e : center.getWorld().getNearbyEntities(center, 5, 4, 5)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  20, 2, false, false));
                }

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void thienHan(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL,    1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,        0.8f, 0.5f);
        p.sendMessage(ChatColor.AQUA + "✦charging...");

        Location target = getRaycastGround(p, 30);
        new BukkitRunnable() {
            int ct = 50;
            @Override public void run() {
                if (ct <= 0) { cancel(); releaseThienHan(p, target); return; }
                double a = Math.toRadians(ct * 20);
                for (int i = 0; i < 8; i++) {
                    double ai = a + Math.toRadians(i * 45);
                    double rad = 1.2 + (50 - ct) * 0.018;
                    Location lp = p.getEyeLocation().clone().add(
                            Math.cos(ai)*rad, Math.sin(ai*0.4)*0.3, Math.sin(ai)*rad);
                    particleApi.spawnColoredParticles(lp,
                            i % 2 == 0 ? C_ICE_WHITE : C_ICE_BLUE, 1.3f, 2, 0.04, 0.04, 0.04);
                }
                if (ct % 5 == 0) {
                    for (double y = 0; y <= 18; y += 1.2) {
                        Location beam = target.clone().add((Math.random()-0.5)*0.5, y, (Math.random()-0.5)*0.5);
                        particleApi.spawnColoredParticles(beam, ct < 20 ? C_ICE_WHITE : C_ICE_CRYSTAL, 1.2f, 2, 0.08, 0.04, 0.08);
                    }
                    p.getWorld().playSound(target, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.6f);
                }
                if (ct == 20) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
                    p.sendMessage(ChatColor.AQUA + "✦ " + ChatColor.BOLD + "天寒 — HEAVEN'S COLD!");
                }
                ct--;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void releaseThienHan(Player p, Location target) {
        new BukkitRunnable() {
            @Override public void run() {
                for (double y = 0; y <= 28; y += 0.45) {
                    Location lp = target.clone().add((Math.random()-0.5)*0.7, y, (Math.random()-0.5)*0.7);
                    particleApi.spawnColoredParticles(lp, C_ICE_WHITE,   1.8f, 4, 0.14, 0.04, 0.14);
                    particleApi.spawnColoredParticles(lp, C_ICE_BLUE,    1.5f, 3, 0.20, 0.04, 0.20);
                    particleApi.spawnColoredParticles(lp, C_ICE_CRYSTAL, 1.3f, 2, 0.28, 0.04, 0.28);
                    if (y < 3 || (y > 12 && y < 16))
                        particleApi.spawnParticles(lp, Particle.SNOWFLAKE, 2, 0.15, 0.05, 0.15, 0.08);
                }
            }
        }.runTask(magicPlugin);
        p.getWorld().playSound(target, Sound.ENTITY_ENDER_DRAGON_GROWL,    1f, 0.3f);
        p.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE,        0.9f, 0.4f);
        p.getWorld().playSound(target, Sound.BLOCK_GLASS_BREAK,             1f,  0.3f);

        new BukkitRunnable() {
            @Override public void run() {
                new BukkitRunnable() {
                    double rad = 0.5; int t = 0;
                    @Override public void run() {
                        if (rad > 9) { cancel(); return; }
                        for (int i = 0; i < 30; i++) {
                            double a = Math.toRadians(i * 12 + t * 8);
                            Location lp = target.clone().add(Math.cos(a)*rad, 0.1, Math.sin(a)*rad);
                            particleApi.spawnColoredParticles(lp,
                                    t%3==0 ? C_ICE_WHITE : t%3==1 ? C_ICE_BLUE : C_ICE_CRYSTAL,
                                    1.3f, 2, 0.05, 0.05, 0.05);
                            particleApi.spawnParticles(lp, Particle.SNOWFLAKE, 1, 0.04, 0.04, 0.04, 0.05);
                        }
                        if (t % 3 == 0 && rad < 7) {
                            double a = Math.toRadians(t * 37);
                            spawnIceSpike(target.clone().add(Math.cos(a)*rad, 0, Math.sin(a)*rad), 4);
                        }
                        rad += 0.7; t++;
                    }
                }.runTaskTimer(magicPlugin, 0, 2);
                particleApi.spawnColoredParticles(target.clone().add(0,1,0), C_ICE_WHITE,   2.5f, 120, 2.5, 2.5, 2.5);
                particleApi.spawnColoredParticles(target.clone().add(0,1,0), C_ICE_BLUE,    2f,   150, 3.0, 3.0, 3.0);
                particleApi.spawnColoredParticles(target.clone().add(0,1,0), C_ICE_CRYSTAL, 1.8f, 100, 3.5, 3.5, 3.5);
                particleApi.spawnParticles(target, Particle.SNOWFLAKE, 200, 4, 3, 4, 0.3);

                for (Entity e : target.getWorld().getNearbyEntities(target, 8, 8, 8)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(target);
                    double dmg  = Math.max(12, 45 - dist * 3.5);
                    ((LivingEntity) e).damage(dmg, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,       80, 9, false, false));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 4, false, false));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,     80, -10, false, false));
                    Vector kb = e.getLocation().subtract(target).toVector().normalize().multiply(1.8).setY(0.4);
                    e.setVelocity(kb);
                }
            }
        }.runTaskLater(magicPlugin, 3L);
    }

    private void applyLongWeiArmorBreak(DealDamageExecute ex) {
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        Entity victim = event.getEntity();
        if (victim == null) return;
        if (!longWeiTargets.contains(victim.getUniqueId())) return;
        event.setDamage(event.getDamage() * 1.11);
    }

    private void longWeiArmorReduction(DamagedByExecute ex) {
    }

    private void handleDamage(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE)
            event.setCancelled(true);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (p.getFreezeTicks() > 0) p.setFreezeTicks(0);
                if (isAuraEnabled(p)) {
                    for (int i = 0; i < 10; i++) {
                        double a1 = Math.toRadians(i * 36 + t * 8);
                        double a2 = Math.toRadians(i * 36 - t * 11);
                        Location lp1 = p.getLocation().clone().add(Math.cos(a1)*1.1, 0.12 + Math.sin(a1*0.5)*0.05, Math.sin(a1)*1.1);
                        Location lp2 = p.getLocation().clone().add(Math.cos(a2)*0.65, 0.06 + Math.sin(a2*0.5)*0.04, Math.sin(a2)*0.65);
                        particleApi.spawnColoredParticles(lp1, AURA_COLORS[i % AURA_COLORS.length], 0.95f, 1, 0.03, 0.03, 0.03);
                        particleApi.spawnColoredParticles(lp2, i%2==0 ? C_ICE_BLUE : C_ICE_CRYSTAL, 0.85f, 1, 0.03, 0.03, 0.03);
                    }
                    if (t % 3 == 0)
                        particleApi.spawnParticles(
                                p.getLocation().clone().add((r.nextDouble()-0.5)*1.2, r.nextDouble()*2, (r.nextDouble()-0.5)*1.2),
                                Particle.SNOWFLAKE, 1, 0.04, 0.04, 0.04, 0.05);
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

                    applyLongWei(le);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 22, 0, false, false));
                    if (t % 2 == 0 && isAuraEnabled(p))
                        particleApi.spawnColoredParticles(le.getLocation().clone().add(0, 1, 0),
                                C_ICE_CRYSTAL, 0.75f, 1, 0.25, 0.4, 0.25);
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
            Entity gone = org.bukkit.Bukkit.getEntity(uid);
            if (gone instanceof LivingEntity) removeLongWei((LivingEntity) gone);
        }
        longWeiTargets.clear();
        longWeiInRange.clear();
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&b龍吼 Dragon Roar";
            case 1: return "&b冰斬 Ice Slash";
            case 2: return "&b寒月陣 Cold Moon Formation";
            case 3: return "&b氷河衝 Glacier Rush";
            case 4: return "&b雪雨 Snow Rain";
            case 5: return "&b&l天寒 Heaven's Cold";
            default: return "&7none";
        }
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
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
        double r = Math.toRadians(deg);
        return new Vector(v.getX()*Math.cos(r)+v.getZ()*Math.sin(r), v.getY(), -v.getX()*Math.sin(r)+v.getZ()*Math.cos(r));
    }

    private static final String LW_META_HP = "lw_hp_base";
    private static final String LW_META_AR = "lw_ar_base";

    private void applyLongWei(LivingEntity target) {
        if (!target.hasMetadata(LW_META_HP)) {
            org.bukkit.attribute.AttributeInstance aiHp =
                    target.getAttribute(Attribute.MAX_HEALTH);
            if (aiHp != null) {
                double base = aiHp.getBaseValue();
                target.setMetadata(LW_META_HP,
                        new org.bukkit.metadata.FixedMetadataValue(magicPlugin, base));
                double newVal = Math.max(1.0, base * 0.9);
                aiHp.setBaseValue(newVal);
                if (target.getHealth() > newVal)
                    target.setHealth(newVal);
            }
        }
        if (!target.hasMetadata(LW_META_AR)) {
            org.bukkit.attribute.AttributeInstance aiAr =
                    target.getAttribute(Attribute.ARMOR);
            if (aiAr != null) {
                double base = aiAr.getBaseValue();
                target.setMetadata(LW_META_AR,
                        new org.bukkit.metadata.FixedMetadataValue(magicPlugin, base));
                aiAr.setBaseValue(base * 0.9);
            }
        }
    }
    private void removeLongWei(LivingEntity target) {
        if (target.hasMetadata(LW_META_HP)) {
            org.bukkit.attribute.AttributeInstance aiHp =
                    target.getAttribute(Attribute.MAX_HEALTH);
            if (aiHp != null) {
                double orig = (double) target.getMetadata(LW_META_HP).get(0).value();
                aiHp.setBaseValue(orig);
            }
            target.removeMetadata(LW_META_HP, magicPlugin);
        }
        if (target.hasMetadata(LW_META_AR)) {
            org.bukkit.attribute.AttributeInstance aiAr =
                    target.getAttribute(Attribute.ARMOR);
            if (aiAr != null) {
                double orig = (double) target.getMetadata(LW_META_AR).get(0).value();
                aiAr.setBaseValue(orig);
            }
            target.removeMetadata(LW_META_AR, magicPlugin);
        }
    }

}

