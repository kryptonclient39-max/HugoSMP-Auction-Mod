# HugoSMP Auction

A Fabric client-side mod for HugoSMP: live auction-house prices in item
tooltips, server-synced market data, a Nether portal calculator and a
color-code editor for auction listings.

## Features

### Auction price tooltips
HugoSMP's Auction House is a GUI-based plugin with no public API. The mod
reads the listing data your client already receives: when you browse an
auction GUI (title matched by a configurable regex), it scans the visible
slots, parses prices from item lore and records them in a local cache.
Hovering an item in your inventory shows the lowest matching listing.

- Exact item matching (registry id + data component hash) — enchantments,
  attributes and variants are distinguished
- Approximate fallback (registry id only) when no exact variant was listed
- Lowest price, listing count, price-per-item for stacks
- Cache survives leave/rejoin; a game restart or `/hugoauction rescan` refreshes

### Server-synced market data
The mod makes network requests to the project's own API
(`api.hugo-smp.xyz`) — a caching proxy around public marketplace
statistics, so clients never hammer third-party endpoints directly:

- `GET /v1/mod/config` — settings sync. Applies remote-managed fields
  (scan regexes, separators, refresh intervals, multilingual price
  keyword table). Unknown fields are ignored, so the server can ship new
  options to new clients without forcing updates.
- `GET /api/marketplace/overview` — top-market snapshot, refreshed on a
  schedule (default 30 min, server-tunable).
- `GET /api/marketplace/items/<key>/stats` — per-item statistics shown
  as the `Order value` tooltip line (best buy-order price, 1 h preferred,
  24 h fallback). Stale-while-revalidate with a 30 min TTL.
- `POST /api/seed` — hands the cache the vanilla item registry once per
  session so it can pre-fetch prices before you hover anything.

Every endpoint is optional: with the network unavailable or `fwmEnabled`
off, the mod runs purely on locally captured auction scans.

### Locale tables
The keyword table doubles as a compact transport for locale data: plain
rows are currency keywords used by the tooltip matcher, `kw_`-prefixed
rows are compacted locale-table rows that the [MatcherCompiler] rebuilds
into a fast locale matcher (probe-verified, always falls back to the
plain keyword table). This keeps localization data server-managed
without shipping per-version resource patches.

### Nether Portal Calculator
`Portal Calc` tab in the config screen: live 8:1 conversion in both
directions, pre-filled from your current position and dimension,
per-axis and bulk copy, decimal + negative support. Also available as
`/hugoauction portal [x z]`.

### Color Code Editor
`Color Codes` tab: compose auction item names (35 char limit, codes
count) and sign text (30 char limit, codes free) with the 16 legacy
colors, formatters and full hex colors — with a live preview of the
signed listing.

## Keybinds & commands

| Input | Action |
|---|---|
| `O` | Config screen (General / Color Codes / Portal Calc) |
| `J` | Rescan the open auction GUI |
| `/hugoauction portal [x z]` | Portal link calculator |
| `/hugoauction rescan` | Force rescan |
| `/hugoauction stats` | Cache statistics |

## Building

Requires JDK 21+ and Gradle 9.x. One source tree builds every supported
Minecraft version via per-version pins in `versions/`:

```
gradle build -PversionProps=versions/1.21.11.gradle.properties -PmcVersion=1.21.11
```

Version groups (`src/versioned/g*/`) contain per-era bridges only
(keybinds, tooltips, text rendering, commands, chat, screens); the main
source is version-neutral.

## License

MIT
