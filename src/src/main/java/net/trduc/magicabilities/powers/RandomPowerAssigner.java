package net.trduc.magicabilities.powers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomPowerAssigner {

    private static final List<PowerType> POOL = Collections.unmodifiableList(Arrays.asList(
            PowerType.ICE,
            PowerType.LIGHTNING,
            PowerType.SHOGUN,
            PowerType.FIRE,
            PowerType.WITCHER,
            PowerType.NATURE,
            PowerType.PHOENIX,
            PowerType.TWILIGHT_MIRAGE,
            PowerType.ETERNITY,
            PowerType.CURSEWEAVER,
            PowerType.THUNDER_GOD,
            PowerType.WIND,
            PowerType.DEMON,
            PowerType.WATER,
            PowerType.WARP,
            PowerType.WITHER,
            PowerType.ICE_DRAGON,
            PowerType.WOOD_DRAGON,
            PowerType.SNOWPARTING_BLADE,
            PowerType.METEOR_LORD,
            PowerType.CULTIVATOR,
            PowerType.BLOOD,
            PowerType.CRYSTAL,
            PowerType.EARTH,
            PowerType.MAGNETIC,
            PowerType.UNSTABLE,
            PowerType.HUASHAN,
            PowerType.DEMON_LORD,
            PowerType.GRAVITY,
            PowerType.SHOCKWAVE,
            PowerType.PORTAL,
            PowerType.CLOUD,
            PowerType.VAMPIRE,
            PowerType.POISON,
            PowerType.AIR,
            PowerType.DEATH,
            PowerType.SOUND,
            PowerType.SPIKE
    ));

    private static final Random RNG = new Random();

    public static PowerType randomPower() {
        return POOL.get(RNG.nextInt(POOL.size()));
    }

    public static List<PowerType> getPool() {
        return POOL;
    }
}
