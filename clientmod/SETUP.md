# Client mod installation and developer tools

For the player-facing feature overview, see [README.md](README.md).

## Install the optional music mod

1. Install **Fabric Loader 0.20.1 or newer** and **Fabric API** for **Minecraft 26.3**.
2. Download `KatzeGames-Client-<version>+26.3.jar` from the release linked in the server's join message.
3. Put the JAR in your Minecraft `mods` folder and launch the Fabric profile.

The mod acts only on KatzeGames Jump & Run music; it does not change music on other servers. Players do not need it for Bingo's exported pictures.

## Export Bingo icons

Run these tasks from the repository root:

| Task                                       | Output                                                              |
|--------------------------------------------|---------------------------------------------------------------------|
| `gradlew.bat :clientmod:exportItemIcons`   | Transparent 64×64 inventory pictures rendered at GUI scale 4.       |
| `gradlew.bat :clientmod:exportMobIcons`    | Transparent 128×128 portraits rendered from Minecraft's mob models. |
| `gradlew.bat :clientmod:generateBingoPack` | Export both sets of pictures and regenerate the Bingo pack.         |

Export tasks launch hidden developer clients in isolated run directories and exit after writing the images. They require a graphics device and display session. Dedicated export properties enable these modes; normal clients retain the music features.

The local caches are ignored by Git:

- Item pictures: `bingo/src/main/item-icons/<minecraft-version>/`.
- Mob portraits: `bingo/src/main/mob-icons/<minecraft-version>/`.

Only required generated pictures in the resource pack are committed. Mob portraits appear on the sidebar; the inventory GUI uses spawn eggs.

[Full Bingo pack workflow](../bingo/SETUP.md) · [Server setup](../SETUP.md)
