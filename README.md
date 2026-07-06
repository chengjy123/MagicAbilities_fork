# ✨ MagicAbilities - Minecraft Plugin

> **Transform your Minecraft server with 50+ spectacular superpowers!**
> **Custom orders accepted for SMP!**

This Spigot plugin adds a variety of amazing superpowers through the Spigot API, allowing players to harness elemental forces, mystical abilities, and gravity-defying powers.

![](https://i.imgur.com/QlSqjAX.png)
![](https://i.imgur.com/SfTBcXA.png)

---

## 📋 Table of Contents
- [Features](#-features)
- [Installation](#-installation)
- [Commands](#-commands)
- [Available Powers](#-available-powers)
- [Tips & Tricks](#-tips--tricks)
- [Contributing](#-contributing)
- [License](#-license)

---

## ⚡ Features

- **50+ Unique Superpowers** - From fire and ice to gravity and demons
- **Power Customization** - Configure power behavior and effects
- **Database Support** - Persistent player power storage
- **Interactive GUI** - Beautiful inventory system for power management
- **Power Binding** - Bind powers to specific actions (sneak, click, etc.)
- **Cooldown System** - Customizable ability cooldowns
- **Smooth Integration** - Built entirely on the Spigot API
- **Still in Development** - New features and powers being added regularly

> [!NOTE]
> **Balance Warning:** Not all powers are equal - some are significantly more overpowered than others. Server admins should test powers before enabling them in survival mode.

---

## 🔧 Installation

### Requirements
- **Java 8** or higher (compiled for Java 8)
- **Spigot 1.21.10** or compatible version
- Maven (for building from source)

### Quick Setup
1. Download the compiled JAR file (`MagicAbilities_fork.jar`) from the releases
2. Place it in your server's `plugins/` folder
3. Restart your Minecraft server
4. The plugin will create configuration files automatically

### Building from Source
```bash
git clone https://github.com/your-repo/MagicAbilities-fork.git
cd MagicAbilities-fork
mvn clean package
# JAR will be in target/MagicAbilities_fork.jar
```

---

## 🎮 Commands

### 🔱 Main Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/setpower <player> <power>` | `magicabilities.setpower` | Grant a player a specific power |
| `/powerset <on/off>` | `magicabilities.powerset` | Enable/disable power sets |
| `/powersetaura <on/off>` | `magicabilities.aura` | Toggle power aura visual effects |
| `/binds` | `magicabilities.binds` | Display power keybinds and actions |
| `/enable` | `magicabilities.enable` | Enable your current powers |
| `/disable` | `magicabilities.disable` | Disable your current powers |
| `/destination <set/tp>` | `magicabilities.destination` | Set and teleport to destinations |

### 📖 Usage Examples
```
# Give a player fire power
/setpower Steve FIRE

# Enable all power sets
/powerset on

# View your keybinds
/binds

# Teleport to a saved destination
/destination tp
```

> [!TIP]
> Use `/binds` to see what actions trigger each power ability!

> [!TIP]
> `/powerset <on/off>` enables or disables entire power sets for easier management

---

## 🌟 Available Powers

### 🔥 Elemental Powers

| Power | Type | Description |
|-------|------|-------------|
| **Fire** | FIRE | Control blazing flames and ignite everything in sight |
| **Ice** | ICE | Freeze enemies and create icy barriers |
| **Water** | WATER | Command aquatic forces and create whirlpools |
| **Wind** | WIND | Harness air currents and push entities |
| **Earth** | EARTH | Manipulate the ground and summon stone |
| **Lightning** | LIGHTNING | Strike with devastating electrical power |
| **Thunder God** | THUNDER_GOD | Master of lightning with enhanced abilities |
| **Lunar** | LUNAR | Harness moonlight and night powers |

### 🐉 Dragon Powers

| Power | Type | Description |
|-------|------|-------------|
| **Ice Dragon** | ICE_DRAGON | Draconic ice control and dragon abilities |
| **Wood Dragon** | WOOD_DRAGON | Nature-infused dragon powers |
| **Phoenix** | PHOENIX | Rise from ashes with regenerative fire abilities |
| **Wither** | WITHER | Dark decay and withering effects |

### 🌑 Dark & Evil Powers

| Power | Type | Description |
|-------|------|-------------|
| **Demon** | DEMON | Demonic corruption and dark magic |
| **Demon Lord** | DEMON_LORD | Supreme demonic power and dominance |
| **Death** | DEATH | Command the forces of death itself |
| **Curseweaver** | CURSEWEAVER | Weave hexes and curses on enemies |
| **Poison** | POISON | Toxic and venomous attacks |
| **Blood** | BLOOD | Blood magic and crimson abilities |
| **Vampire** | VAMPIRE | Drain life force from enemies |

### 🌍 Movement & Gravity Powers

| Power | Type | Description |
|-------|------|-------------|
| **Warp** | WARP | Short-range teleportation |
| **Superior Warp** | SUPERIOR_WARP | Enhanced warp with greater range |
| **Portal** | PORTAL | Create portals for transportation |
| **Gravity** | GRAVITY | Manipulate gravity and float |
| **Cloud** | CLOUD | Walk on clouds and float through air |
| **Shockwave** | SHOCKWAVE | Create devastating shockwaves |

### 🎭 Mystical & Special Powers

| Power | Type | Description |
|-------|------|-------------|
| **Witcher** | WITCHER | Ancient witchcraft and magic hunting |
| **Cultivator** | CULTIVATOR | Oriental martial power cultivation |
| **Shogun** | SHOGUN | Samurai warrior abilities |
| **Huashan** | HUASHAN | Chinese martial arts mastery |
| **Snowparting Blade** | SNOWPARTING_BLADE | Ice blade techniques |
| **Meteor Lord** | METEOR_LORD | Rain meteors from the sky |
| **Twilight Mirage** | TWILIGHT_MIRAGE | Illusions and twilight magic |
| **Eternity** | ETERNITY | Temporal manipulation |

### 💎 Nature & Support Powers

| Power | Type | Description |
|-------|------|-------------|
| **Nature** | NATURE | Control plants and nature |
| **Crystal** | CRYSTAL | Crystalline structures and barriers |
| **Magnetic** | MAGNETIC | Magnetic field manipulation |
| **Sound** | SOUND | Sonic attacks and vibrations |
| **Spike** | SPIKE | Spike generation and piercing attacks |

### 🎪 Fun & Experimental Powers

| Power | Type | Description |
|-------|------|-------------|
| **Alcoholizm** | ALCOHOLIZM | Drink-powered abilities (for laughs!) |
| **Potato** | POTATO | The legendary potato power! 🥔 |
| **Unstable** | UNSTABLE | Unpredictable chaotic energy |
| **Comic Test** | COMICTEST | Experimental power (WIP) |

### ❌ None
| Power | Type | Description |
|-------|------|-------------|
| **None** | NONE | No power - return to normal |

---

## 💡 Tips & Tricks

### Getting Started
- Each power is triggered by different **actions**: Left Click, Right Click, Sneak, Move, Mine Block, Consume Item, etc.
- Use `/binds` to see which action triggers what for your current power
- Powers can be **individually disabled** while keeping others active

### Power Management
- Multiple powers can be active at once through power sets
- Cooldowns prevent ability spam - check your cooldown status before using abilities
- Some powers are stronger than others - test them in creative mode first!

### Server Administration
```
/setpower <player> <power>     # Give a player a power
/powerset on                    # Enable power system
/disable                        # Disable powers temporarily
```

> [!WARNING]
> **⚠️ Balance Notice:** The plugin is still in active development. Powers are NOT balanced - some are significantly overpowered. Test thoroughly before using in survival PvP.

> [!WARNING]
> **📚 Documentation:** The plugin lacks a comprehensive wiki. Use `/binds` to discover mechanics, and join the community for tips!

---

## 📦 Dependencies

The plugin uses these external libraries:

- **Spigot API** - Server plugin framework
- **InventoryGUI** - Interactive inventory menus
- **SkullCreator** - Custom skull generation
- **Commons IO** - File utilities

All dependencies are automatically shaded into the JAR during build.

---

## 🚀 Development

### Project Structure
```
src/
├── main/java/net/trduc/magicabilitiesfork/
│   ├── powers/              # Core power system
│   │   ├── custom/          # Individual power implementations
│   │   └── executions/      # Action triggers (click, sneak, etc.)
│   ├── commands/            # Command handlers
│   ├── guis/               # GUI and animation system
│   ├── cooldowns/          # Cooldown management
│   ├── data/               # Database and player data
│   ├── events/             # Event handlers
│   └── misc/               # Utility classes
└── resources/              # Plugin configuration
```

### Adding a New Power
1. Create a new class in `powers/custom/` extending `Power`
2. Add your power type to `PowerType.java` enum
3. Implement power mechanics using `Execute` classes
4. Register in the power system

---

## 🤝 Contributing

Contributions are welcome! Please:

1. **Fork** the repository
2. **Create a feature branch** (`git checkout -b feature/AmazingPower`)
3. **Commit your changes** (`git commit -m 'Add new power'`)
4. **Push to the branch** (`git push origin feature/AmazingPower`)
5. **Open a Pull Request**

### Areas for Contribution
- ✨ New power implementations
- 🐛 Bug fixes and optimizations
- 📚 Documentation and wiki
- ⚖️ Power balance adjustments
- 🎨 Visual effects improvements

---

## 📄 License

This project is licensed under the terms defined in the repository. Check the LICENSE file for details.

---

## 📞 Support

- **Found a bug?** Open an issue on GitHub
- **Have a suggestion?** Discuss it in the Issues section
- **Need help?** Check the `/binds` command first!

---

**Made with ❤️ for the Minecraft community**

*Last Updated: 2026-07-05*
