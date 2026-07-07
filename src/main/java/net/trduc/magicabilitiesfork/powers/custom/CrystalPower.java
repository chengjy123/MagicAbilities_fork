package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class CrystalPower extends Power implements IdlePower {

    private static final String CD_SHELL    = "crystal.shell";
    private static final String CD_SPIRE    = "crystal.spire";
    private static final String CD_FIELD    = "crystal.field";
    private static final String CD_PRISON   = "crystal.prison";
    private static final String CD_SHATTER  = "crystal.shatter";

    private static final Color C_DEEP    = Color.fromRGB(60,  0,  120);
    private static final Color C_MID     = Color.fromRGB(130, 30, 200);
    private static final Color C_BRIGHT  = Color.fromRGB(200, 100, 255);
    private static final Color C_WHITE   = Color.fromRGB(230, 200, 255);

    private boolean shellActive = false;
    private double  shellAbsorbed = 0.0;

    private boolean fieldActive = false;

    private int activeShards = 0;

    private final Random rng = new Random();

    public CrystalPower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) { passiveReflect((DamagedByExecute) ex); return; }
        if (ex instanceof DamagedExecute)   { handleShellAbsorb((DamagedExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeft((LeftClickExecute) ex);
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: crystalShell(p);      break;
            case 1: amethystSpire(p);     break;
            case 2: fractureField(p);     break;
            case 3: crystalPrison(p);     break;
            case 4: resonanceShatter(p);  break;
        }
    }

    private void crystalShell(Player p) {
        if (onCd(CD_SHELL, p, this)) return;
        if (shellActive) { sendActionBar(p, "§5Crystal Shell is already active!"); return; }

        shellActive   = true;
        shellAbsorbed = 0.0;
        activeShards++;

        new BukkitRunnable() {
            @Override public void run() {
                for (int i = 0; i < 3; i++) {
                    particleCircle(p.getLocation().clone().add(0, i * 0.7 + 0.1, 0),
                            1.0, C_BRIGHT, 2.5f, 16, i * 20);
                }
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
            }
        }.runTask(magicPlugin);

        BukkitRunnable aura = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!shellActive || !p.isOnline()) { cancel(); return; }
                particleCircle(p.getLocation().clone().add(0, 0.1, 0), 1.0, C_MID,   2f, 12, t * 18);
                particleCircle(p.getLocation().clone().add(0, 1.0, 0), 1.0, C_BRIGHT, 2f, 12, -t * 18);
                particleCircle(p.getLocation().clone().add(0, 2.0, 0), 1.0, C_MID,   2f, 12, t * 18);
                t++;
            }
        };
        aura.runTaskTimer(magicPlugin, 0, 3);

        new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { aura.cancel(); shellActive = false; activeShards = Math.max(0, activeShards - 1); return; }
                aura.cancel();
                shellActive = false;
                activeShards = Math.max(0, activeShards - 1);
                if (shellAbsorbed < 0.5) return;

                double releaseDmg = shellAbsorbed * 0.80;
                Location center = p.getLocation().clone().add(0, 1, 0);

                for (int ring = 1; ring <= 4; ring++) {
                    final int r = ring;
                    new BukkitRunnable() {
                        @Override public void run() {
                            particleCircle(center, r * 1.3, C_BRIGHT, 3f, 24, r * 15);
                            particleCircle(center, r * 1.3, C_WHITE,  2f, 12, r * 15 + 15);
                        }
                    }.runTaskLater(magicPlugin, ring * 2L);
                }

                for (Entity e : p.getNearbyEntities(5, 5, 5)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    ((LivingEntity) e).damage(releaseDmg, p);
                    spawnCrystalBurst(e.getLocation().add(0, 1, 0), 10);
                }

                p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.5f);
                p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
                sendActionBar(p, String.format("§5💎 Shell shattered — released §f%.1f §5damage!", releaseDmg));
            }
        }.runTaskLater(magicPlugin, 20 * 4);

        sendActionBar(p, "§5💎 Crystal Shell activated!");
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
        addCd(CD_SHELL, p);
    }

    private void handleShellAbsorb(DamagedExecute ex) {
        if (!shellActive) return;
        EntityDamageEvent event = (EntityDamageEvent) ex.getRawEvent();

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;
        shellAbsorbed += event.getFinalDamage();
        event.setDamage(0);
        Player p = ex.getPlayer();
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0),
                C_BRIGHT, 2.5f, 5, 0.3, 0.3, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.4f);
    }

    private void amethystSpire(Player p) {
        if (onCd(CD_SPIRE, p, this)) return;
        LivingEntity target = getInSight(p, 16, 0.92);
        if (target == null) { sendActionBar(p, "§cNo target!"); return; }

        final LivingEntity finalTarget = target;
        final Location base = finalTarget.getLocation().clone();
        activeShards++;

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 12) { cancel(); return; }
                double h = t * 0.35;
                particleCircle(base.clone().add(0, h, 0), 0.5 - t * 0.03, C_MID,   2.5f, 10, t * 20);
                particleCircle(base.clone().add(0, h, 0), 0.5 - t * 0.03, C_BRIGHT, 2f,   6, -t * 20);
                particleApi.spawnColoredParticles(base.clone().add(0, h + 0.3, 0), C_WHITE, 3f, 2, 0.2, 0.05, 0.2);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override public void run() {
                if (!finalTarget.isValid()) return;
                finalTarget.damage(9.0, p);
                finalTarget.setVelocity(new Vector(0, 1.3, 0));
                applyPotion(finalTarget, PotionEffectType.SLOWNESS, 20 * 2, 1);
                spawnCrystalBurst(finalTarget.getLocation().add(0, 1, 0), 16);
                finalTarget.getWorld().playSound(finalTarget.getLocation(),
                        Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 0.6f);

                new BukkitRunnable() {
                    @Override public void run() {
                        activeShards = Math.max(0, activeShards - 1);
                    }
                }.runTaskLater(magicPlugin, 40);
            }
        }.runTaskLater(magicPlugin, 8);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.7f);
        addCd(CD_SPIRE, p);
    }

    private void fractureField(Player p) {
        if (onCd(CD_FIELD, p, this)) return;
        if (fieldActive) { sendActionBar(p, "§5Fracture Field is already active!"); return; }

        fieldActive = true;
        activeShards += 2;
        final Location center = p.getLocation().clone();
        final Set<UUID> slowed = new HashSet<>();

        for (int r = 1; r <= 3; r++) {
            final int fr = r;
            new BukkitRunnable() {
                @Override public void run() {
                    particleCircle(center.clone().add(0, 0.1, 0), fr * 2.0, C_MID,  2f, 28, fr * 25);
                    particleCircle(center.clone().add(0, 0.1, 0), fr * 2.0, C_DEEP, 1.5f, 14, -fr * 25);
                }
            }.runTaskLater(magicPlugin, r * 3L);
        }

        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.5f);
        sendActionBar(p, "§5💠 Fracture Field expanding!");

        new BukkitRunnable() {
            int seconds = 0;

            @Override public void run() {
                if (seconds >= 5 || !fieldActive || !p.isOnline()) {
                    fieldActive = false;
                    activeShards = Math.max(0, activeShards - 2);
                    slowed.clear();
                    cancel();
                    return;
                }

                if (isAuraEnabled(p)) {
                    for (int i = 0; i < 8; i++) {
                        double angle = rng.nextDouble() * Math.PI * 2;
                        double dist  = 2 + rng.nextDouble() * 4;
                        Location shard = center.clone().add(
                                Math.cos(angle) * dist, rng.nextDouble() * 1.5, Math.sin(angle) * dist);
                        particleApi.spawnColoredParticles(shard,
                                rng.nextBoolean() ? C_MID : C_DEEP, 2f, 1, 0.05, 0.1, 0.05);
                    }
                }

                for (Entity e : center.getWorld().getNearbyEntities(center, 6, 6, 6)) {
                    if (!(e instanceof LivingEntity) || e.equals(p)) continue;
                    LivingEntity le = (LivingEntity) e;
                    le.damage(2.0, p);
                    applyPotion(le, PotionEffectType.SLOWNESS, 25, 2);
                    if (!slowed.contains(e.getUniqueId())) {
                        spawnCrystalBurst(e.getLocation().add(0, 1, 0), 5);
                        slowed.add(e.getUniqueId());
                    }
                }

                seconds++;
            }
        }.runTaskTimer(magicPlugin, 0, 20);

        addCd(CD_FIELD, p);
    }

    private void crystalPrison(Player p) {
        if (onCd(CD_PRISON, p, this)) return;
        LivingEntity target = getInSight(p, 12, 0.91);
        if (target == null) target = getNearestTarget(p, 5);
        if (target == null) { sendActionBar(p, "§cNo target!"); return; }

        final LivingEntity prisoner = target;
        activeShards++;

        final Location cage = prisoner.getLocation().clone().add(0, 1, 0);

        new BukkitRunnable() {
            @Override public void run() {
                for (int lvl = 0; lvl <= 2; lvl++) {
                    particleCircle(cage.clone().add(0, lvl - 1.0, 0), 1.2, C_MID,   2.5f, 16, lvl * 30);
                    particleCircle(cage.clone().add(0, lvl - 1.0, 0), 1.2, C_BRIGHT, 2f,   8, lvl * 30 + 22);
                }
                cage.getWorld().playSound(cage, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 0.5f);
            }
        }.runTask(magicPlugin);

        applyPotion(prisoner, PotionEffectType.SLOWNESS,  20 * 3, 10);
        applyPotion(prisoner, PotionEffectType.JUMP_BOOST, 20 * 3, -1);
        prisoner.setVelocity(new Vector(0, 0, 0));

        BukkitRunnable cageVfx = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!prisoner.isValid() || t > 55) { cancel(); return; }
                Location loc = prisoner.getLocation().clone().add(0, 1, 0);
                for (int lvl = -1; lvl <= 1; lvl++) {
                    particleCircle(loc.clone().add(0, lvl, 0), 1.1, t % 4 < 2 ? C_BRIGHT : C_MID,
                            2f, 10, t * 15);
                }

                prisoner.setVelocity(new Vector(0, 0, 0));
                t++;
            }
        };
        cageVfx.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override public void run() {
                cageVfx.cancel();
                activeShards = Math.max(0, activeShards - 1);
                if (!prisoner.isValid()) return;

                Location loc = prisoner.getLocation().clone().add(0, 1, 0);

                for (int ring = 1; ring <= 3; ring++) {
                    final int r = ring;
                    new BukkitRunnable() {
                        @Override public void run() {
                            particleCircle(loc, r * 1.1, C_WHITE,  3f, 20, 0);
                            particleCircle(loc, r * 1.1, C_BRIGHT, 2f, 10, 18);
                        }
                    }.runTaskLater(magicPlugin, ring * 2L);
                }

                prisoner.damage(8.0, p);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                    if (!(e instanceof LivingEntity) || e.equals(p) || e.equals(prisoner)) continue;
                    ((LivingEntity) e).damage(4.0, p);
                }

                loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.5f);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.8f);
            }
        }.runTaskLater(magicPlugin, 20 * 3);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.6f);
        addCd(CD_PRISON, p);
    }

    private void resonanceShatter(Player p) {
        if (onCd(CD_SHATTER, p, this)) return;

        sendActionBar(p, "§5⬡ Charging energy...");
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f);

        BukkitRunnable charge = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t > 24 || !p.isOnline()) { cancel(); return; }
                double rad = 0.4 + t * 0.04;
                particleCircle(p.getLocation().clone().add(0, 1, 0), rad, C_BRIGHT, 3f, 10, t * 22);
                particleCircle(p.getLocation().clone().add(0, 1, 0), rad, C_MID,    2f,  6, -t * 22);
                t++;
            }
        };
        charge.runTaskTimer(magicPlugin, 0, 1);

        new BukkitRunnable() {
            @Override public void run() {
                charge.cancel();
                if (!p.isOnline()) return;

                final double damage = 12.0 + activeShards * 3.0;
                final Vector dir    = p.getEyeLocation().getDirection().normalize();
                final ArmorStand wave = spawnProjectile(p);
                final List<Entity> hit = new ArrayList<>();

                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (wave.isDead() || t > 40) { safeRemove(wave); cancel(); return; }
                        wave.teleport(wave.getLocation().add(dir.clone().multiply(1.5)));

                        Location wl = wave.getLocation();
                        particleCircle(wl, 0.6, C_BRIGHT, 3f, 12, t * 25);
                        particleCircle(wl, 0.6, C_MID,    2f,  8, -t * 25);
                        particleApi.spawnColoredParticles(wl, C_WHITE, 4f, 2, 0.15, 0.15, 0.15);

                        for (Entity e : wl.getChunk().getEntities()) {
                            if (e instanceof ArmorStand || e.equals(p) || hit.contains(e)) continue;
                            if (e instanceof LivingEntity && wl.distanceSquared(e.getLocation()) <= 4.5) {
                                ((LivingEntity) e).damage(damage, p);
                                hit.add(e);
                                spawnCrystalBurst(e.getLocation().add(0, 1, 0), 14);
                                e.getWorld().playSound(e.getLocation(),
                                        Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.7f);
                            }
                        }

                        if (!wl.getBlock().isPassable()) {
                            spawnCrystalBurst(wl, 12);
                            safeRemove(wave); cancel(); return;
                        }
                        t++;
                    }
                }.runTaskTimer(magicPlugin, 0, 1);

                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.4f);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.8f);
                sendActionBar(p, String.format("§5⬡ Resonance Shatter — §f%.0f §5dame!", damage));
            }
        }.runTaskLater(magicPlugin, 30);

        addCd(CD_SHATTER, p);
    }

    private void passiveReflect(DamagedByExecute ex) {
        if (shellActive) return;
        if (!(ex.getRawEvent() instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ex.getRawEvent();
        if (!(event.getDamager() instanceof LivingEntity)) return;
        if (rng.nextFloat() >= 0.30f) return;

        double reflect = event.getFinalDamage() * 0.15;
        if (reflect < 0.2) return;

        LivingEntity attacker = (LivingEntity) event.getDamager();
        attacker.damage(reflect, getOwner());

        Player p = ex.getPlayer();
        particleLine(p.getLocation().add(0, 1, 0), attacker.getLocation().add(0, 1, 0),
                0.4, C_BRIGHT, 2f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 1.6f);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                applyPotionSilent(p, PotionEffectType.RESISTANCE, 30, 0);

                if (isAuraEnabled(p)) {

                    particleCircle(p.getLocation().clone().add(0, 0.08, 0),
                            0.7, C_MID, 1.5f, 8, t * 20);
                    particleCircle(p.getLocation().clone().add(0, 1.9, 0),
                            0.7, C_DEEP, 1.5f, 8, -t * 20);

                    if (t % 2 == 0) {
                        double a = rng.nextDouble() * Math.PI * 2;
                        Location drop = p.getLocation().clone().add(
                                Math.cos(a) * 0.6, 2.2 + rng.nextDouble() * 0.3, Math.sin(a) * 0.6);
                        particleApi.spawnColoredParticles(drop, C_BRIGHT, 2.5f, 1, 0.04, 0.04, 0.04);
                    }
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 16);
        return r;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§5Crystal Shell";
            case 1: return "§5Amethyst Spire";
            case 2: return "§5Fracture Field";
            case 3: return "§5Crystal Prison";
            case 4: return "§5Resonance Shatter";
            default: return "§7none";
        }
    }

    private void spawnCrystalBurst(Location loc, int count) {
        particleApi.spawnColoredParticles(loc, C_BRIGHT, 3f,   count / 2, 0.4, 0.4, 0.4);
        particleApi.spawnColoredParticles(loc, C_DEEP,   1.5f, count / 2, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_WHITE,  4f,   2,         0.2, 0.2, 0.2);
    }
}

