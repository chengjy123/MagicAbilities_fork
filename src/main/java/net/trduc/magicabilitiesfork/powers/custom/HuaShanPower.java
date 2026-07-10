package net.trduc.magicabilitiesfork.powers.custom;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
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

public class HuaShanPower extends Power implements IdlePower, Removeable {

    private static final String hs_tamtai   = "huashan.tam-tai";
    private static final String hs_luchop   = "huashan.luc-hop";
    private static final String hs_24       = "huashan.nhi-thap-tu";
    private static final String hs_maihoabo = "huashan.mai-hoa-bo";
    private static final String hs_chanthien= "huashan.chan-thien";
    private static final String hs_lachoa   = "huashan.lac-hoa";
    private static final String hs_maihoaan = "huashan.mai-hoa-an";
    private static final String hs_vohang   = "huashan.vo-hang";
    private static final String hs_72       = "huashan.that-thap-nhi";
    private static final String hs_bachhoa  = "huashan.bach-hoa";
    private static final String hs_kiemy    = "huashan.kiem-y";

    private int UNLOCK_1, UNLOCK_2, UNLOCK_3, UNLOCK_4, UNLOCK_5;

    private double DMG_TAMTAI;
    private double LUCHOP_REDUCTION, LUCHOP_DURATION;
    private double DMG_24_HIT;       private int HITS_24;
    private double DMG_CHANTHIEN, CHANTHIEN_STUN;
    private double LACHOA_REDUCTION, LACHOA_RADIUS, LACHOA_DURATION;
    private double DMG_MAIHOAAN, MAIHOAAN_PIERCE;
    private double DMG_VOHANG;
    private double DMG_72_HIT;       private int HITS_72;
    private double DMG_BACHHOA_HIT;  private int HITS_BACHHOA;
    private double KIEMY_RADIUS, KIEMY_DMG_TICK, KIEMY_DURATION, KIEMY_CHARGE;
    private int    PASSIVE_REGEN_INTERVAL; private double PASSIVE_REGEN_AMOUNT, PASSIVE_MAXHP_BONUS;

    private static final Color C_SAKURA       = Color.fromRGB(255, 183, 206);
    private static final Color C_SAKURA_LIGHT = Color.fromRGB(255, 224, 235);
    private static final Color C_LOTUS_WHITE  = Color.fromRGB(255, 245, 248);
    private static final Color C_SAKURA_DEEP  = Color.fromRGB(235, 110, 150);
    private static final Color C_VIOLET_DUSK  = Color.fromRGB(190, 150, 220);

    private static final Color[] AURA_COLS  = { C_SAKURA, C_SAKURA_LIGHT, C_LOTUS_WHITE };
    private static final Color[] BLOOM_COLS = { C_SAKURA, C_SAKURA_LIGHT, C_SAKURA_DEEP, C_LOTUS_WHITE };

    private final Map<UUID, Long> lucHopBuffUntil = new HashMap<>();

    private final Map<UUID, Long> lacHoaDebuffUntil = new HashMap<>();

    private boolean charging = false;
    private BukkitRunnable chargeTask = null;

    private long lastCombatTick = 0;

    private BukkitRunnable hudTask = null;
    private int tickCounter = 0;
    private final MessagesManager messages = MessagesManager.getInstance();

    public HuaShanPower(Player owner) {
        super(owner);
        loadConfig();
        applyMaxHpBonus(owner, true);
    }

