package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.misc.GeneralMethods.rotateVector;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class FirePower extends Power implements IdlePower, Removeable {

    private static final String fire_blast       = "fire.blast";
    private static final String fire_barrage     = "fire.barrage";
    private static final String fire_surge       = "fire.surge";
    private static final String divine_flame     = "fire.flame";
    private static final String fire_meteor      = "fire.meteor";
    private static final String fire_shield      = "fire.shield";
    private static final String fire_dash        = "fire.dash";
    private static final String fire_retaliation = "fire.retaliation";
    private BukkitRunnable shieldRunnable = null;
    private boolean shieldActive = false;

    public FirePower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) {
            flameRetaliation((DamagedByExecute) ex);
            return;
        }

        if (!isEnabled()) return;

        if (ex instanceof LeftClickExecute) {
            executeLeftClick((LeftClickExecute) ex);
            return;
        }
        if (ex instanceof RightClickExecute) {
            executeRightClick((RightClickExecute) ex);
            return;
        }
        if (ex instanceof SneakExecute) {
            executeSneak((SneakExecute) ex);
        }
    }

    private void executeLeftClick(LeftClickExecute execute) {
        final Player p = execute.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());

        switch (slot) {
            case 0:
                if (onCd(fire_blast, p, this)) return;
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.5f);
                fireBlast(p, 1.0, 0);
                addCd(fire_blast, p);
                return;

            case 1:
                if (onCd(fire_barrage, p, this)) return;
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.2f);
                infernoBarrage(p);
                addCd(fire_barrage, p);
                return;

            case 2:
                if (onCd(fire_surge, p, this)) return;
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.9f);
                flameSurge(p);
                addCd(fire_surge, p);
                return;

            case 3:
                if (onCd(divine_flame, p, this)) return;
                divineFlame(p);
                addCd(divine_flame, p);
                return;

            case 4:
                if (onCd(fire_meteor, p, this)) return;
                meteorStrike(p);
                addCd(fire_meteor, p);
                return;
        }
    }

    private void executeRightClick(RightClickExecute execute) {
        final Player p = execute.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;

        if (shieldActive) return;
        if (onCd(fire_shield, p, this)) return;

        emberShield(p, false);
        addCd(fire_shield, p);
    }

    private void executeSneak(SneakExecute execute) {
        final Player p = execute.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;

        if (onCd(fire_dash, p, this)) return;
        combustionDash(p);
        addCd(fire_dash, p);
    }

    private void fireBlast(Player p, double damageMult, int yawOffset) {
        ArmorStand as = spawnProjectile(p);
        Vector dir = rotateVector(p.getEyeLocation().getDirection().clone(), yawOffset).normalize();

        Color[] colors = {Color.fromRGB(255, 72, 5), Color.fromRGB(255, 119, 0), Color.fromRGB(255, 210, 0)};
        Random r = new Random();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (as.isDead() || ticks > 50) { safeRemove(as); cancel(); return; }
                as.teleport(as.getLocation().add(dir.clone().multiply(1.5)));
                Location loc = as.getLocation();
                particleApi.spawnParticles(loc, Particle.FLAME, 8, 0.15, 0.15, 0.15, 0.05);
                particleApi.spawnColoredParticles(loc, colors[r.nextInt(colors.length)], 1.2f, 3, 0.1, 0.1, 0.1);
                particleApi.spawnParticles(loc, Particle.LAVA, 1, 0.1, 0.1, 0.1, 0);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (e instanceof ArmorStand || e.equals(p)) continue;
                    if (e instanceof LivingEntity) {
                        explodeFireBlast(loc, p, 2.5, 10 * damageMult);
                        safeRemove(as); cancel(); return;
                    }
                }
                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    explodeFireBlast(loc, p, 2.2, 8 * damageMult);
                    safeRemove(as); cancel(); return;
                }

                ticks++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void explodeFireBlast(Location loc, Player p, double radius, double damage) {
        particleApi.spawnParticles(loc, Particle.FLAME, 30, 0.5, 0.5, 0.5, 0.15);
        particleApi.spawnColoredParticles(loc, Color.fromRGB(255, 200, 0), 1.5f, 20, 0.4, 0.4, 0.4);

        spawnFireRing(loc, (int) radius, 40L);

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (e instanceof LivingEntity) {
                ((LivingEntity) e).damage(damage, p);
                e.setFireTicks(100);
            }
        }
    }

    private void infernoBarrage(Player p) {
        int bolts = 7;
        int startAngle = -30;
        for (int i = 0; i < bolts; i++) {
            final int yawOff = startAngle + (i * 10);
            new BukkitRunnable() {
                @Override
                public void run() {
                    fireBlast(p, 0.65, yawOff);
                }
            }.runTaskLater(magicPlugin, i * 2L);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.7f);
            }
        }.runTaskLater(magicPlugin, 5L);
    }

    private void flameSurge(Player p) {
        Vector dir = p.getLocation().getDirection().clone().setY(0.15).normalize();
        ArmorStand as = spawnProjectile(p);
        HashMap<Block, Material> oldBlocks = new HashMap<>();
        Random r = new Random();

        p.setVelocity(dir.clone().multiply(1.6));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (as.isDead() || ticks > 18) {
                    Location end = as.getLocation();
                    particleApi.spawnParticles(end, Particle.FLAME, 150, 1.2, 1.2, 1.2, 0.3);
                    particleApi.spawnColoredParticles(end, Color.fromRGB(255, 150, 0), 2f, 40, 0.8, 0.8, 0.8);
                    end.getWorld().playSound(end, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.9f);

                    for (Entity e : end.getWorld().getNearbyEntities(end, 2.5, 2.5, 2.5)) {
                        if (e.equals(p) || e instanceof ArmorStand) continue;
                        if (e instanceof LivingEntity) {
                            ((LivingEntity) e).damage(7, p);
                            e.setFireTicks(60);
                        }
                    }
                    restoreBlocks(oldBlocks, 40L);
                    safeRemove(as);
                    cancel();
                    return;
                }

                as.teleport(as.getLocation().add(dir.clone().multiply(1.4)));
                Location ground = getGroundBelow(as.getLocation());
                Block b = ground.getBlock();
                if (!oldBlocks.containsKey(b) && b.isPassable() && !b.isLiquid()) {
                    oldBlocks.put(b, b.getType());
                    b.setType(Material.FIRE);
                }
                if (r.nextBoolean())
                    particleApi.spawnParticles(as.getLocation(), Particle.FLAME, r.nextInt(8) + 3, 0.2, 0.2, 0.2, 0.05);
                particleApi.spawnColoredParticles(as.getLocation(), Color.fromRGB(255, 80, 0), 1f, 2, 0.1, 0.1, 0.1);
                for (Entity e : as.getLocation().getWorld().getNearbyEntities(as.getLocation(), 1.2, 1.2, 1.2)) {
                    if (e instanceof ArmorStand || e.equals(p)) continue;
                    if (e instanceof LivingEntity) {
                        ((LivingEntity) e).damage(4, p);
                        e.setFireTicks(50);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void divineFlame(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.7f);

        new BukkitRunnable() {
            int t = 70;
            @Override
            public void run() {
                if (t < 1) {
                    cancel();
                    launchDivineOrb(p);
                    return;
                }

                double speed = 3.5 - ((double) 70 / t) * 1.8;
                double angle = t * speed;
                Location eye = p.getEyeLocation().clone();
                Vector fwd = eye.getDirection().setY(0).normalize();

                for (int ring = 0; ring < 4; ring++) {
                    double a = Math.toRadians(angle + ring * 90);
                    Vector offset = new Vector(Math.cos(a) * 0.9, (double) t / 60, Math.sin(a) * 0.9);
                    Location lp = eye.clone().add(fwd).add(offset);
                    particleApi.spawnParticles(lp, Particle.FLAME, 4, 0.03, 0.03, 0.03, 0);
                    particleApi.spawnColoredParticles(lp, Color.fromRGB(255, 200 - t, 0), 1f, 2, 0.02, 0.02, 0.02);
                }
                particleApi.spawnParticles(p.getLocation().add(0, 1, 0),
                        Particle.FLAME, (int) ((double) 70 / (t * 8)) + 1, 0.05, 0.05, 0.05, 1.2f);

                if (t % 20 == 0) p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1f, 2f);
                if (t == 20)    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1.4f);

                t--;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void launchDivineOrb(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1.1f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 2f);

        ArmorStand orb = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        Color[] orbColors = {Color.fromRGB(255, 60, 0), Color.fromRGB(255, 140, 0), Color.fromRGB(255, 255, 0)};
        Random r = new Random();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (orb.isDead() || ticks > 80) { safeRemove(orb); cancel(); return; }

                orb.teleport(orb.getLocation().add(dir.clone().multiply(0.9)));
                Location loc = orb.getLocation();

                for (int ring = 0; ring < 3; ring++) {
                    double a = Math.toRadians(ticks * 25 + ring * 120);
                    Vector rv = new Vector(Math.cos(a), Math.sin(a * 0.5), Math.sin(a)).multiply(0.8);
                    particleApi.spawnParticles(loc.clone().add(rv), Particle.FLAME, 5, 0.1, 0.1, 0.1, 0.05);
                    particleApi.spawnColoredParticles(loc.clone().add(rv), orbColors[r.nextInt(3)], 1.5f, 3, 0.05, 0.05, 0.05);
                }
                particleApi.spawnParticles(loc, Particle.LAVA, 4, 0.3, 0.3, 0.3, 0);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.4, 1.4, 1.4)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        divineExplosion(loc, p);
                        safeRemove(orb); cancel(); return;
                    }
                }

                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    divineExplosion(loc, p);
                    safeRemove(orb); cancel(); return;
                }

                ticks++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void divineExplosion(Location loc, Player p) {
        particleApi.spawnParticles(loc, Particle.FLAME, 50, 1.5, 1.5, 1.5, 0.2);
        particleApi.spawnColoredParticles(loc, Color.fromRGB(255, 220, 0), 2f, 40, 1, 1, 1);

        spawnFireRing(loc, 8, 60L);

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.9f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (e instanceof LivingEntity) {
                double dist = e.getLocation().distance(loc);
                double dmg = Math.max(8, 32 - dist * 3.5);
                ((LivingEntity) e).damage(dmg, p);
                e.setFireTicks(160);
                Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(1.5).setY(0.4);
                e.setVelocity(kb);
            }
        }
    }

    private void meteorStrike(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 0.6f);

        Location target = p.getEyeLocation().clone();
        for (int i = 0; i < 30; i++) {
            target.add(p.getEyeLocation().getDirection().normalize());
            if (!target.getBlock().isPassable()) break;
        }

        final Location finalTarget = target.clone();
        Random r = new Random();

        for (int m = 0; m < 5; m++) {
            final int mIdx = m;
            double ox = (r.nextDouble() - 0.5) * 10;
            double oz = (r.nextDouble() - 0.5) * 10;
            Location spawnPos = finalTarget.clone().add(ox, 25, oz);
            Location impactPos = finalTarget.clone().add(ox, 0, oz);

            new BukkitRunnable() {
                @Override
                public void run() {
                    ArmorStand meteor = p.getWorld().spawn(spawnPos, ArmorStand.class, en -> {
                        en.setVisible(false); en.setGravity(false);
                        en.setSmall(true); en.setMarker(true);
                    });

                    Vector fall = impactPos.clone().subtract(spawnPos).toVector().normalize().multiply(2.0);

                    new BukkitRunnable() {
                        int t = 0;

                        @Override
                        public void run() {
                            if (meteor.isDead() || t > 60) { safeRemove(meteor); cancel(); return; }

                            meteor.teleport(meteor.getLocation().add(fall));
                            Location ml = meteor.getLocation();

                            particleApi.spawnParticles(ml, Particle.FLAME, 12, 0.3, 0.3, 0.3, 0.1);
                            particleApi.spawnColoredParticles(ml, Color.fromRGB(255, 100, 0), 1.5f, 6, 0.2, 0.2, 0.2);
                            particleApi.spawnParticles(ml, Particle.LAVA, 3, 0.2, 0.2, 0.2, 0);

                            boolean hitBlock = !ml.getBlock().isPassable() || ml.getBlock().isLiquid();
                            boolean hitEntity = !ml.getWorld().getNearbyEntities(ml, 1.5, 1.5, 1.5)
                                    .stream().filter(e -> !e.equals(p) && !e.equals(meteor) && e instanceof LivingEntity)
                                    .findFirst().map(e -> false).orElse(true);

                            if (hitBlock || !hitEntity) {
                                meteorImpact(ml, p);
                                safeRemove(meteor); cancel();
                            }
                            t++;
                        }
                    }.runTaskTimer(magicPlugin, 0, 1);
                }
            }.runTaskLater(magicPlugin, mIdx * 8L + r.nextInt(10));
        }
    }

    private void meteorImpact(Location loc, Player p) {
        particleApi.spawnParticles(loc, Particle.FLAME, 40, 1.0, 1.0, 1.0, 0.15);
        particleApi.spawnColoredParticles(loc, Color.fromRGB(255, 180, 0), 2f, 30, 0.8, 0.8, 0.8);
        spawnFireRing(loc, 4, 40L);

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.1f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.6f, 1.4f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (e instanceof LivingEntity) {
                ((LivingEntity) e).damage(12, p);
                e.setFireTicks(100);
                Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(0.9).setY(0.4);
                e.setVelocity(kb);
            }
        }
    }

    private void emberShield(Player p, boolean mini) {
        if (shieldActive && !mini) return;
        shieldActive = true;

        int duration = mini ? 30 : 80;
        double radius = mini ? 1.2 : 2.0;
        Color[] shieldColors = {Color.fromRGB(255, 80, 0), Color.fromRGB(255, 180, 0), Color.fromRGB(255, 255, 100)};
        Random r = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 2f);

        shieldRunnable = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= duration || !p.isOnline()) {
                    shieldActive = false;
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.5f);
                    cancel();
                    return;
                }

                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30 + t * 8);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location lp = p.getLocation().add(x, 1.2, z);
                    particleApi.spawnParticles(lp, Particle.FLAME, 2, 0.05, 0.05, 0.05, 0.01);
                    if (t % 3 == 0)
                        particleApi.spawnColoredParticles(lp, shieldColors[r.nextInt(3)], 1f, 1, 0.05, 0.05, 0.05);
                }

                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45 - t * 10);
                    double x = Math.cos(angle) * (radius * 0.6);
                    double z = Math.sin(angle) * (radius * 0.6);
                    Location lp = p.getLocation().add(x, 1.6, z);
                    particleApi.spawnParticles(lp, Particle.FLAME, 1, 0.04, 0.04, 0.04, 0.02);
                }

                if (t % 5 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius + 0.2, radius + 0.2, radius + 0.2)) {
                        if (e.equals(p) || e instanceof ArmorStand) continue;
                        if (e instanceof LivingEntity) {
                            ((LivingEntity) e).damage(2, p);
                            e.setFireTicks(20);
                        }
                    }
                }

                t++;
            }
        };
        shieldRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void combustionDash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

        Vector dir = p.getLocation().getDirection().clone().setY(0.2).normalize();
        p.setVelocity(dir.clone().multiply(2.2));

        Color[] dashColors = {Color.fromRGB(255, 50, 0), Color.fromRGB(255, 150, 0)};
        Random r = new Random();
        Set<Entity> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 12) { cancel(); return; }

                Location loc = p.getLocation().add(0, 1, 0);

                particleApi.spawnParticles(loc, Particle.FLAME, 20, 0.3, 0.4, 0.3, 0.1);
                particleApi.spawnColoredParticles(loc, dashColors[r.nextInt(2)], 1.2f, 8, 0.2, 0.3, 0.2);
                particleApi.spawnParticles(loc, Particle.LAVA, 2, 0.2, 0.1, 0.2, 0);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.8, 1.8, 1.8)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (e instanceof LivingEntity) {
                        hit.add(e);
                        ((LivingEntity) e).damage(7, p);
                        e.setFireTicks(80);
                        Vector kb = dir.clone().multiply(1.4).setY(0.5);
                        e.setVelocity(kb);
                        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.7f, 1.5f);
                    }
                }

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void flameRetaliation(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (CooldownApi.isOnCooldown(fire_retaliation, p)) return;

        Entity attacker = ((org.bukkit.event.entity.EntityDamageByEntityEvent) ex.getRawEvent()).getDamager();
        if (!(attacker instanceof LivingEntity)) return;

        attacker.setFireTicks(60);
        ((LivingEntity) attacker).damage(3, p);

        Location between = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnParticles(between, Particle.FLAME, 30, 0.5, 0.5, 0.5, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.6f, 2f);

        if (!shieldActive) emberShield(p, true);

        addCdFixed(fire_retaliation, p, 4.0);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (p.getFireTicks() > 0) p.setFireTicks(0);
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 25, 0, false, false));
                if (isAuraEnabled(p)) {
                    particleApi.spawnParticles(p.getLocation().clone().add(0, 0.1, 0),
                            Particle.FLAME, 2, 0.3, 0.02, 0.3, 0.01);
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 0.08, 0),
                            Color.fromRGB(255, 140, 0), 0.8f, 1, 0.25, 0.01, 0.25);
                }
            }
        };
        r.runTaskTimer(magicPlugin, 0, 20);
        return r;
    }

    @Override
    public void remove() {
        if (shieldRunnable != null) {
            shieldRunnable.cancel();
            shieldRunnable = null;
        }
        shieldActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&cFire Blast";
            case 1: return "&cInferno Barrage";
            case 2: return "&cFlame Surge";
            case 3: return "&c&lDivine Flame";
            case 4: return "&4Meteor Strike";
            case 5: return "&6Ember Shield";
            case 6: return "&cCombustion Dash";
            default: return "&7none";
        }
    }

}

