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
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class ThunderGodPower extends Power implements IdlePower, Removeable {
    private static final String tg_triple   = "thundergod.triple";
    private static final String tg_wrath    = "thundergod.wrath";
    private static final String tg_cage     = "thundergod.cage";
    private static final String tg_step     = "thundergod.step";
    private static final String tg_judgment = "thundergod.judgment";
    private static final String tg_aura     = "thundergod.aura";
    private static final Color C_GOLD_BRIGHT = Color.fromRGB(255, 230, 0);
    private static final Color C_GOLD_DEEP   = Color.fromRGB(220, 170, 0);
    private static final Color C_GOLD_WARM   = Color.fromRGB(255, 200, 50);
    private static final Color C_PURPLE_LT   = Color.fromRGB(200, 120, 255);
    private static final Color C_PURPLE_MID  = Color.fromRGB(160, 60,  240);
    private static final Color C_PURPLE_DEEP = Color.fromRGB(100, 20,  200);
    private static final Color C_WHITE_HOT   = Color.fromRGB(255, 255, 200);
    private static final Color C_CREEPER_LIME   = Color.fromRGB(77,  255, 100);
    private static final Color C_CREEPER_GREEN  = Color.fromRGB(30,  200, 60);
    private static final Color C_CREEPER_TEAL   = Color.fromRGB(20,  255, 180);
    private static final Color C_CREEPER_WHITE  = Color.fromRGB(210, 255, 220);

    private static final Color[] STRIKE_COLORS = {
            C_GOLD_BRIGHT, C_GOLD_WARM, C_PURPLE_LT, C_WHITE_HOT, C_GOLD_DEEP
    };
    private static final Color[] AURA_COLORS = {
            C_GOLD_BRIGHT, C_GOLD_WARM, C_PURPLE_MID, C_PURPLE_LT, C_WHITE_HOT
    };
    private static final Color[] CREEPER_COLORS = {
            C_CREEPER_LIME, C_CREEPER_GREEN, C_CREEPER_TEAL, C_CREEPER_WHITE
    };

    public ThunderGodPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DeathExecute) {
            deathThunder((DeathExecute) ex);
            return;
        }
        if (ex instanceof DamagedExecute) {
            preventLightningDamage((DamagedExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(tg_triple, p, this)) return; tripleStrike(p);   addCd(tg_triple, p);   return;
            case 1: if (onCd(tg_wrath, p, this)) return; heavensWrath(p);   addCd(tg_wrath, p);    return;
            case 2: if (onCd(tg_cage, p, this)) return; thunderCage(p);    addCd(tg_cage, p);     return;
            case 3: if (onCd(tg_step, p, this)) return; stormStep(p);      addCd(tg_step, p);     return;
            case 4: if (onCd(tg_judgment, p, this)) return; divineJudgment(p); addCd(tg_judgment, p);
        }
    }

    private void tripleStrike(Player p) {
        Location target = getRaycastGround(p, 24);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.6f);

        for (int i = 0; i < 3; i++) {
            final Location strikePos = target.clone().add(
                    (Math.random() - 0.5) * 0.8, 0, (Math.random() - 0.5) * 0.8);
            final int idx = i;
            new BukkitRunnable() {
                @Override public void run() {
                    strikeColumnVisual(strikePos);
                    new BukkitRunnable() {
                        @Override public void run() {
                            safeRealLightning(strikePos, p);
                            if (idx == 2) {
                                for (Entity e : strikePos.getWorld().getNearbyEntities(strikePos, 2.5, 2.5, 2.5)) {
                                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 4, false, false));
                                }
                            }
                        }
                    }.runTaskLater(magicPlugin, 1L);
                }
            }.runTaskLater(magicPlugin, idx * 3L);
        }
    }

    private void heavensWrath(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.7f);

        new BukkitRunnable() {
            int ct = 40;
            @Override public void run() {
                if (ct <= 0) { cancel(); releaseWrath(p); return; }
                double a = Math.toRadians(ct * 22);
                for (int i = 0; i < 6; i++) {
                    double ai = a + Math.toRadians(i * 60);
                    Location lp = p.getEyeLocation().clone()
                            .add(Math.cos(ai) * 0.9, Math.sin(ai * 0.4) * 0.3, Math.sin(ai) * 0.9);
                    particleApi.spawnColoredParticles(lp,
                            i % 2 == 0 ? C_GOLD_BRIGHT : C_PURPLE_LT, 1.3f, 2, 0.03, 0.03, 0.03);
                }
                if (ct == 20) p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.7f, 0.8f);
                ct--;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void releaseWrath(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.6f);
        Location center = getRaycastGround(p, 20);
        Random r = new Random();
        particleApi.spawnColoredParticles(center.clone().add(0,1,0), C_WHITE_HOT,   2f,  60, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(center.clone().add(0,1,0), C_GOLD_BRIGHT, 1.8f,80, 2.0, 2.0, 2.0);
        particleApi.spawnParticles(center, Particle.ELECTRIC_SPARK, 30, 2, 0.5, 2, 1.5);

        for (int i = 0; i < 7; i++) {
            final Location strikePos = center.clone().add(
                    (r.nextDouble() - 0.5) * 9, 0, (r.nextDouble() - 0.5) * 9);
            new BukkitRunnable() {
                @Override public void run() {
                    strikeColumnVisual(strikePos);
                    new BukkitRunnable() {
                        @Override public void run() { safeRealLightning(strikePos, p); }
                    }.runTaskLater(magicPlugin, 1L);
                }
            }.runTaskLater(magicPlugin, r.nextInt(20));
        }
    }

    private void thunderCage(Player p) {
        LivingEntity target = getNearestTarget(p, 8);
        if (target == null) {
            p.sendMessage(ChatColor.YELLOW + "There are no targets within 8 blocks!");
            return;
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);

        final Location cageCenter = target.getLocation().clone();
        final double cageRadius = 2.5;
        Random r = new Random();
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0, false, false));

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80 || !target.isValid() || target.isDead()) { cancel(); return; }
                for (double y : new double[]{0.3, 1.2, 2.1}) {
                    for (int i = 0; i < 24; i++) {
                        double a = Math.toRadians(i * 15 + t * 6);
                        Location lp = cageCenter.clone().add(Math.cos(a)*cageRadius, y, Math.sin(a)*cageRadius);
                        Color c = (t + i) % 3 == 0 ? C_WHITE_HOT :
                                  (t + i) % 3 == 1 ? C_GOLD_BRIGHT : C_PURPLE_LT;
                        particleApi.spawnColoredParticles(lp, c, 1.1f, 1, 0.03, 0.03, 0.03);
                    }
                }
                for (int col = 0; col < 4; col++) {
                    double a = Math.toRadians(col * 90 + t * 4);
                    for (double y = 0; y <= 2.5; y += 0.35) {
                        Location lp = cageCenter.clone().add(Math.cos(a)*cageRadius, y, Math.sin(a)*cageRadius);
                        particleApi.spawnColoredParticles(lp,
                                col % 2 == 0 ? C_GOLD_DEEP : C_PURPLE_MID, 1f, 1, 0.03, 0.03, 0.03);
                    }
                }
                if (t % 5 == 0) {
                    double a1 = Math.toRadians(r.nextInt(360));
                    double a2 = a1 + Math.toRadians(90 + r.nextInt(90));
                    Location p1 = cageCenter.clone().add(Math.cos(a1)*cageRadius, 1.2, Math.sin(a1)*cageRadius);
                    Location p2 = cageCenter.clone().add(Math.cos(a2)*cageRadius, 1.2, Math.sin(a2)*cageRadius);
                    drawBolt(p1, p2, C_GOLD_BRIGHT);
                    p.getWorld().playSound(cageCenter, Sound.ENTITY_CREEPER_HURT, 0.2f, 2f);
                }
                if (t % 15 == 0) {
                    for (Entity e : cageCenter.getWorld().getNearbyEntities(cageCenter, cageRadius+0.5, 3, cageRadius+0.5)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(5, p);
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 3, false, false));
                    }
                }
                if (!target.isDead() && target.getLocation().distance(cageCenter) > cageRadius + 0.5) {
                    safeRealLightning(target.getLocation(), p);
                    target.teleport(cageCenter.clone().add(0, 0.5, 0));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void stormStep(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CREEPER_HURT, 1f, 1.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);

        Location from = p.getLocation().clone();
        Vector dir    = p.getEyeLocation().getDirection().clone().setY(0.1).normalize();
        Location to   = from.clone();

        for (int i = 0; i < 16; i++) {
            to.add(dir.clone().multiply(0.5));
            if (!to.getBlock().isPassable()) { to.subtract(dir.clone().multiply(0.5)); break; }
        }

        drawBoltPath(from.clone().add(0,1,0), to.clone().add(0,1,0));
        p.teleport(to.clone().add(0, 0.5, 0));

        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_WHITE_HOT,   1.8f, 30, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_GOLD_BRIGHT, 1.4f, 40, 0.7, 0.7, 0.7);
        particleApi.spawnParticles(to.clone().add(0,1,0), Particle.ELECTRIC_SPARK, 30, 0.6, 0.6, 0.6, 1.2);
        for (int i = 1; i <= 5; i++) {
            final double frac = (double) i / 6;
            Location trapLoc = from.clone().add(to.toVector().subtract(from.toVector()).multiply(frac));
            spawnLightningTrap(trapLoc, p);
        }
    }

    private void spawnLightningTrap(Location loc, Player p) {
        particleApi.spawnColoredParticles(loc.clone().add(0,0.1,0), C_GOLD_BRIGHT, 1.3f, 8, 0.3, 0.05, 0.3);

        new BukkitRunnable() {
            int t = 0;
            boolean triggered = false;
            @Override public void run() {
                if (t >= 60 || triggered) { cancel(); return; }
                if (t % 5 == 0) {
                    particleApi.spawnColoredParticles(loc.clone().add(0,0.1,0),
                            t % 10 == 0 ? C_GOLD_BRIGHT : C_PURPLE_LT, 1f, 3, 0.25, 0.05, 0.25);
                    particleApi.spawnParticles(loc, Particle.ELECTRIC_SPARK, 2, 0.2, 0.05, 0.2, 0.4);
                }
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.2, 1.5, 1.2)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    triggered = true;
                    safeRealLightning(loc, p);
                    cancel();
                    return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void divineJudgment(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.6f);
        p.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.BOLD + "DIVINE JUDGMENT charging...");

        new BukkitRunnable() {
            int ct = 60;
            @Override public void run() {
                if (ct <= 0) { cancel(); releaseJudgment(p); return; }
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 + (60 - ct) * 8);
                    double radius = 1.5 + (60 - ct) * 0.04;
                    Location lp = p.getLocation().clone().add(
                            Math.cos(a) * radius, 0.5 + (60 - ct) * 0.03, Math.sin(a) * radius);
                    particleApi.spawnColoredParticles(lp,
                            i % 2 == 0 ? C_GOLD_BRIGHT : C_PURPLE_MID, 1.4f, 3, 0.05, 0.05, 0.05);
                }
                if (ct % 10 == 0) {
                    Location skyPos = p.getLocation().clone().add(
                            (Math.random()-0.5)*3, 0, (Math.random()-0.5)*3);
                    for (double y = 0; y <= 12; y += 0.8) {
                        Location lp = skyPos.clone().add((Math.random()-0.5)*0.5, y, (Math.random()-0.5)*0.5);
                        particleApi.spawnColoredParticles(lp,
                                y < 6 ? C_PURPLE_LT : C_GOLD_BRIGHT, 1.2f, 2, 0.08, 0.04, 0.08);
                    }
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CREEPER_HURT, 0.3f, 1.8f);
                }
                if (ct == 30) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.7f);
                    p.sendMessage(ChatColor.YELLOW + "JUDGMENT IMMINENT...");
                }
                ct--;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void releaseJudgment(Player p) {
        Location target = getRaycastGround(p, 30);
        Random r = new Random();
        new BukkitRunnable() {
            @Override public void run() {
                for (double y = 0; y <= 28; y += 0.5) {
                    Location lp = target.clone().add((Math.random()-0.5)*0.6, y, (Math.random()-0.5)*0.6);
                    particleApi.spawnColoredParticles(lp, C_WHITE_HOT,   1.8f, 4, 0.1, 0.05, 0.1);
                    particleApi.spawnColoredParticles(lp, C_GOLD_BRIGHT, 1.5f, 3, 0.2, 0.05, 0.2);
                    particleApi.spawnColoredParticles(lp, C_PURPLE_LT,   1.3f, 2, 0.3, 0.05, 0.3);
                    particleApi.spawnParticles(lp, Particle.ELECTRIC_SPARK, 2, 0.15, 0.05, 0.15, 0.8);
                }
            }
        }.runTask(magicPlugin);

        p.getWorld().playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
        p.getWorld().playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.7f);
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ DIVINE JUDGMENT!");

        new BukkitRunnable() {
            @Override public void run() {
                safeRealLightning(target, p);
                particleApi.spawnColoredParticles(target.clone().add(0,1,0), C_WHITE_HOT,   2.5f, 100, 1.0, 1.0, 1.0);
                particleApi.spawnColoredParticles(target.clone().add(0,1,0), C_GOLD_BRIGHT, 2f,   150, 2.0, 2.0, 2.0);
                particleApi.spawnParticles(target, Particle.ELECTRIC_SPARK, 40, 3, 1, 3, 2f);
                for (int i = 0; i < 10; i++) {
                    final Location sp = target.clone().add(
                            (r.nextDouble() - 0.5) * 10, 0, (r.nextDouble() - 0.5) * 10);
                    new BukkitRunnable() {
                        @Override public void run() { safeRealLightning(sp, p); }
                    }.runTaskLater(magicPlugin, r.nextInt(25));
                }
                new BukkitRunnable() {
                    double rad = 0.5; int t = 0;
                    @Override public void run() {
                        if (rad > 7) { cancel(); return; }
                        for (int i = 0; i < 28; i++) {
                            double a = Math.toRadians(i * (360.0/28) + t * 10);
                            Location lp = target.clone().add(Math.cos(a)*rad, 0.1, Math.sin(a)*rad);
                            particleApi.spawnColoredParticles(lp,
                                    t%3==0 ? C_WHITE_HOT : t%3==1 ? C_GOLD_BRIGHT : C_PURPLE_LT,
                                    1.3f, 2, 0.04, 0.04, 0.04);
                        }
                        rad += 0.65; t++;
                    }
                }.runTaskTimer(magicPlugin, 0, 2);
                for (Entity e : target.getWorld().getNearbyEntities(target, 6, 6, 6)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    e.setVelocity(e.getLocation().subtract(target).toVector().normalize().multiply(2.0).setY(1.2));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,    60, 5, false, false));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,   40, 0, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 5, false, false));
                }
            }
        }.runTaskLater(magicPlugin, 3L);
    }

    private void deathThunder(DeathExecute ex) {
        Player p = ex.getPlayer();
        Location loc = p.getLocation().clone();

        boolean hasTarget = false;
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            hasTarget = true;
            break;
        }
        if (!hasTarget) return;

        new BukkitRunnable() {
            @Override public void run() {
                Random r = new Random();
                p.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.6f);
                for (int i = 0; i < 10; i++) {
                    final Location sp = loc.clone().add(
                            (r.nextDouble() - 0.5) * 10, 0, (r.nextDouble() - 0.5) * 10);
                    new BukkitRunnable() {
                        @Override public void run() { safeRealLightning(sp, p); }
                    }.runTaskLater(magicPlugin, r.nextInt(25));
                }
            }
        }.runTaskLater(magicPlugin, 1L);
    }

    private void preventLightningDamage(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING)
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

                Location base = p.getLocation().clone();
                Location center = base.clone().add(0, 1.1, 0);

                if (isAuraEnabled(p)) {
                    for (int i = 0; i < 16; i++) {
                        double a = Math.toRadians(i * 22.5 + t * 5);
                        double x = Math.cos(a) * 1.15;
                        double z = Math.sin(a) * 1.15;
                        particleApi.spawnParticles(
                            center.clone().add(x, r.nextDouble() * 0.1 - 0.05, z),
                            Particle.SCULK_SOUL, 1, 0.02, 0.02, 0.02, 0);
                        particleApi.spawnColoredParticles(
                            center.clone().add(x, r.nextDouble() * 0.1, z),
                            CREEPER_COLORS[i % CREEPER_COLORS.length], 1.0f, 1, 0.03, 0.03, 0.03);
                    }

                    for (int i = 0; i < 10; i++) {
                        double a = Math.toRadians(i * 36 - t * 8);
                        double x = Math.cos(a) * 0.75;
                        double z = Math.sin(a) * 0.75;
                        particleApi.spawnColoredParticles(
                            center.clone().add(x, 0.06 + Math.sin(a * 0.7) * 0.04, z),
                            i % 2 == 0 ? C_CREEPER_LIME : C_CREEPER_TEAL, 0.9f, 1, 0.02, 0.02, 0.02);
                    }

                    if (t % 4 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double a = Math.toRadians(i * 45 + t * 3);
                            Location foot = base.clone().add(Math.cos(a)*1.0, 0.05, Math.sin(a)*1.0);
                            particleApi.spawnParticles(foot, Particle.ELECTRIC_SPARK, 1, 0.04, 0.01, 0.04, 0.3);
                            particleApi.spawnColoredParticles(foot, C_CREEPER_LIME, 0.8f, 1, 0.05, 0.01, 0.05);
                        }
                    }
                    if (t % 3 == 0) {
                        particleApi.spawnParticles(
                            center.clone().add((r.nextDouble()-0.5)*1.8, r.nextDouble()*1.5, (r.nextDouble()-0.5)*1.8),
                            Particle.ELECTRIC_SPARK, 1, 0.05, 0.05, 0.05, 0.4);
                    }
                    for (int i = 0; i < 6; i++) {
                        double a1 = Math.toRadians(i * 60 + t * 9);
                        Location lp1 = center.clone().add(Math.cos(a1)*1.05, Math.sin(a1*0.5)*0.15, Math.sin(a1)*1.05);
                        particleApi.spawnColoredParticles(lp1, AURA_COLORS[i % AURA_COLORS.length], 1.0f, 1, 0.03, 0.03, 0.03);
                    }
                }
                if (t % 20 == 0 && !CooldownApi.isOnCooldown(tg_aura, p)) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 1.8, 1.8, 1.8)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        e.getWorld().strikeLightningEffect(e.getLocation());
                        ((LivingEntity) e).damage(2, p);
                        drawBolt(p.getLocation().clone().add(0,1,0), e.getLocation().clone().add(0,1,0), C_CREEPER_LIME);
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CREEPER_HURT, 0.4f, 2f);
                        addCd(tg_aura, p);
                    }
                }

                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 20);
        return task;
    }
    @Override
    public void remove() {}

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&e三连击";
            case 1: return "&6天罚";
            case 2: return "&5雷电牢笼";
            case 3: return "&e风暴步";
            case 4: return "&6&l神圣裁决";
            default: return "&7none";
        }
    }

    private void safeRealLightning(Location loc, Player owner) {
        loc.getWorld().strikeLightning(loc);
    }
    private void strikeColumnVisual(Location ground) {
        Random r = new Random();
        for (double y = 0; y <= 14; y += 0.7) {
            Location lp = ground.clone().add((r.nextDouble()-0.5)*0.4, y, (r.nextDouble()-0.5)*0.4);
            Color c = STRIKE_COLORS[r.nextInt(STRIKE_COLORS.length)];
            particleApi.spawnColoredParticles(lp, c, 1.2f, 2, 0.06, 0.03, 0.06);
            if (y < 1.5)
                particleApi.spawnParticles(lp, Particle.ELECTRIC_SPARK, 2, 0.08, 0.04, 0.08, 0.7);
        }
    }

    private void drawBolt(Location from, Location to, Color color) {
        int steps = Math.max(4, (int) from.distance(to) * 3);
        if (steps == 0) return;
        Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
        Location cur = from.clone();
        Random r = new Random();
        for (int i = 0; i < steps; i++) {
            Vector jitter = new Vector((r.nextDouble()-0.5)*0.35, (r.nextDouble()-0.5)*0.35, (r.nextDouble()-0.5)*0.35);
            Location lp = cur.clone().add(step).add(jitter);
            particleApi.spawnColoredParticles(lp, color, 1.1f, 2, 0.04, 0.04, 0.04);
            if (i % 3 == 0)
                particleApi.spawnParticles(lp, Particle.ELECTRIC_SPARK, 1, 0.04, 0.04, 0.04, 0.3);
            cur.add(step);
        }
    }

    private void drawBoltPath(Location from, Location to) {
        new BukkitRunnable() {
            @Override public void run() {
                drawBolt(from, to, C_GOLD_BRIGHT);
                Vector offset = new Vector(0.15, 0, 0.15);
                drawBolt(from.clone().add(offset), to.clone().add(offset), C_PURPLE_LT);
            }
        }.runTask(magicPlugin);
    }

    private Location getRaycastGround(Player p, int maxDist) {
        Location cur = p.getEyeLocation().clone();
        Vector dir   = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < maxDist * 2; i++) {
            cur.add(dir.clone().multiply(0.5));
            if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                cur.subtract(dir.clone().multiply(0.5));
                break;
            }
        }
        return cur;
    }

}

