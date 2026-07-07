package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.*;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.cooldowns.Cooldowns.cooldowns;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class WitherPower extends Power implements IdlePower, Removeable {
    private static final String wt_bolt    = "wither.bolt";
    private static final String wt_barrage = "wither.barrage";
    private static final String wt_mark    = "wither.mark";
    private static final String wt_shatter = "wither.shatter";
    private static final String wt_storm   = "wither.storm";
    private static final String wt_rez     = "wither.rez";
    private static final String wt_aura    = "wither.aura";
    private static final String wt_kill    = "wither.kill";
    private static final Color C_WITHER_TEAL  = Color.fromRGB(  0, 210, 180);
    private static final Color C_WITHER_LIME  = Color.fromRGB( 50, 255, 120);
    private static final Color C_WITHER_CYAN  = Color.fromRGB(  0, 240, 220);
    private static final Color C_VOID         = Color.fromRGB( 10,   5,  20);
    private static final Color C_DARK_GRAY    = Color.fromRGB( 35,  35,  40);
    private static final Color C_BONE_WHITE   = Color.fromRGB(210, 225, 210);
    private static final Color C_SKULL_GREEN  = Color.fromRGB( 20, 180, 100);
    private static final Color C_DEEP_TEAL    = Color.fromRGB(  0, 130, 110);
    private static final Color[] AURA_COLORS = {
            C_WITHER_TEAL, C_WITHER_LIME, C_WITHER_CYAN, C_SKULL_GREEN, C_DEEP_TEAL
    };
    private static final Color[] BOLT_COLORS = {
            C_WITHER_TEAL, C_WITHER_CYAN, C_BONE_WHITE, C_WITHER_LIME
    };
    private final Map<UUID, BukkitRunnable> activeMarks = new HashMap<>();
    private boolean rezTriggered = false;
    public WitherPower(Player owner) { super(owner); }
    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DealDamageExecute) {
            killHeal((DealDamageExecute) ex);
            return;
        }
        if (ex instanceof DamagedExecute) {
            preventWither((DamagedExecute) ex);
            checkAutoRez(((DamagedExecute) ex).getPlayer());
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(wt_bolt, p, this)) return;
                    witherBolt(p, p.isSneaking() ? 3 : 1);
                    addCdFixed(wt_bolt, p, p.isSneaking()
                            ? cooldowns.get(wt_bolt) * 2.2
                            : cooldowns.get(wt_bolt));
                    return;
            case 1: if (onCd(wt_barrage, p, this)) return; skullBarrage(p);    addCd(wt_barrage, p); return;
            case 2: if (onCd(wt_mark, p, this)) return; deathMark(p);       addCd(wt_mark, p);    return;
            case 3: if (onCd(wt_shatter, p, this)) return; soulShatter(p);     addCd(wt_shatter, p); return;
            case 4: if (onCd(wt_storm, p, this)) return; witherStorm(p);     addCd(wt_storm, p);   return;
            case 5:
                if (p.getHealth() > 8 && !CooldownApi.isOnCooldown(wt_rez, p)) {
                    p.sendMessage(ChatColor.DARK_GRAY + "Dark Resurrection: HP must be ≤ 4 to use manually!");
                    return;
                }
                if (onCd(wt_rez, p, this)) return;
                darkResurrection(p);
                addCd(wt_rez, p);
        }
    }

    private void witherBolt(Player p, int count) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.4f);

        for (int volley = 0; volley < count; volley++) {
            final int v = volley;
            new BukkitRunnable() {
                @Override public void run() { fireBolt(p); }
            }.runTaskLater(magicPlugin, v * 4L);
        }
    }

    private void fireBolt(Player p) {
        final Location start = p.getEyeLocation().clone();
        final Vector dir = p.getEyeLocation().getDirection().clone().normalize().multiply(0.8);
        final Set<UUID> hit = new HashSet<>();
        final Random r = new Random();

        new BukkitRunnable() {
            Location cur = start.clone();
            int t = 0;

            @Override public void run() {
                if (t > 38) { cancel(); return; }

                cur.add(dir);

                if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                    boltPop(cur);
                    cancel();
                    return;
                }
                particleApi.spawnParticles(cur, Particle.SOUL_FIRE_FLAME, 3, 0.1, 0.1, 0.1, 0.02);
                particleApi.spawnColoredParticles(cur, BOLT_COLORS[t % BOLT_COLORS.length], 1.3f, 3, 0.08, 0.08, 0.08);
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(i * 120 + t * 28);
                    Location bone = cur.clone().add(Math.cos(a)*0.28, Math.sin(a*0.5)*0.14, Math.sin(a)*0.28);
                    particleApi.spawnColoredParticles(bone, C_BONE_WHITE, 0.9f, 1, 0.03, 0.03, 0.03);
                }
                if (t % 5 == 0)
                    particleApi.spawnParticles(cur, Particle.ASH, 2, 0.05, 0.05, 0.05, 0.02);
                for (Entity e : cur.getWorld().getNearbyEntities(cur, 0.8, 0.8, 0.8)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || hit.contains(e.getUniqueId())) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(14, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1, false, true));
                    particleApi.spawnParticles(e.getLocation().clone().add(0,1,0), Particle.SOUL_FIRE_FLAME, 10, 0.3, 0.3, 0.3, 0.04);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_WITHER_TEAL, 1.3f, 10, 0.3, 0.3, 0.3);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_WITHER_HURT, 0.4f, 1.7f);
                }

                if (t % 10 == 0)
                    p.getWorld().playSound(cur, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.1f, 1.4f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void boltPop(Location loc) {
        particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 16, 0.3, 0.3, 0.3, 0.04);
        particleApi.spawnColoredParticles(loc, C_WITHER_TEAL,  1.3f, 12, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_BONE_WHITE,   1.0f, 8,  0.35, 0.35, 0.35);
        particleApi.spawnParticles(loc, Particle.ASH, 5, 0.2, 0.2, 0.2, 0.04);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, 0.3f, 1.9f);
    }
    private void skullBarrage(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.2f);

        Random r = new Random();
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            new BukkitRunnable() {
                @Override public void run() {
                    fireSkull(p, idx, r);
                }
            }.runTaskLater(magicPlugin, idx * 2L);
        }
    }

    private void fireSkull(Player p, int idx, Random r) {
        Location start = p.getEyeLocation().clone();
        int[] spreads = {0, -12, 12, -22, 22};
        double spreadAngle = spreads[idx];
        Vector dir = rotateY(p.getEyeLocation().getDirection().clone().normalize(), spreadAngle)
                .multiply(0.85);
        final Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            Location cur = start.clone();
            int t = 0;

            @Override public void run() {
                if (t > 36) { cancel(); return; }

                cur.add(dir);
                if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                    skullPop(cur, p);
                    cancel();
                    return;
                }
                particleApi.spawnParticles(cur, Particle.ASH,            2, 0.1, 0.1, 0.1, 0.04);
                particleApi.spawnParticles(cur, Particle.SOUL_FIRE_FLAME, 2, 0.1, 0.1, 0.1, 0.02);
                particleApi.spawnColoredParticles(cur, C_WITHER_LIME, 1.1f, 2, 0.08, 0.08, 0.08);
                if (t % 3 == 0) {
                    for (int j = 0; j < 2; j++) {
                        Location frag = cur.clone().add(
                                (r.nextDouble()-0.5)*0.4, (r.nextDouble()-0.5)*0.4, (r.nextDouble()-0.5)*0.4);
                        particleApi.spawnColoredParticles(frag, C_BONE_WHITE, 0.85f, 1, 0.04, 0.04, 0.04);
                    }
                }

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 0.7, 0.7, 0.7)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || hit.contains(e.getUniqueId())) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(10, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0, false, true));
                    skullPop(e.getLocation().clone().add(0,1,0), p);
                    cancel();
                    return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void skullPop(Location loc, Player p) {
        particleApi.spawnParticles(loc, Particle.ASH, 12, 0.4, 0.4, 0.4, 0.06);
        particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 8, 0.3, 0.3, 0.3, 0.04);
        particleApi.spawnColoredParticles(loc, C_WITHER_LIME, 1.2f, 10, 0.35, 0.35, 0.35);
        particleApi.spawnColoredParticles(loc, C_BONE_WHITE,  0.9f, 8,  0.4,  0.4,  0.4);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, 0.35f, 1.6f);
    }
    private void deathMark(Player p) {
        if (activeMarks.size() >= 2) {
            p.sendMessage(ChatColor.DARK_GRAY + "Already reached the max of 2 Death Marks!");
            return;
        }

        LivingEntity target = getNearestTarget(p, 6);
        if (target == null) {
            p.sendMessage(ChatColor.DARK_GRAY + "No target in sight 6 block!");
            return;
        }
        if (activeMarks.containsKey(target.getUniqueId())) {
            p.sendMessage(ChatColor.DARK_GRAY + "This target is already marked!");
            return;
        }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.9f, 0.6f);
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 65, 0, false, false));

        BukkitRunnable markRunnable = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!target.isValid() || target.isDead()) {
                    activeMarks.remove(target.getUniqueId());
                    cancel(); return;
                }
                if (t >= 60) {
                    detonateMarkAt(target.getLocation(), target, p);
                    activeMarks.remove(target.getUniqueId());
                    cancel(); return;
                }

                Location tLoc = target.getLocation().clone().add(0, 1, 0);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 45 + t * 12);
                    double pulse = 0.85 + Math.sin(t * 0.35) * 0.15;
                    Location ring = tLoc.clone().add(Math.cos(a)*pulse, Math.sin(a*0.4)*0.25, Math.sin(a)*pulse);
                    particleApi.spawnColoredParticles(ring, AURA_COLORS[i % AURA_COLORS.length], 1.1f, 1, 0.03, 0.03, 0.03);
                }
                if (t % 5 == 0)
                    particleApi.spawnParticles(tLoc, Particle.ASH, 3, 0.25, 0.25, 0.25, 0.03);
                if (t == 40 || t == 50 || t == 55 || t == 58)
                    target.getWorld().playSound(tLoc, Sound.ENTITY_WITHER_SHOOT, 0.3f, 2.0f);

                t++;
            }
        };
        activeMarks.put(target.getUniqueId(), markRunnable);
        markRunnable.runTaskTimer(magicPlugin, 0, 1);
        p.sendMessage(ChatColor.DARK_AQUA + "☠ Death Mark placed! Detonates in 3s...");
    }

    private void detonateMarkAt(Location loc, LivingEntity marked, Player p) {
        marked.damage(20, p);
        marked.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 2, false, true));
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (e.equals(p) || e.equals(marked) || !(e instanceof LivingEntity)) continue;
            ((LivingEntity) e).damage(10, p);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
        }
        particleApi.spawnParticles(loc.clone().add(0,1,0), Particle.SOUL_FIRE_FLAME, 35, 0.7, 0.7, 0.7, 0.06);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_WITHER_TEAL,  1.6f, 40, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_BONE_WHITE,   1.1f, 25, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc.clone().add(0,1,0), C_VOID,         0.5f, 20, 1.2, 1.2, 1.2);
        particleApi.spawnParticles(loc, Particle.ASH, 20, 0.8, 0.5, 0.8, 0.07);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH,  0.8f, 1.4f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.6f);
    }

    private void soulShatter(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        Random r = new Random();

        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.65f);
        p.getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT,    0.8f, 1.3f);
        new BukkitRunnable() {
            double radius = 0.2;
            int t = 0;
            @Override public void run() {
                if (radius > 5.2) { cancel(); return; }
                for (int i = 0; i < 22; i++) {
                    double a = Math.toRadians(i * (360.0/22) + t * 10);
                    Location lp = loc.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius);
                    Color c = (t+i)%3==0 ? C_WITHER_TEAL : (t+i)%3==1 ? C_WITHER_LIME : C_BONE_WHITE;
                    particleApi.spawnColoredParticles(lp, c, 1.2f, 2, 0.05, 0.05, 0.05);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(lp, Particle.SOUL, 1, 0.04, 0.04, 0.04, 0.03);
                }
                if (t % 2 == 0) {
                    for (int j = 0; j < 6; j++) {
                        double a = Math.toRadians(j * 60 + t * 20);
                        Location spike = loc.clone().add(Math.cos(a)*radius*0.7, 0, Math.sin(a)*radius*0.7);
                        particleApi.spawnColoredParticles(spike, C_VOID,       0.4f, 1, 0.04, 0.22, 0.04);
                        particleApi.spawnColoredParticles(spike, C_BONE_WHITE, 0.9f, 1, 0.04, 0.12, 0.04);
                    }
                }
                radius += 0.52;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : p.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(loc);
                    double dmg = Math.max(7, 16 - dist * 2.0);
                    ((LivingEntity) e).damage(dmg, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    40, 1, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, true));
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_VOID,        0.4f, 6, 0.3, 0.4, 0.3);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_WITHER_CYAN, 1.2f, 5, 0.25, 0.3, 0.25);
                }
            }
        }.runTaskLater(magicPlugin, 5L);
    }
    private void witherStorm(Player p) {
        Location center = getRaycastTarget(p, 20);
        Random r = new Random();

        p.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN,  0.7f, 0.7f);
        p.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.8f);
        p.sendMessage(ChatColor.DARK_AQUA + "☠ Wither Storm!");

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80) {
                    stormEnd(center, p);
                    cancel(); return;
                }

                for (int i = 0; i < 7; i++) {
                    double a = Math.toRadians(i * 36 + t * 24);
                    double yOff = (i % 5) * 0.35;
                    Location lp = center.clone().add(Math.cos(a)*0.22*(1+yOff*0.06), yOff, Math.sin(a)*0.22*(1+yOff*0.06));
                    particleApi.spawnColoredParticles(lp, C_BONE_WHITE, 1.3f, 2, 0.03, 0.03, 0.03);
                }
                for (int i = 0; i < 7; i++) {
                    double a = Math.toRadians(i * 22.5 + t * 15);
                    double yOff = (i % 8) * 0.24;
                    double rx = 0.5 + yOff * 0.1;
                    Location lp = center.clone().add(Math.cos(a)*rx, yOff, Math.sin(a)*rx);
                    particleApi.spawnColoredParticles(lp, (t+i)%2==0 ? C_WITHER_TEAL : C_WITHER_LIME, 1.1f, 2, 0.04, 0.04, 0.04);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(lp, Particle.SOUL_FIRE_FLAME, 1, 0.04, 0.04, 0.04, 0.02);
                }
                for (int i = 0; i < 11; i++) {
                    double a = Math.toRadians(i * 20 - t * 10);
                    double yOff = (i % 9) * 0.22;
                    double rx = 0.9 + yOff * 0.12;
                    Location lp = center.clone().add(Math.cos(a)*rx, yOff, Math.sin(a)*rx);
                    particleApi.spawnParticles(lp, Particle.ASH, 1, 0.05, 0.05, 0.05, 0.03);
                    particleApi.spawnColoredParticles(lp, C_DARK_GRAY, 0.8f, 1, 0.05, 0.05, 0.05);
                }
                if (t % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 30 + t * 8);
                        Location fp = center.clone().add(Math.cos(a)*1.0, 0.05, Math.sin(a)*1.0);
                        particleApi.spawnColoredParticles(fp, AURA_COLORS[i % AURA_COLORS.length], 0.9f, 1, 0.04, 0.01, 0.04);
                        particleApi.spawnParticles(fp, Particle.SOUL, 1, 0.04, 0.01, 0.04, 0.02);
                    }
                }
                if (t % 5 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 4, 4, 4)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(3, p);
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 25, 0, false, true));
                    }
                }
                if (t % 15 == 0 && t > 0) {
                    for (int g = 0; g < 6; g++) {
                        double ang = Math.toRadians(g * 60 + r.nextInt(30));
                        Vector gDir = new Vector(Math.cos(ang), r.nextDouble()*0.3 - 0.1, Math.sin(ang)).normalize().multiply(0.9);
                        fireSpike(center.clone().add(0, 1, 0), gDir, p);
                    }
                    center.getWorld().playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.5f);
                }

                if (t % 15 == 0)
                    center.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.35f, 0.9f);

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void fireSpike(Location start, Vector dir, Player p) {
        final Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            Location cur = start.clone();
            int t = 0;
            @Override public void run() {
                if (t > 14) { cancel(); return; }
                cur.add(dir);
                if (!cur.getBlock().isPassable()) { cancel(); return; }
                particleApi.spawnColoredParticles(cur, C_BONE_WHITE, 1.1f, 2, 0.05, 0.05, 0.05);
                particleApi.spawnParticles(cur, Particle.ASH, 1, 0.04, 0.04, 0.04, 0.03);
                for (Entity e : cur.getWorld().getNearbyEntities(cur, 0.6, 0.6, 0.6)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || hit.contains(e.getUniqueId())) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(8, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), C_BONE_WHITE, 1.2f, 8, 0.25, 0.25, 0.25);
                    cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void stormEnd(Location center, Player p) {
        particleApi.spawnColoredParticles(center.clone().add(0,1,0), C_BONE_WHITE,  1.8f, 40, 0.7, 0.7, 0.7);
        particleApi.spawnColoredParticles(center.clone().add(0,1,0), C_WITHER_TEAL, 1.5f, 50, 1.0, 1.0, 1.0);
        particleApi.spawnParticles(center, Particle.SOUL_FIRE_FLAME, 25, 0.8, 0.5, 0.8, 0.05);
        particleApi.spawnParticles(center, Particle.ASH, 20, 1.0, 0.6, 1.0, 0.06);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.6f, 1.2f);
    }

    private void darkResurrection(Player p) {
        rezTriggered = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN,  0.9f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f,  0.6f);
        p.sendMessage(ChatColor.DARK_AQUA + "☠ " + ChatColor.BOLD + "DARK RESURRECTION!");

        Location loc = p.getLocation().clone().add(0, 1, 0);
        Random r = new Random();
        new BukkitRunnable() {
            @Override public void run() {
                for (double y = 0; y <= 8; y += 0.5) {
                    double rx = y * 0.15;
                    for (int i = 0; i < 7; i++) {
                        double a = Math.toRadians(i * 36 + y * 20);
                        Location lp = loc.clone().add(Math.cos(a)*rx, y, Math.sin(a)*rx);
                        particleApi.spawnColoredParticles(lp, AURA_COLORS[i % AURA_COLORS.length], 1.4f, 3, 0.07, 0.04, 0.07);
                        particleApi.spawnParticles(lp, Particle.SOUL_FIRE_FLAME, 1, 0.06, 0.04, 0.06, 0.03);
                    }
                }
            }
        }.runTask(magicPlugin);
        p.setHealth(Math.min(getMaxHp(p), p.getHealth() + 8));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 1, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, false, false));
        new BukkitRunnable() {
            @Override public void run() {
                new BukkitRunnable() {
                    double rad = 0.3; int t = 0;
                    @Override public void run() {
                        if (rad > 6) { cancel(); return; }
                        for (int i = 0; i < 26; i++) {
                            double a = Math.toRadians(i * (360.0/26) + t * 9);
                            Location lp = loc.clone().add(Math.cos(a)*rad, 0.1, Math.sin(a)*rad);
                            particleApi.spawnColoredParticles(lp, AURA_COLORS[t%AURA_COLORS.length], 1.2f, 2, 0.04, 0.04, 0.04);
                        }
                        rad += 0.65; t++;
                    }
                }.runTaskTimer(magicPlugin, 0, 2);

                particleApi.spawnColoredParticles(loc, C_WITHER_TEAL,  2f,   60, 1.5, 1.5, 1.5);
                particleApi.spawnColoredParticles(loc, C_BONE_WHITE,   1.5f, 20, 2.0, 2.0, 2.0);
                particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 40, 1.5, 1.0, 1.5, 0.06);
                particleApi.spawnParticles(loc, Particle.ASH, 30, 1.8, 1.0, 1.8, 0.07);
                loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 1f, 0.6f);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(loc);
                    double dmg = Math.max(8, 25 - dist * 2.8);
                    ((LivingEntity) e).damage(dmg, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, true));
                    Vector kb = e.getLocation().subtract(p.getLocation()).toVector();
                    if (kb.lengthSquared() < 0.01) kb = new Vector(r.nextDouble()-0.5, 0.2, r.nextDouble()-0.5);
                    e.setVelocity(kb.normalize().multiply(1.8).setY(0.5));
                }

                new BukkitRunnable() {
                    @Override public void run() { rezTriggered = false; }
                }.runTaskLater(magicPlugin, 100L);
            }
        }.runTaskLater(magicPlugin, 2L);
    }
    private void checkAutoRez(Player p) {
        if (rezTriggered) return;
        if (p.getHealth() > 2) return;
        if (CooldownApi.isOnCooldown(wt_rez, p)) return;
        darkResurrection(p);
        addCd(wt_rez, p);
    }

    private void killHeal(DealDamageExecute ex) {
        Player p = ex.getPlayer();
        Entity target = ((org.bukkit.event.entity.EntityDamageByEntityEvent) ex.getRawEvent()).getEntity();
        if (!(target instanceof LivingEntity)) return;
        LivingEntity le = (LivingEntity) target;
        if (le.getHealth() - ((org.bukkit.event.entity.EntityDamageByEntityEvent) ex.getRawEvent()).getFinalDamage() > 0) return;
        if (CooldownApi.isOnCooldown(wt_kill, p)) return;

        new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) return;
                p.setHealth(Math.min(getMaxHp(p), p.getHealth() + 2));
                Location loc = p.getLocation().clone().add(0, 1, 0);
                particleApi.spawnColoredParticles(loc, C_WITHER_TEAL,  1.3f, 15, 0.4, 0.4, 0.4);
                particleApi.spawnColoredParticles(loc, C_WITHER_LIME,  1.1f, 10, 0.5, 0.5, 0.5);
                particleApi.spawnParticles(loc, Particle.SOUL_FIRE_FLAME, 8, 0.35, 0.35, 0.35, 0.04);
                p.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, 0.3f, 1.8f);
            }
        }.runTaskLater(magicPlugin, 1L);
        addCd(wt_kill, p);
    }
    private void preventWither(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.WITHER)
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
                p.setRemainingAir(p.getMaximumAir());

                Location base   = p.getLocation().clone();
                Location center = base.clone().add(0, 1.1, 0);
                if (isAuraEnabled(p)) {
                    for (int i = 0; i < 10; i++) {
                        double a = Math.toRadians(i * 22.5 + t * 7);
                        double x = Math.cos(a) * 1.15;
                        double z = Math.sin(a) * 1.15;
                        particleApi.spawnColoredParticles(
                            center.clone().add(x, Math.sin(a*0.4)*0.22, z),
                            AURA_COLORS[i % AURA_COLORS.length], 1.05f, 1, 0.03, 0.03, 0.03);
                        if (i % 2 == 0)
                            particleApi.spawnParticles(
                                center.clone().add(x, r.nextDouble()*0.25, z),
                                Particle.SOUL, 1, 0.04, 0.04, 0.04, 0.02);
                    }
                    for (int i = 0; i < 7; i++) {
                        double a = Math.toRadians(i * 36 - t * 10);
                        Location lp = center.clone().add(Math.cos(a)*0.72, 0.5+Math.sin(a*0.6)*0.18, Math.sin(a)*0.72);
                        particleApi.spawnColoredParticles(lp, i%2==0 ? C_BONE_WHITE : C_DEEP_TEAL, 0.9f, 1, 0.03, 0.03, 0.03);
                    }

                    if (t % 3 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double a = Math.toRadians(i * 45 + t * 5);
                            Location foot = base.clone().add(Math.cos(a)*0.95, 0.05, Math.sin(a)*0.95);
                            particleApi.spawnParticles(foot, Particle.SOUL_FIRE_FLAME, 1, 0.04, 0.01, 0.04, 0.01);
                            particleApi.spawnParticles(foot, Particle.ASH, 1, 0.05, 0.01, 0.05, 0.02);
                        }
                    }
                    if (t % 4 == 0)
                        particleApi.spawnParticles(
                            center.clone().add((r.nextDouble()-0.5)*1.4, r.nextDouble()*1.7-0.3, (r.nextDouble()-0.5)*1.4),
                            Particle.ASH, 2, 0.05, 0.05, 0.05, 0.02);
                }
                if (t % 30 == 0 && !CooldownApi.isOnCooldown(wt_aura, p)) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.8, 1.8, 1.8)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                        ((LivingEntity) e).damage(1, p);
                        addCd(wt_aura, p);
                    }
                }
                if (t % 100 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.15f, 0.7f);

                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 20);
        return task;
    }

    @Override
    public void remove() {
        for (BukkitRunnable run : activeMarks.values()) {
            try { run.cancel(); } catch (Exception ignored) {}
        }
        activeMarks.clear();
        rezTriggered = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&3Wither Bolt";
            case 1: return "&aSkull Barrage";
            case 2: return "&3Death Mark";
            case 3: return "&8Soul Shatter";
            case 4: return "&3Wither Storm";
            case 5: return "&8&lDark Resurrection";
            default: return "&7none";
        }
    }

}

