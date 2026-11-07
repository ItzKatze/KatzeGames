# Bingo

Race another player or team to claim five objectives in a row on a shared **5×5 board**. Complete a row, column, or diagonal to win.

## How it works

- The first side to complete an objective owns that cell. Your opponent can no longer claim it.
- Teammates share their board and claimed cells. Each pair of players or teams gets its own match.
- If time runs out, the side with **more claimed cells wins**. Equal counts are a draw.
- If both sides have no possible winning line left, the match ends in a draw.

## Your objectives

| Type        | What to do                                                 |
|-------------|------------------------------------------------------------|
| Item        | Collect **one** of the requested item. It is not consumed. |
| Mob         | Kill the requested mob.                                    |
| Biome       | Visit the requested biome.                                 |
| Structure   | Find the requested structure.                              |
| Advancement | Complete the requested advancement during the round.       |

Keep collection items in your inventory or offhand; armor slots and items inside containers do not count.

With **Simplified Objectives**, any allowed member of the requested mob, biome, or structure group counts.

## Read your board

Accept the server's **required resource pack** when joining. No client mod is needed.

The sidebar shows your board, each side's color, and claimed-cell counts. Remaining time appears above the hotbar.

Open `/bingo` and hover cells for details:

- **Item icons** are still available to claim.
- **Blue or orange glass panes** are claimed by the matching side.
- **Glowing panes** mark a completed winning line.

After your match ends, you can spectate other matches. If you disconnect, rejoin within two minutes to avoid forfeiting.

## Commands

| Command | What it does |
| --- | --- |
| `/bingo` | Open your board and inspect objectives. |
| `/bingo overlay` | Hide or show the sidebar board. |
| `/recipe <item>` | Look up crafting and processing recipes. |
| `/bingo matches` | Choose a match to watch as a spectator. |
| `/results` | Review saved rounds you played in. |

## Set up a round

Operators can enable Bingo in `/settings` and right-click it to change the rules.

- Choose **30, 45, or 60 minutes**, or unlimited time. The default is **45 minutes**.
- Choose objective types and use the bottom-right arrow to exclude categories such as End content, ores, or armor templates.
- Change **Simplified Objectives** in Global settings, reached through the bottom-right arrow in `/settings`.

Bring an even number of players or teams and use `/start`. Bingo must be the only enabled gamemode. Teams and Bingo settings are locked during the round.

[Server setup and pack tools](SETUP.md) · [Back to KatzeGames](../README.md)
