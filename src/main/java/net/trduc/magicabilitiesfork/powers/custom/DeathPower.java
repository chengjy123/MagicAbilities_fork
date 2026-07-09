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

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.*;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class DeathPower extends Power implements IdlePower, Removeable {

    private static final String dp_rend    = "death.rend";
    private static final String dp_scythe  = "death.scythe";
    private static final String dp_step    = "death.step";
    private static final String dp_grasp   = "death.grasp";
    private static final String dp_shroud  = "death.shroud";
    private static final String dp_domain  = "death.domain";

    private static final Color C_BLACK      = Color.fromRGB(10,  10,  10);
    private static final Color C_DARK_GRAY  = Color.fromRGB(45,  45,  45);
    private static final Color C_MID_GRAY   = Color.fromRGB(90,  90,  90);
    private static final Color C_VOID       = Color.fromRGB(20,   0,  30);
    private static final Color C_SOUL_WHITE = Color.fromRGB(200, 210, 220);

    private static final Color[] AURA_COLS   = { C_BLACK, C_DARK_GRAY, C_VOID };
    private static final Color[] SCYTHE_COLS = { C_BLACK, C_DARK_GRAY, C_MID_GRAY };
    private static final Color[] DOMAIN_COLS = { C_BLACK, C_VOID, C_DARK_GRAY };

    private boolean isInDomain = false;

    public DeathPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(dp_rend,   p, this)) return; soulRend(p);         addCd(dp_rend,   p); return;
            case 1: if (onCd(dp_scythe, p, this)) return; deathScythe(p);      addCd(dp_scythe, p); return;
            case 2: if (onCd(dp_step,   p, this)) return; voidStep(p);         addCd(dp_step,   p); return;
            case 3: if (onCd(dp_grasp,  p, this)) return; necroticGrasp(p);    addCd(dp_grasp,  p); return;
            case 4: if (onCd(dp_shroud, p, this)) return; darkShroud(p);       addCd(dp_shroud, p); return;
            case 5: if (onCd(dp_domain, p, this)) return; domainRealmOfDeath(p); addCd(dp_domain, p); return;
        }
    }

    private void soulRend(Player p) {
        LivingEntity target = getNearestTarget(p, 8);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.9f, 1.9f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_BITE, 0.6f, 0.8f);

        final Random r = new Random();

        if (target != null) {
            final LivingEntity tgt = target;
            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    if (t > 6) { cancel(); return; }
                    Location from = p.getEyeLocation().clone();
                    Location to   = tgt.getEyeLocation().clone();

                    Vector step = to.clone().subtract(from).toVector().normalize().multiply(0.4);
                    Location cur = from.clone();
                    while (cur.distanceSquared(to) > 0.5) {
                        Color col = SCYTHE_COLS[r.nextInt(SCYTHE_COLS.length)];
                        particleApi.spawnColoredParticles(cur.clone(), col, 1.8f, 2, 0.05, 0.05, 0.05);
                        cur.add(step);
                    }
                    t++;
                }
            }.runTaskTimer(magicPlugin, 0, 1);

            new BukkitRunnable() {
                @Override
                public void run() {
                    target.damage(22, p);
                    target.setNoDamageTicks(0);
                    applyPotion(target, PotionEffectType.WITHER, 60, 1);

                    particleApi.spawnParticles(target.getLocation().clone().add(0, 1, 0),
                            Particle.SMOKE, 30, 0.3, 0.5, 0.3, 0.05);
                    particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0),
                            C_SOUL_WHITE, 1.5f, 15, 0.3, 0.5, 0.3);
                    p.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 1.5f);
                }
            }.runTaskLater(magicPlugin, 6);
        } else {

            for (int i = 0; i < 16; i++) {
                particleApi.spawnColoredParticles(
                        p.getLocation().clone().add(
                                r.nextDouble()*2-1, r.nextDouble()*2, r.nextDouble()*2-1),
                        AURA_COLS[r.nextInt(AURA_COLS.length)], 1.5f, 2, 0.1, 0.1, 0.1);
            }
            sendActionBar(p, "§8No target in range.");
        }
    }

    private void deathScythe(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);

        final ArmorStand blade = spawnProjectile(p);
        final Vector dir = p.getEyeLocation().getDirection().clone().normalize().multiply(1.1);
        final Random r = new Random();
        final Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (blade.isDead() || t > 45) { safeRemove(blade); cancel(); return; }

                blade.teleport(blade.getLocation().add(dir));

                for (Entity e : blade.getNearbyEntities(1.4, 1.4, 1.4)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    if (hit.contains(e.getUniqueId())) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).setNoDamageTicks(0);
                    ((LivingEntity) e).damage(28, p);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 40, 0);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_SOUL_WHITE, 1.5f, 10, 0.2, 0.3, 0.2);
                    p.getWorld().playSound(e.getLocation(), Sound.ENTITY_PHANTOM_BITE, 0.8f, 0.7f);
                }

                if (!blade.getLocation().getBlock().isPassable()) {

                    for (int i = 0; i < 20; i++) {
                        particleApi.spawnColoredParticles(blade.getLocation(),
                                SCYTHE_COLS[r.nextInt(SCYTHE_COLS.length)], 2f, 3, 0.2, 0.2, 0.2);
                    }
                    safeRemove(blade); cancel(); return;
                }

                for (int i = 0; i < 12; i++) {
                    double ang = Math.toRadians(i * 30);
                    Vector perp = new Vector(Math.cos(ang)*0.7, Math.sin(ang)*0.7, 0);
                    particleApi.spawnColoredParticles(
                            blade.getLocation().clone().add(perp),
                            SCYTHE_COLS[r.nextInt(SCYTHE_COLS.length)], 1.8f, 2, 0.01, 0.01, 0.01);
                }
                particleApi.spawnParticles(blade.getLocation(), Particle.SMOKE, 4, 0.05, 0.05, 0.05, 0.01);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void voidStep(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.5f);

        final Location from = p.getLocation().clone();
        final Vector dir    = p.getEyeLocation().getDirection().clone().setY(0).normalize();
        final Random r      = new Random();

        for (int i = 0; i < 20; i++) {
            particleApi.spawnColoredParticles(
                    from.clone().add(r.nextDouble()*0.6-0.3, r.nextDouble()*2, r.nextDouble()*0.6-0.3),
                    AURA_COLS[r.nextInt(AURA_COLS.length)], 2f, 3, 0.1, 0.1, 0.1);
        }
        particleApi.spawnParticles(from.clone().add(0, 1, 0), Particle.SMOKE, 25, 0.3, 0.5, 0.3, 0.03);

        Location dest = from.clone();
        for (int i = 1; i <= 16; i++) {
            Location candidate = from.clone().add(dir.clone().multiply(i * 0.5));
            candidate.setY(from.getY());
            if (!candidate.getBlock().isPassable()) break;
            dest = candidate;
        }
        dest.setYaw(p.getLocation().getYaw());
        dest.setPitch(p.getLocation().getPitch());

        final Location finalDest = dest;
        new BukkitRunnable() {
            @Override
            public void run() {
                p.teleport(finalDest);
                p.getWorld().playSound(finalDest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);

                for (int i = 0; i < 20; i++) {
                    particleApi.spawnColoredParticles(
                            finalDest.clone().add(r.nextDouble()*0.6-0.3, r.nextDouble()*2, r.nextDouble()*0.6-0.3),
                            AURA_COLS[r.nextInt(AURA_COLS.length)], 2f, 3, 0.1, 0.1, 0.1);
                }
                particleApi.spawnParticles(finalDest.clone().add(0, 1, 0), Particle.SMOKE, 25, 0.3, 0.5, 0.3, 0.03);
            }
        }.runTaskLater(magicPlugin, 1);
    }

    private void necroticGrasp(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.7f, 0.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.6f);

        final Random r = new Random();
        final Location center = p.getLocation().clone().add(0, 1, 0);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 8) { cancel(); return; }
                for (int i = 0; i < 24; i++) {
                    double ang = Math.toRadians(i * 15 + t * 10);
                    double rad = 10 - t;
                    Location ring = center.clone().add(Math.cos(ang)*rad, 0, Math.sin(ang)*rad);
                    particleApi.spawnColoredParticles(ring,
                            DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 1.6f, 2, 0.05, 0.1, 0.05);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                double totalHeal = 0;
                for (Entity e : p.getNearbyEntities(10, 10, 10)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    le.setNoDamageTicks(0);
                    le.damage(10, p);
                    applyPotion(le, PotionEffectType.SLOWNESS, 60, 1);

                    particleApi.spawnColoredParticles(le.getLocation().clone().add(0,1,0),
                            C_SOUL_WHITE, 1.3f, 8, 0.2, 0.2, 0.2);
                    totalHeal += 3;
                    p.getWorld().playSound(le.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.5f, 1.2f);
                }
                if (totalHeal > 0) {
                    safeHeal(p, Math.min(totalHeal, getMaxHp(p) * 0.35));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.5f);
                }
            }
        }.runTaskLater(magicPlugin, 8);
    }

    private void darkShroud(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.8f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 1f, 0.4f);

        final Random r = new Random();
        final Location center = p.getLocation().clone().add(0, 1, 0);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 10) { cancel(); return; }
                double rad = t * 0.8;
                for (int i = 0; i < 36; i++) {
                    double ang = Math.toRadians(i * 10);
                    Location ring = center.clone().add(Math.cos(ang)*rad, 0, Math.sin(ang)*rad);
                    particleApi.spawnColoredParticles(ring,
                            DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 2.2f, 3, 0.05, 0.1, 0.05);
                    particleApi.spawnParticles(ring, Particle.SMOKE, 2, 0.05, 0.1, 0.05, 0.01);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : p.getNearbyEntities(8, 8, 8)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    le.setNoDamageTicks(0);
                    le.damage(20, p);
                    applyPotion(le, PotionEffectType.BLINDNESS, 60, 0);
                    applyPotion(le, PotionEffectType.SLOWNESS, 40, 0);
                    particleApi.spawnColoredParticles(le.getLocation().clone().add(0,1,0),
                            C_DARK_GRAY, 2f, 12, 0.2, 0.3, 0.2);
                }

                for (int i = 0; i < 40; i++) {
                    particleApi.spawnColoredParticles(
                            center.clone().add(r.nextDouble()*4-2, r.nextDouble()*2-0.5, r.nextDouble()*4-2),
                            DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 2.2f, 3, 0.1, 0.1, 0.1);
                }
                particleApi.spawnParticles(center, Particle.SMOKE, 60, 1.5, 1.5, 1.5, 0.04);
            }
        }.runTaskLater(magicPlugin, 5);
    }

    private void domainRealmOfDeath(Player p) {
        final Random r = new Random();
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 0.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.6f);

        Location start;
        if (p.getLocation().clone().add(0, -1, 0).getBlock().isPassable()) {
            start = p.getLocation().clone().add(0, -1, 0);
        } else {
            start = p.getLocation().clone();
        }
        final Location center = start.clone().add(0, 10, 0);

        final Vector v = new Vector(1, 0, 0);
        for (int i = 0; i < 30; i++) {
            int fi = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 360; j++) {
                        double rad = Math.toRadians(j);
                        Location l = start.clone().add(Math.cos(rad)*fi, 0, Math.sin(rad)*fi);
                        adjustToGround(l);
                        particleApi.spawnColoredParticles(l,
                                DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 2.2f, 3, 0.01, 0.01, 0.01);
                        particleApi.spawnParticles(l, Particle.SMOKE, 1, 0.01, 0.05, 0.01, 0);
                    }
                }
            }.runTaskLater(magicPlugin, i);
        }

        for (int i = 0; i < 30; i++) {
            int fi = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 360; j++) {
                        double rad = Math.toRadians(j);
                        Location l = start.clone().add(Math.cos(rad)*fi, 30-fi, Math.sin(rad)*fi);
                        particleApi.spawnColoredParticles(l,
                                DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 2.2f, 3, 0.01, 0.01, 0.01);
                    }
                }
            }.runTaskLater(magicPlugin, 60 - i);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isInDomain = true;
                final int domainTime = 20;

                p.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.8f, 0.7f);
                p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.9f);

                applyPotion(p, PotionEffectType.RESISTANCE, domainTime*20, 1);
                applyPotion(p, PotionEffectType.STRENGTH, domainTime*20, 1);
                applyPotion(p, PotionEffectType.SPEED, domainTime*20, 0);

                applyDomainDebuffs(p, center, domainTime);

                new BukkitRunnable() {
                    double remaining = domainTime;
                    int i = 0;

                    @Override
                    public void run() {
                        if (remaining <= 0) {
                            isInDomain = false;
                            cancel();
                            return;
                        }

                        if (i % 3 == 0) {
                            double ang = Math.toRadians(i * 7);
                            for (int layer = 0; layer < 5; layer++) {
                                double a   = ang + Math.toRadians(layer * 72);
                                double rad = 10 + Math.sin(i * 0.06 + layer) * 6;
                                double ht  = 1.5 + Math.sin(i * 0.09 + layer * 1.2) * 1.2;
                                Location soul = start.clone().add(Math.cos(a)*rad, ht, Math.sin(a)*rad);
                                particleApi.spawnColoredParticles(soul,
                                        DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 1.6f, 1, 0.05, 0.05, 0.05);
                                particleApi.spawnParticles(soul, Particle.SOUL, 1, 0.04, 0.04, 0.04, 0.01);
                            }
                        }

                        if (i % 10 == 0) {
                            for (int k = 0; k < 40; k++) {
                                double ox = (r.nextDouble()*2-1)*30;
                                double oz = (r.nextDouble()*2-1)*30;
                                Location ash = start.clone().add(ox, r.nextDouble()*1.5, oz);
                                particleApi.spawnParticles(ash, Particle.ASH, 1, 0.4, 0.1, 0.4, 0.02);
                                particleApi.spawnColoredParticles(ash,
                                        DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 1.4f, 1, 0.3, 0.05, 0.3);
                            }
                        }

                        if (i % 20 == 0) {
                            double beatRad = (i / 20 % 6) * 5.0;
                            for (int j = 0; j < 48; j++) {
                                double ang = Math.toRadians(j * 7.5);
                                Location ring = start.clone().add(Math.cos(ang)*beatRad, 0.05, Math.sin(ang)*beatRad);
                                particleApi.spawnColoredParticles(ring, C_BLACK, 1.8f, 1, 0.02, 0.01, 0.02);
                            }
                            p.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.5f + r.nextFloat()*0.3f);
                        }

                        applyDomainDebuffs(p, center, (int) remaining);

                        if (i % 2 == 0) {
                            int borderPts = 64;
                            for (int b = 0; b < borderPts; b++) {
                                double ba = Math.toRadians(b * (360.0 / borderPts) + i * 1.5);
                                Location bl = start.clone().add(Math.cos(ba) * 30, 0.15, Math.sin(ba) * 30);
                                particleApi.spawnColoredParticles(bl, C_BLACK, 2.0f, 1, 0.01, 0.1, 0.01);
                                if (b % 8 == 0) {
                                    particleApi.spawnColoredParticles(bl.clone().add(0, 0.5, 0), C_VOID, 1.5f, 1, 0.01, 0.06, 0.01);
                                    particleApi.spawnParticles(bl.clone().add(0, 0.3, 0), Particle.SOUL, 1, 0.01, 0.05, 0.01, 0.01);
                                }
                            }
                        }

                        remaining -= 0.05;
                        i++;
                    }
                }.runTaskTimer(magicPlugin, 0, 1);
            }
        }.runTaskLater(magicPlugin, 60);
    }

    private void applyDomainDebuffs(Player owner, Location center, int remainingSeconds) {
        for (Entity e : owner.getWorld().getNearbyEntities(center, 30, 30, 30)) {
            if (!(e instanceof LivingEntity)) continue;
            if (e.equals(owner)) continue;
            LivingEntity le = (LivingEntity) e;

            if (e instanceof Player) {
                applyPotion((Player) e, PotionEffectType.HUNGER, remainingSeconds*20, 4);
                applyPotion((Player) e, PotionEffectType.MINING_FATIGUE, remainingSeconds*20, 1);
            }

            applyPotion(le, PotionEffectType.WITHER, remainingSeconds*20, 0);
            applyPotion(le, PotionEffectType.GLOWING, remainingSeconds*20, 0);
        }
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAuraEnabled(p)) return;
                particleApi.spawnParticles(p.getLocation().clone().add(0, 0.5, 0),
                        Particle.SMOKE, 6, 0.2, 0.5, 0.2, 0.01);
                particleApi.spawnParticles(p.getLocation().clone().add(0, 1, 0),
                        Particle.ASH, 4, 0.2, 0.3, 0.2, 0.02);
                particleApi.spawnColoredParticles(
                        p.getLocation().clone().add(
                                r.nextDouble()*0.4-0.2, r.nextDouble()*1.8, r.nextDouble()*0.4-0.2),
                        AURA_COLS[r.nextInt(AURA_COLS.length)], 1.4f, 1, 0.05, 0.05, 0.05);
            }
        };
        task.runTaskTimer(magicPlugin, 0, 12);
        return task;
    }

    @Override
    public void remove() {
        isInDomain = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&8灵魂撕裂";
            case 1: return "&8死亡镰刀";
            case 2: return "&8虚空步";
            case 3: return "&8坏死抓取";
            case 4: return "&8黑暗遮蔽";
            case 5: return "&8领域展开：死亡之境";
            default: return "&7none";
        }
    }
}

