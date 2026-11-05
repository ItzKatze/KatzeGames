# KatzeGames

Minecraft challenges with teams, a shared lobby, and fresh maps between rounds.

**Minecraft 26.3** · [GPLv3](LICENSE)

## Game guides

[ForceBattle](forcebattle/README.md) · [DeathSwap](deathswap/README.md) · [AllItems](allitems/README.md)

[ItemRain](itemrain/README.md) · [Randomizer](randomizer/README.md) · [Optional client mod](clientmod/README.md)

## Get a round going

1. An operator opens `/settings` to choose the challenges, timer, and rules.
2. Sort out teams with `/team`, or use `/spectate` in the lobby to sit this round out.
3. The operator runs `/start`. Once the map is ready, a five-second countdown begins.

Global settings include no hunger, no damage, portable crafting, scoreboard visibility, and objective rules.
In `/settings`, left-click a challenge to toggle it or right-click to open its settings.

## Handy commands

| Command | What it does |
| --- | --- |
| `/team create`, `/team join <id>`, `/team leave` | Create a team, request to join one, or leave it. |
| `/team list` | See the teams. |
| `/chat team`, `/chat all` | Choose where your chat messages go. |
| `/t chat <message>` | Send one message to your team. |
| `/spectate` | Toggle spectating before the next round. |
| `/level [player]` | Check your level and XP, or someone else's. |
| `/achievements` | Browse achievements and rewards. |

## Hosting a round

These commands need operator permissions by default.

| Command | What it does |
| --- | --- |
| `/settings` | Choose challenges and adjust the rules. |
| `/start` | Start the selected challenges. |
| `/timer pause`, `/timer resume` | Pause or resume the round. |
| `/timer set <seconds>` | Change the remaining time. |
| `/reset` | Return everyone to the lobby and replace the game map with a fresh seed. |
| `/map [overworld\|nether\|end]` | Inspect the next map in creative mode before starting. |
| `/map limbo` | Return to the lobby after a map visit. |

**Resetting replaces the game worlds.** Levels, achievements, and cosmetics carry over.
Players wait in the lobby while the new map is prepared.

## Watching a round

Joining mid-round makes you a spectator. Fly around and right-click a player to inspect their inventory,
check their progress, or watch through their eyes. Spectator chat and private messages stay between spectators.

Late joiners play again after a reset. If you chose `/spectate`, use it again in the lobby when you're ready to play.

## Between rounds

- **Jump & Run:** try the lobby parkour and collect cosmetics for your runs.
- **Levels:** earn XP through playtime and challenges. Your level appears beside your name.
- **Achievements:** completed stages award separate achievement XP. Unfinished requirements stay hidden.
  Use the name-tag button in `/achievements` to switch between translated and custom names.

Menus and messages support English and German.

Lobby builds: **q8h** (classic) and **IchmagYorkleffs** (balloninsel, inselmithaus).