    private void loadConfig() {
        FileConfiguration cfg = magicPlugin.getConfig();

        UNLOCK_1 = cfg.getInt("huashan.unlock.slot1", 30);
        UNLOCK_2 = cfg.getInt("huashan.unlock.slot2", 50);
        UNLOCK_3 = cfg.getInt("huashan.unlock.slot3", 70);
        UNLOCK_4 = cfg.getInt("huashan.unlock.slot4", 90);
        UNLOCK_5 = cfg.getInt("huashan.unlock.slot5", 100);

        DMG_TAMTAI         = cfg.getDouble("huashan.dmg.tam-tai", 5.0);
        LUCHOP_REDUCTION   = cfg.getDouble("huashan.dmg.luc-hop-reduction", 0.35);
        LUCHOP_DURATION    = cfg.getDouble("huashan.dmg.luc-hop-duration", 4.0);
        DMG_24_HIT         = cfg.getDouble("huashan.dmg.nhi-thap-tu-hit", 4.0);
        HITS_24            = cfg.getInt("huashan.dmg.nhi-thap-tu-hits", 5);
        DMG_CHANTHIEN      = cfg.getDouble("huashan.dmg.chan-thien", 9.0);
        CHANTHIEN_STUN     = cfg.getDouble("huashan.dmg.chan-thien-stun", 1.2);
        LACHOA_REDUCTION   = cfg.getDouble("huashan.dmg.lac-hoa-reduction", 0.4);
        LACHOA_RADIUS      = cfg.getDouble("huashan.dmg.lac-hoa-radius", 4.0);
        LACHOA_DURATION    = cfg.getDouble("huashan.dmg.lac-hoa-duration", 5.0);
        DMG_MAIHOAAN       = cfg.getDouble("huashan.dmg.mai-hoa-an", 16.0);
        MAIHOAAN_PIERCE    = cfg.getDouble("huashan.dmg.mai-hoa-an-armor-pierce", 0.5);
        DMG_VOHANG         = cfg.getDouble("huashan.dmg.vo-hang", 22.0);
        DMG_72_HIT         = cfg.getDouble("huashan.dmg.that-thap-nhi-hit", 5.0);
        HITS_72            = cfg.getInt("huashan.dmg.that-thap-nhi-hits", 9);
        DMG_BACHHOA_HIT    = cfg.getDouble("huashan.dmg.bach-hoa-hit", 4.5);
        HITS_BACHHOA       = cfg.getInt("huashan.dmg.bach-hoa-hits", 7);
        KIEMY_RADIUS       = cfg.getDouble("huashan.dmg.kiem-y-radius", 6.0);
        KIEMY_DMG_TICK     = cfg.getDouble("huashan.dmg.kiem-y-dmg-per-tick", 3.0);
        KIEMY_DURATION     = cfg.getDouble("huashan.dmg.kiem-y-duration", 4.0);
        KIEMY_CHARGE       = cfg.getDouble("huashan.dmg.kiem-y-charge", 1.8);

        PASSIVE_REGEN_INTERVAL = cfg.getInt("huashan.passive.regen-interval", 60);
        PASSIVE_REGEN_AMOUNT   = cfg.getDouble("huashan.passive.regen-amount", 1.0);
        PASSIVE_MAXHP_BONUS    = cfg.getDouble("huashan.passive.max-hp-bonus", 2.0);
    }

