package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class MagneticPower extends Power implements IdlePower {

    private static final String CD_PULL    = "magnetic.pull";
    private static final String CD_WAVE    = "magnetic.wave";
    private static final String CD_GRIP    = "magnetic.grip";
    private static final String CD_POLARITY= "magnetic.polarity";
    private static final String CD_SCRAP   = "magnetic.scrap";
    private static final String CD_CAGE    = "magnetic.cage";

    private static final Color C_SILVER = Color.fromRGB(192, 192, 210);
    private static final Color C_BLUE   = Color.fromRGB(80,  160, 255);
    private static final Color C_CYAN   = Color.fromRGB(0,   220, 220);
    private static final Color C_WHITE  = Color.fromRGB(240, 240, 255);

    private boolean pullFieldActive = false;
    private BukkitRunnable pullFieldTask = null;

    private final Map<UUID, BukkitRunnable> cagedEntities = new HashMap<>();

    private final Random rng = new Random();

    public MagneticPower(Player owner) { super(owner); }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) { passiveMagneticSnap((DamagedByExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeft((LeftClickExecute) ex);
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: magneticPull(p);    break;
            case 1: repulsionWave(p);   break;
            case 2: ironGrip(p);        break;
            case 3: polaritySwap(p);    break;
            case 4: scrapStorm(p);      break;
            case 5: magneticCage(p);    break;
        }
    }

    private void magneticPull(Player p) {
        if (onCd(CD_PULL, p, this)) return;

        List<LivingEntity> targets = getNearbyTargets(p, 10);
        if (targets.isEmpty()) { sendActionBar(p, "§bNo target!"); return; }

        final Location center = p.getLocation().clone().add(0, 1, 0);

        new BukkitRunnable() {
            double r = 10;
            int t = 0;
            @Override public void run() {
                if (r < 0.5) { cancel(); return; }
                particleCircle(center, r, C_BLUE,   2f, 20, t * 15);
                particleCircle(center, r, C_SILVER, 1.5f, 12, -t * 15);
                r -= 0.9; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        for (LivingEntity le : targets) {
            Vector pull = center.clone().subtract(le.getLocation()).toVector();
            if (!isVecFinite(pull) || pull.lengthSquared() < 0.01) continue;
            pull.normalize().multiply(1.6).setY(0.35);
            le.setVelocity(pull);

            particleLine(le.getLocation().add(0,1,0), center, 0.6, C_CYAN, 1.5f);
        }

        p.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        p.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
        sendActionBar(p, "§b⬡ Magnetic Pull — pulled §f" + targets.size() + " §btarget(s)!");
        addCd(CD_PULL, p);
    }

    private void repulsionWave(Player p) {
        if (onCd(CD_WAVE, p, this)) return;

        final Location center = p.getLocation().clone().add(0, 1, 0);

        new BukkitRunnable() {
            double r = 0.3;
            int t = 0;
            @Override public void run() {
                if (r > 9) { cancel(); return; }
                particleCircle(center, r, C_SILVER, 2.5f, 22, t * 18);
                particleCircle(center, r, C_WHITE,  3f,   8,  -t * 18);
                r += 0.8; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        for (LivingEntity le : getNearbyTargets(p, 8)) {
            le.damage(6.0, p);
            Vector push = knockbackVector(center, le, 1.8, 0.5);
            le.setVelocity(push);
            spawnMagBurst(le.getLocation().add(0, 1, 0), 8);
        }

        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.6f);
        p.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.5f);
        addCd(CD_WAVE, p);
    }

    private void ironGrip(Player p) {
        if (onCd(CD_GRIP, p, this)) return;
        LivingEntity target = getInSight(p, 16, 0.93);
        if (target == null) { sendActionBar(p, "§cNo target!"); return; }

        final LivingEntity t2 = target;
        final Location dest = p.getLocation().clone().add(
                p.getLocation().getDirection().normalize().multiply(1.8)).add(0, 0.5, 0);

        particleLine(p.getEyeLocation(), t2.getLocation().add(0,1,0), 0.4, C_CYAN, 2f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!t2.isValid() || t > 12) { cancel(); return; }
                Vector pull = dest.clone().subtract(t2.getLocation()).toVector();
                if (!isVecFinite(pull)) { cancel(); return; }
                t2.setVelocity(pull.normalize().multiply(1.5));

                particleApi.spawnColoredParticles(t2.getLocation().add(0,1,0),
                        C_BLUE, 2f, 3, 0.15, 0.15, 0.15);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override public void run() {
                if (!t2.isValid()) return;
                applyPotion(t2, PotionEffectType.SLOWNESS, 20 * 2, 10);
                t2.setVelocity(new Vector(0, 0, 0));
                t2.damage(4.0, p);
                spawnMagBurst(t2.getLocation().add(0, 1, 0), 14);
                t2.getWorld().playSound(t2.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
            }
        }.runTaskLater(magicPlugin, 12);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 0.6f);
        addCd(CD_GRIP, p);
    }

    private void polaritySwap(Player p) {
        if (onCd(CD_POLARITY, p, this)) return;

        if (!pullFieldActive) {

            pullFieldActive = true;
            sendActionBar(p, "§b⬡ Pull Field ON — pulling enemies in!");
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);

            pullFieldTask = new BukkitRunnable() {
                int t = 0;
                @Override public void run() {
                    if (!pullFieldActive || !p.isOnline()) { cancel(); return; }

                    if (isAuraEnabled(p)) {
                        particleCircle(p.getLocation().clone().add(0, 0.5, 0),
                                8.0, C_BLUE, 1f, 28, t * 12);
                    }

                    if (t % 20 == 0) {
                        for (LivingEntity le : getNearbyTargets(p, 8)) {
                            Vector pull = p.getLocation().add(0,1,0)
                                    .subtract(le.getLocation()).toVector();
                            if (!isVecFinite(pull) || pull.lengthSquared() < 0.01) continue;
                            pull.normalize().multiply(0.9).setY(0.2);
                            le.setVelocity(pull);
                        }
                    }
                    t++;
                }
            };
            pullFieldTask.runTaskTimer(magicPlugin, 0, 1);

        } else {

            pullFieldActive = false;
            if (pullFieldTask != null) { pullFieldTask.cancel(); pullFieldTask = null; }

            for (LivingEntity le : getNearbyTargets(p, 8)) {
                le.damage(5.0, p);
                Vector push = knockbackVector(p.getLocation(), le, 2.0, 0.6);
                le.setVelocity(push);
                spawnMagBurst(le.getLocation().add(0,1,0), 10);
            }

            final Location center = p.getLocation().clone().add(0, 1, 0);
            new BukkitRunnable() {
                double r = 0.3; int t = 0;
                @Override public void run() {
                    if (r > 9) { cancel(); return; }
                    particleCircle(center, r, C_CYAN, 2.5f, 20, t * 20);
                    r += 0.85; t++;
                }
            }.runTaskTimer(magicPlugin, 0, 1);

            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
            sendActionBar(p, "§b⬡ Pull Field OFF — knockback burst!");
        }

        addCdFixed(CD_POLARITY, p, 2.0);
    }

    private void scrapStorm(Player p) {
        if (onCd(CD_SCRAP, p, this)) return;

        List<LivingEntity> targets = getNearbyTargets(p, 8);
        if (targets.isEmpty()) { sendActionBar(p, "§cNo target!"); return; }

        ItemStack stolen = null;
        Player victim = null;
        for (LivingEntity le : targets) {
            if (le instanceof Player) {
                victim = (Player) le;
                ItemStack held = victim.getInventory().getItemInMainHand();
                if (held.getType() != Material.AIR) {
                    stolen = held.clone();
                    victim.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    break;
                }
            }
        }

        final String itemName = stolen != null
                ? stolen.getType().name().replace('_', ' ').toLowerCase()
                : "scrap metal";
        final Player finalVictim = victim;
        final ItemStack finalStolen = stolen;

        int count = Math.min(3, targets.size());
        for (int i = 0; i < count; i++) {
            final LivingEntity fireTarget = targets.get(i);
            final int delay = i * 4;

            new BukkitRunnable() {
                @Override public void run() {
                    if (!fireTarget.isValid()) return;

                    particleLine(p.getLocation().add(0,1,0),
                            fireTarget.getLocation().add(0,1,0), 0.45, C_SILVER, 2.5f);
                    fireTarget.damage(8.0, p);
                    spawnMagBurst(fireTarget.getLocation().add(0,1,0), 10);
                    fireTarget.getWorld().playSound(fireTarget.getLocation(),
                            Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 0.7f);
                }
            }.runTaskLater(magicPlugin, delay);
        }

        if (finalStolen != null && finalVictim != null) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (finalVictim.isOnline()) {
                        finalVictim.getInventory().setItemInMainHand(finalStolen);
                        finalVictim.sendMessage("§bYour item has been returned.");
                    }
                }
            }.runTaskLater(magicPlugin, 60);
        }

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 0.8f);
        sendActionBar(p, "§b⬡ Scrap Storm — fired §f" + itemName + " §bat " + count + " target(s)!");
        addCd(CD_SCRAP, p);
    }

    private void magneticCage(Player p) {
        if (onCd(CD_CAGE, p, this)) return;
        LivingEntity target = getInSight(p, 12, 0.92);
        if (target == null) target = getNearestTarget(p, 6);
        if (target == null) { sendActionBar(p, "§cNo target!"); return; }
        if (cagedEntities.containsKey(target.getUniqueId())) {
            sendActionBar(p, "§cThis target is already caged!"); return;
        }

        final LivingEntity caged = target;
        applyPotion(caged, PotionEffectType.SLOWNESS,  20 * 5, 10);
        applyPotion(caged, PotionEffectType.JUMP_BOOST, 20 * 5, -1);

        BukkitRunnable orbit = new BukkitRunnable() {
            double angle = 0;
            int seconds  = 0;
            int ticks    = 0;

            @Override public void run() {
                if (!caged.isValid() || !p.isOnline() || seconds >= 5) {

                    if (caged.isValid()) {
                        Vector release = knockbackVector(p.getLocation(), caged, 2.0, 0.7);
                        caged.setVelocity(release);
                        caged.damage(6.0, p);
                        spawnMagBurst(caged.getLocation().add(0,1,0), 16);
                    }
                    cagedEntities.remove(caged.getUniqueId());
                    cancel(); return;
                }

                Location orbitLoc = p.getLocation().clone().add(
                        Math.cos(angle) * 4.0,
                        1.0,
                        Math.sin(angle) * 4.0);

                Vector toOrbit = orbitLoc.clone().subtract(caged.getLocation()).toVector();
                if (isVecFinite(toOrbit) && toOrbit.lengthSquared() > 0.01) {
                    caged.setVelocity(toOrbit.normalize().multiply(1.3));
                }

                if (isAuraEnabled(p)) {
                    particleApi.spawnColoredParticles(orbitLoc, C_CYAN, 2.5f, 2, 0.1, 0.1, 0.1);

                    if (ticks % 5 == 0)
                        particleLine(p.getLocation().add(0,1,0),
                                caged.getLocation().add(0,1,0), 0.6, C_BLUE, 1.5f);
                }

                angle += 0.18;

                if (ticks % 20 == 0 && ticks > 0) {
                    caged.damage(3.0, p);
                    seconds++;
                }
                ticks++;
            }
        };
        orbit.runTaskTimer(magicPlugin, 0, 1);
        cagedEntities.put(caged.getUniqueId(), orbit);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1.0f);
        sendActionBar(p, "§b⬡ Magnetic Cage — target caged in orbit for 5s!");
        addCd(CD_CAGE, p);
    }

    private void passiveMagneticSnap(DamagedByExecute ex) {
        if (!(ex.getRawEvent() instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        if (!(event.getDamager() instanceof LivingEntity)) return;
        if (rng.nextFloat() >= 0.20f) return;

        LivingEntity attacker = (LivingEntity) event.getDamager();
        Player p = ex.getPlayer();

        Vector snap = p.getLocation().add(0,1,0).subtract(attacker.getLocation()).toVector();
        if (isVecFinite(snap) && snap.lengthSquared() > 0.01) {
            attacker.setVelocity(snap.normalize().multiply(1.2).setY(0.3));
        }
        applyPotion(attacker, PotionEffectType.SLOWNESS, 10, 4);
        particleLine(p.getLocation().add(0,1,0), attacker.getLocation().add(0,1,0),
                0.4, C_CYAN, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.7f, 1.5f);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                applyPotionSilent(p, PotionEffectType.SPEED, 30, 0);

                if (isAuraEnabled(p)) {

                    for (int i = 0; i < 3; i++) {
                        double a = Math.toRadians(t * (12 + i * 5) + i * 120);
                        double rad = 0.6 + i * 0.25;
                        double h   = 1.0 + Math.sin(t * 0.08 + i * 2.1) * 0.4;
                        Location loc = p.getLocation().clone().add(
                                Math.cos(a) * rad, h, Math.sin(a) * rad);
                        Color c = i == 0 ? C_BLUE : (i == 1 ? C_CYAN : C_SILVER);
                        particleApi.spawnColoredParticles(loc, c, 2f, 1, 0.03, 0.03, 0.03);
                    }
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 2);
        return r;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§b磁力牵引";
            case 1: return "§b排斥波";
            case 2: return "§b铁握";
            case 3: return "§b极性交换";
            case 4: return "§b碎片风暴";
            case 5: return "§b磁力牢笼";
            default: return "§7none";
        }
    }

    private void spawnMagBurst(Location loc, int count) {
        particleApi.spawnColoredParticles(loc, C_BLUE,   2.5f, count / 2, 0.35, 0.35, 0.35);
        particleApi.spawnColoredParticles(loc, C_SILVER, 2f,   count / 2, 0.25, 0.25, 0.25);
        particleApi.spawnColoredParticles(loc, C_WHITE,  3.5f, 2,         0.15, 0.15, 0.15);
    }
}

