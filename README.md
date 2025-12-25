![SRP Logo](https://raw.githubusercontent.com/Fx-Costa/SpeedRunPlus/main/.config/resources/banner.png)

[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/plugin/speedrunplus)
[![Hangar](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/hangar_vector.svg)](https://hangar.papermc.io/Fx-Costa/SpeedRunPlus)
[![Spigot](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/cozy/available/spigot_vector.svg)](https://www.spigotmc.org/resources/speedrun.130642/)

[![Release](https://img.shields.io/github/v/release/Fx-Costa/SpeedRunPlus)](https://github.com/Fx-Costa/SpeedRunPlus/releases/latest)

# SpeedRun+

**SpeedRun+** (_SpeedRunPlus_) is a plugin for Minecraft (1.16.1) multiplayer servers which provides several speedrun-related
game modes to your server. **SpeedRun+** manages isolated, temporary worlds when speedrunning and supplies a range
of quality of life utilities such as: In-game timers, AFK detection, leaderboards, filtered seeds, and more. Game modes include:

- **Solo** - A classic single-player speedrun where you aim to defeat the Ender Dragon in the shortest time possible.
- **Battles** - A head-to-head race between two players in identical worlds. Each player rushes to defeat the Ender Dragon first — the first to do so wins the match.
- **Co-op** - A cooperative speedrun for up to two players sharing the same world. The run ends when the team defeats the Ender Dragon together.

With more coming!

A great solution for providing fun and competitive game modes to a proxy-based server network
([Velocity](https://github.com/PaperMC/Velocity) / [BungeeCord](https://github.com/SpigotMC/BungeeCord))
or simply on a _1.16.1_-native server!

---

## 🖼️ Preview

<p align="center">
  <img src="https://raw.githubusercontent.com/Fx-Costa/SpeedRunPlus/main/.config/resources/solo-preview.png"
    width="400" alt="Solo preview" />
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Fx-Costa/SpeedRunPlus/main/.config/resources/battle-preview.png"
    width="400" alt="Battle preview" />
  <img src="https://raw.githubusercontent.com/Fx-Costa/SpeedRunPlus/main/.config/resources/coop-preview.png"
    width="400" alt="Battle preview" />
</p>

## 🖥️ Installation

> ⚠️ **SpeedRun+** does not support Minecraft Bedrock servers.
> **SpeedRun+** works on a Bukkit server such as [Spigot](https://www.spigotmc.org/) or [Paper](https://papermc.io/).

1. Download the plugin from your favorite platform
    - [GitHub - **SpeedRun+**](https://github.com/Fx-Costa/SpeedRunPlus/releases/latest)
    - [Modrinth - **SpeedRun+**](https://modrinth.com/plugin/speedrunplus)
    - [Hangar - **SpeedRun+**](https://hangar.papermc.io/Fx-Costa/SpeedRunPlus)
    - [Spigot - **SpeedRun+**](https://www.spigotmc.org/resources/speedrun.130642/)

2. Add the _JAR_ file to your server's `plugin`-folder

3. Restart / Reload the server and start speedrunning by executing the provided commands!

### ⚙️ Prerequisites

**SpeedRun+** relies on [Multiverse-Core](https://github.com/Multiverse/Multiverse-Core) and [Multiverse-NetherPortals](https://github.com/Multiverse/Multiverse-NetherPortals)
to create isolated speedrun worlds. These plugins must be present on the server, for this plugin to work:

- [GitHub - Multiverse-Core v. 4.3.16](https://github.com/Multiverse/Multiverse-Core/releases/tag/4.3.16) / [Modrinth - Multiverse-Core v. 4.3.16](https://modrinth.com/plugin/multiverse-core/version/4.3.16)
- [GitHub - Multiverse-NetherPortals v. 4.2.3](https://github.com/Multiverse/Multiverse-NetherPortals/releases/tag/4.2.3) / [Modrinth - Multiverse-Core v. 4.3.16](https://modrinth.com/plugin/multiverse-netherportals/version/4.2.3)

## 🏃️ Commands

**SpeedRun+** uses the following in-game command structure:

```terminaloutput
/srp <gamemode> <action> [player]
```

For additional info and help, see the subsections below or use the `/srp help` command in-game.

### 🕹️ Player commands

#### 👤 **Solo**

| Command           | Description                             |
|:------------------|:----------------------------------------|
| `/srp solo start` | Start a new solo speedrun               |
| `/srp solo reset` | Reset the world of the current speedrun |
| `/srp solo stop`  | Stop the current speedrun               |

#### 👥 **Battle**

| Command                        | Description                                                            |
|:-------------------------------|:-----------------------------------------------------------------------|
| `/srp battle request [player]` | Challenge a player to a battle (1v1) speedrun                          |
| `/srp battle accept`           | Accept a challenge and start the battle speedrun                       |
| `/srp battle decline`          | Decline a request to a battle speedrun                                 |
| `/srp battle reset`            | Reset the player's world in the current speedrun battle                |
| `/srp battle surrender`        | Surrender and stop the battle speedrun - awarding the opponent the win |

#### 🫂 **Co-op**

| Command                      | Description                                            |
|:-----------------------------|:-------------------------------------------------------|
| `/srp coop request [player]` | Request a player to join you in a cooperative speedrun |
| `/srp coop accept`           | Accept a request and start a cooperative speedrun      |
| `/srp coop decline`          | Decline a request to a cooperative speedrun            |
| `/srp coop surrender`        | Stop the current cooperative speedrun - no winners     |

### 🛡️ Admin commands

Administrative commands exist for managing runs, troubleshooting issues, and performing maintenance actions.
The player must have the administrative permission `srp.admin` (disabled by default) to allow use of these.

For the full list of admin commands, use:

```terminaloutput
/srp admin help
```

## ️🛠️ Configuration & Permissions

**SpeedRun+** is highly configurable, allowing server owners to fine-tune game rules, world generation, timer behavior,
AFK handling, podiums to display top leaderboard contestants, and more. All settings can be adjusted through the
plugin’s `config.yml`, giving you control over how speedruns operate on your server.

Below is a complete example configuration file to help you understand the available options and their default values.
You can use this as a reference when customizing your own setup.

<details> <summary><strong>Config Example <i>(Click to expand)</i></strong></summary>

```yaml
# ========================================
# SpeedRunPlus (SRP) Configuration
# ========================================

# Main world names - used to teleport players back to when runs complete
main-overworld: world
main-nether: world_nether
main-end: world_the_end

# Game rule settings
game-rules:

  # Speedrun world seeds
  filtered-seeds:
    # Whether to use filtered seeds or rely on random seeds
    use-filtered-seeds: false

    # Weights for each type of seed (higher number = more likely to appear) (non-positive weights excludes the type)
    weights:
      MAPLESS: 0
      VILLAGE: 3
      TEMPLE: 1
      SHIPWRECK: 2
      RUINED_PORTAL: 1
      RANDOM: 1

  # Maximum number of players allowed to speedrun simultaneously
  # Used it to limit the toll on the server's resources
  max-players: 4

  # Maximum duration of a speedrun in minutes
  # Use it to limit the time a player can spend in a single run
  max-time-minutes: 120

  # Time before requests for BattleSpeedrun / Co-op are invalidated
  max-request-seconds: 30

# Prefix used for generated speedrun worlds
world-prefix:
  overworld: srp-overworld-
  nether: srp-nether-
  end: srp-end-

# Timer settings
timer:
  # The amount of time to countdown from, when starting a speedrun in seconds
  countdown-seconds: 10

# Podium configuration
# Use it to display the top N contestants and their times in the main world using armor-stands and player heads
podium:
  max: 10
  world: world
  positions:
    1:
      x: 0.5
      y: 80.5
      z: 0.5
      yaw: 90

# AFK configuration
afk:
  # Minutes before ending a speedrun due to inactivity
  timeout-minutes: 5

  # Intervals between each AFK check
  check-interval-seconds: 60

  # Minimum number of blocks the player needs to move
  # to be considered active between each AFK check
  min-distance: 1.0
```

</details>

### 🔑 Permissions

**SpeedRun+** includes a set of permissions, that integrate seamlessly into [LuckPerms](https://luckperms.net/), and let
server owners control which features and gamemodes players can access. Most servers will likely grant _solo_, _battle_,
or _co-op_ permissions based on player ranks, while administrative permissions should be reserved for staff.

Below is a brief overview of permissions, but be sure to see the `plugin.yml` for the full list:

| Permission   | Description                                         | Default |
|--------------|-----------------------------------------------------|---------|
| `srp.use`    | Allows the player overall usage of **SpeedRun+**    | _True_  |
| `srp.solo`   | Allows the player to use all Solo commands          | _True_  |
| `srp.battle` | Allows the player to use all Battle commands        | _True_  |
| `srp.coop`   | Allows the player to use all Co-op commands         | _True_  |
| `srp.admin`  | Allows the player to use all administrator commands | _False_ |

## 🤝 Support & Contact

If you encounter issues, have suggestions, or want to request new features, please use the official issue tracker on:

[GitHub Issues](https://github.com/Fx-Costa/SpeedRunPlus/issues)

For anything else—questions, feedback, or general discussion—feel free to open an issue as well. This helps keep all support requests organized and ensures nothing gets lost.

## 📜 Licensing and Contributions

This project is licensed under the [AGPL-3.0 License](https://raw.githubusercontent.com/Fx-Costa/SpeedRunPlus/main/LICENSE.md), allowing open use, modification, and redistribution under
the terms of the license.

Contributions are welcome! However, all contributions must follow the rules
outlined in [CONTRIBUTING](https://raw.githubusercontent.com/Fx-Costa/SpeedRunPlus/main/CONTRIBUTING.md) and are subject to the [Contributor License Agreement](https://raw.githubusercontent.com/Fx-Costa/SpeedRunPlus/main/CLA.md).
Only contributions approved by the project owner will be merged into the official version.
