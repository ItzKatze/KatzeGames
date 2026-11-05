# ForceBattle

Complete random objectives before time runs out. The player or team with the most points wins.

## Your next objective

| Type | What to do |
| --- | --- |
| Item | Collect the requested item. |
| Mob | Kill the requested mob. |
| Biome | Find the requested biome. |
| Advancement | Complete the requested advancement. |
| Height | Reach the requested height. |
| Coordinates | Travel to the requested coordinates. |
| Structure | Find the requested structure. |
| Death | Die from the requested damage type. |

## A little help

- **Skip jokers** replace a difficult personal or team objective.
- **Swap jokers** trade your personal objective with an opponent's.
- **Backpacks** give you extra storage, shared with your teammates when you're in a team.
- **Recipes** are a command away if you forget how to craft something.

Right-click a joker item to use it.

## Set up the battle

Operators can use `/settings` to choose objective types for players, teams, or both.

- Turn on **chain mode** to give players the same objective sequence, with a separate sequence for teams.
- Exclude categories such as End content or armor templates from the objective pool.
- Adjust shared objective rules, including simplified objectives and duplicates.
- Enable or disable backpacks.

Before the countdown, the plugin checks for biomes and structures needed by the selected objectives
and rerolls unsuitable maps. Everyone waits in the lobby during preparation.
By default, checks cover a 5,000-block radius; this is **not a world border** and does not guarantee rare loot or mob spawns.

## Commands

| Command | What it does |
| --- | --- |
| `/backpack` or `/bp` | Open your personal or team backpack. |
| `/recipe <item>` | Look up a crafting recipe. |
| `/displayresults <player\|team>` | Show the final player or team leaderboard (operator). |
| `/result player <name>` | View a player's objective history while the timer is paused or after the battle. |
| `/result team <id>` | View a team's results. |
| `/results` | Browse saved rounds you played in, including standings and objectives. |

Completed rounds stay available after map resets and server restarts. Spectating a round does not add it to your history.

Based on [nicolaigrv's original ForceBattle](https://github.com/nicolaigrv/ForceBattle).

[Back to KatzeGames](../README.md)
