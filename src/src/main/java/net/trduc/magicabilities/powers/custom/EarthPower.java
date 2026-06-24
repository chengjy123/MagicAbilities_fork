package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilities.MagicAbilities.magicPlugin;
import static net.trduc.magicabilities.MagicAbilities.particleApi;
import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.misc.PowerUtils.*;
import static net.trduc.magicabilities.players.PowerPlayer.players;

public class EarthPower extends Power implements IdlePower, Removeable {

    private static final String CD_IMPACT   = "earth.impact";
    private static final String CD_FORTRESS = "earth.fortress";
    private static final String CD_BOULDER  = "earth.boulder";
    private static final String CD_TOMB     = "earth.tomb";
    private static final String CD_SLAM     = "earth.slam";

    private static final Color C_DIRT  = Color.fromRGB(101, 67,  33);
    private static final Color C_STONE = Color.fromRGB(128, 128, 128);
    private static final Color C_SAND  = Color.fromRGB(194, 178, 128);
    private static final Color C_DARK  = Color.fromRGB(60,  40,  20);

    private boolean fortressActive = false;

    private final List<Block> tombBlocks = new ArrayList<>();

    private double slamStartY = -1;

    private final Random rng = new Random();

    public EarthPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) { passiveRockSkin((DamagedByExecute) ex); return; }
        if (ex instanceof DamagedExecute)   { handleFortressDmg((DamagedExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeft((LeftClickExecute) ex);
        if (ex instanceof SneakExecute)     onSneak((SneakExecute) ex);
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: earthImpact(p);    break;
            case 1: stoneFortress(p);  break;
            case 2: boulderThrow(p);   break;
            case 3: rockTomb(p);       break;
        }
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot == 4) tectonicSlam(p);
    }

    private void earthImpact(Player p) {
        if (onCd(CD_IMPACT, p, this)) return;

        final Location ground = p.getLocation().clone();

        new BukkitRunnable() {
            double radius = 0.3;
            int t = 0;
            @Override public void run() {
                if (radius > 7.5) { cancel(); return; }
                particleCircle(ground.clone().add(0, 0.08, 0), radius, C_DIRT,  2f, 20, t * 18);
                particleCircle(ground.clone().add(0, 0.08, 0), radius, C_STONE, 1.5f, 10, -t * 18);

                for (int i = 0; i < 3; i++) {
                    double a = rng.nextDouble() * Math.PI * 2;
                    Location dust = ground.clone().add(Math.cos(a)*radius*0.8, 0, Math.sin(a)*radius*0.8);
                    particleApi.spawnColoredParticles(dust, C_SAND, 1.5f, 1, 0.1, 0.3, 0.1);
                }
                if (t % 3 == 0)
                    ground.getWorld().playSound(ground, Sound.BLOCK_STONE_HIT, 0.6f, 0.5f);
                radius += 0.55;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        for (Entity e : p.getNearbyEntities(7, 4, 7)) {
            if (!(e instanceof LivingEntity) || e.equals(p)) continue;
            LivingEntity le = (LivingEntity) e;
            le.damage(7.0, p);

            Vector kb = knockbackVector(p.getLocation(), le, 1.0, 0.55);
            le.setVelocity(kb);
            spawnEarthBurst(le.getLocation().add(0, 0.5, 0), 8);
        }

        ground.getWorld().playSound(ground, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.4f);
        ground.getWorld().playSound(ground, Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
        addCd(CD_IMPACT, p);
    }

    private void stoneFortress(Player p) {
        if (onCd(CD_FORTRESS, p, this)) return;
        if (fortressActive) { sendActionBar(p, "§7Stone Armor is already active!"); return; }

        fortressActive = true;

        applyPotion(p, PotionEffectType.RESISTANCE,  20 * 6, 1);
        applyPotion(p, PotionEffectType.ABSORPTION,  20 * 6, 2);

        new BukkitRunnable() {
            @Override public void run() {
                for (int lvl = 0; lvl <= 2; lvl++) {
                    particleCircle(p.getLocation().clone().add(0, lvl * 0.9 + 0.1, 0),
                            0.8, C_STONE, 2.5f, 14, lvl * 30);
                    particleCircle(p.getLocation().clone().add(0, lvl * 0.9 + 0.1, 0),
                            0.8, C_DARK,  1.5f,  8, lvl * 30 + 22);
                }
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 0.5f);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.5f);
            }
        }.runTask(magicPlugin);

        sendActionBar(p, "§7🪨 Stone Fortress activated!");

        new BukkitRunnable() {
            @Override public void run() {
                fortressActive = false;
                if (p.isOnline()) {
                    sendActionBar(p, "§7Stone Armor shattered.");
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.6f);
                    spawnEarthBurst(p.getLocation().add(0, 1, 0), 14);
                }
            }
        }.runTaskLater(magicPlugin, 20 * 6);

        addCd(CD_FORTRESS, p);
    }

    private void handleFortressDmg(DamagedExecute ex) {
        if (!fortressActive) return;
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;
        event.setDamage(event.getDamage() * 0.80);
        Player p = ex.getPlayer();
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0),
                C_STONE, 2f, 4, 0.25, 0.25, 0.25);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_HIT, 0.7f, 1.2f);
    }

    private void boulderThrow(Player p) {
        if (onCd(CD_BOULDER, p, this)) return;

        Vector dir = p.getEyeLocation().getDirection().normalize();
        ArmorStand boulder = spawnProjectile(p);
        final List<Entity> hit = new ArrayList<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (boulder.isDead() || t > 36) { safeRemove(boulder); cancel(); return; }

                boulder.teleport(boulder.getLocation().add(dir.clone().multiply(1.3)));
                Location bl = boulder.getLocation();

                for (int i = 0; i < 4; i++) {
                    double a = rng.nextDouble() * Math.PI * 2;
                    double r = rng.nextDouble() * 0.5;
                    Location ring = bl.clone().add(Math.cos(a)*r, rng.nextDouble()*0.5-0.25, Math.sin(a)*r);
                    particleApi.spawnColoredParticles(ring,
                            rng.nextBoolean() ? C_STONE : C_DARK, 3f, 1, 0.05, 0.05, 0.05);
                }

                particleApi.spawnColoredParticles(bl, C_DIRT, 1.5f, 2, 0.2, 0.1, 0.2);

                for (Entity e : bl.getChunk().getEntities()) {
                    if (e instanceof ArmorStand || e.equals(p) || hit.contains(e)) continue;
                    if (e instanceof LivingEntity && bl.distanceSquared(e.getLocation()) <= 5.0) {
                        ((LivingEntity) e).damage(11.0, p);

                        Vector kb = knockbackVector(p.getLocation(), e, 1.5, 0.4);
                        e.setVelocity(kb);
                        hit.add(e);
                        spawnEarthBurst(bl, 16);
                        bl.getWorld().playSound(bl, Sound.BLOCK_STONE_BREAK, 1f, 0.4f);
                        safeRemove(boulder); cancel(); return;
                    }
                }

                if (!bl.getBlock().isPassable()) {
                    spawnEarthBurst(bl, 12);
                    bl.getWorld().playSound(bl, Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
                    safeRemove(boulder); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 0.6f);
        addCd(CD_BOULDER, p);
    }

    private void rockTomb(Player p) {
        if (onCd(CD_TOMB, p, this)) return;
        LivingEntity target = getInSight(p, 12, 0.91);
        if (target == null) target = getNearestTarget(p, 5);
        if (target == null) { sendActionBar(p, "§cNo target!"); return; }

        final LivingEntity prisoner = target;
        final Location base = prisoner.getLocation().clone();

        final List<Block> placed = new ArrayList<>();
        int[][] offsets = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] off : offsets) {
            for (int h = 0; h < 3; h++) {
                Block b = base.clone().add(off[0], h, off[1]).getBlock();
                if (b.getType() == Material.AIR || b.getType() == Material.CAVE_AIR) {
                    b.setType(Material.STONE);
                    placed.add(b);
                    tombBlocks.add(b);
                }
            }
        }

        Block lid = base.clone().add(0, 3, 0).getBlock();
        if (lid.getType() == Material.AIR) { lid.setType(Material.STONE); placed.add(lid); tombBlocks.add(lid); }

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 10) { cancel(); return; }
                particleCircle(base.clone().add(0, t * 0.3, 0), 1.2, C_STONE, 2f, 12, t * 20);
                particleCircle(base.clone().add(0, t * 0.3, 0), 1.2, C_DARK,  1.5f, 6, -t * 20);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        prisoner.damage(6.0, p);
        applyPotion(prisoner, PotionEffectType.SLOWNESS,  20 * 4, 10);
        applyPotion(prisoner, PotionEffectType.JUMP_BOOST, 20 * 4, -1);
        prisoner.setVelocity(new Vector(0, 0, 0));

        base.getWorld().playSound(base, Sound.BLOCK_STONE_PLACE, 1f, 0.4f);
        base.getWorld().playSound(base, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.6f);

        new BukkitRunnable() {
            @Override public void run() {
                for (Block b : placed) {
                    if (b.getType() == Material.STONE) b.setType(Material.AIR);
                    tombBlocks.remove(b);
                }
                spawnEarthBurst(base.clone().add(0, 1, 0), 20);
                if (prisoner.isValid()) prisoner.damage(5.0, p);
                base.getWorld().playSound(base, Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
            }
        }.runTaskLater(magicPlugin, 20 * 4);

        addCd(CD_TOMB, p);
    }

    private void tectonicSlam(Player p) {
        if (onCd(CD_SLAM, p, this)) return;

        p.setVelocity(new Vector(0, 1.8, 0));
        slamStartY = p.getLocation().getY();
        sendActionBar(p, "§6🌍 Charging energy...");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.4f);

        new BukkitRunnable() {
            boolean rising = true;
            int safety = 0;

            @Override public void run() {
                if (!p.isOnline() || safety > 100) { cancel(); return; }
                safety++;

                if (isAuraEnabled(p)) {
                    particleApi.spawnColoredParticles(
                            p.getLocation().clone().add(0, 0.5, 0),
                            C_DIRT, 2f, 2, 0.3, 0.1, 0.3);
                }

                double vy = p.getVelocity().getY();
                if (vy < 0) rising = false;

                if (!rising && p.isOnGround()) {
                    cancel();

                    double fallHeight = Math.max(0, slamStartY - p.getLocation().getY() + 2);

                    double actualFall = Math.abs(p.getLocation().getY() - slamStartY) + 2;
                    double damage = 8.0 + actualFall * 1.5;
                    damage = Math.min(damage, 28.0);

                    final Location impact = p.getLocation().clone();

                    new BukkitRunnable() {
                        double r = 0.3;
                        int t2 = 0;
                        @Override public void run() {
                            if (r > 7) { cancel(); return; }
                            particleCircle(impact.clone().add(0, 0.06, 0), r, C_DIRT,  3f, 24, t2 * 20);
                            particleCircle(impact.clone().add(0, 0.06, 0), r, C_STONE, 2f, 14, -t2 * 20);
                            for (int i = 0; i < 4; i++) {
                                double a = rng.nextDouble() * Math.PI * 2;
                                Location dust = impact.clone().add(Math.cos(a)*r*0.8, 0, Math.sin(a)*r*0.8);
                                particleApi.spawnColoredParticles(dust, C_SAND, 2f, 1, 0.1, 0.5, 0.1);
                            }
                            r += 0.6; t2++;
                        }
                    }.runTaskTimer(magicPlugin, 0, 1);

                    final double finalDamage = damage;
                    for (Entity e : p.getNearbyEntities(6, 4, 6)) {
                        if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                        ((LivingEntity) e).damage(finalDamage, p);
                        Vector kb = knockbackVector(impact, e, 1.2, 0.6);
                        e.setVelocity(kb);
                        spawnEarthBurst(e.getLocation().add(0, 0.5, 0), 10);
                    }

                    impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.3f);
                    impact.getWorld().playSound(impact, Sound.BLOCK_STONE_BREAK, 1f, 0.4f);
                    impact.getWorld().playSound(impact, Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
                    sendActionBar(p, String.format("§6🌍 Tectonic Slam — §f%.0f §6dame!", finalDamage));
                }
            }
        }.runTaskTimer(magicPlugin, 5, 1);

        addCd(CD_SLAM, p);
    }

    private void passiveRockSkin(DamagedByExecute ex) {
        if (!(ex.getRawEvent() instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        if (!(event.getDamager() instanceof LivingEntity)) return;
        if (rng.nextFloat() >= 0.25f) return;

        LivingEntity attacker = (LivingEntity) event.getDamager();
        attacker.damage(3.0, getOwner());
        spawnEarthBurst(attacker.getLocation().add(0, 1, 0), 6);
        attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_STONE_HIT, 0.8f, 1.3f);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                applyPotionSilent(p, PotionEffectType.STRENGTH,  30, 0);
                applyPotionSilent(p, PotionEffectType.SLOWNESS,  30, 0);

                if (isAuraEnabled(p)) {

                    particleCircle(p.getLocation().clone().add(0, 0.07, 0),
                            0.65, C_DIRT, 1.5f, 7, t * 18);
                    particleCircle(p.getLocation().clone().add(0, 0.07, 0),
                            0.65, C_STONE, 1f, 5, -t * 18 + 25);

                    if (t % 3 == 0) {
                        double a = rng.nextDouble() * Math.PI * 2;
                        Location pebble = p.getLocation().clone().add(
                                Math.cos(a) * 0.5, 1.6 + rng.nextDouble() * 0.4, Math.sin(a) * 0.5);
                        particleApi.spawnColoredParticles(pebble,
                                rng.nextBoolean() ? C_STONE : C_DARK, 2f, 1, 0.05, 0.05, 0.05);
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
        for (Block b : tombBlocks) {
            if (b.getType() == Material.STONE) b.setType(Material.AIR);
        }
        tombBlocks.clear();
        fortressActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§6Earth Impact";
            case 1: return "§6Stone Fortress";
            case 2: return "§6Boulder Throw";
            case 3: return "§6Rock Tomb";
            case 4: return "§6Tectonic Slam";
            default: return "§7none";
        }
    }

    private void spawnEarthBurst(Location loc, int count) {
        particleApi.spawnColoredParticles(loc, C_DIRT,  2f,   count / 2, 0.4, 0.3, 0.4);
        particleApi.spawnColoredParticles(loc, C_STONE, 1.5f, count / 2, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_SAND,  2.5f, 2,         0.2, 0.2, 0.2);
    }
}
