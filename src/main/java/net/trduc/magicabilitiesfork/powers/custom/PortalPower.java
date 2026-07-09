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

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
import static net.trduc.magicabilitiesfork.cooldowns.CooldownApi.isOnCooldown;

public class PortalPower extends Power implements IdlePower, Removeable {

    private static final String p_bolt     = "portal.bolt";
    private static final String p_phase    = "portal.phase";
    private static final String p_mirror   = "portal.mirror";
    private static final String p_rift     = "portal.rift";
    private static final String p_barrage  = "portal.barrage";
    private static final String p_step     = "portal.step";
    private static final String p_blink    = "portal.blink";
    private static final String p_gate     = "portal.gate";
    private static final String p_dodge    = "portal.dodge";

    private int XP_GATE;

    private static final Color C_PURPLE     = Color.fromRGB(148, 0,   211);
    private static final Color C_VIOLET     = Color.fromRGB(180, 50,  255);
    private static final Color C_LAVENDER   = Color.fromRGB(220, 150, 255);
    private static final Color C_GOLD       = Color.fromRGB(255, 210, 30);
    private static final Color C_GOLD_DIM   = Color.fromRGB(200, 160, 10);
    private static final Color C_WHITE_VOID = Color.fromRGB(240, 230, 255);
    private static final Color[] PORTAL_COLORS = {
            C_PURPLE, C_VIOLET, C_LAVENDER, C_GOLD, C_GOLD_DIM
    };

    private Location portalA = null;
    private Location portalB = null;
    private BukkitRunnable portalCheckTask = null;
    private BukkitRunnable portalParticleTask = null;

    private boolean gateActive = false;

    public PortalPower(Player owner) {
        super(owner);
        XP_GATE = magicPlugin.getConfig().getInt("portal.xp.gate", 20);
    }

