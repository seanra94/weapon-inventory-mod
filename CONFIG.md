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
- `Enable Fixer's Market tag inference`
- `Sector Market price multiplier`
- `Fixer's Market price multiplier`

Sector Market uses real live stock from markets in the current save. Fixer's Market uses observed eligible stock plus optional inference, and is intentionally expensive.

## Rollback Debug Hook

`DEV ONLY: force trade rollback failure` is for local validation of trade rollback. Keep it set to `none` for normal play and public packages.

Accepted test values are:

- `after-source-removal`
- `after-player-cargo-remove`
- `after-player-cargo-add`
- `after-target-cargo-add`
- `after-credit-mutation`

The release validation script fails if this setting's default is not `none`.
