# Weapons Procurement

Weapons Procurement adds a stock review and trade-planning popup for Starsector markets and storage dialogs.

## Requirements

- Starsector `0.98a`
- [LunaLib](https://fractalsoftworks.com/forum/index.php?topic=12712.0) (required)

## Main Workflow (Clean Path)

1. Open a market or storage interaction dialog.
2. Press `F8` to open the popup.
3. Queue buys/sells per item.
4. Open `Review Trades`.
5. Confirm trades.

This clean path does not require patching `starfarer_obf.jar`.

## Optional Patched-Badge Path

An optional advanced path can render ownership badges directly in vanilla cargo cells by patching `starfarer_obf.jar`.

- This is not required for normal popup usage.
- The clean/default setting leaves patched badges disabled (`wp_enable_patched_badges=false`).
- If you patch your core jar, you can enable badges in Luna settings.

Use [PACKAGING.md](PACKAGING.md) for patch/restore and validation steps.

## Source Modes

- `Local`: current market/submarkets.
- `Sector Market`: remote live stock currently for sale across sector markets.
- `Fixer's Market`: virtual stock from eligible cataloged items, with configurable multiplier.

Availability of source modes is controlled by Luna settings.

## Key Settings (LunaLib)

- `Enable market dialogue option`
- `Enable Sector Market`
- `Enable Fixer's Market`
- `Enable Fixer's Market tag inference` (default off)
- `Sector Market price multiplier`
- `Fixer's Market price multiplier`
- desired stock thresholds for small/medium/large weapons and wings

## Troubleshooting

- Popup does not open:
  - ensure you are inside an active market/storage dialog;
  - ensure source/filter settings are not hiding all rows.
- No rows visible:
  - check source mode and filters;
  - verify current market or inventory has relevant items.
- Patched badges do not show:
  - expected on clean installs;
  - requires patched core jar plus enabled Luna setting.

## Build/Validation

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```