    @Override
    public void executePower(Execute ex) {

        if (ex instanceof DamagedExecute)   { onDamaged((DamagedExecute) ex);   return; }
        if (ex instanceof DamagedByExecute) { lastCombatTick = tickCounter;     return; }

        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) { onLeft((LeftClickExecute) ex); }
    }

    private void onDamaged(DamagedExecute ex) {
        Player p = ex.getPlayer();
        org.bukkit.event.entity.EntityDamageEvent event =
                (org.bukkit.event.entity.EntityDamageEvent) ex.getRawEvent();
        lastCombatTick = tickCounter;

        double multiplier = 1.0;
        long now = System.currentTimeMillis();

        Long lucHopUntil = lucHopBuffUntil.get(p.getUniqueId());
        if (lucHopUntil != null && lucHopUntil > now) {
            multiplier *= (1.0 - LUCHOP_REDUCTION);
        }

        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
            Entity damager = ((org.bukkit.event.entity.EntityDamageByEntityEvent) event).getDamager();
            Long lacHoaUntil = lacHoaDebuffUntil.get(damager.getUniqueId());
            if (lacHoaUntil != null && lacHoaUntil > now) {
                multiplier *= (1.0 - LACHOA_REDUCTION);
            }
        }

        if (multiplier < 1.0) {
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p     = ex.getPlayer();
        int    slot  = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        boolean sneak = p.isSneaking();

        if (!isSlotUnlocked(p, slot)) {
            hud(p, "§c Need " + unlockNeeded(slot) + " XP to use this ability!");
            return;
        }

        switch (slot) {
            case 0:
                if (sneak) lucHopKiem(p); else tamTaiKiem(p);
                return;
            case 1:
                if (sneak) maiHoaBo(p);   else nhiThapTuMaiHoa(p);
                return;
            case 2:
                if (sneak) lacHoaKiemPhap(p); else chanThienKiem(p);
                return;
            case 3:
                if (sneak) voHangKiem(p); else maiHoaAn(p);
                return;
            case 4:
                if (sneak) bachHoaTuTaiKiem(p); else thatThapNhiMaiHoa(p);
                return;
            case 5:
                if (!sneak) kiemY(p);
                return;
        }
    }

    private void tamTaiKiem(Player p) {
        if (onCd(hs_tamtai, p, this)) return;
        lastCombatTick = tickCounter;

        Location eye = p.getEyeLocation();
        Vector   dir = eye.getDirection().normalize();
        double[] pitchOffsets = { 0.0, 12.0, -12.0 };

        for (double off : pitchOffsets) {
            Vector v = pitchRotate(dir, off);
            Location end = eye.clone().add(v.clone().multiply(6));
            List<Entity> hit = particleApi.drawColoredLine(eye, end, 0.4, C_SAKURA, 1.1f, 0.3);
            for (Entity e : hit) {
                if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                ((LivingEntity) e).damage(DMG_TAMTAI, p);
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
        addCd(hs_tamtai, p);
    }

    private void lucHopKiem(Player p) {
        if (onCd(hs_luchop, p, this)) return;
        lucHopBuffUntil.put(p.getUniqueId(),
                System.currentTimeMillis() + (long) (LUCHOP_DURATION * 1000));
        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, (int) (LUCHOP_DURATION * 20), 1, false, true));
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.5, 0), C_LOTUS_WHITE, 0.9f, 1, 0, 0, 0);
        safeCircle(p.getLocation().clone().add(0, 0.1, 0), 1.3, C_LOTUS_WHITE, 0.9f, 24);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
        hud(p, "§d Six-Harmonies Sword: Reduced " + (int) (LUCHOP_REDUCTION * 100) + "% dmg received!");
        addCd(hs_luchop, p);
    }

    private void nhiThapTuMaiHoa(Player p) {
        if (onCd(hs_24, p, this)) return;
        lastCombatTick = tickCounter;

        LivingEntity target = getInSight(p, 8.0, 0.5);
        Vector dir = (target != null)
                ? target.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize()
                : p.getLocation().getDirection().setY(0).normalize();

        new BukkitRunnable() {
            int hitsLeft = HITS_24;
            @Override public void run() {
                if (!p.isOnline() || hitsLeft <= 0) { cancel(); return; }
                Location loc = p.getLocation().clone().add(dir.clone().multiply(1.2)).add(0, 1, 0);
                p.teleport(loc.clone().setDirection(p.getLocation().getDirection()));
                bloomBurst(loc, BLOOM_COLS, 6, 0.4);
                for (Entity e : p.getWorld().getNearbyEntities(loc, 1.6, 1.6, 1.6)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(DMG_24_HIT, p);
                }
                p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
                hitsLeft--;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        addCd(hs_24, p);
    }

    private void maiHoaBo(Player p) {
        if (onCd(hs_maihoabo, p, this)) return;
        Vector dir = p.getLocation().getDirection().setY(0.15).normalize().multiply(1.6);
        p.setVelocity(dir);
        bloomBurst(p.getLocation().clone().add(0, 0.2, 0), BLOOM_COLS, 10, 0.5);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.8f);
        addCd(hs_maihoabo, p);
    }

    private void chanThienKiem(Player p) {
        if (onCd(hs_chanthien, p, this)) return;
        lastCombatTick = tickCounter;

        LivingEntity target = getInSight(p, 5.0, 0.4);
        if (target == null) { hud(p, "§7 No target in sight!"); return; }

        target.damage(DMG_CHANTHIEN, p);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (CHANTHIEN_STUN * 20), 4, false, true));
        Location loc = target.getLocation().clone().add(0, 1, 0);
        safeCircle(loc, 1.0, C_SAKURA_DEEP, 1.3f, 18);
        p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
        addCd(hs_chanthien, p);
    }

    private void lacHoaKiemPhap(Player p) {
        if (onCd(hs_lachoa, p, this)) return;
        Location center = p.getLocation();
        long until = System.currentTimeMillis() + (long) (LACHOA_DURATION * 1000);

        for (Entity e : center.getWorld().getNearbyEntities(center, LACHOA_RADIUS, LACHOA_RADIUS, LACHOA_RADIUS)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            lacHoaDebuffUntil.put(e.getUniqueId(), until);
        }
        safeCircle(center.clone().add(0, 0.1, 0), LACHOA_RADIUS, C_SAKURA_LIGHT, 1.0f, 28);
        p.getWorld().playSound(center, Sound.BLOCK_AZALEA_LEAVES_STEP, 1.5f, 0.6f);
        hud(p, "§d Falling Blossom: softening zone deployed!");
        addCd(hs_lachoa, p);
    }

    private void maiHoaAn(Player p) {
        if (onCd(hs_maihoaan, p, this)) return;
        lastCombatTick = tickCounter;

        Location loc = p.getEyeLocation();
        Vector   dir = loc.getDirection().normalize();
        Set<UUID> hitAlready = new HashSet<>();

        new BukkitRunnable() {
            double travelled = 0;
            @Override public void run() {
                if (!p.isOnline() || travelled > 22) { cancel(); return; }
                loc.add(dir.clone().multiply(1.0));
                travelled += 1.0;
                drawFivePetalSeal(loc, dir, C_LOTUS_WHITE, C_VIOLET_DUSK);
                if (!loc.getChunk().isLoaded()) { cancel(); return; }
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.3, 1.3, 1.3)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || hitAlready.contains(e.getUniqueId())) continue;
                    LivingEntity le = (LivingEntity) e;
                    applyArmorPiercingDamage(le, DMG_MAIHOAAN, MAIHOAAN_PIERCE, p);
                    hitAlready.add(e.getUniqueId());
                }
                if (loc.getBlock().getType().isSolid()) cancel();
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.3f);
        addCd(hs_maihoaan, p);
    }

    private void voHangKiem(Player p) {
        if (onCd(hs_vohang, p, this)) return;
        lastCombatTick = tickCounter;

        LivingEntity target = getNearestTarget(p, 7.0);
        if (target == null) { hud(p, "§7 No target in sight!"); return; }

        Vector behind = target.getLocation().getDirection().normalize().multiply(-1.0);
        Location dest = target.getLocation().clone().add(behind).add(0, 0, 0);
        dest.setDirection(target.getLocation().toVector().subtract(dest.toVector()));
        p.teleport(dest);
        target.damage(DMG_VOHANG, p);

        particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0), C_LOTUS_WHITE, 1.0f, 3, 0.1, 0.3, 0.1);
        p.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.9f);
        addCd(hs_vohang, p);
    }

    private void thatThapNhiMaiHoa(Player p) {
        if (onCd(hs_72, p, this)) return;
        lastCombatTick = tickCounter;

        Location center = p.getLocation();
        new BukkitRunnable() {
            int hitsLeft = HITS_72;
            final Random rnd = new Random();
            @Override public void run() {
                if (!p.isOnline() || hitsLeft <= 0) { cancel(); return; }
                double angle = rnd.nextDouble() * 360;
                double radius = 1.5 + rnd.nextDouble() * 3.0;
                Vector offset = new Vector(Math.cos(Math.toRadians(angle)) * radius, 0,
                                            Math.sin(Math.toRadians(angle)) * radius);
                Location loc = center.clone().add(offset).add(0, 1, 0);
                bloomBurst(loc, BLOOM_COLS, 8, 0.5);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.4, 1.4, 1.4)) {
                    if (e.equals(p) || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(DMG_72_HIT, p);
                }
                p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.6f + rnd.nextFloat() * 0.4f);
                hitsLeft--;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        addCd(hs_72, p);
    }

    private void bachHoaTuTaiKiem(Player p) {
        if (onCd(hs_bachhoa, p, this)) return;
        lastCombatTick = tickCounter;

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 6, 6, 6)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            targets.add((LivingEntity) e);
        }
        if (targets.isEmpty()) { hud(p, "§7 No target in sight!"); return; }

        new BukkitRunnable() {
            int hitsLeft = HITS_BACHHOA;
            final Random rnd = new Random();
            @Override public void run() {
                if (!p.isOnline() || hitsLeft <= 0 || targets.isEmpty()) { cancel(); return; }
                LivingEntity tgt = targets.get(rnd.nextInt(targets.size()));
                if (!tgt.isValid()) { targets.remove(tgt); return; }
                tgt.damage(DMG_BACHHOA_HIT, p);
                bloomBurst(tgt.getLocation().clone().add(0, 1, 0), BLOOM_COLS, 7, 0.6);
                p.getWorld().playSound(tgt.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 2.0f);
                hitsLeft--;
            }
        }.runTaskTimer(magicPlugin, 0, 2);

        addCd(hs_bachhoa, p);
    }

    private void kiemY(Player p) {
        if (charging) { hud(p, "§d Condensing Sword Intent..."); return; }
        if (onCd(hs_kiemy, p, this)) return;
        charging = true;
        lastCombatTick = tickCounter;

        chargeTask = new BukkitRunnable() {
            double t = 0;
            @Override public void run() {
                if (!p.isOnline() || !charging) { cancel(); charging = false; return; }
                t += 0.05;
                safeCircle(p.getLocation().clone().add(0, 0.1, 0), 0.6 + t, C_VIOLET_DUSK, 0.8f, 16);
                particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.2, 0),
                        C_LOTUS_WHITE, 0.7f, 2, 0.3, 0.3, 0.3);
                if (t >= KIEMY_CHARGE) {
                    cancel();
                    charging = false;
                    releaseKiemY(p);
                }
            }
        };
        chargeTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void releaseKiemY(Player p) {
        Location center = p.getLocation();
        safeCircle(center.clone().add(0, 0.1, 0), KIEMY_RADIUS, C_SAKURA, 1.4f, 36);
        bloomBurst(center.clone().add(0, 1, 0), BLOOM_COLS, 30, 1.2);
        p.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 1.6f);
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, 0.8f);

        Set<UUID> affected = new HashSet<>();
        for (Entity e : center.getWorld().getNearbyEntities(center, KIEMY_RADIUS, KIEMY_RADIUS, KIEMY_RADIUS)) {
            if (e.equals(p) || !(e instanceof LivingEntity)) continue;
            affected.add(e.getUniqueId());
        }

        new BukkitRunnable() {
            double elapsed = 0;
            @Override public void run() {
                if (!p.isOnline() || elapsed >= KIEMY_DURATION) { cancel(); return; }
                for (Entity e : center.getWorld().getEntities()) {
                    if (!(e instanceof LivingEntity) || !affected.contains(e.getUniqueId())) continue;
                    LivingEntity le = (LivingEntity) e;
                    if (!le.isValid() || le.isDead()) continue;
                    le.damage(KIEMY_DMG_TICK, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, true));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, true));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 30, 0, false, true));
                }
                elapsed += 1.0;
            }
        }.runTaskTimer(magicPlugin, 0, 20);

        addCd(hs_kiemy, p);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        hudTask = new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                tickCounter++;

                if (tickCounter % 6 == 0 && p.getVelocity().lengthSquared() < 0.01) {
                    Location loc = p.getLocation().clone().add(0, 1.0, 0);
                    Color col = AURA_COLS[new Random().nextInt(AURA_COLS.length)];
                    particleApi.spawnColoredParticles(loc, col, 0.6f, 1,
                            0.4, 0.6, 0.4);
                }

                if (tickCounter % PASSIVE_REGEN_INTERVAL == 0
                        && (tickCounter - lastCombatTick) > PASSIVE_REGEN_INTERVAL
                        && p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),
                            p.getHealth() + PASSIVE_REGEN_AMOUNT));
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0),
                            C_VIOLET_DUSK, 0.7f, 4, 0.3, 0.5, 0.3);
                }

                long now = System.currentTimeMillis();
                lucHopBuffUntil.values().removeIf(t -> t < now);
                lacHoaDebuffUntil.entrySet().removeIf(en -> en.getValue() < now);

                if (tickCounter % 20 == 0) hud(p, null);
            }
        };
        hudTask.runTaskTimer(magicPlugin, 0, 1);
        return hudTask;
    }

    @Override
    public void remove() {
        if (chargeTask != null) { try { chargeTask.cancel(); } catch (Exception ignored) {} chargeTask = null; }
        if (hudTask != null)    { try { hudTask.cancel();    } catch (Exception ignored) {} hudTask = null; }
        charging = false;
        applyMaxHpBonus(getOwner(), false);
    }

    private void hud(Player p, String msg) {
        int curXp = calcTotalXp(p);
        String stageName = stageNameForXp(curXp);
        String bar = "§d✦ " + stageName + " §7[" + curXp + " XP]";
        String m = msg != null ? " §r §f" + msg : "";
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar + m));
    }

    private String stageNameForXp(int xp) {
        if (xp >= UNLOCK_5) return ChatColor.translateAlternateColorCodes('&', messages.get("powers.huashan.stage5"));
        if (xp >= UNLOCK_4) return ChatColor.translateAlternateColorCodes('&', messages.get("powers.huashan.stage4"));
        if (xp >= UNLOCK_3) return ChatColor.translateAlternateColorCodes('&', messages.get("powers.huashan.stage3"));
        if (xp >= UNLOCK_2) return ChatColor.translateAlternateColorCodes('&', messages.get("powers.huashan.stage2"));
        if (xp >= UNLOCK_1) return ChatColor.translateAlternateColorCodes('&', messages.get("powers.huashan.stage1"));
        return ChatColor.translateAlternateColorCodes('&', messages.get("powers.huashan.stage0"));
    }

    private boolean isSlotUnlocked(Player p, int slot) {
        int xp = calcTotalXp(p);
        switch (slot) {
            case 0: return true;
            case 1: return xp >= UNLOCK_1;
            case 2: return xp >= UNLOCK_2;
            case 3: return xp >= UNLOCK_3;
            case 4: return xp >= UNLOCK_4;
            case 5: return xp >= UNLOCK_5;
            default: return false;
        }
    }

    private int unlockNeeded(int slot) {
        switch (slot) {
            case 1: return UNLOCK_1;
            case 2: return UNLOCK_2;
            case 3: return UNLOCK_3;
            case 4: return UNLOCK_4;
            case 5: return UNLOCK_5;
            default: return 0;
        }
    }

    private void applyArmorPiercingDamage(LivingEntity target, double dmg, double pierceRatio, Player source) {
        if (target instanceof Player) {

            target.damage(dmg * (1 + pierceRatio), source);
        } else {
            target.damage(dmg, source);
        }
    }

    private void applyMaxHpBonus(Player p, boolean add) {
        AttributeInstance ai = p.getAttribute(Attribute.MAX_HEALTH);
        if (ai == null) return;
        double base = ai.getBaseValue();
        if (add) {
            ai.setBaseValue(base + PASSIVE_MAXHP_BONUS);
        } else {
            ai.setBaseValue(Math.max(1, base - PASSIVE_MAXHP_BONUS));
        }
    }

    private void bloomBurst(Location loc, Color[] palette, int amount, double spread) {
        Random rnd = new Random();
        for (int i = 0; i < amount; i++) {
            Color c = palette[rnd.nextInt(palette.length)];
            particleApi.spawnColoredParticles(loc, c, 0.7f + rnd.nextFloat() * 0.5f, 1,
                    spread, spread, spread);
        }
    }

    private void safeCircle(Location center, double radius, Color color, float size, int steps) {
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            Location p = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            particleApi.spawnColoredParticles(p, color, size, 1, 0, 0, 0);
        }
    }

    private void drawFivePetalSeal(Location center, Vector forward, Color outer, Color inner) {
        Vector up = Math.abs(forward.getY()) > 0.9 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(up).normalize();
        Vector trueUp = right.clone().crossProduct(forward).normalize();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72);
            Vector petal = right.clone().multiply(Math.cos(angle)).add(trueUp.clone().multiply(Math.sin(angle)))
                    .multiply(0.5);
            Location p1 = center.clone().add(petal);
            particleApi.spawnColoredParticles(p1, outer, 0.8f, 1, 0.05, 0.05, 0.05);
        }
        particleApi.spawnColoredParticles(center, inner, 0.6f, 1, 0.05, 0.05, 0.05);
    }

}

