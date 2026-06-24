package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.cooldowns.CooldownApi;
import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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

public class WitcherPower extends Power implements IdlePower, Removeable {

    private static final String witcher_igni  = "witcher.igni";
    private static final String witcher_aard  = "witcher.aard";
    private static final String witcher_quen  = "witcher.quen";
    private static final String witcher_axii  = "witcher.aksji";
    private static final String witcher_yrden = "witcher.yrden";
    private static final Color IGNI_RED    = Color.fromRGB(255, 70,  0);
    private static final Color IGNI_ORANGE = Color.fromRGB(255, 160, 0);
    private static final Color AARD_BLUE   = Color.fromRGB(160, 210, 255);
    private static final Color AARD_WHITE  = Color.fromRGB(220, 240, 255);
    private static final Color QUEN_GOLD   = Color.fromRGB(255, 200, 50);
    private static final Color AXII_GREEN  = Color.fromRGB(60,  255, 110);
    private static final Color AXII_LIME   = Color.fromRGB(150, 255, 80);
    private static final Color YRDEN_PURPLE= Color.fromRGB(180, 60,  255);
    private static final Color YRDEN_DARK  = Color.fromRGB(100, 0,   200);
    private boolean shield        = false;
    private BukkitRunnable quenRunnable = null;
    private int quenHits          = 0;
    private static final int QUEN_MAX = 3;
    private boolean igniCharged   = false;
    private final Map<UUID, Long> axiiMarked = new HashMap<>();
    private final List<Location> yrdenTraps = new ArrayList<>();
    public WitcherPower(Player owner) { super(owner); }
    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) {
            if (shield) quenAbsorb((DamagedByExecute) ex);
            else        witcherParry((DamagedByExecute) ex);
            return;
        }
        if (ex instanceof DamagedExecute) {
            if (shield) quenAbsorbEnv((DamagedExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof SneakExecute) { onSneak((SneakExecute) ex); return; }
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 0 || CooldownApi.isOnCooldown(witcher_igni, p)) return;
        igniCharged = true;
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1f, 1.8f);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.2, 0),
                IGNI_RED, 1.5f, 20, 0.3, 0.3, 0.3);
        p.sendMessage(ChatColor.RED + "✦ Igni charged!");
        new BukkitRunnable() {
            @Override public void run() {
                if (igniCharged) { igniCharged = false; p.sendMessage(ChatColor.GRAY + "Igni charge faded."); }
            }
        }.runTaskLater(magicPlugin, 160L);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(witcher_igni, p, this)) return; igni(p);  addCd(witcher_igni, p, igniCharged ? 1.6 : 1.0); igniCharged=false; return;
            case 1: if (onCd(witcher_aard, p, this)) return; aard(p);  addCd(witcher_aard, p);  return;
            case 2: if (shield) return; if (onCd(witcher_quen, p, this)) return; quen(p); return;
            case 3: if (onCd(witcher_axii, p, this)) return; axii(p);  addCd(witcher_axii, p);  return;
            case 4: if (onCd(witcher_yrden, p, this)) return; yrden(p); addCd(witcher_yrden, p);
        }
    }

    private void igni(Player p) {
        Location eyeTarget = getEyeTarget(p, 12);
        Location yrdenHit  = getNearestYrdenTrap(eyeTarget, 5);

        if (yrdenHit != null) {
            infernoTrap(p, yrdenHit);
            yrdenTraps.remove(yrdenHit);
            return;
        }

        if (igniCharged) { igniBlast(p); return; }
        igniCone(p);
    }

    private void igniCone(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT,    0.7f, 0.6f);

        Set<Entity> hit = new HashSet<>();
        Vector fwd = p.getEyeLocation().getDirection().clone().normalize();
        Random r = new Random();

        for (int i = 0; i < 12; i++) {
            final int idx = i;
            new BukkitRunnable() {
                @Override public void run() {
                    int yaw   = -55 + idx * 10 + r.nextInt(8) - 4;
                    int pitch = r.nextInt(16) - 8;
                    shootIgniRay(p, rotateVec(fwd.clone(), yaw, pitch), 18, hit, false);
                }
            }.runTaskLater(magicPlugin, idx / 3L);
        }
    }

    private void shootIgniRay(Player p, Vector dir, double baseDmg, Set<Entity> hit, boolean fromCombo) {
        ArmorStand as = spawnAs(p.getEyeLocation().clone());
        Random r = new Random();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (as.isDead() || t > 16) { safeRemove(as); cancel(); return; }
                as.teleport(as.getLocation().add(dir.clone().multiply(1.3)));
                Location loc = as.getLocation();

                particleApi.spawnParticles(loc, Particle.FLAME, 5, 0.1, 0.1, 0.1, 0.04);
                particleApi.spawnColoredParticles(loc, r.nextBoolean() ? IGNI_RED : IGNI_ORANGE, 1.1f, 3, 0.07, 0.07, 0.07);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    double dmg = isAxiiMarked(e) ? baseDmg * 1.6 : baseDmg;
                    ((LivingEntity) e).damage(dmg, p);
                    e.setFireTicks(100);
                    if (isAxiiMarked(e)) axiiMarkConsumeVisual(e.getLocation(), IGNI_RED);
                    particleApi.spawnParticles(loc, Particle.FLAME, 40, 0.4, 0.4, 0.4, 0.3);
                    loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 0.6f, 1.2f);
                    safeRemove(as); cancel(); return;
                }
                if (!loc.getBlock().isPassable()) { safeRemove(as); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void igniBlast(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 2f);

        ArmorStand orb = spawnAs(p.getEyeLocation().clone());
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (orb.isDead() || t > 60) { safeRemove(orb); cancel(); return; }
                orb.teleport(orb.getLocation().add(dir.clone().multiply(1.1)));
                Location loc = orb.getLocation();

                for (int ring = 0; ring < 4; ring++) {
                    double a = Math.toRadians(t * 30 + ring * 90);
                    Vector rv = new Vector(Math.cos(a), Math.sin(a * 0.5), Math.sin(a)).multiply(0.6);
                    particleApi.spawnParticles(loc.clone().add(rv), Particle.FLAME, 6, 0.07, 0.07, 0.07, 0.05);
                    particleApi.spawnColoredParticles(loc.clone().add(rv),
                            r.nextBoolean() ? IGNI_RED : IGNI_ORANGE, 1.3f, 3, 0.05, 0.05, 0.05);
                }

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.3, 1.3, 1.3)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) { igniBlastExplode(loc, p); safeRemove(orb); cancel(); return; }
                }
                if (!loc.getBlock().isPassable()) { igniBlastExplode(loc, p); safeRemove(orb); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void igniBlastExplode(Location loc, Player p) {
        particleApi.spawnParticles(loc, Particle.FLAME, 300, 1.5, 1.5, 1.5, 0.5);
        particleApi.spawnColoredParticles(loc, IGNI_ORANGE, 2f, 20, 1.2, 1.2, 1.2);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 6, 6, 6)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist  = e.getLocation().distance(loc);
            double dmg   = isAxiiMarked(e) ? (40 - dist * 4) * 1.6 : (40 - dist * 4);
            ((LivingEntity) e).damage(Math.max(10, dmg), p);
            e.setFireTicks(140);
            e.setVelocity(e.getLocation().subtract(loc).toVector().normalize().multiply(1.5).setY(0.5));
            if (isAxiiMarked(e)) axiiMarkConsumeVisual(e.getLocation(), IGNI_RED);
        }
    }
    private void infernoTrap(Player p, Location trapCenter) {
        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✦ COMBO: INFERNO TRAP!");
        p.getWorld().playSound(trapCenter, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.7f);
        p.getWorld().playSound(trapCenter, Sound.ENTITY_GENERIC_EXPLODE,         1f, 0.6f);
        p.getWorld().playSound(trapCenter, Sound.BLOCK_BEACON_DEACTIVATE,        0.8f, 2f);
        particleApi.spawnParticles(trapCenter, Particle.FLAME, 600, 3, 3, 3, 0.8);
        particleApi.spawnColoredParticles(trapCenter, IGNI_ORANGE, 2.5f, 150, 2.5, 2.5, 2.5);
        new BukkitRunnable() {
            double radius = 0.5; int t = 0;
            @Override public void run() {
                if (radius > 9) { cancel(); return; }
                for (int i = 0; i < 28; i++) {
                    double a = Math.toRadians(i * (360.0/28) + t * 8);
                    Location lp = trapCenter.clone().add(Math.cos(a)*radius, 0.1, Math.sin(a)*radius);
                    particleApi.spawnColoredParticles(lp, t%2==0 ? IGNI_RED : IGNI_ORANGE, 1.5f, 3, 0.05, 0.05, 0.05);
                    particleApi.spawnParticles(lp, Particle.FLAME, 2, 0.05, 0.1, 0.05, 0.05);
                }
                radius += 0.7; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : trapCenter.getWorld().getNearbyEntities(trapCenter, 8, 8, 8)) {
                    if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(trapCenter);
                    ((LivingEntity) e).damage(Math.max(15, 50 - dist * 3.5), p);
                    e.setFireTicks(200);
                    e.setVelocity(e.getLocation().subtract(trapCenter).toVector().normalize().multiply(2.0).setY(0.6));
                }
            }
        }.runTaskLater(magicPlugin, 5L);
    }

    private void aard(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_HURT,   1f, 2f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.6f);

        Set<Entity> hit = new HashSet<>();
        Vector fwd = p.getEyeLocation().getDirection().clone().normalize();

        new BukkitRunnable() {
            int seg = 0;
            @Override public void run() {
                if (seg > 10) { cancel(); return; }
                Location wave = p.getEyeLocation().clone().add(fwd.clone().multiply(seg * 1.0));
                for (int i = 0; i < 18; i++) {
                    double angle = Math.toRadians(i * 20);
                    double wx = Math.cos(angle) * (seg * 0.14 + 0.5);
                    double wz = Math.sin(angle) * (seg * 0.14 + 0.5);
                    Location lp = wave.clone().add(wx, 0, wz);
                    particleApi.spawnColoredParticles(lp, seg%2==0 ? AARD_BLUE : AARD_WHITE, 1.1f, 2, 0.05, 0.1, 0.05);
                    particleApi.spawnParticles(lp, Particle.CLOUD, 2, 0.08, 0.08, 0.08, 0.01);
                }

                for (Entity e : wave.getWorld().getNearbyEntities(wave, 1.8 + seg*0.1, 1.8, 1.8 + seg*0.1)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);

                    boolean inYrden = isInYrdenTrap(e.getLocation());
                    boolean marked  = isAxiiMarked(e);

                    double dmg;
                    if (inYrden && marked) {
                        dmg = 14 * 3.0;
                        shockwaveTrapCombo(e, p, fwd, "DOUBLE COMBO: AARD × YRDEN × AXII", 3.5);
                    } else if (inYrden) {
                        dmg = 14 * 2.5;
                        shockwaveTrapCombo(e, p, fwd, "COMBO: SHOCKWAVE TRAP", 2.2);
                    } else if (marked) {
                        dmg = 35;
                        markBlastCombo(e, p, "COMBO: MARK BLAST");
                    } else {
                        dmg = 14;
                        e.setVelocity(fwd.clone().multiply(1.8).add(new Vector(0, 0.35, 0)));
                    }

                    ((LivingEntity) e).damage(dmg, p);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,  80, 0, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  60, 2, false, false));
                    if (marked) axiiMarkConsumeVisual(e.getLocation(), AARD_BLUE);
                    particleApi.spawnParticles(e.getLocation().clone().add(0,1,0), Particle.CLOUD, 30, 0.4, 0.4, 0.4, 0.1);
                }
                seg++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void shockwaveTrapCombo(Entity e, Player p, Vector fwd, String msg, double launchPower) {
        p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "✦ " + msg + "!");
        e.setVelocity(fwd.clone().multiply(0.8).add(new Vector(0, launchPower, 0)));
        particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), AARD_WHITE, 2f, 50, 0.6, 0.6, 0.6);
        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.8f);
    }

    private void markBlastCombo(Entity e, Player p, String msg) {
        p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✦ " + msg + "!");
        e.setVelocity(new Vector(0, 2.5, 0));
        particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0), AXII_GREEN, 1.8f, 40, 0.5, 0.5, 0.5);
        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1.5f);
    }

    private void quen(Player p) {
        shield    = true;
        quenHits  = 0;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 2f);
        p.sendMessage(ChatColor.YELLOW + "✦ Quen active! (" + QUEN_MAX + " hits)");

        quenRunnable = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!shield || !p.isOnline()) { cancel(); return; }
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30 + t * 10);
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(Math.cos(a)*1.3, 1.2, Math.sin(a)*1.3),
                            QUEN_GOLD, 1.1f, 2, 0.04, 0.04, 0.04);
                }
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 - t * 14);
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(Math.cos(a)*0.7, 1.6, Math.sin(a)*0.7),
                            Color.WHITE, 0.9f, 1, 0.03, 0.03, 0.03);
                }
                if (t > 300) {
                    shield = false; quenCleanup();
                    addCd(witcher_quen, p);
                    p.sendMessage(ChatColor.YELLOW + "Quen expired.");
                    cancel();
                }
                t++;
            }
        };
        quenRunnable.runTaskTimer(magicPlugin, 0, 1);
    }

    private void quenAbsorb(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        ((EntityDamageByEntityEvent) ex.getRawEvent()).setCancelled(true);
        quenHits++;
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.4f);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), QUEN_GOLD, 1.5f, 30, 0.6, 0.6, 0.6);
        if (quenHits >= QUEN_MAX) quenShatter(p);
        else p.sendMessage(ChatColor.YELLOW + "✦ Quen blocked! (" + (QUEN_MAX - quenHits) + " left)");
    }

    private void quenAbsorbEnv(DamagedExecute ex) {
        ((EntityDamageEvent) ex.getRawEvent()).setCancelled(true);
        quenHits++;
        Player p = ex.getPlayer();
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), QUEN_GOLD, 1.3f, 20, 0.5, 0.5, 0.5);
        if (quenHits >= QUEN_MAX) quenShatter(p);
    }

    private void quenShatter(Player p) {
        shield = false; quenCleanup();
        addCd(witcher_quen, p);
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Quen shattered — retaliation!");
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 2f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.6f);

        Location center = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(center, QUEN_GOLD, 2f, 120, 1.5, 1.5, 1.5);
        for (int i = 0; i < 16; i++) {
            double a = Math.toRadians(i * 22.5);
            particleApi.drawColoredLine(center, center.clone().add(Math.cos(a)*3.5, 0, Math.sin(a)*3.5),
                    1.5, QUEN_GOLD, 1.3f, 0);
        }
        for (Entity e : p.getWorld().getNearbyEntities(center, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(center);
            ((LivingEntity) e).damage(Math.max(8, 28 - dist * 3.5), p);
            e.setVelocity(e.getLocation().subtract(center).toVector().normalize().multiply(1.8).setY(0.6));
        }
    }

    private void quenCleanup() { if (quenRunnable != null) { quenRunnable.cancel(); quenRunnable = null; } }

    private void axii(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 0.5f);
        LivingEntity target = getNearestTarget(p, 10);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "No target in range!"); return;
        }
        axiiMarked.put(target.getUniqueId(), System.currentTimeMillis() + 6000L);
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,  120, 1, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   80, 1, false, false));
        target.damage(8, p);

        p.sendMessage(ChatColor.GREEN + "✦ Axii — " + target.getType().name() + " marked! (+60% dmg for 6s)");
        p.getWorld().playSound(target.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.7f, 1.4f);

        drawBeam(p.getEyeLocation(), target.getLocation().clone().add(0, 1, 0), AXII_GREEN);
        final LivingEntity t = target;
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick > 120 || !t.isValid() || t.isDead() || !isAxiiMarked(t)) { cancel(); return; }
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 + tick * 14);
                    Location lp = t.getLocation().clone().add(Math.cos(a)*0.9, 1.8 + Math.sin(Math.toRadians(tick*15))*0.3, Math.sin(a)*0.9);
                    particleApi.spawnColoredParticles(lp, tick%3==0 ? AXII_GREEN : AXII_LIME, 1f, 2, 0.03, 0.03, 0.03);
                }
                tick++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        if (!yrdenTraps.isEmpty()) {
            Location underTarget = target.getLocation().clone().add(0, 0.1, 0);
            p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ COMBO: AXII × YRDEN — trap placed under target!");
            yrdenTraps.add(underTarget);
            spawnYrdenGlyph(p, underTarget, false);
        }
    }

    private void yrden(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
        Location center = getGroundTarget(p, 10);
        yrdenTraps.add(center);
        spawnYrdenGlyph(p, center, true);
    }

    private void spawnYrdenGlyph(Player owner, Location center, boolean withDamage) {
        List<LivingEntity> frozen = new ArrayList<>();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 160) {
                    for (LivingEntity e : frozen) if (e.isValid() && !e.isDead()) e.setAI(true);
                    frozen.clear();
                    yrdenTraps.remove(center);
                    owner.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 0.8f);
                    cancel(); return;
                }
                for (int i = 0; i < 40; i++) {
                    double a = Math.toRadians(i * 9 + t * 4);
                    Location lp = center.clone().add(Math.cos(a)*3.8, 0.05, Math.sin(a)*3.8);
                    particleApi.spawnColoredParticles(lp, t%2==0 ? YRDEN_PURPLE : YRDEN_DARK, 1f, 1, 0.02, 0.02, 0.02);
                }
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 - t * 6);
                    Location lp = center.clone().add(Math.cos(a)*1.9, 0.05, Math.sin(a)*1.9);
                    particleApi.spawnColoredParticles(lp, YRDEN_PURPLE, 1.2f, 2, 0.04, 0.1, 0.04);
                    for (double s = 0; s <= 1.0; s += 0.25) {
                        particleApi.spawnColoredParticles(center.clone().add(Math.cos(a)*1.9*s, 0.04, Math.sin(a)*1.9*s),
                                YRDEN_DARK, 0.9f, 1, 0.02, 0.02, 0.02);
                    }
                }
                if (t % 25 == 0) {
                    particleApi.spawnColoredParticles(center.clone().add(0, 0.1, 0), Color.WHITE, 1.5f, 15, 1.5, 0.1, 1.5);
                    center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.3f, 1.3f);
                }

                for (Entity e : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
                    if (e.equals(owner) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;
                    if (le.hasAI() && !frozen.contains(le)) { le.setAI(false); frozen.add(le); }
                    if (withDamage && t % 20 == 0) {
                        double dmg = isAxiiMarked(e) ? 8 : 5;
                        le.damage(dmg, owner);
                    }
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  30, 4, false, false));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, true));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,   25, 0, false, false));
                    if (le.getLocation().distance(center) > 4.5 && !le.hasAI()) {
                        le.setAI(true); frozen.remove(le);
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void witcherParry(DamagedByExecute ex) {
        if (new Random().nextInt(10) >= 4) return;
        Player p = ex.getPlayer();
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        event.setCancelled(true);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.3f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.4f);
        p.setVelocity(p.getLocation().getDirection().clone().normalize().multiply(-0.3));
        Location l1 = p.getLocation().clone().add(0, 2, 0).add(p.getLocation().getDirection().normalize().multiply(0.5)).add(rotateVec(p.getLocation().getDirection().normalize().multiply(0.3), 90, 0));
        Location l2 = p.getLocation().clone().add(p.getLocation().getDirection().normalize().multiply(0.5)).add(rotateVec(p.getLocation().getDirection().normalize().multiply(0.3), -90, 0));
        particleApi.drawColoredLine(l1, l2, 1.5, Color.SILVER, 1.2f, 0);

        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity) ((LivingEntity) damager).damage(6, p);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0, false, false));
                if (p.getHealth() < 8)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 2, false, false));
                if (t % 2 == 0)
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.8, 0),
                            Color.fromRGB(255, 220, 80), 0.8f, 2, 0.08, 0.05, 0.08);
                axiiMarked.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis());
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 20);
        return r;
    }

    @Override
    public void remove() {
        quenCleanup();
        shield = false;
        igniCharged = false;
        axiiMarked.clear();
        yrdenTraps.clear();
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&cIgni";
            case 1: return "&bAard";
            case 2: return "&eQuen";
            case 3: return "&aAxii";
            case 4: return "&5Yrden";
            default: return "&7none";
        }
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
    }

    private Vector rotateVec(Vector v, double yawDeg, double pitchDeg) {
        double y = Math.toRadians(yawDeg);
        double sy = Math.sin(y), cy = Math.cos(y);
        double x2 = v.getX()*cy + v.getZ()*sy;
        double z2 = -v.getX()*sy + v.getZ()*cy;
        v.setX(x2).setZ(z2);
        if (pitchDeg != 0) {
            double pit = Math.toRadians(pitchDeg);
            double sp = Math.sin(pit), cp = Math.cos(pit);
            double y2 = v.getY()*cp - v.getZ()*sp;
            double z2b = v.getY()*sp + v.getZ()*cp;
            v.setY(y2).setZ(z2b);
        }
        return v;
    }

    private Location getGroundTarget(Player p, int maxDist) {
        Location cur = p.getEyeLocation().clone();
        Vector dir   = p.getEyeLocation().getDirection().clone().setY(-0.3).normalize();
        for (int i = 0; i < maxDist * 4; i++) {
            cur.add(dir.clone().multiply(0.5));
            if (!cur.getBlock().isPassable()) { cur.subtract(dir.clone().multiply(0.5)); break; }
        }
        return cur;
    }
    private Location getEyeTarget(Player p, int maxDist) {
        Location cur = p.getEyeLocation().clone();
        Vector dir   = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < maxDist; i++) {
            cur.add(dir);
            if (!cur.getBlock().isPassable()) break;
        }
        return cur;
    }

    private boolean isInYrdenTrap(Location loc) {
        for (Location trap : yrdenTraps) {
            if (trap.getWorld().equals(loc.getWorld()) && trap.distance(loc) <= 4.5) return true;
        }
        return false;
    }
    private Location getNearestYrdenTrap(Location loc, double radius) {
        for (Location trap : yrdenTraps) {
            if (trap.getWorld().equals(loc.getWorld()) && trap.distance(loc) <= radius) return trap;
        }
        return null;
    }

    private boolean isAxiiMarked(Entity e) {
        Long expiry = axiiMarked.get(e.getUniqueId());
        if (expiry == null) return false;
        if (expiry < System.currentTimeMillis()) { axiiMarked.remove(e.getUniqueId()); return false; }
        return true;
    }

    private void axiiMarkConsumeVisual(Location loc, Color c) {
        particleApi.spawnColoredParticles(loc.clone().add(0, 1, 0), c, 1.8f, 50, 0.6, 0.8, 0.6);
        particleApi.spawnColoredParticles(loc.clone().add(0, 1, 0), AXII_GREEN, 1.2f, 20, 0.4, 0.4, 0.4);
        loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.6f);
    }

    private void drawBeam(Location from, Location to, Color color) {
        new BukkitRunnable() {
            @Override public void run() {
                if (from.getWorld() == null) return;
                double dist = from.distance(to);
                if (dist == 0) return;
                int steps = (int)(dist * 4);
                Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur = from.clone();
                for (int i = 0; i < steps; i++) {
                    particleApi.spawnColoredParticles(cur, color, 1f, 2, 0.04, 0.04, 0.04);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);
    }
}
