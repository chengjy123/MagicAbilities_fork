package net.trduc.magicabilitiesfork.powers.custom;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
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

public class CultivatorPower extends Power implements IdlePower, Removeable {

    private static final String cv_sword   = "cultivator.sword-qi";
    private static final String cv_seal    = "cultivator.seal";
    private static final String cv_press   = "cultivator.pressure";
    private static final String cv_medi    = "cultivator.meditate";
    private static final String cv_thunder = "cultivator.thunder-array";
    private static final String cv_judg    = "cultivator.judgment";

    private int XP_SWORD, XP_SWORD_SNEAK, XP_SEAL, XP_PRESS, XP_MEDI, XP_THUNDER, XP_JUDG;

    private int STAGE_2, STAGE_3, STAGE_4, STAGE_5, STAGE_6;

    private static final Color C_JADE       = Color.fromRGB( 80, 200, 130);
    private static final Color C_JADE_LIGHT = Color.fromRGB(140, 230, 170);
    private static final Color C_JADE_DEEP  = Color.fromRGB( 30, 140,  80);
    private static final Color C_GOLD       = Color.fromRGB(255, 210,  50);
    private static final Color C_GOLD_LIGHT = Color.fromRGB(255, 240, 150);
    private static final Color C_GOLD_DEEP  = Color.fromRGB(200, 150,  10);
    private static final Color C_WHITE_JADE = Color.fromRGB(220, 255, 230);
    private static final Color C_SPIRIT     = Color.fromRGB(160, 255, 200);

    private static final Color[] AURA_COLS  = { C_JADE_LIGHT, C_GOLD, C_SPIRIT};
    private static final Color[] SWORD_COLS = { C_JADE_LIGHT, C_WHITE_JADE, C_GOLD_LIGHT, C_SPIRIT };

    private int maxReachedStage = 1;
    private int lastCheckedXp   = -1;

    private final Map<UUID, BukkitRunnable> sealedTargets = new HashMap<>();

    private boolean meditating = false;
    private BukkitRunnable mediTask = null;

    private boolean channeling = false;
    private BukkitRunnable chanTask = null;

    private boolean charging = false;
    private BukkitRunnable chargeTask = null;

    private BukkitRunnable hudTask = null;

    public CultivatorPower(Player owner) {
        super(owner);
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration cfg = magicPlugin.getConfig();
        XP_SWORD       = cfg.getInt("cultivator.xp.sword-qi",        3);
        XP_SWORD_SNEAK = cfg.getInt("cultivator.xp.sword-qi-sneak",  6);
        XP_SEAL        = cfg.getInt("cultivator.xp.seal",            5);
        XP_PRESS       = cfg.getInt("cultivator.xp.pressure",        6);
        XP_MEDI        = cfg.getInt("cultivator.xp.meditate",        0);
        XP_THUNDER     = cfg.getInt("cultivator.xp.thunder-array",   8);
        XP_JUDG        = cfg.getInt("cultivator.xp.judgment",       15);

        STAGE_2 = cfg.getInt("cultivator.stage.truc-co",   10);
        STAGE_3 = cfg.getInt("cultivator.stage.kim-dan",   20);
        STAGE_4 = cfg.getInt("cultivator.stage.nguyen-anh",30);
        STAGE_5 = cfg.getInt("cultivator.stage.hoa-than",  40);
        STAGE_6 = cfg.getInt("cultivator.stage.dai-thua",  50);
    }

