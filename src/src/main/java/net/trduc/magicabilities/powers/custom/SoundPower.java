package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
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

public class SoundPower extends Power implements IdlePower, Removeable {

    private static final String sp_burst     = "sound.burst";
    private static final String sp_resonance = "sound.resonance";
    private static final String sp_dissonance= "sound.dissonance";
    private static final String sp_veil      = "sound.veil";
    private static final String sp_wail      = "sound.wail";
    private static final String sp_domain    = "sound.domain";

    private static final Color C_CYAN      = Color.fromRGB(0,   220, 255);
    private static final Color C_INDIGO    = Color.fromRGB(80,  0,   200);
    private static final Color C_LAVENDER  = Color.fromRGB(180, 130, 255);
    private static final Color C_TEAL      = Color.fromRGB(0,   180, 180);
    private static final Color C_WHITE_SOF = Color.fromRGB(210, 240, 255);
    private static final Color C_DISSONANCE= Color.fromRGB(150, 0,   180);
    private static final Color C_WAIL_GREY = Color.fromRGB(60,  60,  90);

    private static final Color[] AURA_COLS   = { C_CYAN, C_INDIGO, C_LAVENDER };
    private static final Color[] BURST_COLS  = { C_CYAN, C_WHITE_SOF, C_TEAL };
    private static final Color[] DOMAIN_COLS = { C_INDIGO, C_LAVENDER, C_CYAN };

    private final Set<UUID> dissonanceMarked = new HashSet<>();
    private boolean isInDomain = false;

