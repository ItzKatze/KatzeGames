# KatzeGames

Play Minecraft challenges solo or with friends, then return to the lobby for the next round.

## Choose a gamemode

| Gamemode                             | Your goal                                                       |
|--------------------------------------|-----------------------------------------------------------------|
| [ForceBattle](forcebattle/README.md) | Complete objectives and score the most points.                  |
| [Bingo](bingo/README.md)             | Claim five objectives in a row before your opponent.            |
| [MobArmy](mobarmy/README.md)         | Collect an army and beat your opponents' waves fastest.         |
| [DeathSwap](deathswap/README.md)     | Build traps and survive until you are the last player standing. |
| [AllItems](allitems/README.md)       | Work through a collection checklist.                            |
| [Randomizer](randomizer/README.md)   | Discover which sources give you the items you need.             |
| [ItemRain](itemrain/README.md)       | Make use of random items arriving throughout the round.         |

## Join a round

1. Choose your team in the lobby, or stay unteamed to play solo.
2. Use `/spectate` if you want to sit out the next round.
3. Wait for the host to start the round and follow your gamemode's guide.

**Joining mid-round makes you a spectator.** You can play in the next round.

## Player commands

| Command                                          | What it does                                                |
|--------------------------------------------------|-------------------------------------------------------------|
| `/team create`, `/team join <id>`, `/team leave` | Create, join, or leave a team.                              |
| `/team list`                                     | Show the teams.                                             |
| `/chat team`, `/chat all`                        | Choose your chat channel.                                   |
| `/t chat <message>`                              | Send one message to your team.                              |
| `/spectate`                                      | Choose whether to play the next round; use it in the lobby. |
| `/results`                                       | Review saved rounds you played in.                          |
| `/level [player]`                                | Check your level or another player's level.                 |
| `/achievements`                                  | Browse achievements and rewards.                            |

## Settings

The host chooses the round rules. Each gamemode guide lists its own settings; these shared settings apply where supported.

| Setting                 | What changes                                                                      | Default |
|-------------------------|-----------------------------------------------------------------------------------|---------|
| No hunger               | Keep hunger full.                                                                 | Off     |
| No damage               | Prevent player damage in modes that allow it.                                     | Off     |
| Mobile crafting         | Open a crafting table with `/craft` or `/crafting`.                               | Off     |
| Hide points             | Hide points in player name tags.                                                  | On      |
| Hide objectives         | Hide objectives in player name tags.                                              | Off     |
| No duplicate objectives | Avoid repeated objectives in modes that use this rule.                            | On      |
| Simplified Objectives   | Accept allowed members of an objective group, such as any matching biome variant. | On      |
| Extra team objective    | Enable an additional shared team objective in ForceBattle.                        | On      |
| Show scoreboard         | Display the sidebar.                                                              | On      |

## Watch or take a break

- **Spectating:** fly around and right-click a player to inspect their inventory, progress, or view.
- **Spectator chat:** chat and private messages stay between spectators.
- **Lobby:** try Jump & Run, earn XP, and collect cosmetics between rounds.
- **Progress:** levels, achievements, and cosmetics carry over between rounds.
- **Music:** the [optional client mod](clientmod/README.md) adds soundtrack songs to Jump & Run.

## Credits

- **Lobby builds:** q8h (classic) and IchmagYorkleffs (balloninsel, inselmithaus).
- **ForceBattle:** based on [nicolaigrv’s original ForceBattle](https://github.com/nicolaigrv/ForceBattle).

[Server setup and development](SETUP.md)