    @Override
    public void executePower(Execute ex) {

        if (ex instanceof DamagedExecute)   { onDamaged((DamagedExecute) ex); return; }
        if (ex instanceof DamagedByExecute) {   return; }

        if (ex instanceof SneakExecute)     { onSneak((SneakExecute) ex);     return; }

        if (ex instanceof MoveExecute)      { onMove((MoveExecute) ex);       return; }

        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) { onLeft((LeftClickExecute) ex); }
    }

    private void onSneak(SneakExecute ex) {
        if (!isEnabled()) return;
        startMeditation(ex.getPlayer());
    }

    private void onMove(MoveExecute ex) {
        if (!meditating) return;
        org.bukkit.event.player.PlayerMoveEvent ev =
                (org.bukkit.event.player.PlayerMoveEvent) ex.getRawEvent();
        Location from = ev.getFrom();
        Location to   = ev.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            stopMeditation();
        }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p    = ex.getPlayer();
        int    slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());

        if (!isSlotUnlocked(slot)) {
            hud(p, org.bukkit.ChatColor.RED + "Need a floor" + stageNeeded(slot) + " to use this trick!");
            return;
        }

        switch (slot) {
            case 0: {
                if (onCd(cv_sword, p, this)) return;
                int cost = p.isSneaking() ? XP_SWORD_SNEAK : XP_SWORD;
                if (!checkXp(p, cost, this)) return;
                spendXp(p, cost);
                swordQi(p, p.isSneaking() ? 5 : 3);
                addCd(cv_sword, p);
                return;
            }
            case 1: {
                if (onCd(cv_seal, p, this)) return;

                if (!checkXp(p, XP_SEAL, this)) return;
                if (!sealTechnique(p)) return;
                spendXp(p, XP_SEAL);
                addCd(cv_seal, p);
                return;
            }
            case 2: {
                if (onCd(cv_press, p, this)) return;
                if (!checkXp(p, XP_PRESS, this)) return;
                spendXp(p, XP_PRESS);
                spiritPressure(p);
                addCd(cv_press, p);
                return;
            }
            case 3: {

                if (channeling) { hud(p, "recovering!"); return; }
                if (onCd(cv_medi, p, this)) return;
                if (!checkXp(p, XP_MEDI, this)) return;
                spendXp(p, XP_MEDI);
                breathCultivation(p);
                return;

            }
            case 4: {
                if (onCd(cv_thunder, p, this)) return;
                if (!checkXp(p, XP_THUNDER, this)) return;
                spendXp(p, XP_THUNDER);
                thunderSwordArray(p);
                addCd(cv_thunder, p);
                return;
            }
            case 5: {

                if (charging) { hud(p, "Decree of the Heavenly Dao!"); return; }
                if (onCd(cv_judg, p, this)) return;
                if (!checkXp(p, XP_JUDG, this)) return;
                spendXp(p, XP_JUDG);
                heavenlyJudgment(p);
                addCd(cv_judg, p);
                return;
            }
        }
    }

    private void swordQi(Player p, int count) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW,         0.6f, 1.8f);

        double baseDmg = cfgDouble("cultivator.dmg.sword-qi-base", 8.0);
        double dmg     = baseDmg + (maxReachedStage - 1)
                         * cfgDouble("cultivator.dmg.sword-qi-per-stage", 2.0);

        int[] offsets;
        if (count == 3)      offsets = new int[]{-10, 0, 10};
        else if (count == 5) offsets = new int[]{-20, -10, 0, 10, 20};
        else                 offsets = new int[]{0};

        for (int i = 0; i < count; i++) {
            final int deg = offsets[i];
            final int idx = i;
            final double fd = dmg;
            new BukkitRunnable() {
                @Override public void run() { fireSwordQiProjectile(p, deg, fd); }
            }.runTaskLater(magicPlugin, idx * 2L);
        }
    }

    private void fireSwordQiProjectile(Player p, int yawDeg, double dmg) {
        final Location start = p.getEyeLocation().clone();
        final Vector   dir   = rotateY(p.getEyeLocation().getDirection().clone().normalize(), yawDeg)
                                       .multiply(0.9);

        final Set<UUID> hitSet = new HashSet<>();

        new BukkitRunnable() {
            Location cur = start.clone();
            int t = 0;
            @Override public void run() {
                if (t > 35) { swordPop(cur); cancel(); return; }
                cur.add(dir);
                if (!cur.getBlock().isPassable() || cur.getBlock().isLiquid()) {
                    swordPop(cur); cancel(); return;
                }

                particleApi.spawnColoredParticles(cur, C_WHITE_JADE, 1.7f, 3, 0.05, 0.05, 0.05);
                particleApi.spawnColoredParticles(cur, C_JADE_LIGHT, 1.3f, 2, 0.07, 0.07, 0.07);
                for (int j = 0; j < 3; j++) {
                    double a = Math.toRadians(j * 120 + t * 30);
                    Location edge = cur.clone().add(Math.cos(a)*0.22, Math.sin(a*0.5)*0.1, Math.sin(a)*0.22);
                    particleApi.spawnColoredParticles(edge, SWORD_COLS[(t+j) % SWORD_COLS.length],
                            0.9f, 1, 0.03, 0.03, 0.03);
                }
                if (t % 4 == 0)
                    particleApi.spawnParticles(cur, Particle.ENCHANT, 2, 0.08, 0.08, 0.08, 0.6);

                for (Entity e : cur.getWorld().getNearbyEntities(cur, 0.75, 0.75, 0.75)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || hitSet.contains(e.getUniqueId())) continue;
                    hitSet.add(e.getUniqueId());
                    boolean sealed = sealedTargets.containsKey(e.getUniqueId());
                    ((LivingEntity) e).damage(sealed ? dmg * 1.2 : dmg, p);
                    Location eHit = e.getLocation().clone().add(0, 1, 0);
                    particleApi.spawnColoredParticles(eHit, C_JADE_LIGHT, 1.4f, 8, 0.2, 0.2, 0.2);
                    particleApi.spawnColoredParticles(eHit, C_GOLD,       1.1f, 5, 0.25, 0.25, 0.25);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.7f);
                }
                if (t % 7 == 0)
                    p.getWorld().playSound(cur, Sound.ITEM_TRIDENT_HIT_GROUND, 0.1f, 2f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void swordPop(Location loc) {
        particleApi.spawnColoredParticles(loc, C_WHITE_JADE, 1.5f, 10, 0.25, 0.25, 0.25);
        particleApi.spawnColoredParticles(loc, C_GOLD,       1.2f,  8, 0.3,  0.3,  0.3);
        particleApi.spawnParticles(loc, Particle.ENCHANT, 8, 0.3, 0.3, 0.3, 0.8);
        loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.3f, 1.8f);
    }

    private boolean sealTechnique(Player p) {
        LivingEntity target = getNearestTarget(p, 6);
        if (target == null) {
            hud(p, "Bamboo Foundation!");
            return false;
        }
        if (sealedTargets.containsKey(target.getUniqueId())) {
            hud(p, "This person has been sealed away.!");
            return false;
        }

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.7f, 1.2f);

        target.damage(cfgDouble("cultivator.dmg.seal", 10.0), p);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,       65, 3, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 65, 2, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,        65, 0, false, false));

        final LivingEntity ft = target;
        new BukkitRunnable() {
            @Override public void run() {
                if (!ft.isValid()) return;
                Location from  = p.getEyeLocation().clone();
                Location to    = ft.getLocation().clone().add(0, 1, 0);
                int      steps = Math.max(4, (int) from.distance(to) * 3);
                Vector   step  = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur   = from.clone();
                for (int i = 0; i < steps; i++) {
                    double a = Math.toRadians(i * 30);
                    particleApi.spawnColoredParticles(
                            cur.clone().add(Math.cos(a)*0.15, 0, Math.sin(a)*0.15),
                            AURA_COLS[i % AURA_COLS.length], 1.1f, 1, 0.04, 0.04, 0.04);
                    particleApi.spawnParticles(cur, Particle.ENCHANT, 1, 0.03, 0.03, 0.03, 0.5);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);

        BukkitRunnable sealRun = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {

                if (t >= 65 || !ft.isValid() || ft.isDead()) {
                    sealedTargets.remove(ft.getUniqueId());
                    cancel(); return;
                }
                Location tl = ft.getLocation().clone().add(0, 1, 0);
                for (int i = 0; i < 8; i++) {
                    double a     = Math.toRadians(i * 45 + t * 10);
                    double pulse = 0.9 + Math.sin(t * 0.3 + i * 0.6) * 0.12;
                    particleApi.spawnColoredParticles(
                            tl.clone().add(Math.cos(a)*pulse, Math.sin(a*0.3)*0.2, Math.sin(a)*pulse),
                            AURA_COLS[i % AURA_COLS.length], 1.05f, 1, 0.03, 0.03, 0.03);
                }
                if (t % 5 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.toRadians(i * 90 + t * 15);
                        particleApi.spawnColoredParticles(
                                ft.getLocation().clone().add(0, 2.4, 0)
                                        .add(Math.cos(a)*0.35, 0, Math.sin(a)*0.35),
                                C_GOLD, 1.1f, 1, 0.03, 0.03, 0.03);
                    }
                }
                if (t % 20 == 0)
                    ft.getWorld().playSound(tl, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.1f, 1.5f);
                t++;
            }
        };
        sealedTargets.put(target.getUniqueId(), sealRun);
        sealRun.runTaskTimer(magicPlugin, 0, 1);
        hud(p, org.bukkit.ChatColor.GREEN + "✦ Seal Technique!");
        return true;
    }

    private void spiritPressure(Player p) {
        double radius = maxReachedStage >= 5 ? 6.0 : maxReachedStage >= 3 ? 5.0 : 4.0;

        final Location loc = p.getLocation().clone().add(0, 1, 0);

        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 1.2f);
        p.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 0.7f);

        new BukkitRunnable() {
            double r = 0.3; int t = 0;
            @Override public void run() {
                if (r > radius + 1.5) { cancel(); return; }
                for (int i = 0; i < 14; i++) {
                    double a  = Math.toRadians(i * (360.0 / 14) + t * 8);
                    Color  c  = (t+i)%3==0 ? C_JADE_LIGHT : (t+i)%3==1 ? C_GOLD : C_WHITE_JADE;
                    particleApi.spawnColoredParticles(
                            loc.clone().add(Math.cos(a)*r, 0, Math.sin(a)*r),
                            c, 1.1f, 1, 0.04, 0.04, 0.04);
                }
                if (t % 2 == 0)
                    particleApi.spawnParticles(loc.clone().add(0,-0.9,0), Particle.ENCHANT,
                            3, (float)r*0.15f, 0.1f, (float)r*0.15f, 0.8f);
                r += 0.42; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        final double finalRadius = radius;
        final org.bukkit.World world = p.getWorld();
        new BukkitRunnable() {
            @Override public void run() {
                Random rng = new Random();
                for (Entity e : world.getNearbyEntities(loc, finalRadius, 2.5, finalRadius)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    double dist = e.getLocation().distance(loc);
                    double dmg  = Math.max(7, magicPlugin.getConfig()
                            .getDouble("cultivator.dmg.pressure-max", 18) - dist * 2.0);
                    ((LivingEntity) e).damage(dmg, p);
                    ((LivingEntity) e).addPotionEffect(
                            new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, true));
                    Vector kb = e.getLocation().subtract(loc).toVector();
                    if (kb.lengthSquared() < 0.01)
                        kb = new Vector(rng.nextDouble()-0.5, 0.2, rng.nextDouble()-0.5);
                    e.setVelocity(kb.normalize().multiply(1.5).setY(0.4));
                    particleApi.spawnColoredParticles(
                            e.getLocation().clone().add(0,1,0), C_JADE_LIGHT, 1.3f, 6, 0.2, 0.2, 0.2);
                }
            }
        }.runTaskLater(magicPlugin, 5L);
    }

    private void breathCultivation(Player p) {
        channeling = true;
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        hud(p, org.bukkit.ChatColor.GREEN + "... Breath Recovery ...");

        final Location startLoc = p.getLocation().clone();

        chanTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) {

                    channeling = false; chanTask = null; cancel(); return;
                }
                if (p.getLocation().distance(startLoc) > 1.0) {
                    if (!channeling) { cancel(); return; }
                    hud(p, org.bukkit.ChatColor.RED + "Breath Recovery interrupted!");
                    channeling = false; chanTask = null;
                    addCd(cv_medi, p, 0.5);
                    cancel(); return;
                }
                if (t >= 60) {
                    endChannel(p, true); cancel(); return;
                }
                Location center = p.getLocation().clone().add(0, 1.1, 0);
                for (int i = 0; i < 6; i++) {
                    double a  = Math.toRadians(i * 60 + t * 12);
                    double ri = 2.5 - (t / 60.0) * 2.0;
                    particleApi.spawnColoredParticles(
                            center.clone().add(Math.cos(a)*ri, Math.sin(a*0.3)*0.5, Math.sin(a)*ri),
                            AURA_COLS[i % AURA_COLS.length], 1.2f, 1, 0.04, 0.04, 0.04);
                }
                particleApi.spawnParticles(center, Particle.ENCHANT, 3, 0.4, 0.4, 0.4, 0.6);
                particleApi.spawnColoredParticles(center, C_JADE_LIGHT, 1.0f, 2, 0.2, 0.2, 0.2);
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 25, 1, false, false));
                if (t % 10 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.7f);
                t++;
            }
        };
        chanTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void endChannel(Player p, boolean success) {
        channeling = false;
        chanTask   = null;
        if (!success) return;

        double healHp   = cfgDouble("cultivator.meditate-heal-hp", 6.0);
        int    rewardXp = cfgInt("cultivator.meditate-reward-xp", 10);

        p.setHealth(Math.min(getMaxHp(p), p.getHealth() + healHp));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, false, false));
        p.giveExp(rewardXp);

        Location loc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(loc, C_WHITE_JADE, 1.8f, 20, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(loc, C_GOLD,       1.5f, 15, 0.8, 0.8, 0.8);
        particleApi.spawnParticles(loc, Particle.TOTEM_OF_UNDYING, 10, 0.5, 0.5, 0.5, 0.4);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP,    0.7f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE,  0.5f, 1.8f);
        hud(p, org.bukkit.ChatColor.GOLD + "✦ Breath Recovery! +" + healHp + "HP +" + rewardXp + "XP");
        addCd(cv_medi, p);
        checkStageUp(p);
    }

    private void thunderSwordArray(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE,   1f,   0.7f);
        hud(p, org.bukkit.ChatColor.YELLOW + "✦ Thunder Sword Formation!");

        final Set<UUID> hitCd = new HashSet<>();

        for (int ph = 0; ph < 5; ph++) {
            final double initOffset = (2 * Math.PI / 5) * ph;
            final int    phIdx      = ph;

            new BukkitRunnable() {
                int      t          = 0;
                boolean  striking   = false;
                boolean  returning  = false;
                Location strikeDest = null;

                Location swordLoc   = null;

                @Override public void run() {
                    if (t >= 100 || !p.isOnline()) { cancel(); return; }

                    double   angle = initOffset + t * 0.14;
                    Location orbit = p.getLocation().clone().add(0, 1.4, 0).add(
                            Math.cos(angle) * 2.1,
                            Math.sin(angle * 0.6) * 0.4,
                            Math.sin(angle) * 2.1);

                    if (swordLoc == null) swordLoc = orbit.clone();

                    if (!striking && !returning) {
                        swordLoc = orbit.clone();
                        drawThunderSword(swordLoc, phIdx, t, false);
                        if (t % 10 == phIdx * 2) {
                            LivingEntity tgt = findTargetFromLoc(p, swordLoc, 7.0);
                            if (tgt != null && !hitCd.contains(tgt.getUniqueId())) {
                                striking   = true;
                                strikeDest = tgt.getLocation().clone().add(0, 1, 0);
                            }
                        }
                    } else if (striking) {

                        Vector toTarget = strikeDest.toVector().subtract(swordLoc.toVector());
                        if (!isVecFinite(toTarget) || toTarget.lengthSquared() < 0.7) {

                            for (Entity e : swordLoc.getWorld().getNearbyEntities(swordLoc, 1.2, 1.2, 1.2)) {
                                if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                                if (hitCd.contains(e.getUniqueId())) continue;
                                boolean sealed = sealedTargets.containsKey(e.getUniqueId());
                                double  dmg    = magicPlugin.getConfig()
                                        .getDouble("cultivator.dmg.thunder-sword", 14.0);
                                ((LivingEntity) e).damage(sealed ? dmg * 1.2 : dmg, p);
                                swordLoc.getWorld().strikeLightningEffect(swordLoc);
                                hitCd.add(e.getUniqueId());
                                final UUID uid = e.getUniqueId();
                                new BukkitRunnable() {
                                    @Override public void run() { hitCd.remove(uid); }
                                }.runTaskLater(magicPlugin, 30L);
                                particleApi.spawnColoredParticles(swordLoc, C_GOLD_LIGHT, 1.5f, 12, 0.25, 0.25, 0.25);
                                p.getWorld().playSound(swordLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f);
                            }
                            striking = false; returning = true;
                        } else {
                            swordLoc = swordLoc.clone().add(toTarget.normalize().multiply(0.85));
                            drawThunderSword(swordLoc, phIdx, t, true);
                        }
                    } else {
                        Vector toOrbit = orbit.toVector().subtract(swordLoc.toVector());
                        if (!isVecFinite(toOrbit) || toOrbit.lengthSquared() < 1.0) {
                            returning = false;
                        } else {
                            swordLoc = swordLoc.clone().add(toOrbit.normalize().multiply(0.7));
                            drawThunderSword(swordLoc, phIdx, t, false);
                        }
                    }
                    t++;
                }
            }.runTaskTimer(magicPlugin, ph * 3L, 1);
        }
    }

    private void drawThunderSword(Location loc, int idx, int t, boolean striking) {
        Color c1 = idx % 2 == 0 ? C_GOLD : C_JADE_LIGHT;
        if (striking) {
            particleApi.spawnColoredParticles(loc, C_WHITE_JADE, 1.7f, 4, 0.06, 0.06, 0.06);
            particleApi.spawnColoredParticles(loc, c1,           1.4f, 3, 0.06, 0.06, 0.06);
            particleApi.spawnParticles(loc, Particle.ENCHANT, 2, 0.05, 0.05, 0.05, 0.6);
        } else {
            particleApi.spawnColoredParticles(loc, c1,           1.2f, 2, 0.07, 0.07, 0.07);
            particleApi.spawnColoredParticles(loc, C_WHITE_JADE, 0.9f, 1, 0.04, 0.04, 0.04);
            if (t % 5 == 0)
                particleApi.spawnParticles(loc, Particle.ENCHANT, 1, 0.04, 0.04, 0.04, 0.4);
        }
    }

    private void heavenlyJudgment(Player p) {
        charging = true;
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,    1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 0.6f);
        p.sendMessage(org.bukkit.ChatColor.GOLD + "⚡ 天道裁決 — Charging...");
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 55, 255, false, false));

        chargeTask = new BukkitRunnable() {
            int ct = 50;
            @Override public void run() {
                if (!p.isOnline()) {

                    charging = false; chargeTask = null; cancel(); return;
                }
                if (ct <= 0) {
                    charging = false; chargeTask = null;
                    cancel(); releaseJudgment(p); return;
                }
                Location loc = p.getLocation().clone().add(0, 1, 0);
                Random   rng = new Random();
                for (int i = 0; i < 8; i++) {
                    double a  = Math.toRadians(i * 45 + (50-ct) * 9);
                    double r  = Math.max(0.2, 2.5 - (50-ct) * 0.04);
                    Location lp = loc.clone().add(Math.cos(a)*r, rng.nextDouble()*2-1, Math.sin(a)*r);
                    particleApi.spawnColoredParticles(lp, i%2==0 ? C_GOLD : C_JADE_LIGHT, 1.3f, 2, 0.05, 0.05, 0.05);
                    particleApi.spawnParticles(lp, Particle.ENCHANT, 1, 0.04, 0.04, 0.04, 0.6);
                }
                if (ct % 8 == 0) {
                    for (double y = 0; y <= 6; y += 0.8) {
                        Location col = loc.clone().add((rng.nextDouble()-0.5)*0.5, y, (rng.nextDouble()-0.5)*0.5);
                        particleApi.spawnColoredParticles(col, C_GOLD_LIGHT, 1.4f, 2, 0.06, 0.04, 0.06);
                    }
                    p.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.6f);
                }
                if (ct == 25)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);
                ct--;
            }
        };
        chargeTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void releaseJudgment(Player p) {
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE,       1f, 0.8f);
        p.sendMessage(org.bukkit.ChatColor.GOLD + "⚡ " + org.bukkit.ChatColor.BOLD + "THE WAY OF HEAVEN WILL RE-DETERMINE!");

        final Location  start  = p.getEyeLocation().clone();
        LivingEntity    target = getInSight(p, 35, 0.4);

        particleApi.spawnColoredParticles(start, C_WHITE_JADE, 2.5f, 30, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(start, C_GOLD,       2.0f, 40, 0.7, 0.7, 0.7);
        particleApi.spawnParticles(start, Particle.ENCHANT,          40, 0.6, 0.6, 0.6, 1.0);
        particleApi.spawnParticles(start, Particle.TOTEM_OF_UNDYING, 20, 0.5, 0.5, 0.5, 0.6);

        if (target != null) {
            final LivingEntity ft  = target;

            final Vector[]     dir = { ft.getLocation().clone().add(0,1,0)
                                         .subtract(start).toVector().normalize().multiply(1.1) };
            if (!isVecFinite(dir[0])) dir[0] = p.getEyeLocation().getDirection().normalize().multiply(1.1);

            new BukkitRunnable() {
                Location cur = start.clone(); int t = 0;
                @Override public void run() {
                    if (!p.isOnline() || t > 65) { cancel(); return; }

                    if (t % 4 == 0 && ft.isValid() && !ft.isDead()) {
                        Vector toT = ft.getLocation().clone().add(0,1,0).subtract(cur).toVector();
                        if (isVecFinite(toT) && toT.lengthSquared() > 0.4)
                            dir[0] = toT.normalize().multiply(1.1);
                    }
                    cur.add(dir[0]);
                    particleApi.spawnColoredParticles(cur, C_WHITE_JADE, 2.0f, 5, 0.1, 0.1, 0.1);
                    particleApi.spawnColoredParticles(cur, C_GOLD,       1.7f, 4, 0.12, 0.12, 0.12);
                    particleApi.spawnColoredParticles(cur, C_GOLD_LIGHT, 1.4f, 3, 0.15, 0.15, 0.15);
                    particleApi.spawnParticles(cur, Particle.ENCHANT, 5, 0.1, 0.1, 0.1, 0.8);
                    for (int j = 0; j < 4; j++) {
                        double a = Math.toRadians(j * 90 + t * 22);
                        particleApi.spawnColoredParticles(
                                cur.clone().add(Math.cos(a)*0.3, Math.sin(a*0.5)*0.15, Math.sin(a)*0.3),
                                SWORD_COLS[j % SWORD_COLS.length], 0.9f, 1, 0.03, 0.03, 0.03);
                    }

                    if (ft.isValid() && cur.distance(ft.getLocation().clone().add(0,1,0)) < 1.2) {
                        judgmentHit(cur, ft, p); cancel(); return;
                    }
                    if (t > 60) { judgmentHit(cur, null, p); cancel(); return; }
                    t++;
                }
            }.runTaskTimer(magicPlugin, 0, 1);
        } else {
            final Vector fdir = p.getEyeLocation().getDirection().clone().normalize();
            new BukkitRunnable() {
                Location cur = start.clone(); int t = 0;
                @Override public void run() {
                    if (!p.isOnline() || t > 40) { cancel(); return; }
                    cur.add(fdir);
                    if (!cur.getBlock().isPassable()) { judgmentHit(cur, null, p); cancel(); return; }
                    particleApi.spawnColoredParticles(cur, C_WHITE_JADE, 2.0f, 4, 0.09, 0.09, 0.09);
                    particleApi.spawnColoredParticles(cur, C_GOLD,       1.6f, 3, 0.11, 0.11, 0.11);
                    particleApi.spawnParticles(cur, Particle.ENCHANT, 4, 0.09, 0.09, 0.09, 0.7);
                    for (Entity e : cur.getWorld().getNearbyEntities(cur, 1.0, 1.0, 1.0)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        judgmentHit(cur, (LivingEntity) e, p); cancel(); return;
                    }
                    t++;
                }
            }.runTaskTimer(magicPlugin, 0, 1);
        }
    }

    private void judgmentHit(Location loc, LivingEntity target, Player p) {
        particleApi.spawnColoredParticles(loc, C_WHITE_JADE, 3.0f, 50, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc, C_GOLD,       2.5f, 60, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(loc, C_GOLD_LIGHT, 2.0f, 50, 2.0, 2.0, 2.0);
        particleApi.spawnParticles(loc, Particle.ENCHANT,          40, 1.5, 1.5, 1.5, 1.0);
        particleApi.spawnParticles(loc, Particle.TOTEM_OF_UNDYING, 30, 1.5, 1.5, 1.5, 0.7);

        new BukkitRunnable() {
            double r = 0.3; int t = 0;
            @Override public void run() {
                if (r > 5) { cancel(); return; }
                for (int i = 0; i < 18; i++) {
                    double a = Math.toRadians(i * 20 + t * 7);
                    particleApi.spawnColoredParticles(
                            loc.clone().add(0, 0.1, 0).add(Math.cos(a)*r, 0, Math.sin(a)*r),
                            t<4 ? C_WHITE_JADE : t<9 ? C_GOLD : C_GOLD_LIGHT,
                            1.1f, 1, 0.04, 0.04, 0.04);
                }
                r += 0.4; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE,       1f, 0.8f);

        loc.getWorld().strikeLightningEffect(loc);

        if (target == null || !target.isValid() || target.isDead()) return;
        boolean sealed = sealedTargets.containsKey(target.getUniqueId());
        double  ratio  = cfgDouble("cultivator.dmg.judgment-ratio", 0.45);
        double  dmg    = target.getHealth() * ratio;
        if (sealed) dmg *= 1.2;

        target.setHealth(Math.max(0.01, target.getHealth() - dmg));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   100, 2, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
        target.setVelocity(new Vector(0, 1.8, 0));
        hud(p, org.bukkit.ChatColor.GOLD + "⚡ " + (int) dmg + " dmg" + (sealed ? " (SEAL)" : ""));
    }

    private void onDamaged(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();

        if (event.getCause() == EntityDamageEvent.DamageCause.POISON)
            event.setCancelled(true);

        if (channeling && chanTask != null) {

            channeling = false;
            BukkitRunnable ct = chanTask;
            chanTask = null;
            try { ct.cancel(); } catch (Exception ignored) {}
            ex.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "Breath Recovery interrupted by attack!");
            addCd(cv_medi, ex.getPlayer(), 0.5);
        }
    }

    private void startMeditation(Player p) {
        if (meditating) return;
        meditating = true;

        mediTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {

                if (!p.isOnline() || !p.isSneaking()) {
                    stopMeditation(); cancel(); return;
                }
                if (t >= 200) {
                    p.giveExp(2);
                    Location loc = p.getLocation().clone().add(0, 1, 0);
                    particleApi.spawnColoredParticles(loc, C_JADE_LIGHT, 1.4f, 10, 0.5, 0.5, 0.5);
                    particleApi.spawnColoredParticles(loc, C_GOLD,       1.2f,  8, 0.6, 0.6, 0.6);
                    p.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                    hud(p, org.bukkit.ChatColor.GREEN + "✦ Meditation +2 XP");
                    meditating = false; mediTask = null;
                    checkStageUp(p);
                    cancel(); return;
                }
                if (t % 6 == 0) {
                    Location loc = p.getLocation().clone().add(0, 1, 0);
                    for (int i = 0; i < 5; i++) {
                        double a  = Math.toRadians(i * 72 + t * 4);
                        double ri = 1.5 - (t / 200.0) * 1.2;
                        particleApi.spawnColoredParticles(
                                loc.clone().add(Math.cos(a)*ri, Math.sin(a*0.3)*0.3, Math.sin(a)*ri),
                                AURA_COLS[i % AURA_COLS.length], 0.9f, 1, 0.03, 0.03, 0.03);
                    }
                }
                t++;
            }
        };
        mediTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void stopMeditation() {
        meditating = false;
        if (mediTask != null) {

            BukkitRunnable mt = mediTask;
            mediTask = null;
            try { mt.cancel(); } catch (Exception ignored) {}
        }
    }

    private void checkStageUp(Player p) {
        int totalXp  = calcTotalXp(p);
        int newStage = getStageFromXp(totalXp);
        if (newStage > maxReachedStage) {
            maxReachedStage = newStage;
            onBreakthrough(p, newStage);
        }
    }

    private int getStageFromXp(int totalXp) {
        if (totalXp >= STAGE_6) return 6;
        if (totalXp >= STAGE_5) return 5;
        if (totalXp >= STAGE_4) return 4;
        if (totalXp >= STAGE_3) return 3;
        if (totalXp >= STAGE_2) return 2;
        return 1;
    }

    private void onBreakthrough(Player p, int newStage) {
        String[] names = {"", "Qi Refining", "Foundation Building", "Golden Core", "Nascent Soul", "Spirit Transformation", "Great Vehicle"};
        String   name  = names[Math.min(newStage, 6)];

        p.sendMessage(org.bukkit.ChatColor.GOLD + "✦ " + org.bukkit.ChatColor.BOLD
                + "BREAKTHROUGH! Floor " + newStage + " — " + name + "!");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP,         1f,   0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,         1f,   1.2f);

        Location loc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(loc, C_WHITE_JADE, 2.5f, 40, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(loc, C_GOLD,       2.0f, 50, 1.5, 1.5, 1.5);
        particleApi.spawnParticles(loc, Particle.TOTEM_OF_UNDYING, 25, 1.0, 1.0, 1.0, 0.6);
        particleApi.spawnParticles(loc, Particle.ENCHANT, 50, 1.5, 1.5, 1.5, 1.0);

        int bolts = newStage <= 2 ? 1 : newStage <= 4 ? 3 : 5;

        new BukkitRunnable() {
            int fired = 0;
            @Override public void run() {
                if (fired >= bolts || !p.isOnline()) { cancel(); return; }
                Location strike = p.getLocation().clone().add(
                        (Math.random()-0.5)*4, 0, (Math.random()-0.5)*4);
                for (double y = 0; y <= 12; y += 0.8) {
                    Location col = strike.clone().add(
                            (Math.random()-0.5)*0.4, y, (Math.random()-0.5)*0.4);
                    particleApi.spawnColoredParticles(col,
                            fired%2==0 ? C_GOLD_LIGHT : C_WHITE_JADE, 1.3f, 2, 0.07, 0.04, 0.07);
                    particleApi.spawnParticles(col, Particle.ENCHANT, 1, 0.05, 0.03, 0.05, 0.5);
                }

                strike.getWorld().strikeLightningEffect(strike);
                fired++;
            }
        }.runTaskTimer(magicPlugin, 5L, 8L);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p   = ex.getPlayer();
        final Random rng = new Random();

        hudTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (p.hasPotionEffect(PotionEffectType.POISON))
                    p.removePotionEffect(PotionEffectType.POISON);

                if (t % 20 == 0) {
                    int curXp = calcTotalXp(p);
                    if (curXp != lastCheckedXp) {
                        lastCheckedXp = curXp;
                        checkStageUp(p);
                    }
                }

                if (isAuraEnabled(p)) {
                    Location center = p.getLocation().clone().add(0, 1.1, 0);

                    for (int i = 0; i < 10; i++) {
                        double a    = Math.toRadians(i * 36 + t * 6);
                        double yOsc = Math.sin(t * 0.07 + i * 0.6) * 0.25;
                        particleApi.spawnColoredParticles(
                                center.clone().add(Math.cos(a)*1.15, yOsc, Math.sin(a)*1.15),
                                AURA_COLS[i % AURA_COLS.length], 1.0f, 1, 0.03, 0.03, 0.03);
                        if (i % 5 == 0)
                            particleApi.spawnParticles(center.clone().add(Math.cos(a)*1.15, yOsc, Math.sin(a)*1.15),
                                    Particle.ENCHANT, 1, 0.03, 0.03, 0.03, 0.5);
                    }

                    for (int i = 0; i < 7; i++) {
                        double a = Math.toRadians(i * (360.0 / 7) - t * 8);
                        particleApi.spawnColoredParticles(
                                center.clone().add(Math.cos(a)*0.7, 0.5+Math.sin(a*0.45)*0.16, Math.sin(a)*0.7),
                                i%2==0 ? C_GOLD : C_JADE_DEEP, 0.85f, 1, 0.03, 0.03, 0.03);
                    }

                    if (t % 5 == 0) {
                        for (int i = 0; i < 6; i++) {
                            double a = Math.toRadians(i * 60 + t * 4);
                            Location fp = p.getLocation().clone().add(Math.cos(a)*0.9, 0.05, Math.sin(a)*0.9);
                            particleApi.spawnParticles(fp, Particle.ENCHANT, 1, 0.04, 0.01, 0.04, 0.4);
                            particleApi.spawnColoredParticles(fp, C_GOLD_LIGHT, 0.8f, 1, 0.05, 0.01, 0.05);
                        }
                    }
                    if (t % 4 == 0)
                        particleApi.spawnParticles(
                                center.clone().add((rng.nextDouble()-0.5)*1.3,
                                        rng.nextDouble()*1.7-0.3, (rng.nextDouble()-0.5)*1.3),
                                Particle.GLOW, 1, 0.04, 0.04, 0.04, 0.4);
                    if (t % 100 == 0)
                        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.15f, 1.2f);
                }
                if (t % 10 == 0) hud(p, null);
                t++;
            }
        };
        hudTask.runTaskTimer(magicPlugin, 0, 1);
        return hudTask;
    }

    @Override
    public void remove() {
        stopMeditation();
        channeling = false;
        if (chanTask != null) {
            BukkitRunnable ct = chanTask; chanTask = null;
            try { ct.cancel(); } catch (Exception ignored) {}
        }
        charging = false;
        if (chargeTask != null) {
            BukkitRunnable cht = chargeTask; chargeTask = null;
            try { cht.cancel(); } catch (Exception ignored) {}
        }
        if (hudTask != null) {
            BukkitRunnable ht = hudTask; hudTask = null;
            try { ht.cancel(); } catch (Exception ignored) {}
        }
        for (BukkitRunnable r : sealedTargets.values()) {
            try { r.cancel(); } catch (Exception ignored) {}
        }
        sealedTargets.clear();
    }

    @Override
    public String getAbilityName(int ability) {
        String lock = isSlotUnlocked(ability) ? "" : " §7[Floor " + stageNeeded(ability) + "]";
        switch (ability) {
            case 0: return "§a劍氣 Sword Qi"           + lock;
            case 1: return "§a封印術 Seal Technique"           + lock;
            case 2: return "§a靈壓 Spirit Pressure"             + lock;
            case 3: return "§a服氣術 Breath Recovery"           + lock;
            case 4: return "§e雷劍陣 Thunder Sword Formation"     + lock;
            case 5: return "§6§l天道裁決"                + lock;
            default: return "§7none";
        }
    }

    private void hud(Player p, String msg) {
        String[] names     = {"", "Qi Refining", "Foundation Building", "Golden Core", "Nascent Soul", "Spirit Transformation", "Great Vehicle"};
        String   stageName = names[Math.min(maxReachedStage, 6)];
        String   color     = maxReachedStage >= 5 ? "§6" : maxReachedStage >= 3 ? "§a" : "§7";
        String   bar       = color + "✦ " + stageName + " §7[Floor " + maxReachedStage + "/6]";
        String   m         = msg != null ? " §r §f" + msg : "";
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar + m));
    }

    private boolean isSlotUnlocked(int slot) {
        switch (slot) {
            case 0: return true;
            case 1: return maxReachedStage >= 2;
            case 2: return maxReachedStage >= 3;
            case 3: return maxReachedStage >= 4;
            case 4: return maxReachedStage >= 5;
            case 5: return maxReachedStage >= 6;
            default: return false;
        }
    }

    private int stageNeeded(int slot) { return slot + 1; }

}

