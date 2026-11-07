# Bingo

Race another player or team to own five cells in a row, column, or diagonal on a shared 5×5 board. Each cell belongs permanently to the first side to complete its objective. Every winning line contains two easy, two medium, and one hard objective.

Each pair of competitors gets an independent match. Four competitors produce two simultaneous boards. Teams share claims; unteamed players compete alone. Competitor counts must be even, with at least two competitors. Team sizes must match unless the operator enables unequal sizes.

## Required resource pack

Bingo uses a graphical vanilla sidebar with all 25 objective icons, ownership marks, and winning-line highlights. Only the remaining match time appears above the hotbar; unlimited and finished matches leave it empty. Both competitors use the same square marker in their respective colors. A blank line separates the competitors from the board. Row numbers and column letters use Minecraft's original font, centered beside their rows and columns. The sidebar has no footer text, and board rows and columns have a one-pixel gap. Competitor names and scores appear in full; the sidebar expands to fit the text. No client mod is needed. The pack is mandatory for players and spectators. Declining it, a failed download/reload, or a loading timeout disconnects the client with instructions to retry. There is no pack-free display.

1. Build with `gradlew.bat :loader:shadowJar`. This also creates `bingo/build/pack/KatzeGames-Bingo.zip`; CI and releases attach that ZIP separately.
2. Host the exact ZIP at a direct public HTTP(S) URL. Keep that URL versioned or immutable and update it when replacing the plugin/pack. Do not unzip/recompress it: the plugin checks the hash of the bundled ZIP.
3. Set `bingo.resource-pack.url` in the plugin's `config.yml` and restart the server. A missing or invalid URL prevents enabling Bingo. The plugin also extracts its ZIP to `plugins/KatzeGames/resource-packs/KatzeGames-Bingo.zip` when enabled and logs its SHA-1.
4. Enable Bingo in `/settings`. All online clients receive the pack. `/start` waits until everyone has successfully loaded it.

The configurable `bingo.resource-pack.timeout-seconds` defaults to 120 (15–600). Reconnecting participants must load the pack before returning to play; this does not extend their match's reconnect grace.

The pack targets Minecraft 26.3 (resource format 97.1). The inventory GUI uses ordinary Minecraft items, including spawn eggs for available mob objectives, without custom item models or textures. Available cells keep their objective icons; claimed cells become blue/orange stained glass panes with explicit status and ownership labels. GUI item names contain only the objective; status and ownership appear in the lore. Winning cells also glint. The clock provides a labeled match overview, both sides' cell counts, and the unclaimed count. Internal match IDs are only shown in the match list. Flat HUD icons reference Minecraft's original PNG resources directly at 16 GUI pixels, with separate ownership frames. Three-dimensional and special item icons use transparent PNGs exported by Minecraft's actual inventory renderer, at GUI scale 4 (64×64 pixels), with its native models and lighting. These full-resolution PNGs are retained in the pack rather than reduced to 16×16; the font displays them at the normal 16 GUI-pixel inventory size. No software approximation or per-item drawing is used. Biome and structure objectives use the same vanilla inventory items as the GUI; no custom objective drawings are used. Players only need the resource pack; the Fabric client is used during asset generation. The pack does not override vanilla fonts or item definitions. The HUD uses vanilla's right-side scoreboard anchor.

## Play and settings

- `/bingo`: open your board. Every cell shows its full objective name, difficulty, exact target, progress for collection objectives, and owner/contributor.
- Hover a cell to see its full objective name and details.
- `/bingo overlay`: hide/show the graphical board. The pack remains required.
- `/recipe <item>`: look up crafting and processing recipes, with ingredient links and recipe pagination.
- `/bingo matches`: spectators and finished competitors can select another match to watch.
- `/bingo draw <match-id>`: operator command to end a broken match as a draw. Match IDs appear in the match list.
- `/results`: browse saved matches and their complete final boards, winners, reasons, and contributions.

Right-click Bingo in `/settings` to change its 30/45/60-minute or unlimited time limit, allowed objective categories, and unequal-team rule. The default is 45 minutes with all five categories. The exclusions submenu offers the same independent toggles and defaults as ForceBattle: music discs, banner patterns, armor templates, pottery sherds, ores, End content, trial loot, oxidation-only copper, heads, sniffer content, unrealistic objectives, and structure maps. Both modes share the explicit `exclude.items`, `exclude.mobs`, `exclude.biomes`, `exclude.structures`, and `exclude.advancements` config lists. Unobtainable survival objectives are always excluded. Bingo must be the only active challenge. Settings and teams are locked during the round. Some category combinations cannot meet the board's difficulty and variety constraints; `/start` explains these instead of generating an unbalanced board.

