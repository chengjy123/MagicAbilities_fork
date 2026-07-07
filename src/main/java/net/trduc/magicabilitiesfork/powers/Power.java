package net.trduc.magicabilitiesfork.powers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilitiesfork.powers.custom.*;
import net.trduc.magicabilitiesfork.powers.executions.Execute;
import org.bukkit.entity.Player;

public abstract class Power {
    private final Player owner;
    private boolean enabled = true;

    public Power(Player owner) {
        this.owner = owner;
    }

    public abstract void executePower(Execute ex);

    public Player getOwner() {
        return owner;
    }

    public static Power getPowerFromPowerType(Player p, PowerType powerType){
        switch (powerType){
            case ICE:
                return new IcePower(p);
            case WARP:
                return new WarpPower(p);
            case LIGHTNING:
                return new LightningPower(p);
            case UNSTABLE:
                return new UnstablePower(p);
            case SHOGUN:
                return new ShogunPower(p);
            case FIRE:
                return new FirePower(p);
            case WITCHER:
                return new WitcherPower(p);
            case NATURE:
                return new NaturePower(p);
            case TWILIGHT_MIRAGE:
                return new TwilightMirage(p);
            case ETERNITY:
                return new Eternity(p);
            case CURSEWEAVER:
                return new Curseweaver(p);
            case PHOENIX:
                return new PhoenixPower(p);
            case THUNDER_GOD:
                return new ThunderGodPower(p);
            case WIND:
                return new WindPower(p);
            case DEMON:
                return new DemonPower(p);
            case WATER:
                return new WaterPower(p);
            case WITHER:
                return new WitherPower(p);
            case ICE_DRAGON:
                return new IceDragonPower(p);
            case WOOD_DRAGON:
                return new WoodDragonPower(p);
            case SNOWPARTING_BLADE:
                return new SnowpartingBladePower(p);
            case METEOR_LORD:
                return new MeteorLordPower(p);
            case CULTIVATOR:
                return new CultivatorPower(p);
            case HUASHAN:
                return new HuaShanPower(p);
            case ALCOHOLIZM:
                return new AlcoholizmPower(p);
            case POTATO:
                return new PotatoPower(p);
            case SUPERIOR_WARP:
                return new SuperiorWarpPower(p);
            case DEMON_LORD:
                return new DemonLordPower(p);
            case GRAVITY:
                return new GravityPower(p);
            case SHOCKWAVE:
                return new ShockwavePower(p);
            case PORTAL:
                return new PortalPower(p);
            case CLOUD:
                return new CloudPower(p);
            case VAMPIRE:
                return new VampirePower(p);
            case POISON:
                return new PoisonPower(p);
            case AIR:
                return new AirPower(p);
            case BLOOD:
                return new BloodPower(p);
            case CRYSTAL:
                return new CrystalPower(p);
            case EARTH:
                return new EarthPower(p);
            case MAGNETIC:
                return new MagneticPower(p);
            case DEATH:
                return new DeathPower(p);
            case SOUND:
                return new SoundPower(p);
            case SPIKE:
                return new SpikePower(p);
            case LUNAR:
                return new LunarPower(p);
            case COMICTEST:
                return new Comictest(p);
            default:
                return new Power(p) {
                    @Override
                    public void executePower(Execute ex) {
                    }
                };
        }
    }

    public String getAbilityName(int ability) {
        return "&7none";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void onCooldownInfo(long time){
        String s = (float) ((int) time/100)/10 == 0 ? "0.1" : ((float) ((int) time/100)/10 + "");

        owner.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "On cooldown for " +
                s + "s."));
    }
}

