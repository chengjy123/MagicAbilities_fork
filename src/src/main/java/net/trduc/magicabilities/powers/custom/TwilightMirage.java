package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.cooldowns.CooldownApi;
import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.executions.*;
import net.trduc.magicabilities.powers.executions.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilities.MagicAbilities.magicPlugin;
import static net.trduc.magicabilities.misc.PowerUtils.*;
import static net.trduc.magicabilities.MagicAbilities.particleApi;
import static net.trduc.magicabilities.cooldowns.Cooldowns.cooldowns;
import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.misc.GeneralMethods.rotateVector;
import static net.trduc.magicabilities.players.PowerPlayer.players;

public class TwilightMirage extends Power implements IdlePower {
    private static final String tm_shriek   = "twilight-mirage.shriek-transition";
    private static final String tm_float    = "twilight-mirage.float";
    private static final String tm_missile  = "twilight-mirage.missile";
    private static final String tm_healing  = "twilight-mirage.healing";
    private static final String tm_eclipse  = "twilight-mirage.eclipse";
    private static final String tm_neardeath = "TM-0";
    private static final Color C_TM_PURPLE  = Color.fromRGB(180,  60, 255);
    private static final Color C_TM_TEAL    = Color.fromRGB(  0, 245, 215);
    private static final Color C_TM_LAVENDER= Color.fromRGB(230, 190, 255);
    private static final Color C_TM_DEEP    = Color.fromRGB( 80,   0, 160);
    private static final Color C_TM_CYAN    = Color.fromRGB( 50, 200, 255);
    private static final Color C_TM_WHITE   = Color.fromRGB(240, 230, 255);

    private static final Color[] MISSILE_COLORS = {
            Color.fromRGB(255, 59, 232),
            Color.fromRGB(0, 255, 217),
            Color.fromRGB(240, 214, 255)
    };
    private static final Color[] ECLIPSE_COLORS = {
            C_TM_PURPLE, C_TM_TEAL, C_TM_LAVENDER, C_TM_CYAN
    };

