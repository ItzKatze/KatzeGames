# KatzeGames

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---
Supported Minecraft version: 26.3

---

## 🎮 Gamemodes & Modules

KatzeGames is a modular plugin providing multiple unique gamemodes and challenge mechanics.

| Gamemode        | Description                                                                                                             | Documentation                       |
|:----------------|:------------------------------------------------------------------------------------------------------------------------|:------------------------------------|
| **ForceBattle** | Race against opponents to complete randomized objectives (items, mobs, biomes, coords, etc.) and earn the most points.  | [Read Guide](forcebattle/README.md) |
| **DeathSwap**   | Players periodically swap locations—build traps and use fall damage or lava to eliminate opponents.                     | [Read Guide](deathswap/README.md)   |
| **ItemRain**    | Random items periodically fall from the sky or drop directly into your inventory.                                       | [Read Guide](itemrain/README.md)    |
| **Randomizer**  | Randomize block drops, mob drops, and chest loot tables for a chaotic survival experience.                              | [Read Guide](randomizer/README.md)  |
| **AllItems**    | Work individually or as a team to collect every single item, biome, mob, structure, and advancement. *(In development)* | [Read Guide](allitems/README.md)    |

---

## ⚙️ Generic Settings

Access global plugin settings anytime in-game using `/settings`:

- **No Hunger**: Disables hunger depletion for all players.
- **No Damage**: Prevents players from taking environmental or combat damage.
- **Mobile Crafting**: Grants players a portable crafting table menu.
- **Show Scoreboard**: Toggles the sidebar scoreboard display.
- **Timer**: Set the time of the challenge.
- **Teams**: Create and manage teams with shared objectives and team chat.

---

## 📋 Core Commands

| Command           | Description                                                    |
|:------------------|:---------------------------------------------------------------|
| `/start`          | Starts the selected challenge after a 5 second countdown       |
| `/reset`          | Sends players to limbo and replaces the game map with a new seed |
| `/map [overworld\|nether\|end\|limbo]` | Operators: visit the next map in creative mode or return to limbo |
| `/timer`          | Controls the global timer (start, pause, resume, set duration) |
| `/settings`       | Opens the global settings GUI                                  |
| `/team`           | Create, join, or leave teams                                   |
| `/chat`           | Toggles chat mode (global / local)                             |
| `/teamchat`       | Sends a message exclusively to your team                       |
| `/spectate`       | Watch the next battle instead of playing in it (lobby only)    |
| `/level [player]` | Shows a player's level and XP (ops: `/level add <player> <n>`) |

---

## Limbo and map resets

`/reset` moves everyone to a void world containing the lobby island and all its features. Players stay there until `/start`. The game overworld, Nether and End are unloaded, deleted and recreated with a new random seed. `/reset` also works when no challenge is enabled; `/start` waits until generation finishes. If a reset fails, players stay in limbo and an operator can retry `/reset`. An interrupted reset resumes on server startup.

Game dimensions use the keys `katzegames:game`, `katzegames:game_nether` and `katzegames:game_the_end`; limbo uses `katzegames:limbo`. Their files live under the primary world's `dimensions/katzegames` directory. Reset state lives in `plugins/KatzeGames/data/game-worlds.json`. Existing running sessions continue on their original map until the first reset. Paper cannot unload the primary overworld, so that original server world is retained; subsequent games use the replaceable dimensions. Player levels and cosmetics survive resets.

Operators can use `/map` (or `/map overworld`, `/map nether`, `/map end`) to inspect and edit the generated map in creative mode while everyone else stays in limbo. `/map limbo` returns to the lobby and its normal inventory and adventure mode. Map visits are available once generation finishes and before the start countdown. Staff can use Chunky in the visited world to pre-generate terrain, or `/reset` to discard a bad map and roll another seed. `/start` brings everyone, including visiting staff, into the game with the normal game mode and an empty inventory.

## ⭐ Levels

Playing earns XP, and XP adds up to a level that everyone sees:

- **Showing it**: your level shows as `[12] Name` in the tab list, above your head and in chat, and the lobby sidebar shows your progress. Every 10 minutes you read how much XP you gained.
- **Playtime**: 2 XP per minute in a running game, 1 XP per 2 minutes in the lobby. After 5 minutes without moving you are AFK and earn no playtime XP until you move again.
- **Games**: finishing a game pays 50 XP per 90 minutes you played in it.
- **Challenges**: ForceBattle pays 10 XP per objective, 1 XP per 100 blocks walked and 150 XP to the winners; DeathSwap 10 XP per swap you survive and 150 XP to the last one alive; AllItems 2 to 10 XP per new discovery.
- **Levelling up**: level 2 needs 140 XP and each level after it about 5% more, up to 6,133 XP for level 100, the highest level (216,169 XP in total).
- **Storage**: levels and Jump & Run cosmetics live in `plugins/KatzeGames/data/players.json` and survive `/reset` and new worlds.

---

## 🎵 Client Mod

The Cosmetic Case also holds C418's soundtrack songs (Aria Math, Sweden, Wet Hands and 29 more) as Jump & Run music. They play through an optional Fabric mod for players:

