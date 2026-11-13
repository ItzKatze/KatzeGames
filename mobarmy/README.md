# MobArmy

**Build an army. Send it against your friends. Beat theirs faster.**

Every mob you defeat can become part of a wave your opponents have to fight. Spend the collection phase hunting mobs and gearing up,
then put your army to the test in a race against the clock. Play solo or team up with friends.

## How a round works

### 1. Hunt and gear up

You have **90 minutes by default** to collect equipment, gather supplies, and kill mobs for your army.
Almost every mob counts—even animals, golems, and bosses. The Ender Dragon is the exception.

Mobs keep their equipment and variants when they return at full health. Find a drowned with a trident? Your opponents will have to deal with it too.

- Start with a **diamond axe, pickaxe, and shovel**, each with Efficiency I, when starter equipment is enabled.
- Store supplies in a **27-slot backpack** with `/bp`. Teammates share the same backpack.
- Stay alive: deaths during collection follow normal survival rules, so you can drop your gear. Your starter tools and collected mobs are safe.

### 2. Build your three waves

When collection ends, the **Waves** menu opens. Choose a wave to start adding mobs:

| Control | What it does |
| --- | --- |
| Left-click a mob | Add one to this wave. |
| Right-click a mob | Remove one from this wave. |
| Red back button | Return to your three-wave overview. |
| Ready button on the right | Mark your team ready—or undo it. |

Each collected mob can go into **one wave only**. You can leave mobs unused, and teammates can edit together.
Each mob type has one slot. Hover over it to see your assigned and available counts.

Take your time to organize your waves and supplies. Combat starts when **one player from every team presses Ready**.
Changing your waves makes your team unready again. Use `/waves` if you close the menu.

### 3. Beat the opposing armies

Every team fights in a separate world with its own copy of the village arena, under clear daytime skies.
Clear all three waves from one opponent, then face the next.
With three teams, for example, each team fights **six waves** in total.

Mobs arrive at **five per second**, and the next wave begins once every mob in the current wave is defeated.
Aquatic and lava mobs can spawn on dry ground too, with protection against drying out or losing air.
Mobs can fight one another, but iron golems target players.

**The team with the fastest total combat time wins.** Wait until everyone finishes to see the winners,
then click **[Details]** in chat to browse each team’s collected army.

Finished your waves? You can watch the remaining teams with `/watch <player>`. Spectators stay hidden from teams still fighting.
When five or fewer mobs remain in your current wave, use `/tracker` for a compass pointing to the nearest one.

## What happens if you die?

During combat, you **keep your items and XP**, but must wait on the arena roof before returning.
Your first death costs **30 seconds**, followed by **60 seconds, 90 seconds, 120 seconds**, and another 30 seconds for each further death.
Your team’s clock keeps running while you wait, including when players disconnect.

Death penalties reset when you face the next opponent. Your gear, supplies, backpack, and arena also return to their state at the start of combat.
Within an opponent’s three waves, used supplies and equipment wear carry over—so make them last.

## Arena rules

- **No PvP:** focus on surviving the army.
- **Build your defenses:** place blocks and break player-placed blocks. The arena itself is protected.
- **Bring your supplies:** arena mobs drop no loot or XP.
- **Tied times share the win.**

## Start a game with friends

1. An operator enables **MobArmy** in `/settings` and adjusts its settings with a right-click.
2. Choose teams with `/team`, or stay unteamed to play solo. You need at least two teams or solo players.
3. Run `/start` and get hunting!

Collection time is adjustable. Starter equipment and backpacks are enabled by default and can be switched off.
Want a twist? Add **Randomizer** or **ItemRain** for the collection phase; both stop when wave configuration begins.

## Handy commands

| Command | What it does |
| --- | --- |
| `/waves` | Reopen your wave configuration menu. |
| `/tracker` | Get a mob-tracking compass when five or fewer mobs remain in your wave. |
| `/watch <player>` | Watch a remaining team after yours has finished. |
| `/backpack` or `/bp` | Open your personal or shared team backpack. |
| `/timer pause` / `/timer resume` | Pause or resume the round, including combat timers and roof waits (operator). |
| `/waves forfeit <player>` | Mark that player’s team as having forfeited during configuration or combat (operator). |
| `/reset` | End the round and return to the lobby (operator). |

Rounds are saved across server restarts and return paused. An operator can use `/timer resume` to continue.
Results remain available until the next reset.

[Back to KatzeGames](../README.md)
