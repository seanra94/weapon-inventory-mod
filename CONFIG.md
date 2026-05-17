# Weapons Procurement Configuration

Most settings are exposed through LunaLib. The JSON files under `data/config` are for advanced per-item overrides and remote-market blacklists.

## Item Keys

Weapons Procurement tracks weapons and fighter LPCs as stock items. Prefer explicit item keys in new config:

- `W:<weaponId>` for weapons.
- `F:<wingId>` for fighter LPCs.

Raw ids are still accepted in compatibility paths, but prefixed keys avoid collisions between weapon ids and wing ids.

## Stock Thresholds

`data/config/weapons_procurement_stock.json` controls default desired stock and optional per-item overrides.

`desiredDefaults` sets broad fallback thresholds:

```json
{
  "desiredDefaults": {
    "smallWeapon": 16,
    "mediumWeapon": 8,
    "largeWeapon": 4,
    "fighterWing": 4
  }
}
```

`perItem` is the preferred override section:

```json
{
  "perItem": {
    "W:lightmg": {
      "desired": 24
    },
    "F:broadsword_wing": {
      "desired": 6
    },
    "W:guardian": {
      "ignored": true
    }
  }
}
```

`perWeapon` remains supported as a legacy alias. If both `perWeapon` and `perItem` define the same item, `perItem` wins.

## Remote Market Blacklist

`data/config/weapons_procurement_market_blacklist.json` blocks items from remote sources:

```json
{
  "BANNED_FROM_SECTOR_MARKET": [
    "W:guardian",
    "lightag"
  ],
  "BANNED_FROM_FIXERS_MARKET": [
    "F:broadsword_wing"
  ]
}
```

Entries can be item keys, raw ids, or display names. Display-name matching supports both weapons and fighter LPCs.

## Luna Source Settings

LunaLib owns runtime toggles and price multipliers:

- `Enable Sector Market`
- `Enable Fixer's Market`
- `Legacy Fixer catalog inference flag`
- `Sector Market price multiplier`
- `Fixer's Market price multiplier`

Sector Market uses real live stock from markets in the current save. Fixer's Market uses observed eligible stock plus a safe runtime faction catalog, and is intentionally expensive. The legacy inference flag is kept so old configs load cleanly, but it no longer changes Fixer's Market behavior.

## Rollback Debug Hook

Developer-only diagnostics are not exposed in LunaLib settings.

### Trade rollback fault hook

For a local validation run, start Starsector with the JVM system property `wp.debug.failTradeStep` set to one accepted value.

Accepted test values are:

- `after-source-removal`
- `after-player-cargo-remove`
- `after-player-cargo-add`
- `after-target-cargo-add`
- `after-credit-mutation`

Leave the property unset, empty, or `none` for normal play and public packages.

### Ship catalog diagnostic dump

For a local validation run, start Starsector with the JVM system property `wp.debug.shipCatalog`.

Accepted values are:

- `summary` or `true`: log one `WP_SHIP_CATALOG_DIAG` summary plus observed/theoretical comparison.
- `top` or `all`: also log the top theoretical ship candidates by rarity/source fit.
- comma/space/semicolon-separated hull ids such as `paragon,onslaught,diableavionics_pandemonium`: log those hulls if observed or theoretically reachable.

This diagnostic is intentionally read-only. It scans live mothballed market ships and faction-known hulls, but it does not add ships to Fixer's Market, mutate cargo, force restocks, or change prices.
