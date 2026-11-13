# MobArmy

Collect an army, send it against your opponents, and finish their waves in the **fastest total combat time**. Play solo or in teams.

## 1. Collect and gear up

- You have **90 minutes by default** to gather supplies and kill mobs for your army.
- Almost every mob counts, including animals, golems, and bosses; **the Ender Dragon does not count**.
- Collected mobs return at full health with their equipment and variants.
- Starter equipment, when enabled, is an **unbreakable diamond axe, pickaxe, and shovel with Efficiency I**.
- Collection deaths can drop your other gear; **starter tools you were carrying and collected mobs are safe**.
- Use `/bp` for extra storage, shared with teammates when you are in a team, if backpacks are enabled.

## 2. Build three waves

| Wave menu control | Action                               |
|-------------------|--------------------------------------|
| Left-click a mob  | Add one to the selected wave.        |
| Right-click a mob | Remove one from the selected wave.   |
| Hover a mob       | Check assigned and available counts. |
| Back button       | Return to the three-wave overview.   |
| Ready button      | Mark your team ready or undo it.     |

- Each collected mob can enter **one wave only**; you may leave mobs unused.
- Teammates can edit together. Changing waves makes your team unready again.
- Combat starts when **every team is ready**; one teammate can ready the whole team.

## 3. Beat the opposing armies

- Fight all **three waves from each opponent** in your own village arena.
- A wave ends when all its mobs are defeated. Mobs arrive gradually.
- **No PvP.** You may place defenses and break player-placed blocks; the arena itself is protected.
- Arena mobs drop **no loot or XP**, so bring supplies.
- Captured iron golems attack players; golems created or spawned during battle are friendly.
- Use `/tracker` when **five or fewer mobs remain**, including mobs still waiting to spawn.
- **Fastest total time wins; tied times share the win.** Finished teams can watch the remaining teams.

## Deaths and supplies

- During combat, you **keep your items and XP**, then wait on the roof before returning.
- Each of your deaths increases your roof wait by **30 seconds**: **30, 60, 90, 120 seconds**, and so on.
- Your team's clock keeps running during roof waits and disconnects.
- Before each new opponent, death penalties reset and your gear, supplies, backpack, and arena return to their state at the start of combat.
- Supplies used within one opponent's three waves stay used until you face the next opponent.

## Settings

| Setting           | What changes                                                        | Default    |
|-------------------|---------------------------------------------------------------------|------------|
| Collection time   | Time to collect mobs and supplies: **1–1,440 minutes**.             | 90 minutes |
| Starter equipment | Start with the unbreakable enchanted diamond tools described above. | On         |
| Backpack          | Enable personal or shared team storage.                             | On         |

## Commands

| Command                   | What it does                                                         |
|---------------------------|----------------------------------------------------------------------|
| `/waves`                  | Reopen wave configuration before combat.                             |
| `/backpack` or `/bp`      | Open your personal or shared team backpack, if enabled.              |
| `/tracker`                | Get a compass pointing to a remaining mob when five or fewer remain. |
| `/watch <player>`         | Watch a remaining team after yours has finished.                     |
| `/waves details <player>` | Browse that team's collected army after the round.                   |

[Back to KatzeGames](../README.md)
