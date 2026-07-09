package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.*;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
public class NaturePower extends Power implements IdlePower, Removeable {

    private static final String nature_thorn    = "nature.thorn";
    private static final String nature_entangle = "nature.entangle";
    private static final String nature_spore    = "nature.spore";
    private static final String nature_lance    = "nature.lance";
    private static final String nature_heal     = "nature.heal";
    private static final String nature_tree     = "nature.tree";
    private static final String nature_retaliate = "nature.retaliate";
    private static final Color[] GREENS = {
            Color.fromRGB(34,  139, 34),
            Color.fromRGB(0,   200, 80),
            Color.fromRGB(124, 252, 0),
            Color.fromRGB(0,   128, 0),
            Color.fromRGB(85,  210, 120),
    };
    private static final Color POISON_GREEN = Color.fromRGB(100, 200, 50);
    private static final Color DARK_GREEN   = Color.fromRGB(20,  90,  20);
    private static final Color SPORE_YELLOW = Color.fromRGB(200, 230, 60);

    private static final Set<String> NATURE_BIOMES = new HashSet<>(Arrays.asList(
            "FOREST", "FLOWER_FOREST", "BIRCH_FOREST", "OLD_GROWTH_BIRCH_FOREST",
            "DARK_FOREST", "JUNGLE", "SPARSE_JUNGLE", "BAMBOO_JUNGLE",
            "PLAINS", "SUNFLOWER_PLAINS", "MEADOW",
            "TAIGA", "OLD_GROWTH_SPRUCE_TAIGA", "OLD_GROWTH_PINE_TAIGA",
            "WINDSWEPT_FOREST", "CHERRY_GROVE", "SAVANNA", "SAVANNA_PLATEAU"
    ));

