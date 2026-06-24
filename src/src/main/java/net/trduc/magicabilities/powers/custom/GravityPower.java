package net.trduc.magicabilities.powers.custom;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilities.cooldowns.CooldownApi;
import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
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

public class GravityPower extends Power implements IdlePower, Removeable {
    private static final String g_pull    = "gravity.pull";
    private static final String g_push    = "gravity.push";
    private static final String g_crush   = "gravity.crush";
    private static final String g_field   = "gravity.field";
    private static final String g_reverse = "gravity.reverse";
    private static final String g_collapse= "gravity.collapse";
    private static final Color C_PURPLE      = Color.fromRGB(140,  60, 220);
    private static final Color C_PURPLE_LIGHT= Color.fromRGB(190, 120, 255);
    private static final Color C_PURPLE_DARK = Color.fromRGB( 70,  20, 130);
    private static final Color C_VOID        = Color.fromRGB( 30,  10,  60);
    private static final Color C_SILVER      = Color.fromRGB(180, 180, 200);
    private static final Color C_WHITE       = Color.fromRGB(230, 220, 255);

    private static final Color[] AURA_COLS = {
            C_PURPLE_LIGHT, C_SILVER, C_WHITE,
    };
    private boolean fieldActive   = false;
    private boolean reversed      = false;
    private boolean collapsing    = false;
    private BukkitRunnable fieldTask    = null;
    private BukkitRunnable reverseTask  = null;
    private BukkitRunnable collapseTask = null;
    private BukkitRunnable hudTask      = null;
    private final Set<UUID> crushedSet = new HashSet<>();
    public GravityPower(Player owner) { super(owner); }
    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute)   { onDamaged((DamagedExecute) ex);   return; }
        if (ex instanceof DamagedByExecute) {                                    return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) { onLeft((LeftClickExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p    = ex.getPlayer();
        int    slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(g_pull, p, this)) return; gravitationalPull(p);    addCd(g_pull, p);    return;
            case 1: if (onCd(g_push, p, this)) return; gravitationalPush(p);    addCd(g_push, p);    return;
            case 2: if (onCd(g_crush, p, this)) return; gravityCrush(p);         addCd(g_crush, p);   return;
            case 3: {
                if (fieldActive) { hud(p, "The gravity zone is active!"); return; }
                if (onCd(g_field, p, this)) return;
                gravityField(p);
                addCd(g_field, p);
                return;
            }
            case 4: {
                if (reversed) { hud(p, "Currently in reverse gravity state!"); return; }
                if (onCd(g_reverse, p, this)) return;
                reverseGravity(p);
                addCd(g_reverse, p);
                return;
            }
            case 5: {
                if (collapsing) { hud(p, "Currently collapsing space!"); return; }
                if (onCd(g_collapse, p, this)) return;
                spaceCollapse(p);
                addCd(g_collapse, p);
            }
        }
    }
    private void gravitationalPull(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT,  0.8f, 0.5f);
        p.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,     0.5f, 0.4f);
        hud(p, org.bukkit.ChatColor.LIGHT_PURPLE + "✦ Gravitational Pull!");
        new BukkitRunnable() {
            double r = 8.0; int t = 0;
            @Override public void run() {
                if (r < 0.3) { cancel(); return; }
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5 + t * 12);
                    particleApi.spawnColoredParticles(
                            loc.clone().add(Math.cos(a)*r, Math.sin(t*0.15)*0.5, Math.sin(a)*r),
                            AURA_COLS[i % AURA_COLS.length], 1.1f, 1, 0.04, 0.04, 0.04);
                }
                if (t % 3 == 0)
                    particleApi.spawnParticles(loc, Particle.REVERSE_PORTAL, 3, (float)r*0.1f, 0.2f, (float)r*0.1f, 0.3f);
                r -= 0.55; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
        final org.bukkit.World world = p.getWorld();
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : world.getNearbyEntities(loc, 8, 4, 8)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    Vector toPlayer = loc.clone().subtract(e.getLocation()).toVector();
                    double dist = toPlayer.length();
                    if (dist < 0.5) continue;
                    double strength = Math.min(2.5, 1.0 + (dist / 8.0) * 1.5);
                    e.setVelocity(toPlayer.normalize().multiply(strength).setY(Math.min(0.4, toPlayer.normalize().getY() * strength + 0.15)));
                    ((LivingEntity) e).damage(6.0, p);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_PURPLE_LIGHT, 1.3f, 6, 0.2, 0.2, 0.2);
                }
                world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.5f);
            }
        }.runTaskLater(magicPlugin, 3L);
    }
    private void gravitationalPush(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,    0.7f, 1.4f);
        p.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM,  0.9f, 1.3f);
        hud(p, org.bukkit.ChatColor.LIGHT_PURPLE + "✦ Thrust!");
        new BukkitRunnable() {
            double r = 0.3; int t = 0;
            @Override public void run() {
                if (r > 7.0) { cancel(); return; }
                for (int i = 0; i < 18; i++) {
                    double a = Math.toRadians(i * 20 + t * 10);
                    Color c = t % 3 == 0 ? C_PURPLE : t % 3 == 1 ? C_SILVER : C_WHITE;
                    particleApi.spawnColoredParticles(
                            loc.clone().add(Math.cos(a)*r, Math.sin(a*0.4)*0.5, Math.sin(a)*r),
                            c, 1.2f, 1, 0.05, 0.05, 0.05);
                }
                particleApi.spawnParticles(loc, Particle.REVERSE_PORTAL,
                        4, (float)r*0.12f, 0.1f, (float)r*0.12f, 0.4f);
                r += 0.7; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        final org.bukkit.World world = p.getWorld();
        new BukkitRunnable() {
            @Override public void run() {
                Random rng = new Random();
                for (Entity e : world.getNearbyEntities(loc, 6, 3, 6)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    Vector away = e.getLocation().subtract(loc).toVector();
                    if (away.lengthSquared() < 0.01)
                        away = new Vector(rng.nextDouble()-0.5, 0.3, rng.nextDouble()-0.5);
                    double dist     = away.length();
                    double strength = Math.max(0.8, 2.2 - dist * 0.25);
                    e.setVelocity(away.normalize().multiply(strength).setY(0.55));
                    ((LivingEntity) e).damage(8.0, p);
                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_PURPLE_LIGHT, 1.4f, 8, 0.25, 0.25, 0.25);
                }
            }
        }.runTaskLater(magicPlugin, 2L);
    }
    private void gravityCrush(Player p) {
        Location loc = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND,          0.6f, 0.5f);
        p.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT,  0.5f, 0.3f);
        hud(p, org.bukkit.ChatColor.LIGHT_PURPLE + "✦ Gravity Crush!");
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 15) { cancel(); return; }
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(i * 36 + t * 20);
                    double r = 4.5;
                    Location top = loc.clone().add(Math.cos(a)*r, 3.5 - t*0.2, Math.sin(a)*r);
                    particleApi.spawnColoredParticles(top, C_PURPLE_DARK, 1.2f, 2, 0.1, 0.1, 0.1);
                    particleApi.spawnParticles(top, Particle.REVERSE_PORTAL, 1, 0.1, 0.1, 0.1, 0.3f);
                }
                particleApi.spawnColoredParticles(loc.clone().add(0, 2.5 - t*0.15, 0),
                        C_PURPLE, 1.5f, 3, 0.3, 0.1, 0.3);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        final org.bukkit.World world = p.getWorld();
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity e : world.getNearbyEntities(loc, 5, 4, 5)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    if (crushedSet.contains(e.getUniqueId())) continue;
                    Vector cur = e.getVelocity();
                    e.setVelocity(new Vector(cur.getX() * 0.2, -2.5, cur.getZ() * 0.2));

                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,       60, 3, false, true));
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 2, false, true));
                    ((LivingEntity) e).damage(10.0, p);

                    crushedSet.add(e.getUniqueId());
                    final UUID uid = e.getUniqueId();
                    new BukkitRunnable() {
                        @Override public void run() { crushedSet.remove(uid); }
                    }.runTaskLater(magicPlugin, 40L);

                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_VOID, 1.6f, 10, 0.2, 0.3, 0.2);
                    e.getWorld().playSound(e.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 1.5f);
                }
            }
        }.runTaskLater(magicPlugin, 8L);
    }

    private void gravityField(Player p) {
        fieldActive = true;
        final Location fieldCenter = p.getLocation().clone().add(0, 0.5, 0);
        final org.bukkit.World world = p.getWorld();

        p.getWorld().playSound(fieldCenter, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.4f);
        p.getWorld().playSound(fieldCenter, Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.5f);
        hud(p, org.bukkit.ChatColor.LIGHT_PURPLE + "✦ Gravity Field!");

        fieldTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 80) {
                    fieldActive = false; fieldTask = null;
                    world.playSound(fieldCenter, Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.5f);
                    cancel(); return;
                }
                for (int i = 0; i < 20; i++) {
                    double a     = Math.toRadians(i * 18 + t * 8);
                    double pulse = 5.0 + Math.sin(t * 0.25) * 0.3;
                    particleApi.spawnColoredParticles(
                            fieldCenter.clone().add(Math.cos(a)*pulse, 0.05, Math.sin(a)*pulse),
                            AURA_COLS[i % AURA_COLS.length], 1.0f, 1, 0.04, 0.04, 0.04);
                }
                for (double y = 0; y <= 3.5; y += 0.5) {
                    double a = Math.toRadians(t * 18 + y * 60);
                    particleApi.spawnColoredParticles(
                            fieldCenter.clone().add(Math.cos(a)*0.5, y, Math.sin(a)*0.5),
                            C_PURPLE, 1.1f, 1, 0.04, 0.04, 0.04);
                }
                if (t % 3 == 0)
                    particleApi.spawnParticles(fieldCenter.clone().add(0, 1, 0),
                            Particle.REVERSE_PORTAL, 4, 4.5f, 1.0f, 4.5f, 0.05f);
                if (t % 10 == 0) {
                    for (Entity e : world.getNearbyEntities(fieldCenter, 5, 4, 5)) {
                        if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                        Vector cur = e.getVelocity();
                        e.setVelocity(new Vector(cur.getX() * 0.7, Math.min(cur.getY(), 0) - 0.7, cur.getZ() * 0.7));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 1, false, false));
                        ((LivingEntity) e).damage(3.0, p);
                    }
                    world.playSound(fieldCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.4f);
                }

                if (t % 20 == 0)
                    world.playSound(fieldCenter, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 0.5f);
                t++;
            }
        };
        fieldTask.runTaskTimer(magicPlugin, 0, 1);
    }
    private void reverseGravity(Player p) {
        reversed = true;
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setVelocity(new Vector(0, 2.5, 0));

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT,  1f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,     0.7f, 1.8f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 110, 1, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 110, 3, false, false));
        hud(p, org.bukkit.ChatColor.LIGHT_PURPLE + "✦ Reverse Gravity!");

        reverseTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) {
                    cleanupReverse(p); cancel(); return;
                }
                if (t >= 100) {
                    cleanupReverse(p); cancel(); return;
                }
                Location center = p.getLocation().clone().add(0, 0.5, 0);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 - t * 15);
                    particleApi.spawnColoredParticles(
                            center.clone().add(Math.cos(a)*1.0, Math.sin(a*0.5)*0.5, Math.sin(a)*1.0),
                            AURA_COLS[i % AURA_COLS.length], 1.1f, 1, 0.03, 0.03, 0.03);
                }
                if (t % 4 == 0)
                    particleApi.spawnParticles(center, Particle.REVERSE_PORTAL, 2, 0.5f, 0.3f, 0.5f, 0.1f);
                if (t % 20 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.2f, 1.6f);
                t++;
            }
        };
        reverseTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void cleanupReverse(Player p) {
        reversed = false;
        BukkitRunnable rt = reverseTask; reverseTask = null;
        if (rt != null) { try { rt.cancel(); } catch (Exception ignored) {} }
        if (p.isOnline()) {
            if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            p.removePotionEffect(PotionEffectType.SPEED);
            p.removePotionEffect(PotionEffectType.JUMP_BOOST);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.6f);
            hud(p, org.bukkit.ChatColor.GRAY + "Gravity restored.");
        }
    }

    private void spaceCollapse(Player p) {
        collapsing = true;
        final Location epicenter = p.getLocation().clone().add(0, 1, 0);
        final org.bukkit.World world = p.getWorld();

        p.getWorld().playSound(epicenter, Sound.ENTITY_WARDEN_SONIC_BOOM,    1f, 0.3f);
        p.getWorld().playSound(epicenter, Sound.BLOCK_BEACON_ACTIVATE,        1f, 0.2f);
        p.getWorld().playSound(epicenter, Sound.ENTITY_ENDERMAN_TELEPORT,     0.8f, 0.3f);
        p.sendMessage(org.bukkit.ChatColor.DARK_PURPLE + "" + org.bukkit.ChatColor.BOLD + "⚫SPACE COLLAPSE!");
        hud(p, org.bukkit.ChatColor.DARK_PURPLE + "⚫ Collapsing In progress...");

        collapseTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) {
                    collapsing = false; collapseTask = null; cancel(); return;
                }
                if (t < 30) {
                    double r = 12.0 - t * 0.3;
                    for (int i = 0; i < 24; i++) {
                        double a = Math.toRadians(i * 15 + t * 15);
                        particleApi.spawnColoredParticles(
                                epicenter.clone().add(Math.cos(a)*r, Math.sin(t*0.1+i*0.4)*0.6, Math.sin(a)*r),
                                t < 15 ? AURA_COLS[i%AURA_COLS.length] : C_VOID, 1.3f, 1, 0.05, 0.05, 0.05);
                    }
                    if (t % 5 == 0) {
                        for (Entity e : world.getNearbyEntities(epicenter, 12, 6, 12)) {
                            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                            Vector toCenter = epicenter.clone().subtract(e.getLocation()).toVector();
                            double dist = toCenter.length();
                            if (dist < 0.5) continue;
                            e.setVelocity(toCenter.normalize().multiply(1.8).setY(Math.min(0.5, toCenter.normalize().getY()*1.8 + 0.1)));
                        }
                    }
                    particleApi.spawnParticles(epicenter, Particle.REVERSE_PORTAL,
                            10, (float)(r*0.08f), 0.5f, (float)(r*0.08f), 0.2f);

                    if (t % 8 == 0)
                        world.playSound(epicenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f + t*0.02f);

                    t++;
                    return;
                }
                collapsing = false; collapseTask = null;
                cancel();
                for (int ring = 0; ring < 6; ring++) {
                    final int fr = ring;
                    new BukkitRunnable() {
                        int rt = 0;
                        @Override public void run() {
                            if (rt >= 8) { cancel(); return; }
                            double r = fr * 1.5 + rt * 0.3;
                            for (int i = 0; i < 20; i++) {
                                double a = Math.toRadians(i * 18 + rt * 12);
                                particleApi.spawnColoredParticles(
                                        epicenter.clone().add(Math.cos(a)*r, Math.sin(a*0.3)*0.8, Math.sin(a)*r),
                                        fr < 2 ? C_VOID : fr < 4 ? C_PURPLE_DARK : C_PURPLE_LIGHT,
                                        1.4f, 2, 0.08, 0.08, 0.08);
                            }
                            particleApi.spawnParticles(epicenter.clone().add(0, fr*0.2, 0),
                                    Particle.REVERSE_PORTAL, 6, (float)r*0.1f, 0.3f, (float)r*0.1f, 0.5f);
                            rt++;
                        }
                    }.runTaskTimer(magicPlugin, fr * 3L, 1);
                }

                world.playSound(epicenter, Sound.ENTITY_GENERIC_EXPLODE,     1f, 0.5f);
                world.playSound(epicenter, Sound.ENTITY_WARDEN_SONIC_BOOM,   1f, 0.6f);
                world.playSound(epicenter, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.7f);
                Random rng = new Random();
                for (Entity e : world.getNearbyEntities(epicenter, 12, 6, 12)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;
                    double dist = e.getLocation().distance(epicenter);
                    double dmg = (15.0 + le.getHealth() * 0.20) * Math.max(0.4, 1.0 - dist / 14.0);
                    le.damage(dmg, p);

                    Vector away = e.getLocation().subtract(epicenter).toVector();
                    if (away.lengthSquared() < 0.01)
                        away = new Vector(rng.nextDouble()-0.5, 0.5, rng.nextDouble()-0.5);
                    double pushStr = Math.max(0.5, 2.8 - dist * 0.18);
                    e.setVelocity(away.normalize().multiply(pushStr).setY(0.8));

                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,  40, 0, false, true));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   60, 2, false, true));

                    particleApi.spawnColoredParticles(e.getLocation().clone().add(0,1,0),
                            C_PURPLE_LIGHT, 1.8f, 15, 0.4, 0.4, 0.4);
                }

                hud(p, org.bukkit.ChatColor.DARK_PURPLE + "⚫ Space collapse!");
            }
        };
        collapseTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void onDamaged(DamagedExecute ex) {
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();
        Player p = ex.getPlayer();
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (reversed && reverseTask != null) {
            cleanupReverse(p);
            p.sendMessage(org.bukkit.ChatColor.LIGHT_PURPLE + "The law of reverse gravity is broken.!");
        }
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
            Entity attacker = ((org.bukkit.event.entity.EntityDamageByEntityEvent) event).getDamager();
            if (attacker instanceof LivingEntity && !attacker.equals(p)) {
                Vector away = attacker.getLocation().subtract(p.getLocation()).toVector();
                if (away.lengthSquared() > 0.01)
                    attacker.setVelocity(away.normalize().multiply(1.4).setY(0.5));
                particleApi.spawnColoredParticles(attacker.getLocation().clone().add(0,1,0),
                        C_PURPLE, 1.3f, 8, 0.2, 0.2, 0.2);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.4f);
            }
        }
    }
    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p   = ex.getPlayer();
        final Random rng = new Random();

        hudTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (isAuraEnabled(p)) {
                    Location center = p.getLocation().clone().add(0, 1.0, 0);
                    for (int i = 0; i < 8; i++) {
                        double a    = Math.toRadians(i * 45 + t * 7);
                        double yOsc = Math.sin(t * 0.08 + i * 0.8) * 0.3;
                        particleApi.spawnColoredParticles(
                                center.clone().add(Math.cos(a)*1.2, yOsc, Math.sin(a)*1.2),
                                AURA_COLS[i % AURA_COLS.length], 1.0f, 1, 0.03, 0.03, 0.03);
                    }
                    for (int i = 0; i < 5; i++) {
                        double a = Math.toRadians(i * 72 - t * 10);
                        particleApi.spawnColoredParticles(
                                center.clone().add(Math.cos(a)*0.65, Math.sin(a*0.4)*0.2+0.3, Math.sin(a)*0.65),
                                i%2==0 ? C_PURPLE_DARK : C_VOID, 0.85f, 1, 0.03, 0.03, 0.03);
                    }
                    if (t % 5 == 0)
                        particleApi.spawnParticles(center, Particle.REVERSE_PORTAL,
                                2, 0.5f, 0.4f, 0.5f, 0.04f);
                    if (t % 4 == 0)
                        particleApi.spawnParticles(
                                center.clone().add((rng.nextDouble()-0.5)*1.4, rng.nextDouble()*0.25-0.05, (rng.nextDouble()-0.5)*1.4),
                                Particle.DRAGON_BREATH, 1, 0.04, 0.04, 0.04, 0.1f);
                    if (t % 6 == 0) {
                        for (int i = 0; i < 6; i++) {
                            double a  = Math.toRadians(i * 60 + t * 5);
                            Location fp = p.getLocation().clone().add(Math.cos(a)*0.85, 0.03, Math.sin(a)*0.85);
                            particleApi.spawnColoredParticles(fp, C_PURPLE_DARK, 0.9f, 1, 0.04, 0.01, 0.04);
                        }
                    }

                    if (t % 120 == 0)
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.12f, 0.7f);
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
        fieldActive = false;
        collapsing  = false;

        if (fieldTask != null) {
            BukkitRunnable ft = fieldTask; fieldTask = null;
            try { ft.cancel(); } catch (Exception ignored) {}
        }
        if (reversed) cleanupReverse(getOwner());
        if (reverseTask != null) {
            BukkitRunnable rt = reverseTask; reverseTask = null;
            try { rt.cancel(); } catch (Exception ignored) {}
        }
        if (collapseTask != null) {
            BukkitRunnable ct = collapseTask; collapseTask = null;
            try { ct.cancel(); } catch (Exception ignored) {}
        }
        if (hudTask != null) {
            BukkitRunnable ht = hudTask; hudTask = null;
            try { ht.cancel(); } catch (Exception ignored) {}
        }
        crushedSet.clear();
    }
    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§d⬤ Gravitational Force";
            case 1: return "§d◎ Repulsive Force";
            case 2: return "§5▼ Crushing";
            case 3: return "§5⬡ Gravitational Region";
            case 4: return "§b↑ Reverse Gravity";
            case 5: return "§4§l⚫ Space Collapse";
            default: return "§7none";
        }
    }
    private void hud(Player p, String msg) {
        String state = reversed ? " §b[REVERSE GRAVITY]" : fieldActive ? " §5[ACTIVE ZONE]" : "";
        String bar   = "§5⚫ Gravitational Field" + state;
        String m     = msg != null ? " §r§f" + msg : "";
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar + m));
    }
}
