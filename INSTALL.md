# Installation

## Players

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version (launcher or multiMC-style).
2. Download [Fabric API](https://modrinth.com/project/fabric-api) for the **same** Minecraft version and drop it into your `mods` folder.
3. Download the HugoSMP Auction jar matching your Minecraft version from `dist/<your-version>/` (e.g. `dist/1.21.4/hugosmp-auctionhouse-1.21.4.jar`) and drop it into `mods`.
4. Launch the game and join HugoSMP (`hugosmp.net`).
5. Open the auction house and browse a page or two — the cache fills as you look at listings.
6. Hover any item: the tooltip now shows the lowest observed auction price.

### First-time setup tips

- If the tooltip never appears, press **J** while an auction screen is open to force a rescan.
- If the mod never recognizes the auction screen, open the config screen (**O**) and extend the
  "AH title regex" field so it matches the GUI's title (default already matches
  `auction|auktion|markt|market|bazaar|hugo`).
- If prices show as "approximate match", an exact variant has not been listed in a GUI you
  browsed yet — browse more listings or disable the fallback in settings.
- Keybinds are remappable under Options → Controls → Key Binds → HugoSMP Auction.

## Server owners

Nothing to install. The mod is client-only and reads data your server already sends to the
client when rendering auction GUIs. It performs zero network I/O of its own.

## Building from source

Requirements: Java 21 and 25 toolchains (Gradle provisions/finds them automatically),
internet access for the first build (it downloads Minecraft jars and mappings).

```powershell
.\bootstrap.ps1          # fetches Gradle 9.9.2 into tools\
.\build-and-collect.ps1  # builds all 16 versions into dist\<version>\
```

Build a single version:

```powershell
.\run-gradle.ps1 1.21.4 clean build
```

Jars land in `build\libs\` after each run and are copied to `dist\<version>\` by the
collector script.