    public NaturePower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) {
            thornRetaliation((DamagedByExecute) ex);
            return;
        }
        if (ex instanceof MoveExecute) {
            onMove((MoveExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) {
            onLeftClick((LeftClickExecute) ex);
            return;
        }
        if (ex instanceof SneakExecute) {
            onSneak((SneakExecute) ex);
        }
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0:
                if (onCd(nature_thorn, p, this)) return;
                thornVolley(p);
                addCd(nature_thorn, p);
                return;
            case 1:
                if (onCd(nature_entangle, p, this)) return;
                entangle(p);
                addCd(nature_entangle, p);
                return;
            case 2:
                if (onCd(nature_spore, p, this)) return;
                sporeBurst(p);
                addCd(nature_spore, p);
                return;
            case 3:
                if (onCd(nature_lance, p, this)) return;
                rootLance(p);
                addCd(nature_lance, p);
        }
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 4:
                if (onCd(nature_heal, p, this)) return;
                verdantHeal(p);
                addCd(nature_heal, p);
                return;
            case 0:
                if (onCd(nature_tree, p, this)) return;
                if (p.getWorld().generateTree(
                        p.getLocation().clone().add(p.getLocation().getDirection().clone().setY(0).normalize()),
                        TreeType.values()[new Random().nextInt(TreeType.values().length)])) {
                    addCd(nature_tree, p);
                } else {
                    p.sendMessage(ChatColor.RED + "无法在这里生成树木!");
                }
        }
    }

    private void thornVolley(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BAMBOO_HIT, 1f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.4f);

        int bolts = 5;
        for (int i = 0; i < bolts; i++) {
            final int yawOff = -20 + i * 10;
            final int idx = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    shootThorn(p, yawOff);
                }
            }.runTaskLater(magicPlugin, idx * 2L);
        }
    }

    private void shootThorn(Player p, int yawOffset) {
        ArmorStand thorn = spawnAs(p.getEyeLocation());
        Vector base = p.getEyeLocation().getDirection().clone().normalize();
        double rad = Math.toRadians(yawOffset);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double x = base.getX() * cos + base.getZ() * sin;
        double z = -base.getX() * sin + base.getZ() * cos;
        Vector dir = new Vector(x, base.getY(), z).normalize();

        Random r = new Random();
        new BukkitRunnable() {
            int t = 0;
            final Set<Entity> hit = new HashSet<>();

            @Override
            public void run() {
                if (thorn.isDead() || t > 45) { safeRemove(thorn); cancel(); return; }

                thorn.teleport(thorn.getLocation().add(dir.clone().multiply(1.8)));
                Location loc = thorn.getLocation();

                particleApi.spawnColoredParticles(loc, GREENS[r.nextInt(GREENS.length)], 1.1f, 4, 0.06, 0.06, 0.06);
                particleApi.spawnColoredParticles(loc, DARK_GREEN, 0.9f, 2, 0.04, 0.04, 0.04);
                if (t % 3 == 0)
                    particleApi.spawnParticles(loc, Particle.CHERRY_LEAVES, 2, 0.1, 0.1, 0.1, 0.05);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.1, 1.1, 1.1)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (e instanceof LivingEntity) {
                        hit.add(e);
                        double dmg = inNatureBiome(p) ? 10 : 7;
                        ((LivingEntity) e).damage(dmg, p);
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1, false, true));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                        thornHitBurst(loc);
                        safeRemove(thorn); cancel(); return;
                    }
                }

                if (!loc.getBlock().isPassable()) {
                    thornHitBurst(loc);
                    safeRemove(thorn); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void thornHitBurst(Location loc) {
        particleApi.spawnColoredParticles(loc, GREENS[0], 1.2f, 20, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, POISON_GREEN, 1f, 10, 0.2, 0.2, 0.2);
        particleApi.spawnParticles(loc, Particle.CHERRY_LEAVES, 15, 0.3, 0.3, 0.3, 0.2);
        loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_HIT, 1f, 0.8f);
    }

    private void entangle(Player p) {
        Location center = p.getLocation().clone().add(0, 0.5, 0);
        p.getWorld().playSound(center, Sound.BLOCK_AZALEA_LEAVES_HIT, 1f, 0.5f);
        p.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 1f, 0.6f);

        new BukkitRunnable() {
            double radius = 0.3;
            int t = 0;

            @Override
            public void run() {
                if (radius > 5.5) { cancel(); return; }
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18 + t * 7);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location lp = center.clone().add(x, 0, z);
                    particleApi.spawnColoredParticles(lp, GREENS[t % GREENS.length], 1.1f, 2, 0.04, 0.2, 0.04);
                    particleApi.spawnColoredParticles(lp, DARK_GREEN, 0.9f, 1, 0.03, 0.15, 0.03);
                }
                if (t % 4 == 0)
                    p.getWorld().playSound(center, Sound.BLOCK_ROOTED_DIRT_HIT, 0.5f, 0.8f);
                radius += 0.45;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        List<Entity> trapped = new ArrayList<>();
        for (Entity e : p.getWorld().getNearbyEntities(center, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            trapped.add(e);
        }

        if (trapped.isEmpty()) return;

        for (Entity e : trapped) {
            LivingEntity le = (LivingEntity) e;
            spawnRootVisual(le.getLocation());

            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 70, 10, false, false));
            le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 70, -10, false, false));

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 6 || !e.isValid()) { cancel(); return; }
                    le.damage(3.5, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 25, 0, false, true));
                    particleApi.spawnColoredParticles(le.getLocation().clone().add(0, 1, 0),
                            POISON_GREEN, 1f, 8, 0.3, 0.5, 0.3);
                    ticks++;
                }
            }.runTaskTimer(magicPlugin, 10L, 10L);
        }
    }

    private void spawnRootVisual(Location base) {
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            double ox = Math.cos(angle) * 0.8;
            double oz = Math.sin(angle) * 0.8;
            Location from = base.clone().add(ox, 0, oz);
            Location to   = base.clone().add(ox * 0.3, 1.8, oz * 0.3);
            for (double t2 = 0; t2 <= 1; t2 += 0.15) {
                Location lp = from.clone().add(to.toVector().subtract(from.toVector()).multiply(t2));
                particleApi.spawnColoredParticles(lp, GREENS[r.nextInt(GREENS.length)], 1f, 2, 0.04, 0.04, 0.04);
            }
        }
        base.getWorld().playSound(base, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.7f, 0.9f);
    }

    private void sporeBurst(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 0.4f);

        ArmorStand spore = spawnAs(p.getEyeLocation().clone().add(p.getEyeLocation().getDirection().normalize()));
        Vector dir = p.getEyeLocation().getDirection().clone().normalize().multiply(0.9);
        dir.setY(dir.getY() + 0.15);
        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (spore.isDead() || t > 60) { safeRemove(spore); cancel(); return; }

                spore.teleport(spore.getLocation().add(dir.clone()));
                particleApi.spawnColoredParticles(spore.getLocation(), SPORE_YELLOW, 1.3f, 6, 0.12, 0.12, 0.12);
                particleApi.spawnColoredParticles(spore.getLocation(), POISON_GREEN, 1f, 3, 0.1, 0.1, 0.1);
                particleApi.spawnParticles(spore.getLocation(), Particle.CHERRY_LEAVES, 4, 0.15, 0.15, 0.15, 0.1);
                for (Entity e : spore.getLocation().getWorld().getNearbyEntities(spore.getLocation(), 1.2, 1.2, 1.2)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        sporeExplosion(spore.getLocation(), p);
                        safeRemove(spore); cancel(); return;
                    }
                }
                if (!spore.getLocation().getBlock().isPassable()) {
                    sporeExplosion(spore.getLocation(), p);
                    safeRemove(spore); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void sporeExplosion(Location loc, Player p) {
        Random r = new Random();
        particleApi.spawnColoredParticles(loc, SPORE_YELLOW, 1.5f, 20, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(loc, POISON_GREEN, 1.2f, 20, 1.0, 1.0, 1.0);
        particleApi.spawnParticles(loc, Particle.CHERRY_LEAVES, 30, 1.5, 1.5, 1.5, 0.3);
        particleApi.spawnColoredParticles(loc, GREENS[r.nextInt(GREENS.length)], 1f, 30, 0.8, 0.8, 0.8);

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AZALEA_LEAVES_BREAK, 1f, 0.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 40) { cancel(); return; }
                particleApi.spawnColoredParticles(loc, POISON_GREEN, 1f,
                        8, 1.8, 0.8, 1.8);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 3);
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            double dmg = Math.max(4, 16 - dist * 2.2);
            ((LivingEntity) e).damage(dmg, p);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON,    120, 1, false, true));
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,  60, 0, false, true));
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   60, 1, false, true));
        }
    }

    private void rootLance(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 0.8f, 0.7f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0).normalize();
        Location start = p.getLocation().clone().add(0, 0.5, 0);
        Random r = new Random();
        HashMap<Block, Material> changed = new HashMap<>();
        Set<Entity> hit = new HashSet<>();

        new BukkitRunnable() {
            double dist = 1.5;
            int t = 0;

            @Override
            public void run() {
                if (dist > 16 || t > 40) {
                    restoreBlocks(changed, 30L);
                    cancel();
                    return;
                }

                Location tip = start.clone().add(dir.clone().multiply(dist));
                Location ground = tip.clone();
                while (ground.getBlock().isPassable() && ground.getY() > 0) ground.add(0, -1, 0);
                ground.add(0, 1, 0);

                if (!changed.containsKey(ground.getBlock()) && ground.getBlock().isPassable()
                        && !(ground.getBlock().getState() instanceof org.bukkit.block.Container)) {
                    changed.put(ground.getBlock(), ground.getBlock().getType());
                    ground.getBlock().setType(r.nextBoolean() ? Material.ROOTED_DIRT : Material.MOSS_BLOCK);
                }

                for (int ring = 0; ring < 6; ring++) {
                    double a = Math.toRadians(ring * 60 + t * 20);
                    double ox = Math.cos(a) * 0.5;
                    double oz = Math.sin(a) * 0.5;
                    Location lp = ground.clone().add(ox, r.nextDouble() * 1.5, oz);
                    particleApi.spawnColoredParticles(lp, GREENS[r.nextInt(GREENS.length)], 1.1f, 3, 0.05, 0.1, 0.05);
                    particleApi.spawnColoredParticles(lp, DARK_GREEN, 0.9f, 1, 0.04, 0.08, 0.04);
                }
                particleApi.spawnParticles(ground, Particle.CHERRY_LEAVES, 5, 0.4, 0.3, 0.4, 0.15);

                if (t % 3 == 0)
                    p.getWorld().playSound(ground, Sound.BLOCK_ROOTED_DIRT_HIT, 0.4f, r.nextFloat() + 0.5f);

                for (Entity e : ground.getWorld().getNearbyEntities(ground, 1.5, 2.0, 1.5)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e)) continue;
                    if (e instanceof LivingEntity) {
                        hit.add(e);
                        double dmg = inNatureBiome(p) ? 26 : 20;
                        ((LivingEntity) e).damage(dmg, p);
                        e.setVelocity(new Vector(dir.getX() * 0.5, 1.2, dir.getZ() * 0.5));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3, false, false));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON,   60, 0, false, true));
                        particleApi.spawnColoredParticles(e.getLocation().clone().add(0, 1, 0),
                                GREENS[0], 1.5f, 40, 0.5, 0.6, 0.5);
                        p.getWorld().playSound(e.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1f, 0.9f);
                    }
                }

                dist += 1.1;
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void verdantHeal(Player p) {
        boolean nature = inNatureBiome(p);
        int healAmount = nature ? 12 : 6;
        double healPerTick = (double) healAmount / 20;

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GRASS_PLACE, 1f, 0.7f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AZALEA_LEAVES_PLACE, 1f, 0.8f);

        if (nature) {
            p.sendMessage(ChatColor.GREEN + "✦ 自然之力在此处增强!");
        }
        new BukkitRunnable() {
            int t = 0;
            double healed = 0;

            @Override
            public void run() {
                if (t >= 40 || !p.isOnline()) {
                    p.removePotionEffect(PotionEffectType.POISON);
                    p.removePotionEffect(PotionEffectType.WITHER);
                    p.removePotionEffect(PotionEffectType.SLOWNESS);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.4f);
                    cancel();
                    return;
                }

                double hp = p.getHealth();
                p.setHealth(Math.min(getMaxHp(p), hp + healPerTick));
                healed += healPerTick;

                double angle = Math.toRadians(t * 27);
                double radius = 1.2 - t * 0.02;
                for (int ring = 0; ring < 3; ring++) {
                    double a = angle + Math.toRadians(ring * 120);
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    Location lp = p.getLocation().clone().add(x, t * 0.05, z);
                    particleApi.spawnColoredParticles(lp, GREENS[ring % GREENS.length], 1.2f, 2, 0.04, 0.04, 0.04);
                    if (nature && t % 4 == 0)
                        particleApi.spawnParticles(lp, Particle.CHERRY_LEAVES, 2, 0.1, 0.1, 0.1, 0.05);
                }
                particleApi.spawnColoredParticles(
                        p.getLocation().clone().add(0, 0.1, 0),
                        POISON_GREEN, 0.9f, 4, 0.6, 0.05, 0.6);

                if (t % 10 == 0)
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GRASS_HIT, 0.5f, 1.2f);

                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void thornRetaliation(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (CooldownApi.isOnCooldown(nature_retaliate, p)) return;

        Entity damager = ((EntityDamageByEntityEvent) ex.getRawEvent()).getDamager();
        if (!(damager instanceof LivingEntity)) return;

        ((LivingEntity) damager).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1, false, true));
        ((LivingEntity) damager).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
        ((LivingEntity) damager).damage(3, p);

        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0),
                GREENS[0], 1.3f, 25, 0.5, 0.5, 0.5);
        particleApi.spawnParticles(p.getLocation().clone().add(0, 1, 0),
                Particle.CHERRY_LEAVES, 20, 0.5, 0.5, 0.5, 0.2);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AZALEA_LEAVES_HIT, 1f, 0.7f);

        addCdFixed(nature_retaliate, p, 5.0);
    }

    private long lastMoveProcess = 0;

    private void onMove(MoveExecute ex) {
        long now = System.currentTimeMillis();
        if (now - lastMoveProcess < 500) return;
        lastMoveProcess = now;

        Player p = ex.getPlayer();
        if (!(p.isOnGround() && !p.isSwimming())) return;
        Block below = p.getLocation().clone().add(0, -1, 0).getBlock();
        Block above = p.getLocation().getBlock();
        if (below.getType() == Material.GRASS_BLOCK && above.getType() == Material.AIR) {
            if (new Random().nextInt(5) == 0) {
                above.setType(Material.SHORT_GRASS);
            }
        }
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (isAuraEnabled(p))
                    particleApi.spawnParticles(p.getLocation().clone().add(0, 0.1, 0),
                            Particle.CHERRY_LEAVES, 2, 0.4, 0.02, 0.4, 0.1);
                if (inNatureBiome(p)) {
                    if (t % 4 == 0) {
                        double hp = p.getHealth();
                        if (hp < getMaxHp(p))
                            p.setHealth(Math.min(getMaxHp(p), hp + 1.0));
                    }
                    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 25, 0, false, false));
                    if (isAuraEnabled(p))
                        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 0.5, 0),
                                GREENS[t % GREENS.length], 0.9f, 3, 0.35, 0.3, 0.35);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 20);
        return r;
    }

    @Override
    public void remove() {
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&a荆棘齐射";
            case 1: return "&a缠绕";
            case 2: return "&a孢子爆发";
            case 3: return "&2根矛";
            case 4: return "&a翠绿治愈";
            default: return "&7none";
        }
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false);
            en.setGravity(false);
            en.setSmall(true);
            en.setMarker(true);
        });
    }

    private void restoreBlocks(HashMap<Block, Material> blocks, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block b : blocks.keySet()) b.setType(blocks.get(b));
                blocks.clear();
            }
        }.runTaskLater(magicPlugin, delayTicks);
    }

    private boolean inNatureBiome(Player p) {
        String biome = p.getLocation().getBlock().getBiome().name();
        return NATURE_BIOMES.contains(biome);
    }
}

