# Bingo server setup

## Host the required resource pack

1. Build the plugin with `gradlew.bat :loader:shadowJar`, or download the plugin and matching Bingo pack from the same release.
2. Upload `bingo/build/pack/KatzeGames-Bingo.zip` to a direct public HTTP(S) download URL.
3. Set `bingo.resource-pack.url` in the plugin's `config.yml` and restart the server.

Use the exact ZIP supplied with the plugin. Recompressing or changing it changes its hash. When updating the pack, use a new or versioned URL and install the matching plugin.

Bingo extracts its bundled pack to `plugins/KatzeGames/resource-packs/KatzeGames-Bingo.zip` and logs its SHA-1. A missing or invalid URL prevents enabling Bingo. Players and spectators must load the pack; `/start` waits until everyone is ready. The configurable `bingo.resource-pack.timeout-seconds` defaults to 120 seconds.

## Operator commands

| Command                  | What it does                                                                                     |
|--------------------------|--------------------------------------------------------------------------------------------------|
| `/timer pause`           | Pause every match.                                                                               |
| `/timer resume`          | Resume paused matches.                                                                           |
| `/timer set <seconds>`   | Set the remaining time for unfinished matches. Setting zero ends them using claimed-cell counts. |
| `/bingo draw <match-id>` | End a match as a draw. Find IDs in `/bingo matches`.                                             |
| `/reset`                 | End the round and return everyone to the lobby.                                                  |

Bingo has its own exclusion toggles, with the same categories and defaults as ForceBattle. Explicit `exclude.items`, `exclude.mobs`, `exclude.biomes`, `exclude.structures`, and `exclude.advancements` config lists are shared. Unobtainable survival objectives are always excluded.

A disconnected side gets a two-minute reconnect grace period while its match is frozen. Running out of grace forfeits the match. If both sides are absent, both the match and grace periods freeze.

Rounds are saved and restored paused after a restart. Finish or reset a round before changing the objective catalog; an incompatible save stops recovery rather than replacing player data.

## Build and verify

```text
gradlew.bat :bingo:check :core:test :loader:test
```

This builds the plugin, checks the packaged board icons, and validates translations from the final JAR. Normal builds use the committed resource-pack assets and do not need a running Minecraft client.

## Regenerate the icons and pack assets

```text
gradlew.bat :clientmod:generateBingoPack
gradlew.bat :loader:shadowJar
```

The first command launches a hidden developer Minecraft client in an isolated run directory. It renders inventory icons at GUI scale 4 as transparent 64×64 PNGs, rebuilds the objective catalog, and generates the pack assets. A graphics device and display session are required. The second command packages the ZIP and matching plugin.

The full export cache at `bingo/src/main/item-icons/<minecraft-version>/` stays local and is ignored by Git. Only the required PNGs in the resource pack and a small item-ID list are committed. CI validates the packaged assets without the export cache; local tests also compare them with the exports when available.

To regenerate pack assets from an existing export cache:

```text
gradlew.bat :bingo:generateBingoPack "-PminecraftClientJar=<path-to-minecraft-client.jar>"
gradlew.bat :loader:shadowJar
```

The catalog is generated from Minecraft assets, item exports, and the shared simplified-group definitions. Curated entries come from `objectives-base.tsv` when present, otherwise from non-generated entries in the existing catalog. Keep existing objective IDs stable. The generator checks export compatibility, copies PNGs unchanged, shares icons between objectives, and removes unused generated images.

[Back to the player guide](README.md)