    public TwilightMirage(Player owner) { super(owner); }
    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute)  { onDamagedBy((DamagedByExecute) ex); return; }
        if (ex instanceof DamagedExecute)    { onDamaged((DamagedExecute) ex);     return; }
        if (ex instanceof DealDamageExecute) { onDamage((DealDamageExecute) ex);   return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { executeLeftClick((LeftClickExecute) ex); }
    }

    private void executeLeftClick(LeftClickExecute execute) {
        final Player p = execute.getPlayer();
        if (!p.equals(getOwner()))
            throw new RuntimeException("Event player does not match the power owner!");

        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0:
                if (onCd(tm_shriek, p, this)) return;
                shriekTransition(p);
                addCd(tm_shriek, p);
                return;
            case 1:
                if (onCd(tm_float, p, this)) return;
                twilightLeap(p);
                addCd(tm_float, p);
                return;
            case 2:
                if (onCd(tm_missile, p, this)) return;
                if (p.isSneaking()) {
                    for (int deg : new int[]{-18, 0, 18})
                        magicMissile(p, deg);
                    addCd(tm_missile, p, 2.0);
                } else {
                    magicMissile(p, 0);
                    addCd(tm_missile, p);
                }
                return;
            case 3:
                if (onCd(tm_healing, p, this)) return;
                healingMirage(p);
                addCd(tm_healing, p);
                return;
            case 4:
                if (onCd(tm_eclipse, p, this)) return;
                phantomEclipse(p);
                addCd(tm_eclipse, p);
        }
    }

    private void shriekTransition(Player p) {
        boolean night = isNight(p);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 2f);

        Location from = p.getLocation().clone();
        Location l    = p.getLocation().clone().add(0, 1, 0);
        Vector v      = p.getLocation().getDirection().clone().normalize();
        int maxSteps  = 20;

        while (maxSteps > 0
                && l.clone().add(v).getBlock().isPassable()
                && l.clone().add(v).add(0, 1, 0).getBlock().isPassable()) {
            l.add(v);
            maxSteps--;
        }

        p.teleport(l);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.4f);
        new BukkitRunnable() {
            @Override public void run() {
                int steps = Math.max(4, (int) from.distance(l) * 3);
                Vector step = l.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur = from.clone().add(0, 1, 0);
                Random r = new Random();

                for (int i = 0; i < steps; i++) {
                    particleApi.spawnParticles(cur, Particle.SONIC_BOOM, 1, 0, 0, 0, 1);
                    particleApi.spawnColoredParticles(cur, i%2==0 ? C_TM_PURPLE : C_TM_TEAL,
                            1.3f, 3, 0.08, 0.08, 0.08);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(cur, Particle.SCULK_SOUL, 1, 0.04, 0.04, 0.04, 0.02);
                    if (r.nextBoolean())
                        particleApi.spawnColoredParticles(
                                cur.clone().add((r.nextDouble()-0.5)*0.5, 0, (r.nextDouble()-0.5)*0.5),
                                C_TM_WHITE, 1.0f, 1, 0.03, 0.03, 0.03);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);
        HashMap<Particle, Double> particles = new HashMap<>();
        particles.put(Particle.SONIC_BOOM, 1.0);
        for (Entity e : particleApi.drawMultiParticleLineWRTO(from, l, 0.08, particles, 0, 10)) {
            if (!(e instanceof LivingEntity) || e.equals(p)) continue;
            ((LivingEntity) e).damage(night ? 22 : 10, p);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                    night ? 60 : 40, night ? 1 : 0, false, true));
        }
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), C_TM_PURPLE, 1.5f, 20, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), C_TM_TEAL,   1.3f, 15, 0.6, 0.6, 0.6);
        particleApi.spawnParticles(p.getLocation(), Particle.GUST, 1, 0, 0, 0, 1);
    }

    private void twilightLeap(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1.3f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);

        Location loc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(loc, C_TM_PURPLE,   1.4f, 25, 0.5, 0.4, 0.5);
        particleApi.spawnColoredParticles(loc, C_TM_TEAL,     1.2f, 18, 0.6, 0.4, 0.6);
        particleApi.spawnParticles(loc, Particle.SCULK_SOUL,  8, 0.4, 0.3, 0.4, 0.02);
        particleApi.spawnParticles(loc, Particle.GLOW,       30, 0.5, 0.4, 0.5, 0.8);
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 2, 2, 2)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            Vector kb = e.getLocation().subtract(p.getLocation()).toVector();
            if (kb.lengthSquared() < 0.01) kb = new Vector(0.5, 0.2, 0.5);
            e.setVelocity(kb.normalize().multiply(1.2).setY(0.5));
        }

        p.setVelocity(new Vector(0, 1, 0).multiply(0.9));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 55, 0, false, false));
        if (p.getLocation().getBlock().getLightLevel() < 7) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
            particleApi.spawnColoredParticles(loc, C_TM_LAVENDER, 1.1f, 12, 0.4, 0.3, 0.4);
        }

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 15 || !p.isOnline()) { cancel(); return; }
                Location pl = p.getLocation().clone().add(0, 0.5, 0);
                particleApi.spawnParticles(pl, Particle.SCULK_SOUL, 1, 0.2, 0.2, 0.2, 0.02);
                particleApi.spawnColoredParticles(pl, ECLIPSE_COLORS[t % ECLIPSE_COLORS.length], 0.9f, 1, 0.15, 0.15, 0.15);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void magicMissile(Player p, int vectorRotate) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 2f);
        boolean night = isNight(p);

        ArmorStand as = p.getWorld().spawn(p.getLocation().clone().add(0, 1.5, 0), ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false);
            en.setSmall(true); en.setMarker(true);
        });

        Location dest = p.getLocation().clone().add(
                rotateVector(p.getLocation().getDirection(), vectorRotate).multiply(10));
        Vector v = dest.subtract(p.getLocation().clone()).toVector();

        new BukkitRunnable() {
            final Random r = new Random();
            int i = 1;

            @Override public void run() {
                if (!p.isOnline()) { safeRemove(as); cancel(); return; }
                particleApi.spawnColoredParticles(as.getLocation(),
                        MISSILE_COLORS[r.nextInt(MISSILE_COLORS.length)], 1.2f, 3, 0.01, 0.01, 0.01);
                for (int j = 0; j < 3; j++) {
                    double a = Math.toRadians(j * 120 + i * 22);
                    Location ring = as.getLocation().clone().add(Math.cos(a)*0.25, Math.sin(a*0.5)*0.12, Math.sin(a)*0.25);
                    particleApi.spawnColoredParticles(ring, j==0 ? C_TM_PURPLE : C_TM_TEAL, 0.9f, 1, 0.02, 0.02, 0.02);
                }
                if (r.nextBoolean())
                    particleApi.spawnParticles(as.getLocation(), Particle.SCULK_SOUL, 1, 0.01, 0.01, 0.01, 0.02);

                as.teleport(as.getLocation().add(v.normalize().multiply(1.0)));
                for (Entity entity : as.getLocation().getChunk().getEntities()) {
                    if (as.isDead()) break;
                    if (entity instanceof ArmorStand || entity.equals(p)) continue;
                    if (as.getLocation().distanceSquared(entity.getLocation()) <= 3.8 && entity instanceof LivingEntity) {
                        ((LivingEntity) entity).damage(night ? 22 : 14, p);
                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                                night ? 80 : 40, night ? 1 : 0, false, true));
                        spellExplode(p, as.getLocation().clone());
                        safeRemove(as); cancel(); return;
                    }
                }
                if (!as.getLocation().getBlock().isPassable() || as.getLocation().getBlock().isLiquid()) {
                    if (!as.isDead()) { spellExplode(p, as.getLocation().clone()); safeRemove(as); cancel(); return; }
                }
                if (i > 20) {
                    if (!as.isDead()) { spellExplode(p, as.getLocation().clone()); safeRemove(as); cancel(); return; }
                }
                i++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void spellExplode(Player p, Location l) {
        boolean night = isNight(p);
        particleApi.spawnParticles(l, Particle.GUST, 1, 0, 0, 0, 1);
        particleApi.spawnParticles(l, Particle.GLOW,    80, 1, 1, 1, 8);
        particleApi.spawnParticles(l, Particle.FIREWORK, 30, 0.15, 0.15, 0.15, 0.5);
        particleApi.spawnColoredParticles(l, C_TM_PURPLE,   1.5f, 20, 0.5, 0.5, 0.5);
        particleApi.spawnColoredParticles(l, C_TM_TEAL,     1.3f, 15, 0.6, 0.6, 0.6);
        particleApi.spawnParticles(l, Particle.SCULK_SOUL, 5, 0.4, 0.4, 0.4, 0.04);
        l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST,       1f, 0.6f);
        l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.9f);
        l.getWorld().playSound(l, Sound.ENTITY_WARDEN_SONIC_BOOM,           1f, 2f);

        for (Entity e : l.getWorld().getNearbyEntities(l, 2, 2, 2)) {
            if (e instanceof LivingEntity && !e.equals(p))
                ((LivingEntity) e).damage(night ? 16 : 10, p);
        }
    }
    private void healingMirage(Player p) {
        boolean night = isNight(p);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,    0.6f, 1.4f);

        final Location hmLoc = p.getLocation().clone();
        final Vector step    = hmLoc.getDirection().clone().setY(0).normalize().multiply(4);
        particleApi.spawnColoredParticles(hmLoc.clone().add(0,1,0), C_TM_TEAL,   1.4f, 30, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(hmLoc.clone().add(0,1,0), C_TM_PURPLE, 1.2f, 20, 0.7, 0.7, 0.7);
        particleApi.spawnParticles(hmLoc.clone().add(0,1,0), Particle.GLOW, 60, 1.5, 1.5, 1.5, 1.2);

        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (i > 120) { cancel(); return; }
                if (i % 20 == 0) {
                    for (Entity e : hmLoc.getWorld().getNearbyEntities(hmLoc, 3, 3, 3)) {
                        if (!(e instanceof Player)) continue;
                        ((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                                41, night ? 2 : 1, false, false));
                        if (night)
                            ((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 1, false, false));
                    }
                    for (int j = 0; j < 90; j++) {
                        Location loc = hmLoc.clone().add(rotateVector(step.clone(), j * 4));
                        particleApi.spawnParticles(loc.clone(), Particle.EGG_CRACK, 1, 0, 0, 0, 0.01);
                    }
                }

                particleApi.spawnParticles(hmLoc, Particle.GLOW, 5, 2, 2, 2, 1.2);
                for (int j = 0; j < 4; j++) {
                    double a = Math.toRadians(j * 90 + i * 6);
                    Location ring = hmLoc.clone().add(0,1,0).add(Math.cos(a)*1.5, Math.sin(a*0.3)*0.3, Math.sin(a)*1.5);
                    particleApi.spawnColoredParticles(ring, j%2==0 ? C_TM_TEAL : C_TM_PURPLE, 1.1f, 1, 0.04, 0.04, 0.04);
                }
                particleApi.spawnParticles(hmLoc.clone().add(0,1,0), Particle.SCULK_SOUL, 1, 0.4, 0.4, 0.4, 0.02);

                if (i % 20 == 0)
                    hmLoc.getWorld().playSound(hmLoc, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.6f);
                i++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }
    private void phantomEclipse(Player p) {
        boolean night = isNight(p);
        int count     = night ? 4 : 3;
        int duration  = 100;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.9f, 0.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT,    0.7f, 0.6f);
        if (night) p.sendMessage(ChatColor.DARK_PURPLE + "✦ Phantom Eclipse — Night mode!");
        final Set<UUID> phantomHitCooldown = new HashSet<>();
        for (int ph = 0; ph < count; ph++) {
            final int phantomIdx = ph;
            final double phaseOffset = (2 * Math.PI / count) * ph;
            new BukkitRunnable() {
                int t = 0;
                boolean striking = false;
                boolean returning = false;
                Location strikeTarget = null;
                Location phantomLoc = null;
                @Override public void run() {
                    if (t >= duration || !p.isOnline()) { cancel(); return; }
                    double orbitAngle = phaseOffset + t * 0.18;
                    double orbitRx = 1.8;
                    double orbitRy = 0.5;
                    Location orbitPos = p.getLocation().clone().add(0, 1.2, 0).add(
                            Math.cos(orbitAngle) * orbitRx,
                            Math.sin(orbitAngle * 0.7) * orbitRy,
                            Math.sin(orbitAngle) * orbitRx);
                    if (!striking && !returning) {
                        phantomLoc = orbitPos.clone();
                        drawPhantom(phantomLoc, phantomIdx, t);
                        if (t % 8 == phantomIdx * 2) {
                            LivingEntity target = findTargetFromLoc(p, phantomLoc, 6.0);
                            if (target != null && !phantomHitCooldown.contains(target.getUniqueId())) {
                                striking = true;
                                strikeTarget = target.getLocation().clone().add(0, 1, 0);
                                p.getWorld().playSound(phantomLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.4f, 1.8f);
                            }
                        }
                    } else if (striking && strikeTarget != null) {
                        Vector toTarget = strikeTarget.toVector().subtract(phantomLoc.toVector());
                        if (toTarget.lengthSquared() < 0.8 || !isVectorFinite(toTarget)) {
                            for (Entity e : phantomLoc.getWorld().getNearbyEntities(phantomLoc, 1.2, 1.2, 1.2)) {
                                if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                                if (phantomHitCooldown.contains(e.getUniqueId())) continue;
                                ((LivingEntity) e).damage(night ? 18 : 12, p);
                                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                                        night ? 60 : 40, night ? 1 : 0, false, true));
                                phantomHitCooldown.add(e.getUniqueId());
                                UUID uid = e.getUniqueId();
                                new BukkitRunnable() {
                                    @Override public void run() { phantomHitCooldown.remove(uid); }
                                }.runTaskLater(magicPlugin, 40L);
                                particleApi.spawnParticles(phantomLoc, Particle.SCULK_SOUL, 5, 0.2, 0.2, 0.2, 0.04);
                                particleApi.spawnColoredParticles(phantomLoc, C_TM_TEAL,   1.3f, 12, 0.3, 0.3, 0.3);
                                particleApi.spawnColoredParticles(phantomLoc, C_TM_PURPLE, 1.1f, 8, 0.35, 0.35, 0.35);
                                phantomLoc.getWorld().playSound(phantomLoc, Sound.ENTITY_WARDEN_HURT, 0.4f, 1.8f);
                            }
                            striking = false; returning = true;
                        } else {
                            phantomLoc = phantomLoc.clone().add(toTarget.normalize().multiply(0.7));
                            drawPhantomStrike(phantomLoc, phantomIdx, t);
                        }
                    } else if (returning) {
                        Vector toOrbit = orbitPos.toVector().subtract(phantomLoc.toVector());
                        if (toOrbit.lengthSquared() < 1.0 || !isVectorFinite(toOrbit)) {
                            returning = false;
                            phantomLoc = orbitPos;
                        } else {
                            phantomLoc = phantomLoc.clone().add(toOrbit.normalize().multiply(0.55));
                            drawPhantom(phantomLoc, phantomIdx, t);
                        }
                    }
                    t++;
                }
            }.runTaskTimer(magicPlugin, ph * 3L, 1);
        }
        for (int i = 0; i < count; i++) {
            double a = Math.toRadians(i * (360.0 / count));
            Location summonPos = p.getLocation().clone().add(0, 1.2, 0)
                    .add(Math.cos(a)*1.8, 0, Math.sin(a)*1.8);
            particleApi.spawnParticles(summonPos, Particle.SCULK_SOUL, 6, 0.15, 0.15, 0.15, 0.04);
            particleApi.spawnColoredParticles(summonPos, ECLIPSE_COLORS[i % ECLIPSE_COLORS.length], 1.3f, 8, 0.2, 0.2, 0.2);
        }
    }
    private void drawPhantom(Location loc, int idx, int t) {
        particleApi.spawnParticles(loc, Particle.SCULK_SOUL, 2, 0.08, 0.08, 0.08, 0.02);
        particleApi.spawnColoredParticles(loc, idx%2==0 ? C_TM_PURPLE : C_TM_TEAL, 1.2f, 2, 0.07, 0.07, 0.07);
        if (t % 3 == 0)
            particleApi.spawnColoredParticles(loc, C_TM_WHITE, 0.9f, 1, 0.04, 0.04, 0.04);
        particleApi.spawnColoredParticles(loc.clone().add(
                (Math.random()-0.5)*0.3, -(Math.random()*0.15), (Math.random()-0.5)*0.3),
                C_TM_DEEP, 0.7f, 1, 0.03, 0.03, 0.03);
    }
    private void drawPhantomStrike(Location loc, int idx, int t) {
        particleApi.spawnParticles(loc, Particle.SCULK_SOUL, 3, 0.06, 0.06, 0.06, 0.04);
        particleApi.spawnColoredParticles(loc, C_TM_WHITE,  1.5f, 3, 0.05, 0.05, 0.05);
        particleApi.spawnColoredParticles(loc, idx%2==0 ? C_TM_TEAL : C_TM_LAVENDER, 1.3f, 2, 0.05, 0.05, 0.05);
    }
    private void onDamagedBy(DamagedByExecute execute) {
        final Player p = execute.getPlayer();
        final EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) execute.getRawEvent();
        final Entity damager = event.getDamager();
        if (new Random().nextInt(4) == 0) {
            event.setCancelled(true);
            Vector v = damager.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
            Location toDisplay = p.getEyeLocation().clone().add(v);
            particleApi.spawnParticles(toDisplay, Particle.GUST, 1, 0, 0, 0, 1);
            particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), C_TM_TEAL, 1.3f, 10, 0.3, 0.3, 0.3);
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
            p.setVelocity(p.getLocation().getDirection().clone().normalize().multiply(-0.4));
        }
    }
    private void onDamage(DealDamageExecute execute) {
        final Player p = execute.getPlayer();
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) execute.getRawEvent();
        final Entity e = event.getEntity();

        if (new Random().nextInt(3) == 0 && e instanceof LivingEntity)
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 300, 0));

        if (e instanceof Warden)
            event.setDamage(((Warden) e).getAttribute(Attribute.MAX_HEALTH).getBaseValue() / 2);
    }
    private void onDeath(DamagedExecute execute) {
        if (new Random().nextInt(10) != 0) return;
        final Player p = execute.getPlayer();
        EntityDamageEvent event = (EntityDamageEvent) execute.getRawEvent();
        Warden w = p.getWorld().spawn(p.getLocation(), Warden.class);
        particleApi.spawnParticles(p.getLocation(), Particle.GLOW, 40, 1, 1, 1, 0.3);
        if (event instanceof EntityDamageByEntityEvent)
            w.setAnger(((EntityDamageByEntityEvent) event).getDamager(), 140);
    }
    private void onDamaged(DamagedExecute execute) {
        final Player p = execute.getPlayer();
        final EntityDamageEvent event = (EntityDamageEvent) execute.getRawEvent();
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (p.getLocation().getBlock().getLightLevel() < 13) {
                event.setCancelled(true);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 2f, 2f);
                particleApi.spawnParticles(p.getLocation().clone().add(0, 0.5, 0), Particle.GUST, 1, 0, 0, 0, 0.1);
                particleApi.spawnColoredParticles(p.getLocation().clone().add(0,0.5,0), C_TM_TEAL, 1.2f, 10, 0.4, 0.3, 0.4);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false));
                return;
            }
        }
        if (event.getFinalDamage() >= p.getHealth()) {
            if (CooldownApi.isOnCooldown(tm_neardeath, p)) {
                if (!event.isCancelled()) onDeath(execute);
                return;
            }
            addCdFixed(tm_neardeath, p, 180);
            event.setCancelled(true);
            p.setHealth(isNight(p) ? 6 : 4);
            if (org.bukkit.Bukkit.getOnlinePlayers().size() > 1) {
                ArrayList<Player> pl = new ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
                Collections.shuffle(pl);
                for (Player player : pl) {
                    if (!player.equals(p)) { p.teleport(player); break; }
                }
            }
            p.setVelocity(p.getLocation().getDirection().clone().normalize().multiply(-0.4));
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
            particleApi.spawnParticles(p.getLocation(), Particle.TOTEM_OF_UNDYING, 40, 1, 1, 1, 0.6);
            particleApi.spawnColoredParticles(p.getLocation().clone().add(0,1,0), C_TM_PURPLE, 1.5f, 30, 0.8, 0.8, 0.8);
        }
    }
    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (p.isInWater() && !isNight(p) && isClearWeather(p))
                    p.damage(0.5);
                if (p.getLocation().getBlock().getLightLevel() > 14)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, false));
                if (p.hasPotionEffect(PotionEffectType.WITHER))
                    p.removePotionEffect(PotionEffectType.WITHER);
                Location loc = p.getLocation().clone().add(0, 0.06, 0);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 + t * 8);
                    Location lp = loc.clone().add(Math.cos(a)*0.9, 0, Math.sin(a)*0.9);
                    particleApi.spawnColoredParticles(lp, i%2==0 ? C_TM_PURPLE : C_TM_TEAL, 0.85f, 1, 0.03, 0.03, 0.03);
                }
                particleApi.spawnColoredParticles(loc, C_TM_PURPLE, 0.85f, 1, 0.28, 0.01, 0.28);

                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 15);
        return r;
    }
    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&9Shriek Transition";
            case 1: return "&9Twilight Leap";
            case 2: return "&9Magic Missile";
            case 3: return "&9Healing Mirage";
            case 4: return "&5Phantom Eclipse";
            default: return "&7none";
        }
    }
    private boolean isVectorFinite(org.bukkit.util.Vector v) {
        return !Double.isNaN(v.getX())      && !Double.isNaN(v.getY())      && !Double.isNaN(v.getZ())
            && !Double.isInfinite(v.getX()) && !Double.isInfinite(v.getY()) && !Double.isInfinite(v.getZ());
    }
    private boolean isNight(Player p) {
        return !(p.getWorld().getTime() < 12300 || p.getWorld().getTime() > 23850);
    }
    private boolean isClearWeather(Player p) {
        return p.getWorld().isClearWeather();
    }
}
