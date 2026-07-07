package net.trduc.magicabilitiesfork.misc;

import net.trduc.magicabilitiesfork.cooldowns.Cooldowns;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public final class CooldownValidator {

    private static final double DEFAULT_FALLBACK = 5.0;

    private static final List<String> REQUIRED_KEYS = Arrays.asList(
        "ice.bolt", "ice.machine-gun", "ice.spikes", "ice.star",
        "ice.multi-bolt", "ice.slashes", "ice.phase-change",
        "warp.default",
        "lightning.strike", "lightning.shot", "lightning.field",
        "lightning.transmission", "lightning.passive",
        "lightning.thunderclap", "lightning.ball",
        "unstable.heal-by-others", "unstable.heal-self",
        "shogun.double-jump", "shogun.dash",
        "fire.blast", "fire.barrage", "fire.surge", "fire.flame",
        "fire.meteor", "fire.shield", "fire.dash", "fire.retaliation",
        "witcher.igni", "witcher.aard", "witcher.quen",
        "witcher.aksji", "witcher.yrden",
        "nature.tree", "nature.thorn", "nature.entangle", "nature.spore",
        "nature.lance", "nature.heal", "nature.retaliate",
        "phoenix.wings", "phoenix.flame", "phoenix.storm", "phoenix.dive",
        "phoenix.beam", "phoenix.tornado", "phoenix.ascend", "phoenix.rebirth",
        "twilight-mirage.shriek-transition", "twilight-mirage.float",
        "twilight-mirage.missile", "twilight-mirage.healing", "twilight-mirage.eclipse",
        "eternity.immunity", "eternity.oblivion", "eternity.blink",
        "curse-weaver.domain", "curse-weaver.cleave",
        "curse-weaver.black", "curse-weaver.dawn",
        "sound.burst", "sound.resonance", "sound.dissonance",
        "sound.veil", "sound.wail", "sound.domain",
        "death.rend", "death.scythe", "death.step",
        "death.grasp", "death.shroud", "death.domain",
        "thundergod.triple", "thundergod.wrath", "thundergod.cage",
        "thundergod.step", "thundergod.judgment", "thundergod.aura",
        "wind.slash", "wind.cyclone", "wind.burst",
        "wind.step", "wind.tempest", "wind.leap",
        "demon.soulbolt", "demon.hellfire", "demon.cleave", "demon.drain",
        "demon.charge", "demon.grasp", "demon.counter",
        "water.bolt", "water.whirlpool", "water.wave", "water.dash",
        "water.tsunami", "water.bubble", "water.counter", "water.hydro",
        "wither.bolt", "wither.barrage", "wither.mark", "wither.shatter",
        "wither.storm", "wither.rez", "wither.aura", "wither.kill",
        "icedragon.roar", "icedragon.slash", "icedragon.prison",
        "icedragon.charge", "icedragon.blizzard", "icedragon.heaven",
        "wooddragon.roar", "wooddragon.blades", "wooddragon.net",
        "wooddragon.charge", "wooddragon.grove", "wooddragon.roots",
        "snowblade.slash", "snowblade.step", "snowblade.drive", "snowblade.arctic",
        "meteor.shot", "meteor.rain", "meteor.slam", "meteor.gravity",
        "meteor.armor", "meteor.extinction", "meteor.scorch",
        "cultivator.sword-qi", "cultivator.seal", "cultivator.pressure",
        "cultivator.meditate", "cultivator.thunder-array", "cultivator.judgment",
        "huashan.tam-tai", "huashan.luc-hop", "huashan.nhi-thap-tu",
        "huashan.mai-hoa-bo", "huashan.chan-thien", "huashan.lac-hoa",
        "huashan.mai-hoa-an", "huashan.vo-hang", "huashan.that-thap-nhi",
        "huashan.bach-hoa", "huashan.kiem-y",
        "demonlord.inferno_blade", "demonlord.summon", "demonlord.prison",
        "demonlord.devour", "demonlord.shadow_step",
        "demonlord.judgment", "demonlord.fury",
        "gravity.pull", "gravity.push", "gravity.crush",
        "gravity.field", "gravity.reverse", "gravity.collapse",
        "shockwave.pulse", "shockwave.slam", "shockwave.blast", "shockwave.rift",
        "shockwave.barrage", "shockwave.barrier", "shockwave.dash", "shockwave.counter",
        "portal.bolt", "portal.phase", "portal.mirror", "portal.rift",
        "portal.barrage", "portal.step", "portal.blink", "portal.gate", "portal.dodge",
        "cloud.bolt", "cloud.ascend", "cloud.fog", "cloud.slam", "cloud.barrage",
        "cloud.step", "cloud.dash", "cloud.storm", "cloud.mist",
        "vampire.lance", "vampire.dash", "vampire.nova", "vampire.grip",
        "vampire.swarm", "vampire.mist", "vampire.leap", "vampire.moon", "vampire.instinct",
        "poison.bolt", "poison.slash", "poison.cloud", "poison.strike",
        "poison.barrage", "poison.armor", "poison.shadow", "poison.deluge", "poison.counter",
        "air.shot", "air.chain", "air.vacuum", "air.jet", "air.barrage",
        "air.field", "air.step", "air.collapse", "air.counter",
        "blood.scythe", "blood.puppet", "blood.burst", "blood.veil", "blood.sacrifice",
        "crystal.shell", "crystal.spire", "crystal.field", "crystal.prison", "crystal.shatter",
        "earth.impact", "earth.fortress", "earth.boulder", "earth.tomb", "earth.slam",
        "spike.strike", "spike.burst", "spike.grasp", "spike.wall", "spike.storm",
        "magnetic.pull", "magnetic.wave", "magnetic.grip",
        "magnetic.polarity", "magnetic.scrap", "magnetic.cage",
        "potato.get", "potato.shoot",
        "lunar.crescent", "lunar.shadow", "lunar.tide", "lunar.eclipse", "lunar.domain"
    );

    private CooldownValidator() {}

    public static void validate(JavaPlugin plugin, Cooldowns cooldowns) {
        Logger log = plugin.getLogger();
        List<String> missing  = new ArrayList<>();
        List<String> fallback = new ArrayList<>();

        for (String key : REQUIRED_KEYS) {
            if (!cooldowns.containsKey(key)) {
                missing.add(key);
            }
        }

        if (missing.isEmpty()) {
            log.info("[CooldownValidator] All cooldown keys are valid.");
            return;
        }

        log.warning("[CooldownValidator] Detected " + missing.size()
                + " missing cooldown key(s) in cooldowns.yml:");
        for (String k : missing) {
            log.warning("  MISSING: " + k + " → using fallback " + DEFAULT_FALLBACK + "s");
            fallback.add(k);
        }

        log.warning("[CooldownValidator] Skills with missing keys will use fallback "
                + DEFAULT_FALLBACK + "s cooldown instead of crashing.");
    }

    public static double getFallback() {
        return DEFAULT_FALLBACK;
    }
}