The generated catalog contains 2,051 entries: every Minecraft inventory item, spawn-egg mob, biome, structure, and visible advancement, plus ForceBattle simplified groups and retained legacy IDs. The shared Simplified Objectives toggle in `/settings` → bottom-right arrow → Global settings groups mobs, biomes and structures exactly as in ForceBattle; items remain individual materials. Only allowed members of a group can complete it. Overlapping targets cannot share a board. Collection objectives always require exactly one item; item-only boards are supported while preserving the difficulty balance. Items count in one teammate's main inventory or offhand, without consumption, armor, nested storage, or combining inventories. Selected advancement criteria are cleared at round start so old unlocks cannot count. All 25 cells are active immediately.

A completed line wins immediately. If both sides have no possible line, the match ends as a draw. When time expires, the side with more completed objectives wins; equal counts are a draw. If a side has no ready online members, its match freezes for a two-minute reconnect grace period. Expiry forfeits to the opponent; if both sides are absent, grace freezes too. Other matches continue. `/timer pause` freezes every match, and `/timer set <seconds>` changes every unfinished match's remaining limit.

Finished competitors spectate until reset. Once all matches finish, the round timer pauses. `/reset` archives unfinished matches as aborted and returns everyone to the lobby. Session saves restore exact boards, rosters, ownership, and remaining clocks, paused until `/timer resume`. Progress uses the existing five-second dirty-save interval, so a crash may lose the most recent unsaved changes. Failed result writes are queued durably and retried across resets/restarts. An incompatible or corrupted session blocks logins and automatic saves to preserve player inventories and round data: repair the cause and restart, or discard the round with `/reset` from the server console.

## Development and verification

`gradlew.bat :bingo:test :core:test :loader:shadowJar` checks board generation, match outcomes, save/restore, bilingual texts, pack metadata, and all icon mappings. Generated assets are checked in, so a normal build does not need Minecraft downloads or image tooling.

Native item exports are checked in under `src/main/item-icons/26.3/`, so ordinary builds require no GPU or running Minecraft client. To regenerate every registered item icon and rebuild the pack's fonts and translations, run:

```text
gradlew.bat :clientmod:generateBingoPack
```

This launches a hidden developer Minecraft client, creates a fresh flat world in `bingo/build/item-export-client`, renders the entire item registry in batches, reads transparent PNGs from the GPU, writes a versioned manifest, and exits automatically. It uses a separate run directory and does not use the player's normal Minecraft worlds or resource packs. A graphics device and display session are required for this developer task. `:clientmod:exportItemIcons` runs only the export step.

To rebuild the pack from existing exports after changing objectives:

```text
gradlew.bat :bingo:generateBingoPack "-PminecraftClientJar=<path-to-26.3-minecraft-client.jar>"
```

The pack generator rebuilds `objectives.tsv` from Minecraft assets, the native item manifest, and the shared simplified-group definitions. Curated entries and legacy IDs come from `objectives-base.tsv` when present, otherwise from the existing catalog entries without generated translation keys. Generated entries are rebuilt automatically. Generated objective names use Minecraft translation keys for localized item, entity, biome and advancement names. HUD icons and their spacing use separate supplementary private-use glyph ranges so large catalogs cannot overlap. Item objectives derive their display from their Minecraft material; mob objectives derive a spawn egg; advancements read Minecraft's advancement icon. The exporter iterates Minecraft's full item registry, including special models, so adding an existing item needs no renderer implementation. The pack generator validates the export's Minecraft version, renderer, manifest membership, PNG dimensions, and alpha channel. A missing/incompatible native export fails the build with regeneration instructions. It copies the required exported PNGs byte-for-byte into the pack, names them after their Minecraft items so objectives share icons, removes unused generated PNGs, and writes icon-material metadata and font assets. Visual metadata is separate from objective identities, so a picture update does not invalidate saved rounds. Vanilla texture artwork belongs to Mojang; no third-party font assets are used.

Before deploying, verify in Minecraft: accept/reject/fail the pack; reconnect during a live match; check small/widescreen windows and GUI scales; inspect English/German objective menus; claim from both sides; finish one match while another continues; pause/reset/restart and inspect `/results`. Automated checks do not replace this client-side layout check.

[Back to KatzeGames](../README.md)
