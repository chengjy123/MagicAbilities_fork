package net.trduc.magicabilitiesfork.powers.custom;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import net.trduc.magicabilitiesfork.data.MessagesManager;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.*;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class SnowpartingBladePower extends Power implements IdlePower, Removeable {

    private static final String sb_slash  = "snowblade.slash";
    private static final String sb_step   = "snowblade.step";
    private static final String sb_drive  = "snowblade.drive";
    private static final String sb_arctic = "snowblade.arctic";
    private static final int XP_SLASH_DEFAULT  = 3;
    private static final int XP_STEP_DEFAULT   = 6;
    private static final int XP_DRIVE_DEFAULT  = 9;
    private static final int XP_ARCTIC_DEFAULT = 12;
    private final int XP_SLASH, XP_STEP, XP_DRIVE, XP_ARCTIC;

    private static final int XP_PER_HIT = 1;
    private static final Color C_ICE_WHITE  = Color.fromRGB(240, 248, 255);
    private static final Color C_ICE_BLUE   = Color.fromRGB(140, 210, 255);
    private static final Color C_ICE_SHARP  = Color.fromRGB(180, 230, 255);
    private static final Color C_SILVER     = Color.fromRGB(200, 215, 230);
    private static final Color C_FROST_EDGE = Color.fromRGB(100, 180, 255);

    private static final Color[] BLADE_COLS = {
            C_ICE_WHITE, C_ICE_BLUE, C_ICE_SHARP, C_SILVER, C_FROST_EDGE
    };
    private boolean driving = false;
    private BukkitRunnable auraTask = null;
    private BukkitRunnable hudTask  = null;
    private final MessagesManager messages = MessagesManager.getInstance();

    public SnowpartingBladePower(Player owner) {
        super(owner);
        org.bukkit.configuration.file.FileConfiguration cfg = magicPlugin.getConfig();
        XP_SLASH  = cfg.getInt("snowblade.xp.slash",  XP_SLASH_DEFAULT);
        XP_STEP   = cfg.getInt("snowblade.xp.step",   XP_STEP_DEFAULT);
        XP_DRIVE  = cfg.getInt("snowblade.xp.drive",  XP_DRIVE_DEFAULT);
        XP_ARCTIC = cfg.getInt("snowblade.xp.arctic", XP_ARCTIC_DEFAULT);
    }
    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DealDamageExecute) {
            gainXp(((DealDamageExecute) ex).getPlayer(), XP_PER_HIT);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p   = ex.getPlayer();
        int    slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0:
                if (onCd(sb_slash, p, this)) return;
                if (!checkXp(p, XP_SLASH, this)) return;
                frostSlash(p);
                spendXp(p, XP_SLASH);
                addCd(sb_slash, p);
                return;
            case 1:
                if (onCd(sb_step, p, this)) return;
                if (!checkXp(p, XP_STEP, this)) return;
                blizzardStep(p);
                spendXp(p, XP_STEP);
                addCd(sb_step, p);
                return;
            case 2:
                if (driving) return;
                if (onCd(sb_drive, p, this)) return;
                if (!checkXp(p, XP_DRIVE, this)) return;
                shatterDrive(p);
                spendXp(p, XP_DRIVE);
                addCd(sb_drive, p);
                return;
            case 3:
                if (onCd(sb_arctic, p, this)) return;
                if (!checkXp(p, XP_ARCTIC, this)) return;
                arcticSeverance(p);
                spendXp(p, XP_ARCTIC);
                addCd(sb_arctic, p);
        }
    }
    private void frostSlash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        int[] damages = {8, 10, 13};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            final int dmg = damages[i];
            new BukkitRunnable() {
                @Override public void run() {
                    if (!p.isOnline()) { cancel(); return; }
                    shootFrostBlade(p, idx * 8.0, dmg);
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,
                            0.5f, 1.4f + idx * 0.15f);
                }
            }.runTaskLater(magicPlugin, idx * 4L);
        }
    }

    private void shootFrostBlade(Player p, double yawOffset, int damage) {
        Vector dir   = yawRotate(p.getEyeLocation().getDirection().clone().normalize(), yawOffset);
        Vector right = yawRotate(dir.clone().setY(0).normalize(), 90).normalize();
        ArmorStand blade = spawnAs(p.getEyeLocation().clone().add(dir.clone().multiply(0.5)));
        Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0; double spin = 0;
            @Override public void run() {
                if (blade.isDead() || t > 28) { safeRemove(blade); cancel(); return; }
                blade.teleport(blade.getLocation().add(dir.clone().multiply(1.6)));
                Location loc = blade.getLocation();
                spin += 35;
                for (int i = -2; i <= 2; i++) {
                    double a = Math.toRadians(spin + i * 18);
                    Location lp = loc.clone()
                            .add(right.clone().multiply(Math.cos(a) * 0.55))
                            .add(0, Math.sin(a) * 0.25, 0);
                    particleApi.spawnColoredParticles(lp, BLADE_COLS[Math.abs(i) % BLADE_COLS.length],
                            1.2f, 2, 0.03, 0.03, 0.03);
                }
                particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.4f, 2, 0.04, 0.04, 0.04);
                if (t % 3 == 0)
                    particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 1, 0.06, 0.06, 0.06, 0.02);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 0.9, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(damage, p);
                    applyFrostSlow(e);
                    slashHitBurst(loc);
                    safeRemove(blade); cancel(); return;
                }
                if (!loc.getBlock().isPassable()) { safeRemove(blade); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void blizzardStep(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 1.8f);

        Vector   dir  = p.getEyeLocation().getDirection().clone().setY(0.1).normalize();
        Location from = p.getLocation().clone();
        Location to   = from.clone();
        for (int i = 0; i < 20; i++) {
            to.add(dir.clone().multiply(0.5));
            if (!to.getBlock().isPassable()) { to.subtract(dir.clone().multiply(0.5)); break; }
        }
        new BukkitRunnable() {
            @Override public void run() {
                int steps = Math.max(1, (int)(from.distance(to) * 5));
                Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur = from.clone().add(0, 1, 0);
                for (int i = 0; i < steps; i++) {
                    particleApi.spawnColoredParticles(cur, BLADE_COLS[i % BLADE_COLS.length],
                            1.1f, 2, 0.06, 0.06, 0.06);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(cur, Particle.SNOWFLAKE, 1, 0.05, 0.05, 0.05, 0.02);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);

        p.teleport(to.clone().add(0, 0.5, 0));
        p.setFallDistance(0);
        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_ICE_WHITE, 1.6f, 20, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_ICE_BLUE,  1.3f, 15, 0.8, 0.8, 0.8);
        particleApi.spawnParticles(to.clone().add(0,1,0), Particle.SNOWFLAKE, 12, 0.7, 0.7, 0.7, 0.1);
        p.getWorld().playSound(to, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f);

        for (int i = 1; i <= 4; i++) {
            double frac = (double) i / 5;
            Location cl = from.clone().add(to.toVector().subtract(from.toVector()).multiply(frac));
            spawnFrostCrystal(cl, p);
        }
    }

    private void spawnFrostCrystal(Location loc, Player owner) {
        Location ground = loc.clone();
        while (ground.getBlock().isPassable() && ground.getY() > 0) ground.add(0, -1, 0);
        ground.add(0, 1.2, 0);
        final Location g = ground.clone();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60) { cancel(); return; }
                if (t % 5 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.toRadians(i * 60 + t * 8);
                        Location lp = g.clone().add(Math.cos(a)*0.35, 0, Math.sin(a)*0.35);
                        particleApi.spawnColoredParticles(lp, BLADE_COLS[i % BLADE_COLS.length],
                                0.95f, 1, 0.03, 0.03, 0.03);
                    }
                    particleApi.spawnColoredParticles(g, C_ICE_WHITE, 1.1f, 1, 0.03, 0.03, 0.03);
                    particleApi.spawnParticles(g, Particle.SNOWFLAKE, 1, 0.1, 0.1, 0.1, 0.02);
                }
                for (Entity e : g.getWorld().getNearbyEntities(g, 1.0, 1.3, 1.0)) {
                    if (e.equals(owner) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(3, owner);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
                    particleApi.spawnColoredParticles(g, C_ICE_WHITE, 1.4f, 15, 0.4, 0.4, 0.4);
                    particleApi.spawnParticles(g, Particle.SNOWFLAKE, 8, 0.3, 0.3, 0.3, 0.1);
                    g.getWorld().playSound(g, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);
                    cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void shatterDrive(Player p) {
        driving = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0.08).normalize();
        p.setVelocity(dir.clone().multiply(2.6));
        Set<UUID> hit = new HashSet<>();
        int[] tArr = {0};

        new BukkitRunnable() {
            @Override public void run() {
                int t = tArr[0];
                if (t > 15 || (p.isOnGround() && t > 2)) {
                    driveImpact(p.getLocation(), p);
                    driving = false;
                    cancel(); return;
                }
                Location loc = p.getLocation().clone().add(0, 0.8, 0);
                for (int side = -1; side <= 1; side += 2) {
                    Vector right = yawRotate(dir.clone().setY(0).normalize(), 90 * side).normalize();
                    for (int j = 0; j < 4; j++) {
                        Location lp = loc.clone().add(right.clone().multiply(j * 0.3 * side));
                        particleApi.spawnColoredParticles(lp, BLADE_COLS[j % BLADE_COLS.length],
                                1.2f, 2, 0.04, 0.06, 0.04);
                    }
                }
                particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.5f, 3, 0.1, 0.1, 0.1);
                if (t % 2 == 0)
                    particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 2, 0.2, 0.2, 0.2, 0.05);
                for (Entity e : loc.getWorld().getNearbyEntities(p.getLocation(), 1.4, 1.4, 1.4)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(14, p);
                    applyFrostSlow(e);
                    e.setVelocity(dir.clone().multiply(1.5).setY(0.5));
                }
                tArr[0]++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void driveImpact(Location loc, Player p) {
        p.setFallDistance(0);
        Location center = loc.clone().add(0, 0.5, 0);
        particleApi.spawnColoredParticles(center, C_ICE_WHITE, 2f,   50, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(center, C_ICE_BLUE,  1.7f, 25, 2.0, 2.0, 2.0);
        particleApi.spawnColoredParticles(center, C_ICE_SHARP, 1.5f, 15, 2.5, 2.5, 2.5);
        particleApi.spawnParticles(center, Particle.SNOWFLAKE, 20, 2.5, 1.5, 2.5, 0.2);
        p.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 0.4f);

        new BukkitRunnable() {
            double r = 0.3; int t = 0;
            @Override public void run() {
                if (r > 4) { cancel(); return; }
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5 + t * 10);
                    Location rp = center.clone().add(Math.cos(a)*r, 0.1, Math.sin(a)*r);
                    particleApi.spawnColoredParticles(rp,
                            t%3==0 ? C_ICE_WHITE : t%3==1 ? C_ICE_BLUE : C_ICE_SHARP,
                            1.1f, 1, 0.04, 0.04, 0.04);
                }
                r += 0.5; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        for (Entity e : center.getWorld().getNearbyEntities(center, 3.5, 3.5, 3.5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(center);
            ((LivingEntity) e).damage(Math.max(6, 18 - dist * 3.0), p);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   40, 9, false, false));
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128, false, false));
            e.setVelocity(e.getLocation().subtract(center).toVector().normalize().multiply(1.2).setY(0.4));
        }
    }
    private void arcticSeverance(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.4f);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.get("powers.snowparting_blade.arctic_severance")));

        List<ArmorStand> orbs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(i * 72);
            Location op = p.getLocation().clone().add(0, 1.3, 0)
                    .add(Math.cos(a)*2.0, 0, Math.sin(a)*2.0);
            orbs.add(spawnAs(op));
        }

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 40) { cancel(); launchSeverance(p, orbs); return; }
                Location center = p.getLocation().clone().add(0, 1.3, 0);
                for (int i = 0; i < orbs.size(); i++) {
                    ArmorStand orb = orbs.get(i);
                    if (orb.isDead()) continue;
                    double a = Math.toRadians(i * 72 + t * 9);
                    orb.teleport(center.clone().add(Math.cos(a)*2.0, 0, Math.sin(a)*2.0));
                    Location tl = orb.getLocation();
                    Color c = BLADE_COLS[i % BLADE_COLS.length];
                    for (int wing = -1; wing <= 1; wing++) {
                        double wa = Math.toRadians(wing * 25);
                        Location wl = tl.clone().add(Math.cos(a+wa)*0.4,
                                Math.sin(t*0.15+i)*0.2, Math.sin(a+wa)*0.4);
                        particleApi.spawnColoredParticles(wl, c, 1.2f, 2, 0.04, 0.04, 0.04);
                    }
                    particleApi.spawnColoredParticles(tl, C_ICE_WHITE, 1.4f, 1, 0.03, 0.03, 0.03);
                    if (t % 4 == 0)
                        particleApi.spawnParticles(tl, Particle.SNOWFLAKE, 1, 0.05, 0.05, 0.05, 0.02);
                }
                if (t == 20) p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 0.8f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void launchSeverance(Player p, List<ArmorStand> orbs) {
        Location target = p.getEyeLocation().clone();
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < 50; i++) {
            target.add(dir.clone().multiply(0.5));
            if (!target.getBlock().isPassable()) { target.subtract(dir.clone().multiply(0.5)); break; }
        }
        final Location ft = target.clone();
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.3f);

        Set<UUID> hit = new HashSet<>();
        for (int i = 0; i < orbs.size(); i++) {
            final int idx = i;
            final ArmorStand orb = orbs.get(i);
            new BukkitRunnable() {
                @Override public void run() {
                    if (orb.isDead()) { cancel(); return; }
                    Vector toTarget = ft.toVector().subtract(orb.getLocation().toVector());
                    double dist = toTarget.length();
                    if (dist < 1.2) {
                        arcticSeveranceHit(ft, p, orb, hit, idx == orbs.size()-1);
                        cancel(); return;
                    }
                    orb.teleport(orb.getLocation().add(toTarget.normalize().multiply(Math.min(2.2, dist))));
                    Location loc = orb.getLocation();
                    Color c = BLADE_COLS[idx % BLADE_COLS.length];
                    for (int w = -2; w <= 2; w++) {
                        double wa = Math.toRadians(w * 20 + idx * 72);
                        Location wl = loc.clone().add(Math.cos(wa)*0.4, Math.sin(wa*0.5)*0.2, Math.sin(wa)*0.4);
                        particleApi.spawnColoredParticles(wl, c, 1.2f, 2, 0.04, 0.04, 0.04);
                    }
                    particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.5f, 2, 0.03, 0.03, 0.03);
                    particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 1, 0.04, 0.04, 0.04, 0.02);
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                        if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                        if (!(e instanceof LivingEntity)) continue;
                        hit.add(e.getUniqueId());
                        ((LivingEntity) e).damage(20, p);
                        applyFrostSlow(e);
                        slashHitBurst(loc);
                    }
                }
            }.runTaskTimer(magicPlugin, idx * 2L, 1);
        }
    }

    private void arcticSeveranceHit(Location loc, Player p, ArmorStand orb,
                                    Set<UUID> hit, boolean isLast) {
        safeRemove(orb);
        slashHitBurst(loc);
        if (!isLast) return;

        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.2f);
        p.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.3f);
        particleApi.spawnColoredParticles(loc, C_ICE_WHITE,  2.5f, 50, 2.5, 2.5, 2.5);
        particleApi.spawnColoredParticles(loc, C_ICE_BLUE,   2f,   40, 3.0, 3.0, 3.0);
        particleApi.spawnColoredParticles(loc, C_ICE_SHARP,  1.8f, 15, 3.5, 3.5, 3.5);
        particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 30, 3.5, 3.0, 3.5, 0.3);
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(i * 72);
            particleApi.drawColoredLine(loc, loc.clone().add(Math.cos(a)*3.5, 0.1, Math.sin(a)*3.5),
                    1.5, BLADE_COLS[i % BLADE_COLS.length], 1.2f, 0);
        }
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
            if (!(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            ((LivingEntity) e).damage(Math.max(8, 35 - dist * 4.5), p);
            applyFrostSlow(e);
            if (dist < 2.5) {
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   60, 9, false, false));
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 128, false, false));
            }
            e.setVelocity(e.getLocation().subtract(loc).toVector().normalize().multiply(1.8).setY(0.5));
        }
    }
    private void showHud(Player p) {
        int xp = p.getTotalExperience();

        String s0 = slotHud(p, sb_slash,  "S0", XP_SLASH,  xp);
        String s1 = slotHud(p, sb_step,   "S1", XP_STEP,   xp);
        String s2 = slotHud(p, sb_drive,  "S2", XP_DRIVE,  xp);
        String s3 = slotHud(p, sb_arctic, "S3", XP_ARCTIC, xp);

        String hud = s0 + " " + s1 + " " + s2 + " " + s3
                + ChatColor.GRAY + "  │  "
                + ChatColor.AQUA + "XP:" + ChatColor.WHITE + xp;

        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(hud));
    }
    private String slotHud(Player p, String cdKey, String label, int xpCost, int xp) {
        boolean onCd  = CooldownApi.isOnCooldown(cdKey, p);
        boolean hasXp = xp >= xpCost;

        if (onCd) {
            long ms   = CooldownApi.getCooldownForPlayerLong(cdKey, p);
            String s  = (float)((int)(ms / 100)) / 10 + "s";
            return ChatColor.RED + "[" + label + " " + s + "]";
        }
        if (!hasXp) {
            return ChatColor.GOLD + "[" + label + " !" + xpCost + "xp]";
        }
        return ChatColor.GREEN + "[" + label + " ✓]";
    }
    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        auraTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (isAuraEnabled(p)) {
                    Location center = p.getLocation().clone().add(0, 1.1, 0);
                    for (int i = 0; i < 6; i++) {
                        double a = Math.toRadians(i * 60 + t * 45);
                        double yOsc = Math.sin(t * 0.8 + i) * 0.2;
                        Location lp = center.clone().add(Math.cos(a)*1.05, yOsc, Math.sin(a)*1.05);
                        particleApi.spawnParticles(lp, Particle.SNOWFLAKE, 1, 0.03, 0.03, 0.03, 0.01);
                        particleApi.spawnColoredParticles(lp, BLADE_COLS[i % BLADE_COLS.length],
                                0.85f, 1, 0.03, 0.03, 0.03);
                    }
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45 - t * 60);
                        Location lp = center.clone()
                                .add(Math.cos(a)*0.65, 0.5 + Math.sin(a*0.5)*0.12, Math.sin(a)*0.65);
                        particleApi.spawnColoredParticles(lp, C_ICE_BLUE, 0.8f, 1, 0.03, 0.03, 0.03);
                    }
                }
                t++;
            }
        };
        auraTask.runTaskTimer(magicPlugin, 0, 20);
        hudTask = new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                showHud(p);
            }
        };
        hudTask.runTaskTimer(magicPlugin, 0, 4);
        return auraTask;
    }
    @Override
    public void remove() {
        driving = false;
        if (auraTask != null) { try { auraTask.cancel(); } catch (Exception ignored) {} auraTask = null; }
        if (hudTask  != null) { try { hudTask.cancel();  } catch (Exception ignored) {} hudTask  = null; }
    }
    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&b霜斩 &7(" + XP_SLASH   + "xp)";
            case 1: return "&b暴风雪步 &7(" + XP_STEP  + "xp)";
            case 2: return "&b破碎驱动 &7(" + XP_DRIVE + "xp)";
            case 3: return "&b&l极地切断 &7(" + XP_ARCTIC + "xp)";
            default: return "&7none";
        }
    }

    private void gainXp(Player p, int amount) {
        p.giveExp(amount);
    }

    private void applyFrostSlow(Entity e) {
        if (!(e instanceof LivingEntity)) return;
        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, true));
    }

    private void slashHitBurst(Location loc) {
        particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.5f, 12, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_ICE_BLUE,  1.2f,  8, 0.4, 0.4, 0.4);
        particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 6, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.6f);
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
    }

    private Vector yawRotate(Vector v, double deg) {
        double rad = Math.toRadians(deg);
        return new Vector(
                v.getX() * Math.cos(rad) + v.getZ() * Math.sin(rad),
                v.getY(),
                -v.getX() * Math.sin(rad) + v.getZ() * Math.cos(rad));
    }
}