    @Override
    public void executePower(Execute ex) {

        if (ex instanceof DamagedByExecute) {
            passiveDodge((DamagedByExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeftClick((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRightClick((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex); }
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(p_bolt,    p, this)) return; voidBolt(p, 1.0, 0);  addCd(p_bolt,    p); return;
            case 1: if (onCd(p_phase,   p, this)) return; phaseShift(p);         addCd(p_phase,   p); return;
            case 2: if (onCd(p_mirror,  p, this)) return; mirrorPortal(p);       addCd(p_mirror,  p); return;
            case 3: if (onCd(p_rift,    p, this)) return; riftPull(p);           addCd(p_rift,    p); return;
            case 4: if (onCd(p_barrage, p, this)) return; portalBarrage(p);      addCd(p_barrage, p); return;
            case 7:

                if (onCd(p_gate, p, this)) return;
                if (!checkXp(p, XP_GATE, this)) return;
                spendXp(p, XP_GATE);
                voidGate(p);
                addCd(p_gate, p);
                return;
        }
    }

    private void onRightClick(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (onCd(p_step, p, this)) return;
        voidStep(p);
        addCd(p_step, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(p_blink, p, this)) return;
        blinkDash(p);
        addCd(p_blink, p);
    }

    private void voidBolt(Player p, double damageMult, int yawOff) {
        ArmorStand bolt = spawnProjectile(p);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        if (yawOff != 0) dir = rotateY(dir, yawOff);
        final Vector fDir = dir;
        Random rng = new Random();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (bolt.isDead() || t > 70) { safeRemove(bolt); cancel(); return; }

                bolt.teleport(bolt.getLocation().add(fDir.clone().multiply(1.7)));
                Location loc = bolt.getLocation();

                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 45 + i * 120);
                    Vector off = new Vector(Math.cos(a) * 0.35, Math.sin(a) * 0.25, Math.sin(a) * 0.35);
                    Color c = rng.nextBoolean() ? C_VIOLET : C_PURPLE;
                    particleApi.spawnColoredParticles(loc.clone().add(off), c, 1.2f, 2, 0.04, 0.04, 0.04);
                }
                if (rng.nextInt(3) == 0)
                    particleApi.spawnColoredParticles(loc, C_GOLD, 1.1f, 1, 0.06, 0.06, 0.06);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.1, 1.1, 1.1)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        arcaneExplosion(loc, p, 2.5, 12 * damageMult);
                        safeRemove(bolt); cancel(); return;
                    }
                }
                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    arcaneExplosion(loc, p, 2.0, 9 * damageMult);
                    safeRemove(bolt); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void arcaneExplosion(Location loc, Player p, double radius, double damage) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_HURT, 0.9f, 0.7f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 0.6f);

        particleApi.spawnColoredParticles(loc, C_PURPLE,   1.5f, 60, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(loc, C_GOLD,     1.3f, 30, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(loc, C_LAVENDER, 1.0f, 20, 1.0, 1.0, 1.0);
        particleApi.spawnParticles(loc, Particle.PORTAL, 80, 0.6, 0.6, 0.6, 2.0);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(loc));
            double dmg  = Math.max(4, damage - dist * 1.5);
            ((LivingEntity) e).damage(dmg, p);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 40, 1);
            Vector kb = e.getLocation().subtract(loc).toVector().normalize().multiply(1.6).setY(0.35);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.35, 0));
        }
    }

    private void phaseShift(Player p) {
        Location target = getRaycastTarget(p, 20);

        target.add(p.getEyeLocation().getDirection().normalize().multiply(-0.5));
        target.setY(target.getY() + 0.1);

        Location from = p.getLocation().clone();

        spawnPortalBurst(from.clone().add(0, 1, 0), 40);
        p.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.4f);

        p.teleport(target);

        spawnPortalBurst(target.clone().add(0, 1, 0), 40);
        p.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);

        for (Entity e : target.getWorld().getNearbyEntities(target, 2.5, 2.5, 2.5)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            ((LivingEntity) e).damage(7, p);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 30, 1);
            particleApi.spawnParticles(e.getLocation().add(0, 1, 0), Particle.PORTAL, 20, 0.3, 0.3, 0.3, 1.0);
        }
    }

    private final Map<UUID, Long> portalCooldowns = new HashMap<>();
    private static final long PORTAL_CD_MS = 3000L;

    private static final int PORTAL_LIFETIME_S = 60;

    private void mirrorPortal(Player p) {
        if (portalA == null) {
            portalA = p.getLocation().clone();
            sendActionBar(p, "§5§lPortal A §7placed — click again to place Portal B");
            p.getWorld().playSound(portalA, Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 1.8f);
            spawnPortalBurst(portalA.clone().add(0, 1, 0), 60);
            startPortalParticles();
        } else if (portalB == null) {
            if (p.getLocation().distance(portalA) < 3.0) {
                sendActionBar(p, "§cPortal B must be at least 3 blocks from A!");
                return;
            }
            portalB = p.getLocation().clone();
            sendActionBar(p, "§5§lPortal B §7placed — Portal linked! §8(" + PORTAL_LIFETIME_S + "s)");
            p.getWorld().playSound(portalB, Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.2f);
            spawnPortalBurst(portalB.clone().add(0, 1, 0), 60);
            startPortalCheckTask(p);
            startPortalExpiry(p);
        } else {
            closePortals();
            sendActionBar(p, "§7The portal has closed.");
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 2f);
        }
    }

    private void startPortalExpiry(Player p) {
        new BukkitRunnable() {
            int remaining = PORTAL_LIFETIME_S;
            @Override
            public void run() {
                if (portalA == null && portalB == null) { cancel(); return; }
                if (!p.isOnline()) { closePortals(); cancel(); return; }

                if (remaining == 10)
                    sendActionBar(p, "§cThe portal will close in §l10 §cseconds!");
                if (remaining == 5)
                    sendActionBar(p, "§cThe portal will close in §l5 §cseconds!");

                if (remaining <= 5 && portalA != null) {
                    if (remaining % 2 == 0) {
                        spawnPortalBurst(portalA.clone().add(0, 1, 0), 10);
                        if (portalB != null) spawnPortalBurst(portalB.clone().add(0, 1, 0), 10);
                    }
                }

                if (remaining <= 0) {

                    if (portalA != null) spawnPortalBurst(portalA.clone().add(0, 1, 0), 40);
                    if (portalB != null) spawnPortalBurst(portalB.clone().add(0, 1, 0), 40);
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 2.0f);
                    sendActionBar(p, "§7The portal has vanished.");
                    closePortals();
                    cancel();
                    return;
                }
                remaining--;
            }
        }.runTaskTimer(magicPlugin, 20L, 20L);
    }

    private void startPortalParticles() {
        if (portalParticleTask != null) portalParticleTask.cancel();
        portalParticleTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (portalA == null && portalB == null) { cancel(); return; }
                if (portalA != null) drawPortalRing(portalA, t, C_VIOLET,  C_GOLD);
                if (portalB != null) drawPortalRing(portalB, t, C_PURPLE,  C_GOLD_DIM);
                t++;
            }
        };
        portalParticleTask.runTaskTimer(magicPlugin, 0, 2);
    }

    private void drawPortalRing(Location center, int tick, Color inner, Color outer) {

        particleCircle(center.clone().add(0, 0.8, 0), 0.9, inner, 1.1f, 16, tick * 6);
        particleCircle(center.clone().add(0, 1.5, 0), 0.9, inner, 1.0f, 16, -(tick * 5));

        particleCircle(center.clone().add(0, 1.1, 0), 1.2, outer, 1.0f, 12, tick * 4);

        particleApi.spawnColoredParticles(center.clone().add(0, 1.1, 0),
                C_WHITE_VOID, 0.9f, 1, 0.1, 0.3, 0.1);
        particleApi.spawnParticles(center.clone().add(0, 1.0, 0),
                Particle.PORTAL, 3, 0.2, 0.4, 0.2, 0.5);
    }

    private void startPortalCheckTask(Player p) {
        if (portalCheckTask != null) portalCheckTask.cancel();
        portalCheckTask = new BukkitRunnable() {
            int lifetime = 0;
            @Override public void run() {
                if (!p.isOnline() || portalA == null || portalB == null || lifetime > 2400) {
                    closePortals();
                    cancel();
                    return;
                }

                checkPortalEntry(p, portalA, portalB);
                checkPortalEntry(p, portalB, portalA);
                lifetime++;
            }
        };
        portalCheckTask.runTaskTimer(magicPlugin, 0, 2);
    }

    private void checkPortalEntry(Player owner, Location entry, Location exit) {
        long now = System.currentTimeMillis();
        for (Entity e : entry.getWorld().getNearbyEntities(entry, 1.4, 2.0, 1.4)) {
            if (e instanceof ArmorStand) continue;
            UUID uid = e.getUniqueId();

            Long lastTp = portalCooldowns.get(uid);
            if (lastTp != null && now - lastTp < PORTAL_CD_MS) continue;

            if (e.equals(owner)) {
                portalCooldowns.put(uid, now);
                Location dest = exit.clone();
                dest.setDirection(e.getLocation().getDirection());
                e.teleport(dest);
                spawnPortalBurst(dest.clone().add(0, 1, 0), 30);
                owner.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
            } else if (e instanceof LivingEntity) {
                portalCooldowns.put(uid, now);
                Location dest = exit.clone().add(0, 0.5, 0);
                e.teleport(dest);
                Vector away = dest.getDirection().multiply(1.5).setY(0.5);
                e.setVelocity(isVecFinite(away) ? away : new Vector(0, 0.5, 0));
                ((LivingEntity) e).damage(8, owner);
                spawnPortalBurst(dest.clone().add(0, 1, 0), 20);
            }
        }
    }

    private void closePortals() {
        portalA = null;
        portalB = null;
        if (portalCheckTask   != null) { portalCheckTask.cancel();   portalCheckTask   = null; }
        if (portalParticleTask!= null) { portalParticleTask.cancel(); portalParticleTask= null; }
        portalCooldowns.clear();
    }

    private void riftPull(Player p) {
        Location center = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 0.5f);
        p.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 30) {

                    riftRelease(center, p);
                    cancel();
                    return;
                }

                double r = 3.5 - t * 0.08;
                particleCircle(center, Math.max(0.3, r), C_PURPLE,  1.2f, 20, t * 12);
                particleCircle(center, Math.max(0.3, r * 0.6), C_GOLD, 1.0f, 12, -(t * 15));
                particleApi.spawnParticles(center, Particle.PORTAL, 15, 0.4, 0.4, 0.4, 1.5);

                for (Entity e : center.getWorld().getNearbyEntities(center, 8, 8, 8)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    Vector pull = center.toVector().subtract(e.getLocation().toVector())
                            .normalize().multiply(0.55);
                    pull.setY(pull.getY() * 0.4);
                    Vector cur = e.getVelocity();
                    e.setVelocity(cur.add(pull));
                    if (t % 5 == 0) ((LivingEntity) e).damage(1.5, p);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void riftRelease(Location center, Player p) {
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_DEATH,   0.7f, 0.7f);

        particleApi.spawnColoredParticles(center, C_PURPLE,   2f,   120, 2.0, 2.0, 2.0);
        particleApi.spawnColoredParticles(center, C_GOLD,     1.8f, 60,  1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(center, C_LAVENDER, 1.2f, 40,  2.5, 2.5, 2.5);
        particleApi.spawnParticles(center, Particle.PORTAL, 200, 1.5, 1.5, 1.5, 3.0);

        for (Entity e : center.getWorld().getNearbyEntities(center, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(center));
            double dmg  = Math.max(8, 24 - dist * 2.5);
            ((LivingEntity) e).damage(dmg, p);
            Vector kb = e.getLocation().subtract(center).toVector().normalize().multiply(2.8).setY(0.7);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.7, 0));
            applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 60, 0);
        }
    }

    private void portalBarrage(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        int bolts = 5;
        int start = -20;
        for (int i = 0; i < bolts; i++) {
            final int yaw = start + i * 10;
            final int delay = i * 4;
            new BukkitRunnable() {
                @Override public void run() {

                    Location spawnLoc = p.getEyeLocation().clone()
                            .add(p.getEyeLocation().getDirection().clone().multiply(2));
                    spawnPortalBurst(spawnLoc, 15);
                    voidBolt(p, 0.6, yaw);
                }
            }.runTaskLater(magicPlugin, delay);
        }
    }

    private void voidStep(Player p) {

        LivingEntity target = null;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        for (double d = 1.0; d <= 20.0; d += 0.5) {
            Location check = eye.clone().add(dir.clone().multiply(d));
            for (Entity e : check.getWorld().getNearbyEntities(check, 1.0, 1.0, 1.0)) {
                if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                target = (LivingEntity) e;
                break;
            }
            if (target != null) break;
        }

        if (target == null) {
            sendActionBar(p, "§cNo target in sight!");
            return;
        }

        Location from = p.getLocation().clone();

        Vector behind = target.getLocation().getDirection().normalize().multiply(1.8);
        Location dest  = target.getLocation().clone().subtract(behind);
        dest.setDirection(target.getLocation().toVector().subtract(dest.toVector()));
        dest.setY(dest.getY() + 0.1);

        spawnPortalBurst(from.clone().add(0, 1, 0), 25);
        p.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);

        p.teleport(dest);

        spawnPortalBurst(dest.clone().add(0, 1, 0), 25);
        p.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);

        target.damage(14, p);
        applyPotion(target, PotionEffectType.SLOWNESS, 40, 2);
        particleApi.spawnParticles(target.getLocation().add(0, 1, 0),
                Particle.PORTAL, 30, 0.3, 0.3, 0.3, 1.0);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7f, 0.8f);
    }

    private void blinkDash(Player p) {
        Location from = p.getLocation().clone();
        Vector dir    = p.getLocation().getDirection().clone().setY(0.1).normalize();

        spawnPhantomAfterimage(from);
        p.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);

        p.setVelocity(dir.clone().multiply(2.8));

        Set<Entity> hit = new HashSet<>();
        Random rng = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 15) { cancel(); return; }

                Location loc = p.getLocation().add(0, 0.9, 0);
                Color c = rng.nextBoolean() ? C_VIOLET : C_PURPLE;
                particleApi.spawnColoredParticles(loc, c, 1.2f, 8, 0.3, 0.2, 0.3);
                particleApi.spawnParticles(loc, Particle.PORTAL, 5, 0.2, 0.2, 0.2, 0.8);
                if (rng.nextInt(3) == 0)
                    particleApi.spawnColoredParticles(loc, C_GOLD, 1.0f, 2, 0.15, 0.1, 0.15);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e);
                    ((LivingEntity) e).damage(10, p);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 50, 2);
                    Vector kb = dir.clone().multiply(1.8).setY(0.4);
                    e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 0.4, 0));
                    loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_HURT, 0.5f, 1.2f);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void spawnPhantomAfterimage(Location loc) {

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 20) { cancel(); return; }
                float alpha = 1.0f - t * 0.05f;
                int pts = (int)(16 * alpha);
                particleCircle(loc.clone().add(0, 0.5, 0), 0.4, C_LAVENDER, alpha + 0.5f, pts, t * 20);
                particleCircle(loc.clone().add(0, 1.0, 0), 0.3, C_VIOLET,   alpha + 0.4f, pts, -(t * 18));
                particleCircle(loc.clone().add(0, 1.6, 0), 0.2, C_PURPLE,   alpha + 0.3f, pts, t * 22);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void voidGate(Player p) {
        if (gateActive) return;
        gateActive = true;

        Location center = p.getLocation().clone().add(
                p.getLocation().getDirection().clone().setY(0).normalize().multiply(5));
        center.setY(p.getLocation().getY());

        p.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1f, 0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.3f);
        p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ 虚空之门 ✦");

        new BukkitRunnable() {
            int t = 0;

            @Override public void run() {
                if (!p.isOnline()) { gateActive = false; cancel(); return; }

                Location gate = center.clone().add(0, 1.2, 0);

                if (t < 40) {
                    double grow = (double) t / 40;
                    double r    = grow * 4.0;

                    particleCircle(gate, r,         C_PURPLE,   1.3f, 24, t * 8);
                    particleCircle(gate.clone().add(0, 0.3, 0), r * 0.7, C_VIOLET, 1.1f, 18, -(t * 10));
                    particleCircle(gate.clone().add(0,-0.3, 0), r * 1.2, C_GOLD,   1.0f, 16, t * 6);

                    if (t % 5 == 0)
                        particleApi.spawnParticles(gate, Particle.PORTAL, 30,
                                (float)(r * 0.6), 0.5f, (float)(r * 0.6), 1.5);

                    pullEntitiesTo(gate, center, 8, 0.25, 0, p);

                    if (t == 39) {
                        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.5f);
                        p.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE,     0.8f, 0.3f);
                    }
                }

                else if (t < 80) {
                    double r = 4.0;
                    particleCircle(gate,                          r,       C_PURPLE,   1.6f, 30, t * 14);
                    particleCircle(gate.clone().add(0, 0.4, 0),  r * 0.7, C_VIOLET,   1.4f, 24, -(t * 16));
                    particleCircle(gate.clone().add(0,-0.4, 0),  r * 0.4, C_GOLD,     1.5f, 16, t * 20);
                    particleCircle(gate.clone().add(0, 0.8, 0),  r * 1.3, C_GOLD_DIM, 1.0f, 20, t * 9);

                    for (int h = 0; h < 5; h++) {
                        Location pillar = gate.clone().add(0, h * 0.5 - 1, 0);
                        particleApi.spawnColoredParticles(pillar, C_PURPLE, 1.3f, 3, 0.15, 0.05, 0.15);
                        particleApi.spawnParticles(pillar, Particle.PORTAL, 5, 0.2, 0.05, 0.2, 0.8);
                    }

                    pullEntitiesTo(gate, center, 12, 0.55, 2.0, p);

                    if (t % 10 == 0) {
                        p.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.3f);
                    }
                    if (t == 79) {

                        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_DEATH, 1f, 0.5f);
                        particleApi.spawnColoredParticles(gate, C_WHITE_VOID, 2f, 200, 3, 3, 3);
                    }
                }

                else {
                    gateCollapse(center, p);
                    gateActive = false;
                    cancel();
                    return;
                }

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void pullEntitiesTo(Location gate, Location center, double radius,
                                double strength, double damagePerCall, Player owner) {
        for (Entity e : gate.getWorld().getNearbyEntities(gate, radius, radius, radius)) {
            if (e.equals(owner) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            Vector pull = center.toVector().subtract(e.getLocation().toVector())
                    .normalize().multiply(strength);
            pull.setY(pull.getY() * 0.3);
            e.setVelocity(e.getVelocity().add(pull));
            if (damagePerCall > 0) ((LivingEntity) e).damage(damagePerCall, owner);
        }
    }

    private void gateCollapse(Location center, Player p) {
        Location gate = center.clone().add(0, 1.2, 0);

        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_DEATH,   1f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH,     0.6f, 0.7f);

        particleApi.spawnColoredParticles(gate, C_PURPLE,   2.5f, 300, 4, 4, 4);
        particleApi.spawnColoredParticles(gate, C_GOLD,     2.0f, 150, 3, 3, 3);
        particleApi.spawnColoredParticles(gate, C_LAVENDER, 1.5f, 100, 5, 5, 5);
        particleApi.spawnParticles(gate, Particle.PORTAL, 500, 3, 3, 3, 4.0);

        for (Entity e : gate.getWorld().getNearbyEntities(gate, 10, 10, 10)) {
            if (e.equals(p) || e instanceof ArmorStand) continue;
            if (!(e instanceof LivingEntity)) continue;
            double dist = Math.max(0.5, e.getLocation().distance(gate));
            double dmg  = Math.max(10, 40 - dist * 2.5);
            ((LivingEntity) e).damage(dmg, p);

            Vector kb = e.getLocation().subtract(gate).toVector().normalize().multiply(4.0).setY(1.0);
            e.setVelocity(isVecFinite(kb) ? kb : new Vector(0, 1.0, 0));
            applyPotion((LivingEntity) e, PotionEffectType.BLINDNESS, 80, 0);
            applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS,  100, 2);
        }

        new BukkitRunnable() {
            double r = 0.5;
            @Override public void run() {
                if (r > 12) { cancel(); return; }
                particleCircle(gate, r, C_PURPLE,   1.3f, (int)(r * 8), 0);
                particleCircle(gate, r * 0.7, C_GOLD, 1.1f, (int)(r * 5), 45);
                r += 0.8;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
    }

    private void passiveDodge(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (isOnCooldown(p_dodge, p)) return;

        Random rng = new Random();
        double angle = rng.nextDouble() * 2 * Math.PI;
        double dist  = 2.0 + rng.nextDouble() * 2.0;
        Location dest = p.getLocation().clone().add(
                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        dest.setY(dest.getY() + 0.1);

        if (!dest.getBlock().isPassable()) return;

        spawnPortalBurst(p.getLocation().clone().add(0, 1, 0), 20);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);
        p.teleport(dest);
        spawnPortalBurst(dest.clone().add(0, 1, 0), 20);

        addCdFixed(p_dodge, p, 6.0);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (isAuraEnabled(p)) {
                    particleCircle(p.getLocation().clone().add(0, 0.05, 0),
                            0.6, C_PURPLE, 0.9f, 5, t * 25);
                    particleApi.spawnColoredParticles(
                            p.getLocation().clone().add(0, 0.1, 0),
                            C_GOLD, 0.8f, 1, 0.25, 0.02, 0.25);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 30);
        return r;
    }

    @Override
    public void remove() {
        closePortals();
        gateActive = false;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§5虚空箭";
            case 1: return "§5相位转移";
            case 2: return "§5镜像传送门";
            case 3: return "§5裂隙牵引";
            case 4: return "§5传送门弹幕";
            case 5: return "§5虚空步";
            case 6: return "§5闪烁冲刺";
            case 7: return "§5§l虚空之门 §6[ULT]";
            default: return "§7none";
        }
    }

    private void spawnPortalBurst(Location loc, int count) {
        particleApi.spawnColoredParticles(loc, C_PURPLE,   1.3f, count / 2, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(loc, C_GOLD,     1.1f, count / 4, 0.4, 0.4, 0.4);
        particleApi.spawnParticles(loc, Particle.PORTAL, count, 0.4, 0.4, 0.4, 1.0);
    }
}