    public SoundPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(sp_burst,      p, this)) return; sonicBurst(p);      addCd(sp_burst,      p); return;
            case 1: if (onCd(sp_resonance,  p, this)) return; resonanceStrike(p); addCd(sp_resonance,  p); return;
            case 2: if (onCd(sp_dissonance, p, this)) return; dissonance(p);      addCd(sp_dissonance, p); return;
            case 3: if (onCd(sp_veil,       p, this)) return; sonicVeil(p);       addCd(sp_veil,       p); return;
            case 4: if (onCd(sp_wail,       p, this)) return; wail(p);            addCd(sp_wail,       p); return;
            case 5: if (onCd(sp_domain,     p, this)) return; domainResonantHall(p); addCd(sp_domain,  p); return;
        }
    }

    private void sonicBurst(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 2.0f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.8f, 0.5f);

        final Location origin = p.getEyeLocation().clone();
        final Vector   dir    = origin.getDirection().clone().normalize();
        final Random   r      = new Random();
        final Set<UUID> hit   = new HashSet<>();

        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave > 10) { cancel(); return; }

                double dist = wave * 0.8;
                Location center = origin.clone().add(dir.clone().multiply(dist));

                Vector perp1 = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                Vector perp2 = dir.clone().crossProduct(perp1).normalize();
                double radius = Math.min(dist * 0.5 + 0.4, 2.5);

                for (int i = 0; i < 24; i++) {
                    double ang = Math.toRadians(i * 15);
                    Vector offset = perp1.clone().multiply(Math.cos(ang) * radius)
                            .add(perp2.clone().multiply(Math.sin(ang) * radius));
                    Location pt = center.clone().add(offset);
                    Color c = BURST_COLS[r.nextInt(BURST_COLS.length)];
                    particleApi.spawnColoredParticles(pt, c, 1.3f, 1, 0.02, 0.02, 0.02);
                    if (i % 6 == 0)
                        particleApi.spawnParticles(pt, Particle.ELECTRIC_SPARK, 1, 0.02, 0.02, 0.02, 0.1);
                }

                for (Entity e : p.getWorld().getNearbyEntities(center, radius + 0.5, radius + 0.5, radius + 0.5)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    if (hit.contains(e.getUniqueId())) continue;

                    Vector toE = e.getLocation().toVector().subtract(origin.toVector()).normalize();
                    if (toE.dot(dir) < 0.4) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).setNoDamageTicks(0);
                    ((LivingEntity) e).damage(18, p);
                    e.setVelocity(dir.clone().multiply(1.2).add(new Vector(0, 0.3, 0)));
                    p.getWorld().playSound(e.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.5f, 1.5f);
                }

                wave++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void resonanceStrike(Player p) {
        LivingEntity target = getNearestTarget(p, 10);
        if (target == null) {
            sendActionBar(p, "§bNo target in range.");
            return;
        }

        boolean amplified = dissonanceMarked.contains(target.getUniqueId());
        double  damage    = amplified ? 36 : 18;

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, amplified ? 0.5f : 1.2f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.7f, amplified ? 0.3f : 1.8f);

        final LivingEntity tgt = target;
        final Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 8 || tgt.isDead()) { cancel(); return; }
                double angle = t * 45;
                for (int i = 0; i < 3; i++) {
                    double a   = Math.toRadians(angle + i * 120);
                    double rad = 1.2 - t * 0.12;
                    Location ring = tgt.getLocation().clone().add(0, 1, 0)
                            .add(Math.cos(a) * rad, Math.sin(t * 0.4) * 0.3, Math.sin(a) * rad);
                    Color c = amplified ? C_LAVENDER : C_CYAN;
                    particleApi.spawnColoredParticles(ring, c, amplified ? 2.0f : 1.4f, 2, 0.02, 0.02, 0.02);
                    particleApi.spawnParticles(ring, Particle.NOTE, 1, 0, 0, 0, amplified ? 1 : 0);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tgt.isDead()) return;
                tgt.setNoDamageTicks(0);
                tgt.damage(damage, p);
                dissonanceMarked.remove(tgt.getUniqueId());

                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5);
                    Location pt = tgt.getLocation().clone().add(0, 1, 0)
                            .add(Math.cos(a) * 0.6, 0, Math.sin(a) * 0.6);
                    particleApi.spawnColoredParticles(pt, amplified ? C_LAVENDER : C_CYAN,
                            amplified ? 2.2f : 1.5f, 2, 0.03, 0.03, 0.03);
                }
                particleApi.spawnParticles(tgt.getLocation().clone().add(0, 1, 0),
                        Particle.NOTE, amplified ? 8 : 4, 0.3, 0.3, 0.3, 1);
                p.getWorld().playSound(tgt.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, amplified ? 0.4f : 1.0f);
                if (amplified)
                    sendActionBar(p, "§dResonance! §fx" + (int)(damage/18) + " damage");
            }
        }.runTaskLater(magicPlugin, 9);
    }

    private void dissonance(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.4f, 1.8f);

        final Location center = p.getLocation().clone().add(0, 1, 0);
        final Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 12) { cancel(); return; }
                double rad = t * 0.85;
                for (int i = 0; i < 36; i++) {
                    double ang = Math.toRadians(i * 10 + (t % 2 == 0 ? 5 : 0));

                    double r2 = rad + (i % 3 == 0 ? 0.3 : i % 3 == 1 ? -0.2 : 0.1);
                    Location pt = center.clone().add(Math.cos(ang) * r2, 0, Math.sin(ang) * r2);
                    particleApi.spawnColoredParticles(pt, C_DISSONANCE, 1.2f, 1, 0.02, 0.05, 0.02);
                    if (i % 9 == 0)
                        particleApi.spawnParticles(pt, Particle.SCULK_SOUL, 1, 0.02, 0.05, 0.02, 0.01);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : p.getWorld().getNearbyEntities(center, 10, 10, 10)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    applyPotion(le, PotionEffectType.NAUSEA,         100, 0);
                    applyPotion(le, PotionEffectType.SLOWNESS,        80, 1);
                    applyPotion(le, PotionEffectType.MINING_FATIGUE,  60, 2);
                    dissonanceMarked.add(e.getUniqueId());

                    for (int i = 0; i < 6; i++) {
                        Location pt = le.getLocation().clone().add(
                                r.nextDouble() * 0.8 - 0.4, r.nextDouble() * 2,
                                r.nextDouble() * 0.8 - 0.4);
                        particleApi.spawnColoredParticles(pt, C_DISSONANCE, 1.0f, 1, 0.05, 0.05, 0.05);
                    }
                    particleApi.spawnParticles(le.getLocation().clone().add(0, 1, 0),
                            Particle.NOTE, 3, 0.2, 0.2, 0.2, 1);
                    p.getWorld().playSound(le.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 0.3f);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() { dissonanceMarked.clear(); }
                }.runTaskLater(magicPlugin, 200);
            }
        }.runTaskLater(magicPlugin, 10);
    }

    private void sonicVeil(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.8f);

        applyPotion(p, PotionEffectType.SPEED,      160, 1);
        applyPotion(p, PotionEffectType.RESISTANCE, 160, 1);

        final Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 20) { cancel(); return; }
                for (int layer = 0; layer < 3; layer++) {
                    double y = layer * 0.85;
                    for (int i = 0; i < 12; i++) {
                        double ang = Math.toRadians(i * 30 + t * 18 + layer * 40);
                        double rad = 0.65 + Math.sin(t * 0.3 + layer) * 0.15;
                        Location pt = p.getLocation().clone().add(
                                Math.cos(ang) * rad, y, Math.sin(ang) * rad);
                        particleApi.spawnColoredParticles(pt, C_CYAN, 1.2f, 1, 0.02, 0.02, 0.02);
                    }
                }
                if (t % 4 == 0)
                    particleApi.spawnParticles(p.getLocation().clone().add(0, 1, 0),
                            Particle.NOTE, 2, 0.3, 0.2, 0.3, 1);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        sendActionBar(p, "§bSonic Veil §f— resistance & speed for 8 seconds");
    }

    private void wail(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT,   0.6f, 0.4f);

        final Location origin = p.getEyeLocation().clone();
        final Random   r      = new Random();

        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave > 6) { cancel(); return; }
                double rad = wave * 2.2;
                for (int i = 0; i < 48; i++) {
                    double ang = Math.toRadians(i * 7.5);
                    Location pt = origin.clone().add(
                            Math.cos(ang) * rad, 0, Math.sin(ang) * rad);
                    particleApi.spawnColoredParticles(pt, C_WAIL_GREY, 1.1f, 1, 0.02, 0.1, 0.02);
                    if (i % 12 == 0)
                        particleApi.spawnParticles(pt, Particle.SCULK_SOUL, 1, 0.05, 0.1, 0.05, 0.01);
                }
                wave++;
            }
        }.runTaskTimer(magicPlugin, 0, 3);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : p.getWorld().getNearbyEntities(origin, 15, 10, 15)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    applyPotion(le, PotionEffectType.WITHER,  60, 0);
                    applyPotion(le, PotionEffectType.SLOWNESS, 40, 0);

                    Vector flee = e.getLocation().toVector()
                            .subtract(origin.toVector()).normalize().multiply(1.5)
                            .add(new Vector(0, 0.4, 0));
                    e.setVelocity(flee);
                    particleApi.spawnParticles(le.getLocation().clone().add(0, 1, 0),
                            Particle.NOTE, 2, 0.1, 0.1, 0.1, 1);
                    p.getWorld().playSound(le.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.3f, 1.5f);
                }
            }
        }.runTaskLater(magicPlugin, 10);
    }

    private void domainResonantHall(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,          1f,  0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE,  1f,  0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER,  0.8f, 1.0f);

        final Location start  = p.getLocation().clone();
        final Location center = start.clone().add(0, 10, 0);
        final Random   r      = new Random();

        for (int i = 0; i < 25; i++) {
            int fi = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 360; j += 10) {
                        double ang = Math.toRadians(j);
                        Location pt = start.clone().add(Math.cos(ang) * fi, 0, Math.sin(ang) * fi);
                        adjustToGround(pt);
                        particleApi.spawnColoredParticles(pt, DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)],
                                1.8f, 2, 0.01, 0.01, 0.01);
                        if (j % 30 == 0)
                            particleApi.spawnParticles(pt, Particle.NOTE, 1, 0, 0, 0, r.nextInt(3));
                    }
                }
            }.runTaskLater(magicPlugin, i);
        }

        for (int i = 0; i < 25; i++) {
            int fi = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 360; j += 10) {
                        double ang = Math.toRadians(j);
                        Location pt = start.clone().add(
                                Math.cos(ang) * fi, 25 - fi, Math.sin(ang) * fi);
                        particleApi.spawnColoredParticles(pt, DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)],
                                1.8f, 2, 0.01, 0.01, 0.01);
                    }
                }
            }.runTaskLater(magicPlugin, 60 - i);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isInDomain = true;
                final int domainTime = 20;

                p.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT,         1f, 0.5f);
                p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE,     1f, 0.2f);

                applyPotion(p, PotionEffectType.STRENGTH,  domainTime * 20, 1);
                applyPotion(p, PotionEffectType.HASTE,     domainTime * 20, 1);
                applyPotion(p, PotionEffectType.SPEED,     domainTime * 20, 0);

                applyDomainDebuffs(p, center, domainTime);

                new BukkitRunnable() {
                    double remaining = domainTime;
                    int    i         = 0;

                    @Override
                    public void run() {
                        if (remaining <= 0) {
                            isInDomain = false;
                            cancel();
                            return;
                        }

                        if (i % 4 == 0) {
                            double ang = Math.toRadians(i * 13);
                            for (int layer = 0; layer < 4; layer++) {
                                double a = ang + Math.toRadians(layer * 90);
                                double rad = 8 + Math.sin(i * 0.05 + layer) * 4;
                                double ht  = 2 + Math.sin(i * 0.08 + layer * 1.3) * 1.5;
                                Location notePt = start.clone().add(Math.cos(a)*rad, ht, Math.sin(a)*rad);
                                particleApi.spawnParticles(notePt, Particle.NOTE, 1, 0, 0, 0, r.nextInt(15));
                                particleApi.spawnColoredParticles(notePt,
                                        DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 1.2f, 1, 0.1, 0.1, 0.1);
                            }
                        }

                        if (i % 20 == 0) {
                            double beatRad = (i / 20 % 5) * 5.0;
                            for (int j = 0; j < 48; j++) {
                                double ang = Math.toRadians(j * 7.5);
                                Location ring = start.clone().add(
                                        Math.cos(ang) * beatRad, 0.05, Math.sin(ang) * beatRad);
                                particleApi.spawnColoredParticles(ring,
                                        DOMAIN_COLS[r.nextInt(DOMAIN_COLS.length)], 1.5f, 1, 0.02, 0.01, 0.02);
                            }
                            p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.4f,
                                    0.8f + r.nextFloat() * 0.4f);
                        }

                        applyDomainDebuffs(p, center, (int) remaining);
                        remaining -= 0.05;
                        i++;
                    }
                }.runTaskTimer(magicPlugin, 0, 1);
            }
        }.runTaskLater(magicPlugin, 60);
    }

    private void applyDomainDebuffs(Player owner, Location center, int remainingSeconds) {
        for (Entity e : owner.getWorld().getNearbyEntities(center, 25, 25, 25)) {
            if (!(e instanceof LivingEntity) || e.equals(owner)) continue;
            LivingEntity le = (LivingEntity) e;
            applyPotion(le, PotionEffectType.NAUSEA,        remainingSeconds * 20, 0);
            applyPotion(le, PotionEffectType.SLOWNESS,      remainingSeconds * 20, 1);
            applyPotion(le, PotionEffectType.MINING_FATIGUE,remainingSeconds * 20, 2);
            applyPotion(le, PotionEffectType.GLOWING,       remainingSeconds * 20, 0);
        }
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (isAuraEnabled(p)) {

                    particleCircle(p.getLocation().clone().add(0, 0.06, 0),
                            0.65, C_CYAN, 0.9f, 6, t * 20);
                    particleCircle(p.getLocation().clone().add(0, 0.06, 0),
                            0.65, C_INDIGO, 0.8f, 4, -t * 20 + 45);

                    if (t % 3 == 0) {
                        double ang = Math.toRadians(t * 40);
                        Location notePt = p.getLocation().clone().add(
                                Math.cos(ang) * 0.7, 0.2 + r.nextDouble() * 0.2, Math.sin(ang) * 0.7);
                        particleApi.spawnParticles(notePt, Particle.NOTE, 1, 0, 0, 0, r.nextInt(5));
                    }
                }
                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 16);
        return task;
    }

    @Override
    public void remove() {
        isInDomain = false;
        dissonanceMarked.clear();
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&bSonic Burst";
            case 1: return "&bResonance Strike";
            case 2: return "&5Dissonance";
            case 3: return "&bSonic Veil";
            case 4: return "&8Wail";
            case 5: return "&dDomain Expansion: Resonant Hall";
            default: return "&7none";
        }
    }
}
