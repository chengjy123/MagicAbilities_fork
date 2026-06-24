package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilities.MagicAbilities.magicPlugin;
import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.players.PowerPlayer.players;
import static net.trduc.magicabilities.MagicAbilities.particleApi;
import static net.trduc.magicabilities.misc.PowerUtils.*;

public class SpikePower extends Power implements Removeable {

    private static final String CD_SHOOT   = "spike.shoot";
    private static final String CD_WALL    = "spike.wall";
    private static final String CD_TRAIL   = "spike.trail";
    private static final String CD_PILLAR  = "spike.pillar";

    private static final Color C_STONE  = Color.fromRGB(128, 128, 128);
    private static final Color C_GRAVEL = Color.fromRGB(100,  90,  80);
    private static final Color C_DARK   = Color.fromRGB( 60,  50,  40);
    private static final Color C_DUST   = Color.fromRGB(180, 165, 140);

    private int sneakCount = 0;
    private long lastSneakTime = 0;

    private final List<Block> wallBlocks   = new ArrayList<>();
    private final List<Block> trailBlocks  = new ArrayList<>();
    private final List<Block> pillarBlocks = new ArrayList<>();

    private final Random rng = new Random();

    public SpikePower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeft((LeftClickExecute) ex);
        if (ex instanceof SneakExecute)     onSneak((SneakExecute) ex);
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: shootSpike(p);  break;
            case 1: spikePillar(p); break;
        }
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        long now = System.currentTimeMillis();

        if (now - lastSneakTime > 600) {
            sneakCount = 0;
        }
        sneakCount++;
        lastSneakTime = now;

        if (sneakCount == 1) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (sneakCount == 1) {
                        spikeWall(p);
                        sneakCount = 0;
                    }
                }
            }.runTaskLater(magicPlugin, 12L);
        } else if (sneakCount >= 2) {
            sneakCount = 0;
            spikeTrail(p);
        }
    }

    private void shootSpike(Player p) {
        if (onCd(CD_SHOOT, p, this)) return;

        Location eye  = p.getEyeLocation();
        Vector   dir  = eye.getDirection().normalize();

        final ArmorStand proj = spawnProjectile(p);
        proj.teleport(eye.clone().add(dir.clone().multiply(1.2)));

        new BukkitRunnable() {
            int t = 0;
            final Set<UUID> hit = new HashSet<>();

            @Override public void run() {
                if (t > 40 || proj.isDead()) { safeRemove(proj); cancel(); return; }

                Location cur = proj.getLocation();
                proj.teleport(cur.clone().add(dir.clone().multiply(1.1)));
                Location pos = proj.getLocation();

                particleApi.spawnColoredParticles(pos, C_STONE, 1.8f, 3, 0.08, 0.08, 0.08);
                particleApi.spawnColoredParticles(pos, C_GRAVEL, 1.4f, 2, 0.1, 0.1, 0.1);
                particleApi.spawnColoredParticles(pos.clone().add(0, 0.15, 0), C_DUST, 1.2f, 2, 0.12, 0.05, 0.12);

                if (t % 4 == 0)
                    pos.getWorld().playSound(pos, Sound.BLOCK_STONE_HIT, 0.25f, 1.6f);

                if (!pos.getBlock().isPassable()) {
                    impactBurst(pos);
                    safeRemove(proj);
                    cancel();
                    return;
                }

                for (Entity e : pos.getWorld().getNearbyEntities(pos, 0.9, 0.9, 0.9)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    if (hit.contains(e.getUniqueId())) continue;
                    hit.add(e.getUniqueId());

                    LivingEntity le = (LivingEntity) e;
                    le.damage(9.0, p);
                    Vector kb = dir.clone().multiply(0.9).setY(0.3);
                    le.setVelocity(le.getVelocity().add(kb));

                    spawnImpactParticles(pos);
                    pos.getWorld().playSound(pos, Sound.BLOCK_STONE_BREAK, 0.8f, 0.7f);
                    safeRemove(proj);
                    cancel();
                    return;
                }

                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);

        p.getWorld().playSound(eye, Sound.BLOCK_STONE_BREAK, 0.9f, 0.5f);
        p.getWorld().playSound(eye, Sound.ENTITY_ARROW_SHOOT,  0.5f, 0.4f);
        sendActionBar(p, "§7⬦ Spike Shoot");
        addCd(CD_SHOOT, p);
    }

    private void spikeWall(Player p) {
        if (onCd(CD_WALL, p, this)) return;

        restoreBlockList(wallBlocks, 0L);

        Location center = p.getLocation().clone().add(p.getLocation().getDirection().multiply(2.5));
        center = getGroundBelow(center);

        Vector right = p.getLocation().getDirection().clone().setY(0).normalize();
        right = new Vector(-right.getZ(), 0, right.getX());

        final List<Block> newWall = new ArrayList<>();

        for (int side = -2; side <= 2; side++) {
            Location base = center.clone().add(right.clone().multiply(side));
            base = getGroundBelow(base);

            int height = 2 + rng.nextInt(2);
            for (int h = 0; h < height; h++) {
                Block b = base.clone().add(0, h, 0).getBlock();
                if (!b.isPassable()) continue;
                b.setType(Material.STONE);
                newWall.add(b);
            }
        }

        wallBlocks.clear();
        wallBlocks.addAll(newWall);

        for (Block b : newWall) {
            particleApi.spawnColoredParticles(b.getLocation().add(0.5, 0.5, 0.5), C_STONE, 2f, 5, 0.3, 0.3, 0.3);
            particleApi.spawnColoredParticles(b.getLocation().add(0.5, 0.5, 0.5), C_DUST,  1.4f, 3, 0.2, 0.2, 0.2);
        }

        center.getWorld().playSound(center, Sound.BLOCK_STONE_PLACE,  1f, 0.5f);
        center.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK,  0.7f, 0.4f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.4f);

        sendActionBar(p, "§8▬ Spike Wall");

        new BukkitRunnable() {
            @Override public void run() { restoreBlockList(wallBlocks, 0L); }
        }.runTaskLater(magicPlugin, 80L);

        addCd(CD_WALL, p);
    }

    private void spikeTrail(Player p) {
        if (onCd(CD_TRAIL, p, this)) return;

        restoreBlockList(trailBlocks, 0L);

        Vector dir = p.getLocation().getDirection().clone().setY(0).normalize();

        new BukkitRunnable() {
            int step = 0;
            final List<Block> placed = new ArrayList<>();

            @Override public void run() {
                if (step >= 10) {
                    trailBlocks.addAll(placed);
                    new BukkitRunnable() {
                        @Override public void run() { restoreBlockList(trailBlocks, 0L); }
                    }.runTaskLater(magicPlugin, 60L);
                    cancel();
                    return;
                }

                Location pos = p.getLocation().clone().add(dir.clone().multiply(step + 1.0));
                pos = getGroundBelow(pos);

                int spikeHeight = (step == 4 || step == 5) ? 3 : (step == 3 || step == 6) ? 2 : 1;

                for (int h = 0; h < spikeHeight; h++) {
                    Block b = pos.clone().add(0, h, 0).getBlock();
                    if (!b.isPassable()) continue;
                    b.setType(Material.COBBLESTONE);
                    placed.add(b);
                }

                Location tip = pos.clone().add(0, spikeHeight, 0);
                particleApi.spawnColoredParticles(tip, C_GRAVEL, 1.8f, 4, 0.2, 0.1, 0.2);
                particleApi.spawnColoredParticles(tip, C_DUST,   1.3f, 3, 0.15, 0.05, 0.15);
                tip.getWorld().playSound(tip, Sound.BLOCK_STONE_PLACE, 0.5f, 1.2f + step * 0.05f);

                for (Entity e : tip.getWorld().getNearbyEntities(tip, 1.0, 1.5, 1.0)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    le.damage(6.0 + spikeHeight * 2.0, p);
                    le.setVelocity(le.getVelocity().add(new Vector(0, 0.6 + spikeHeight * 0.15, 0)));
                }

                step++;
            }
        }.runTaskTimer(magicPlugin, 0L, 2L);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK,  1f,  0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.6f);
        sendActionBar(p, "§8▲▲▲ Spike Trail");
        addCd(CD_TRAIL, p);
    }

    private void spikePillar(Player p) {
        if (onCd(CD_PILLAR, p, this)) return;

        restoreBlockList(pillarBlocks, 0L);

        Location center = getGroundBelow(p.getLocation().clone());
        World world = center.getWorld();

        final List<Block> placed = new ArrayList<>();

        new BukkitRunnable() {
            int ring = 0;
            final int[] radii = {0, 1, 2, 3};

            @Override public void run() {
                if (ring >= radii.length) {
                    pillarBlocks.addAll(placed);
                    new BukkitRunnable() {
                        @Override public void run() { restoreBlockList(pillarBlocks, 0L); }
                    }.runTaskLater(magicPlugin, 100L);
                    cancel();
                    return;
                }

                int r = radii[ring];
                int pillarHeight = (r == 0) ? 8 : (r == 1) ? 6 : (r == 2) ? 4 : 2;

                if (r == 0) {
                    buildSpike(center, pillarHeight, Material.STONE, placed);
                } else {
                    int steps = Math.max(8, r * 8);
                    for (int i = 0; i < steps; i++) {
                        double angle = (Math.PI * 2 / steps) * i;
                        Location pos = center.clone().add(
                                Math.cos(angle) * r, 0, Math.sin(angle) * r);
                        pos = getGroundBelow(pos);

                        int h = pillarHeight - rng.nextInt(2);
                        buildSpike(pos, h, r <= 2 ? Material.COBBLESTONE : Material.GRAVEL, placed);
                    }
                }

                Location ringTop = center.clone().add(0, pillarHeight + 1, 0);
                particleApi.spawnColoredParticles(ringTop, C_STONE, 2.5f, 12, r + 0.5, 0.2, r + 0.5);
                particleApi.spawnColoredParticles(ringTop, C_DUST,  1.5f, 8,  r + 0.3, 0.1, r + 0.3);
                world.playSound(center, Sound.BLOCK_STONE_PLACE,       0.9f, 0.3f + ring * 0.1f);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,  0.6f, 0.5f + ring * 0.15f);

                for (Entity e : world.getNearbyEntities(center, r + 1, pillarHeight + 2, r + 1)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    if (ring == 0) {
                        le.damage(18.0, p);
                        le.setVelocity(le.getVelocity().add(new Vector(0, 1.4, 0)));
                    } else {
                        le.damage(8.0 - ring * 1.5, p);
                        Vector away = knockbackVector(center, le, 1.2, 0.5);
                        le.setVelocity(away);
                    }
                }

                ring++;
            }
        }.runTaskTimer(magicPlugin, 0L, 4L);

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,    1f,   0.3f);
        world.playSound(center, Sound.BLOCK_STONE_BREAK,         1f,   0.3f);
        world.playSound(center, Sound.ENTITY_WITHER_SHOOT,       0.7f, 0.4f);
        sendActionBar(p, "§8▲ SPIKE PILLAR ▲");
        addCd(CD_PILLAR, p);
    }

    private void buildSpike(Location base, int height, Material mat, List<Block> placed) {
        for (int h = 0; h < height; h++) {
            Block b = base.clone().add(0, h, 0).getBlock();
            if (!b.isPassable()) continue;
            b.setType(mat);
            placed.add(b);
        }
        Location tip = base.clone().add(0, height, 0);
        particleApi.spawnColoredParticles(tip, C_GRAVEL, 1.8f, 3, 0.15, 0.1, 0.15);
    }

    private void impactBurst(Location loc) {
        particleApi.spawnColoredParticles(loc, C_STONE,  2f,   12, 0.4, 0.4, 0.4);
        particleApi.spawnColoredParticles(loc, C_GRAVEL, 1.5f, 10, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_DUST,   1.2f, 8,  0.5, 0.2, 0.5);
        particleApi.spawnParticles(loc, Particle.EXPLOSION, 2, 0.2, 0.2, 0.2, 0.05f);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK,  0.8f, 0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
    }

    private void spawnImpactParticles(Location loc) {
        particleApi.spawnColoredParticles(loc, C_STONE,  2f,   8, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_GRAVEL, 1.5f, 5, 0.2, 0.2, 0.2);
        particleApi.spawnColoredParticles(loc, C_DARK,   1.2f, 4, 0.2, 0.1, 0.2);
    }

    private void restoreBlockList(List<Block> blocks, long delay) {
        if (blocks.isEmpty()) return;
        List<Block> copy = new ArrayList<>(blocks);
        blocks.clear();
        new BukkitRunnable() {
            @Override public void run() {
                for (Block b : copy) b.setType(Material.AIR);
            }
        }.runTaskLater(magicPlugin, delay);
    }

    @Override
    public void remove() {
        restoreBlockList(wallBlocks,   0L);
        restoreBlockList(trailBlocks,  0L);
        restoreBlockList(pillarBlocks, 0L);
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§7⬦ Spike Shoot";
            case 1: return "§8▬ Spike Wall (Sneak x1)";
            case 2: return "§8▲ Spike Trail (Sneak x2)";
            case 3: return "§c▲ Spike Pillar (Ultimate)";
            default: return "§7none";
        }
    }
}