- **What it adds**: players with the mod hear the soundtrack songs as Jump & Run music, hear the run music (songs and discs) fade out when a run fails, and get no background music from the game during a run.
- **For players**: install Fabric Loader 0.20.1 or newer and Fabric API for Minecraft 26.3, then put `KatzeGames-Client-<version>+26.3.jar` from the release into your `mods` folder. The mod only acts on this plugin's Jump & Run music, so it changes nothing on other servers.
- **Without the mod**: the music discs still play and fade out; the soundtrack songs stay silent and their menu entries say `Needs the client mod`. A few seconds after every join, a chat message links to the latest release.

---

## 👀 Spectators

Spectators watch a running battle without influencing it:

- **Becoming one**: type `/spectate` in the lobby (again to play). Players who join a running battle spectate automatically, and players knocked out of a DeathSwap become spectators too.
- **Watching**: spectators fly in spectator mode and can right-click a player from up to 10 blocks away to see their armor, inventory, health, position and challenge progress, open their backpack, or view through their eyes.
- **No influence**: spectators get no objectives or items, can't be swapped with, and nothing in their views can be moved.
- **Chat**: during the battle a spectator's chat only reaches other spectators, and `/msg`, `/tell` and `/w` only work towards other spectators.
- **After `/reset`**: late joiners and knocked-out players play again; players who chose `/spectate` keep spectating until they type it again.

---

## 🛠️ Building from Source

To build KatzeGames yourself, make sure you have **Gradle** installed.  
Clone this repository and run:

```bash
gradle build
```

The compiled `.jar` file will be located under `build/libs` inside the `loader` module.

The client mod is built on its own, because its build downloads Minecraft itself:

```bash
./gradlew :clientmod:build
```

Its `.jar` file will be located under `clientmod/build/libs`.

---

## Achievements

Use `/achievements` to browse General, all five games, and Lobby. Each category shows your completed stages; clicking an achievement opens its targets and reward details. Games with no exclusive achievements show an empty page, while General achievements work in every active game.

The catalogue has 33 single achievements and 9 achievements with five stages. Rewards are automatic and paid once per player per stage: Common 25 achievement XP, Uncommon 50, Rare 100, Mythical 250. Every stage of a staged achievement pays its base-rarity reward. The nine staged achievements are Common, so each stage gives 25 XP and all five give 125 XP. Death targets are 4/10/15/20/25 in one round.

Unfinished achievements hide their requirements, target counts and progress. Their descriptions appear as obfuscated characters matching the real description's length and line wrapping. Completed stages reveal their own requirements; later stages remain hidden. Unlock-message hovers also describe only the stage just completed.

Achievement XP has its own level system, starting at level 1. Reaching level 2 costs 100 achievement XP, level 3 costs another 150, level 4 another 200, and each following level costs 50 more than the previous one. The summary in `/achievements` shows your achievement level, total achievement XP and progress to the next level. Achievement XP is calculated from completed stages and their current rewards; it is never saved as a balance. Adjusting achievement rewards automatically updates every player's achievement XP and level. Existing saved achievement XP is ignored and removed on the next save. Completion records stay in `data/players.json`; regular XP is preserved and receives no achievement rewards.

To use custom achievement names, select the name-tag button in `/achievements` or run `/achievements names custom`. The custom pack contains the original mixed German/English names from the achievement PDF. `/achievements names auto` restores names in your game language. This personal choice is saved across reconnects and applies to menu entries, detail titles and unlock messages. Descriptions, hover requirements and other interface text continue to follow your game language.

Round progress resets with a new game; completed stages and best progress stay in `data/players.json`. Jump streaks require one uninterrupted lobby attempt, and inventory targets require carrying all required items simultaneously. Restored rounds retain their counters; paused, spectator, excluded and creative gameplay does not count. English and German are supported without the client mod.

Placed or piston-moved Deepslate Coal Ore is tracked in chunk data across rounds. Blocks placed before this feature was installed cannot be distinguished from generated ore. Advancement progress counts new visible vanilla completions during the round; existing vanilla advancement-reset behavior is unchanged.

The full catalogue and tracking rules are in [the achievement design](docs/superpowers/specs/2026-11-03-achievements-design.md).

---

## 🤝 Contributing

Want to contribute? Here is how you can help:

1. **Fork** this repository.
2. **Create a new branch** based on your feature or fix.  
   (Example: `feature/new-feature` or `fix/bug-description`)
3. **Implement your changes**, following `.editorconfig` and standard Java conventions.
4. **Test your changes** carefully.
5. **Push** to your fork and **open a pull request**.

---

## 🏝️ Spawn Credits

The lobby spawns where players wait before a game starts were built by:

| Spawn          | Builder          |
|:---------------|:-----------------|
| `classic`      | q8h              |
| `balloninsel`  | IchmagYorkleffs  |
| `inselmithaus` | IchmagYorkleffs  |

---

## 📜 License

This project is licensed under the **GNU GPLv3**.  
See the [LICENSE](https://www.gnu.org/licenses/gpl-3.0) file for details.
